# =============================================================================
# MF-OMS Project Setup Script (Windows PowerShell)
# Team 29 — Mutual Fund Order Management System
# Run this once on a fresh machine to install everything and start the project.
# =============================================================================

param(
    [switch]$SkipDockerInstall,
    [switch]$SkipNodeInstall,
    [switch]$SkipJavaInstall,
    [switch]$CleanStart = $true
)

$ErrorActionPreference = "Stop"
$ROOT = Split-Path -Parent $MyInvocation.MyCommand.Definition

Write-Host ""
Write-Host "======================================================" -ForegroundColor Cyan
Write-Host "  MF-OMS Project Setup — Team 29" -ForegroundColor Cyan
Write-Host "======================================================" -ForegroundColor Cyan
Write-Host ""

# ─── STEP 1: Check prerequisites ──────────────────────────────────────────────
Write-Host "[1/7] Checking prerequisites..." -ForegroundColor Yellow

function Check-Command($cmd) {
    return $null -ne (Get-Command $cmd -ErrorAction SilentlyContinue)
}

# Java 11+
if (-not (Check-Command "java")) {
    Write-Host "  ERROR: Java not found. Please install Java 11+ from https://adoptium.net" -ForegroundColor Red
    Write-Host "         Then re-run this script." -ForegroundColor Red
    exit 1
}
$javaVersion = (java -version 2>&1)[0] -replace '[^0-9.]',''
Write-Host "  Java: OK ($javaVersion)" -ForegroundColor Green

# Maven
if (-not (Check-Command "mvn")) {
    Write-Host "  ERROR: Maven not found. Please install Maven from https://maven.apache.org/download.cgi" -ForegroundColor Red
    Write-Host "         Add it to PATH, then re-run this script." -ForegroundColor Red
    exit 1
}
$mvnVersion = (mvn -version 2>&1)[0]
Write-Host "  Maven: OK ($mvnVersion)" -ForegroundColor Green

# Node.js
if (-not (Check-Command "node")) {
    Write-Host "  ERROR: Node.js not found. Please install from https://nodejs.org (LTS version)" -ForegroundColor Red
    exit 1
}
$nodeVersion = node --version
Write-Host "  Node.js: OK ($nodeVersion)" -ForegroundColor Green

# npm
if (-not (Check-Command "npm")) {
    Write-Host "  ERROR: npm not found. It should come with Node.js." -ForegroundColor Red
    exit 1
}
Write-Host "  npm: OK ($(npm --version))" -ForegroundColor Green

# Docker
if (-not (Check-Command "docker")) {
    Write-Host "  ERROR: Docker not found." -ForegroundColor Red
    Write-Host "         Install Docker Desktop from https://www.docker.com/products/docker-desktop/" -ForegroundColor Red
    Write-Host "         After install, start Docker Desktop, then re-run this script." -ForegroundColor Red
    exit 1
}
Write-Host "  Docker: found, checking if engine is running..." -ForegroundColor Yellow
$maxAttempts = 15
$attempt = 0
while ($attempt -lt $maxAttempts) {
    $dockerPs = docker ps 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  Docker Engine: OK" -ForegroundColor Green
        break
    }
    $attempt++
    Write-Host "  Waiting for Docker Engine to start... ($attempt/$maxAttempts)" -ForegroundColor Yellow
    Start-Sleep 5
}
if ($attempt -eq $maxAttempts) {
    Write-Host "  ERROR: Docker Engine did not start. Open Docker Desktop and wait for it to be ready." -ForegroundColor Red
    exit 1
}

Write-Host ""

# ─── STEP 2: Start infrastructure (Docker Compose) ────────────────────────────
Write-Host "[2/7] Starting infrastructure (PostgreSQL + Redis + Kafka)..." -ForegroundColor Yellow

Set-Location $ROOT

# Stop any existing containers first
docker compose down 2>&1 | Out-Null

# Start all services
docker compose up -d 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "  ERROR: docker compose up failed." -ForegroundColor Red
    exit 1
}

# Wait for PostgreSQL to be ready (healthcheck)
Write-Host "  Waiting for PostgreSQL to be healthy..." -ForegroundColor Yellow
$attempt = 0
$maxAttempts = 30
while ($attempt -lt $maxAttempts) {
    $health = docker inspect --format="{{.State.Health.Status}}" project3-postgres-1 2>&1
    if ($health -eq "healthy") {
        Write-Host "  PostgreSQL: healthy" -ForegroundColor Green
        break
    }
    $attempt++
    Write-Host "  Still waiting for PostgreSQL... ($attempt/$maxAttempts)" -ForegroundColor Yellow
    Start-Sleep 3
}
if ($attempt -eq $maxAttempts) {
    Write-Host "  WARNING: PostgreSQL health check timed out. Will try to continue..." -ForegroundColor Yellow
}

Write-Host "  Infrastructure: OK" -ForegroundColor Green
Write-Host ""

# ─── STEP 3: Build backend ─────────────────────────────────────────────────────
Write-Host "[3/7] Building backend (Maven compile)..." -ForegroundColor Yellow
Set-Location $ROOT
mvn compile -q
if ($LASTEXITCODE -ne 0) {
    Write-Host "  ERROR: Maven compile failed. Check the output above." -ForegroundColor Red
    exit 1
}
Write-Host "  Backend compiled: OK" -ForegroundColor Green
Write-Host ""

# ─── STEP 4: Install frontend dependencies ────────────────────────────────────
Write-Host "[4/7] Installing admin frontend dependencies (npm install)..." -ForegroundColor Yellow
Set-Location "$ROOT\frontend"
npm install --silent
if ($LASTEXITCODE -ne 0) {
    Write-Host "  ERROR: npm install failed in frontend/" -ForegroundColor Red
    exit 1
}
Write-Host "  Admin frontend deps: OK" -ForegroundColor Green

Write-Host "        Installing user/advisor frontend dependencies..." -ForegroundColor Yellow
Set-Location "$ROOT\user-frontend"
npm install --silent
if ($LASTEXITCODE -ne 0) {
    Write-Host "  ERROR: npm install failed in user-frontend/" -ForegroundColor Red
    exit 1
}
Write-Host "  User frontend deps: OK" -ForegroundColor Green
Write-Host ""

# ─── STEP 5: Start backend ─────────────────────────────────────────────────────
Write-Host "[5/7] Starting Java backend (port 8080)..." -ForegroundColor Yellow
Set-Location $ROOT

$cleanStartVal = if ($CleanStart) { "true" } else { "false" }

$backendCmd = @"
cd "$ROOT"
`$env:OMS_DB_URL="jdbc:postgresql://localhost:5432/oms"
`$env:OMS_DB_USER="oms_user"
`$env:OMS_DB_PASSWORD="oms_pass"
`$env:OMS_DB_CLEAN_START="$cleanStartVal"
`$env:OMS_REDIS_HOST="localhost"
`$env:OMS_REDIS_PORT="6379"
`$env:OMS_KAFKA_BOOTSTRAP_SERVERS="localhost:9092"
mvn exec:java '-Dexec.mainClass=com.iiit.oms.OmsApplication' 2>&1 | Tee-Object -FilePath "$ROOT\oms-server.log"
"@

Start-Process powershell -ArgumentList "-NoProfile", "-Command", $backendCmd -WindowStyle Minimized
Write-Host "  Backend starting... (log: $ROOT\oms-server.log)" -ForegroundColor Yellow

# Wait for backend to be ready
Write-Host "  Waiting for backend to accept connections..." -ForegroundColor Yellow
$attempt = 0
$maxAttempts = 24
while ($attempt -lt $maxAttempts) {
    try {
        $r = Invoke-WebRequest -Uri "http://localhost:8080/orders" -TimeoutSec 2 -ErrorAction SilentlyContinue
        if ($r.StatusCode -lt 500) {
            Write-Host "  Backend: ready on http://localhost:8080" -ForegroundColor Green
            break
        }
    } catch {}
    $attempt++
    Write-Host "  Still waiting for backend... ($attempt/$maxAttempts)" -ForegroundColor Yellow
    Start-Sleep 5
}
if ($attempt -eq $maxAttempts) {
    Write-Host "  WARNING: Backend did not respond in time. Check oms-server.log" -ForegroundColor Yellow
}
Write-Host ""

# ─── STEP 6: Start frontends ──────────────────────────────────────────────────
Write-Host "[6/7] Starting admin frontend (port 5173)..." -ForegroundColor Yellow
$adminCmd = "cd `"$ROOT\frontend`"; npm run dev"
Start-Process powershell -ArgumentList "-NoProfile", "-Command", $adminCmd -WindowStyle Minimized
Write-Host "  Admin frontend starting on http://localhost:5173" -ForegroundColor Yellow

Write-Host "        Starting user/advisor frontend (port 5174)..." -ForegroundColor Yellow
$userCmd = "cd `"$ROOT\user-frontend`"; npm run dev"
Start-Process powershell -ArgumentList "-NoProfile", "-Command", $userCmd -WindowStyle Minimized
Write-Host "  User frontend starting on http://localhost:5174" -ForegroundColor Yellow

Start-Sleep 6
Write-Host ""

# ─── STEP 7: Summary ──────────────────────────────────────────────────────────
Write-Host "[7/7] Setup complete!" -ForegroundColor Green
Write-Host ""
Write-Host "======================================================" -ForegroundColor Cyan
Write-Host "  Services running:" -ForegroundColor Cyan
Write-Host "  - Backend API   : http://localhost:8080" -ForegroundColor White
Write-Host "  - Admin UI      : http://localhost:5173" -ForegroundColor White
Write-Host "  - User/Advisor  : http://localhost:5174" -ForegroundColor White
Write-Host "  - PostgreSQL    : localhost:5432  (db=oms, user=oms_user)" -ForegroundColor White
Write-Host "  - Redis         : localhost:6379" -ForegroundColor White
Write-Host "  - Kafka         : localhost:9092" -ForegroundColor White
Write-Host "" -ForegroundColor Cyan
Write-Host "  Test credentials (User frontend):" -ForegroundColor Cyan
Write-Host "  - Investor : john.miller / invest123  (account ACCT00001)" -ForegroundColor White
Write-Host "  - Investor : emma.johnson / invest123 (account ACCT00002)" -ForegroundColor White
Write-Host "  - Advisor  : advisor1 / advise123     (ADV001)" -ForegroundColor White
Write-Host "  - Advisor  : advisor2 / advise123     (ADV002)" -ForegroundColor White
Write-Host ""
Write-Host "  Admin frontend does not require login." -ForegroundColor White
Write-Host ""
Write-Host "  Backend log: $ROOT\oms-server.log" -ForegroundColor Gray
Write-Host "======================================================" -ForegroundColor Cyan
Write-Host ""
