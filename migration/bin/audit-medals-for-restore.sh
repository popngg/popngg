#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "usage: $0 --legacy-db NAME --target-db NAME [--report FILE]" >&2
  exit 64
}

legacy_db=
target_db=
report_file=
while (($#)); do
  case "$1" in
    --legacy-db) legacy_db=${2:-}; shift 2 ;;
    --target-db) target_db=${2:-}; shift 2 ;;
    --report) report_file=${2:-}; shift 2 ;;
    *) usage ;;
  esac
done

[[ "$legacy_db" =~ ^[A-Za-z][A-Za-z0-9_]{0,63}$ ]] || usage
[[ "$target_db" =~ ^[A-Za-z][A-Za-z0-9_]{0,63}$ ]] || usage
[[ "$legacy_db" != "$target_db" ]] || usage

repo_root=$(git -C "$(dirname "$0")" rev-parse --show-toplevel)
mysql_args=(
  --host="${MYSQL_HOST:-127.0.0.1}"
  --port="${MYSQL_PORT:-3306}"
  --user="${MYSQL_USER:-root}"
  --protocol=tcp
  --batch
  --raw
)

temporary_dir=$(mktemp -d)
trap 'rm -rf "$temporary_dir"' EXIT
sed -e "s/__LEGACY_DB__/$legacy_db/g" \
    -e "s/__TARGET_DB__/$target_db/g" \
    "$repo_root/migration/sql/05_audit_medals_for_restore.sql" \
    > "$temporary_dir/audit.sql"

mysql "${mysql_args[@]}" < "$temporary_dir/audit.sql" \
  > "$temporary_dir/report.tsv"
cat "$temporary_dir/report.tsv"

if [[ -n "$report_file" ]]; then
  mkdir -p "$(dirname "$report_file")"
  cp "$temporary_dir/report.tsv" "$report_file"
fi
