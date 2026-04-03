#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

DB_URL="${OMS_DB_URL:-jdbc:postgresql://localhost:5432/oms}"
DB_USER="${OMS_DB_USER:-$(whoami)}"
DB_PASSWORD="${OMS_DB_PASSWORD:-}"

# Parse jdbc:postgresql://host:port/database
if [[ "$DB_URL" =~ ^jdbc:postgresql://([^:/]+)(:([0-9]+))?/([^?]+) ]]; then
  DB_HOST="${BASH_REMATCH[1]}"
  DB_PORT="${BASH_REMATCH[3]:-5432}"
  DB_NAME="${BASH_REMATCH[4]}"
else
  echo "Error: Unsupported OMS_DB_URL format: $DB_URL"
  echo "Expected format: jdbc:postgresql://<host>:<port>/<database>"
  exit 1
fi

PSQL_BIN="$(command -v psql || true)"
if [[ -z "$PSQL_BIN" && -x "/opt/homebrew/opt/postgresql@16/bin/psql" ]]; then
  PSQL_BIN="/opt/homebrew/opt/postgresql@16/bin/psql"
fi

if [[ -z "$PSQL_BIN" ]]; then
  echo "Error: psql not found in PATH."
  echo "Install PostgreSQL or add psql to PATH."
  exit 1
fi

echo "Connecting to PostgreSQL..."
echo "host=$DB_HOST port=$DB_PORT db=$DB_NAME user=$DB_USER"

if [[ -n "$DB_PASSWORD" ]]; then
  exec env PGPASSWORD="$DB_PASSWORD" "$PSQL_BIN" -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME"
else
  exec "$PSQL_BIN" -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME"
fi
