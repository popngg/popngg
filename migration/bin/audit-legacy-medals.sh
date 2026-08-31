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
    "$repo_root/migration/sql/04_audit_legacy_medals.sql" \
    > "$temporary_dir/audit.sql"

mysql "${mysql_args[@]}" < "$temporary_dir/audit.sql" \
  > "$temporary_dir/report.tsv"
cat "$temporary_dir/report.tsv"

if [[ -n "$report_file" ]]; then
  mkdir -p "$(dirname "$report_file")"
  cp "$temporary_dir/report.tsv" "$report_file"
fi

mismatches=$(mysql "${mysql_args[@]}" --skip-column-names -e "
  SELECT COUNT(*)
    FROM \`$legacy_db\`.playdata legacy
    JOIN \`$target_db\`.migration_playdata_map map
      ON map.old_playdata_id = legacy.playdata_id
    JOIN \`$target_db\`.playdata current
      ON current.playdata_id = map.new_playdata_id
   WHERE current.all_time_score_version = 28
     AND current.version_score_known = FALSE
     AND current.last_renew_log_id IS NULL
     AND current.medal_code <> CASE legacy.medal
       WHEN 0 THEN 13 WHEN 8 THEN 11 WHEN 9 THEN 8
       WHEN 10 THEN 9 WHEN 11 THEN 10 ELSE legacy.medal END;")

echo "legacy_medal_audit mismatches=$mismatches"
[[ "$mismatches" == 0 ]] || exit 2
