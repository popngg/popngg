#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "usage: $0 --dump ABSOLUTE_PATH --legacy-db NAME --target-db NAME [--reset]" >&2
  exit 64
}

dump_path=
legacy_db=
target_db=
reset=false

while (($#)); do
  case "$1" in
    --dump) dump_path=${2:-}; shift 2 ;;
    --legacy-db) legacy_db=${2:-}; shift 2 ;;
    --target-db) target_db=${2:-}; shift 2 ;;
    --reset) reset=true; shift ;;
    *) usage ;;
  esac
done

[[ -n "$dump_path" && -n "$legacy_db" && -n "$target_db" ]] || usage
[[ "$dump_path" = /* && -f "$dump_path" ]] || {
  echo "error: --dump must be an existing absolute file" >&2
  exit 66
}
[[ "$legacy_db" =~ ^[A-Za-z][A-Za-z0-9_]{0,63}$ ]] || {
  echo "error: invalid legacy database name" >&2
  exit 64
}
[[ "$target_db" =~ ^[A-Za-z][A-Za-z0-9_]{0,63}$ && "$target_db" != "$legacy_db" ]] || {
  echo "error: invalid or duplicate target database name" >&2
  exit 64
}

repo_root=$(git -C "$(dirname "$0")" rev-parse --show-toplevel)
dump_real=$(cd "$(dirname "$dump_path")" && pwd -P)/$(basename "$dump_path")
case "$dump_real" in
  "$repo_root"/*)
    echo "error: dump must remain outside the Git worktree" >&2
    exit 65
    ;;
esac

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

existing=$(mysql_cli -e \
  "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name IN ('$legacy_db','$target_db')")
if [[ "$reset" == true ]]; then
  mysql_cli -e "DROP DATABASE IF EXISTS \`$legacy_db\`; DROP DATABASE IF EXISTS \`$target_db\`;"
elif [[ "$existing" != 0 ]]; then
  echo "error: rehearsal databases already exist; choose new names or pass --reset" >&2
  exit 73
fi

cleanup_dir=$(mktemp -d)
trap 'rm -rf "$cleanup_dir"' EXIT

render_sql() {
  local source_file=$1
  local output_file=$2
  sed \
    -e "s/__LEGACY_DB__/$legacy_db/g" \
    -e "s/__TARGET_DB__/$target_db/g" \
    "$source_file" > "$output_file"
}

echo "session=restore status=started"
mysql_cli -e "CREATE DATABASE \`$legacy_db\` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
mysql_cli "$legacy_db" < "$dump_real"
echo "session=restore status=completed"

render_sql "$repo_root/migration/sql/01_mvp_schema.sql" "$cleanup_dir/01.sql"
render_sql "$repo_root/migration/sql/02_transform.sql" "$cleanup_dir/02.sql"
render_sql "$repo_root/migration/sql/03_verify.sql" "$cleanup_dir/03.sql"

echo "session=schema status=started"
mysql_cli < "$cleanup_dir/01.sql"
echo "session=schema status=completed"

echo "session=transform status=started"
mysql_cli < "$cleanup_dir/02.sql"
echo "session=transform status=completed"

echo "session=verification status=started"
mysql_cli --table < "$cleanup_dir/03.sql"
failure_total=$(mysql_cli "$target_db" -e \
  "SELECT COUNT(*) FROM migration_verification_results WHERE failure_count <> 0")
echo "session=verification status=completed failed_checks=$failure_total"

if [[ "$failure_total" != 0 ]]; then
  exit 2
fi
