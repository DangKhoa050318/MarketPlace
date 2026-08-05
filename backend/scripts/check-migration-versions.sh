#!/usr/bin/env bash
#
# Flyway migration version guard.
#
# Fails the build when a migration:
#   * does not use timestamp versioning  V<yyyyMMddHHmmss>__<description>.sql
#     (i.e. any old-style sequential Vn such as V1, V17), or
#   * shares a version with another migration (duplicate version).
#
# Run locally:  bash backend/scripts/check-migration-versions.sh
# Run in CI:    see .github/workflows/backend-ci.yml
#
set -euo pipefail

MIGRATION_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/src/main/resources/db/migration"

if [[ ! -d "$MIGRATION_DIR" ]]; then
  echo "❌ Migration directory not found: $MIGRATION_DIR"
  exit 1
fi

shopt -s nullglob
files=("$MIGRATION_DIR"/*.sql)
if [[ ${#files[@]} -eq 0 ]]; then
  echo "❌ No migration files found in $MIGRATION_DIR"
  exit 1
fi

fail=0
declare -A seen

for f in "${files[@]}"; do
  base="$(basename "$f")"

  # Must be V + exactly 14 digits (yyyyMMddHHmmss) + __ + description + .sql
  if [[ ! "$base" =~ ^V[0-9]{14}__.+\.sql$ ]]; then
    echo "❌ Not timestamp-versioned (expected V<yyyyMMddHHmmss>__desc.sql): $base"
    fail=1
    continue
  fi

  ver="${base%%__*}"   # -> V<digits>
  ver="${ver#V}"       # -> <digits>

  if [[ -n "${seen[$ver]:-}" ]]; then
    echo "❌ Duplicate version $ver:"
    echo "     - ${seen[$ver]}"
    echo "     - $base"
    fail=1
  else
    seen[$ver]="$base"
  fi
done

if [[ "$fail" -ne 0 ]]; then
  echo ""
  echo "Migration version check FAILED."
  echo "Name new migrations with a creation timestamp: V<yyyyMMddHHmmss>__description.sql"
  exit 1
fi

echo "✅ Migration versions OK: ${#files[@]} files, all timestamp-format, no duplicates."
