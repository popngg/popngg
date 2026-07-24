#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "usage: $0 --legacy-db NAME --target-db NAME --session-id ID" >&2
  exit 64
}

legacy_db=
target_db=
session_id=
while (($#)); do
  case "$1" in
    --legacy-db) legacy_db=${2:-}; shift 2 ;;
    --target-db) target_db=${2:-}; shift 2 ;;
    --session-id) session_id=${2:-}; shift 2 ;;
    *) usage ;;
  esac
done

[[ "$legacy_db" =~ ^[A-Za-z][A-Za-z0-9_]{0,63}$ ]] || usage
[[ "$target_db" =~ ^[A-Za-z][A-Za-z0-9_]{0,63}$ ]] || usage
[[ "$legacy_db" != "$target_db" ]] || usage
[[ "$session_id" =~ ^[A-Za-z0-9._-]{1,64}$ ]] || usage

repo_root=$(git -C "$(dirname "$0")" rev-parse --show-toplevel)
mysql_args=(
  --host="${MYSQL_HOST:-127.0.0.1}"
  --port="${MYSQL_PORT:-3306}"
  --user="${MYSQL_USER:-root}"
  --protocol=tcp
  --batch
  --skip-column-names
)

mysql_cli() {
  mysql "${mysql_args[@]}" "$@"
}

schema_count=$(mysql_cli -e \
  "SELECT COUNT(*) FROM information_schema.schemata
   WHERE schema_name IN ('$legacy_db', '$target_db')")
[[ "$schema_count" = 2 ]] || {
  echo "error: legacy and Flyway-migrated target databases are required" >&2
  exit 66
}

temporary_dir=$(mktemp -d)
trap 'rm -rf "$temporary_dir"' EXIT
sed -e "s/__LEGACY_DB__/$legacy_db/g" \
    -e "s/__TARGET_DB__/$target_db/g" \
    -e "s/__SESSION_ID__/$session_id/g" \
    "$repo_root/migration/sql/01_transform.sql" > "$temporary_dir/transform.sql"

echo "migration session=$session_id status=started"
if ! mysql_cli < "$temporary_dir/transform.sql"; then
  mysql_cli "$target_db" -e \
    "UPDATE migration_sessions SET status='FAILED', finished_at=CURRENT_TIMESTAMP
     WHERE session_id='$session_id' AND status='RUNNING'" || true
  echo "migration session=$session_id status=FAILED" >&2
  exit 2
fi
summary=$(mysql_cli "$target_db" -e \
  "SELECT CONCAT('status=', status, ' failures=', failure_count)
     FROM migration_sessions WHERE session_id = '$session_id'")
echo "migration session=$session_id $summary"
