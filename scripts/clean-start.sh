#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

# Build app classes and tests quickly (skip tests for startup helper).
mvn -q -DskipTests clean package

# Ensure runtime dependency jars are present under target/dependency.
mvn -q dependency:copy-dependencies

export OMS_DB_CLEAN_START=true
export OMS_DB_URL="${OMS_DB_URL:-jdbc:postgresql://localhost:5432/oms}"
export OMS_DB_USER="${OMS_DB_USER:-$(whoami)}"
export OMS_DB_PASSWORD="${OMS_DB_PASSWORD:-postgres}"

CP="target/classes"
while IFS= read -r -d '' jar; do
  CP+="${CP:+:}$jar"
done < <(find target/dependency -type f -name "*.jar" -print0 | sort -z)

echo "Starting OMS with clean start..."
echo "OMS_DB_URL=$OMS_DB_URL"
echo "OMS_DB_USER=$OMS_DB_USER"

exec java -cp "$CP" com.iiit.oms.OmsApplication
