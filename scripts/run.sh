#!/usr/bin/env bash
# Starts OMS using Docker Compose (including app_1 + app_2 replicas)
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

echo ""
echo "╔══════════════════════════════════════════════════════╗"
echo "║      MF-OMS — Docker Compose Startup                ║"
echo "╠══════════════════════════════════════════════════════╣"
echo "║  Nginx/API         →  http://localhost              ║"
echo "║  Backend replicas  →  app_1 + app_2                 ║"
echo "║  Postgres/Redis/Kafka via Docker Compose            ║"
echo "╚══════════════════════════════════════════════════════╝"
echo ""

if ! command -v docker >/dev/null 2>&1; then
    echo "[error] Docker CLI not found. Install Docker first."
    exit 1
fi

echo "[run] Building and starting Docker services..."
docker compose up -d --build

echo ""
echo "[run] Current container status:"
docker compose ps

missing=""
for svc in app_1 app_2 nginx postgres redis kafka mongo; do
    if ! docker compose ps --services --filter status=running | grep -qx "$svc"; then
        missing="$missing $svc"
    fi
done

if [ -n "$missing" ]; then
    echo ""
    echo "[warn] Some services are not running:$missing"
    echo "[warn] Run 'docker compose logs --tail=200' to inspect failures."
    exit 1
fi

echo ""
echo "[run] All required containers are running."
echo "[run] Access points:"
echo "  - API via Nginx : http://localhost"
echo "  - Admin UI      : http://localhost:5173 (start separately if needed)"
echo "  - User UI       : http://localhost:5174 (start separately if needed)"
echo ""
echo "[run] Tip: follow logs with: docker compose logs -f app_1 app_2 nginx"
