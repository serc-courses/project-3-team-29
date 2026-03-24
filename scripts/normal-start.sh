#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

# Build app classes and prepare runtime dependencies.
mvn -q -DskipTests clean package
mvn -q dependency:copy-dependencies

# Normal start: do NOT wipe tables/data.
export OMS_DB_CLEAN_START=false
export OMS_DB_URL="${OMS_DB_URL:-jdbc:postgresql://localhost:5432/oms}"
export OMS_DB_USER="${OMS_DB_USER:-$(whoami)}"
export OMS_DB_PASSWORD="${OMS_DB_PASSWORD:-postgres}"

CP="target/classes"
while IFS= read -r -d '' jar; do
  CP+="${CP:+:}$jar"
done < <(find target/dependency -type f -name "*.jar" -print0 | sort -z)

echo "Starting OMS with normal restart (data preserved)..."
echo "OMS_DB_URL=$OMS_DB_URL"
echo "OMS_DB_USER=$OMS_DB_USER"

echo "OMS_DB_CLEAN_START=$OMS_DB_CLEAN_START"
exec java -cp "$CP" com.iiit.oms.OmsApplication
