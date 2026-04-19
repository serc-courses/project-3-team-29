#!/usr/bin/env bash
# Starts backend + admin frontend + user/advisor frontend
set -e

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo ""
echo "╔══════════════════════════════════════════════════════╗"
echo "║          MF-OMS — Starting all services              ║"
echo "╠══════════════════════════════════════════════════════╣"
echo "║  Backend          →  http://localhost:8080           ║"
echo "║  Admin UI         →  http://localhost:5173           ║"
echo "║  Investor/Advisor →  http://localhost:5174           ║"
echo "╚══════════════════════════════════════════════════════╝"
echo ""

# Install frontend deps if needed
if [ ! -d "$ROOT/frontend/node_modules" ]; then
    echo "[setup] Installing admin frontend dependencies..."
    (cd "$ROOT/frontend" && npm install --silent)
fi

if [ ! -d "$ROOT/user-frontend/node_modules" ]; then
    echo "[setup] Installing user-frontend dependencies..."
    (cd "$ROOT/user-frontend" && npm install --silent)
fi

# Start admin frontend
(cd "$ROOT/frontend" && npm run dev 2>&1 | sed 's/^/[admin]   /' ) &
ADMIN_PID=$!

# Start user+advisor frontend
(cd "$ROOT/user-frontend" && npm run dev 2>&1 | sed 's/^/[user]    /') &
USER_PID=$!

# Start backend (foreground so its logs are visible)
echo "[backend] Compiling and starting Java backend..."
(cd "$ROOT" && mvn -q compile exec:java -Dexec.mainClass="com.iiit.oms.OmsApplication" 2>&1 | sed 's/^/[backend] /') &
BACKEND_PID=$!

echo "[run] All services started. Press Ctrl+C to stop."
echo "[run] Backend PID=$BACKEND_PID  Admin PID=$ADMIN_PID  User PID=$USER_PID"

cleanup() {
    echo ""
    echo "[run] Stopping all services..."
    kill "$BACKEND_PID" "$ADMIN_PID" "$USER_PID" 2>/dev/null || true
    wait "$BACKEND_PID" "$ADMIN_PID" "$USER_PID" 2>/dev/null || true
    echo "[run] Done."
    exit 0
}

trap cleanup SIGINT SIGTERM

wait
