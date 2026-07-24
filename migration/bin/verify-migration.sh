#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "usage: $0 --legacy-db NAME --target-db NAME --session-id ID [--report FILE]" >&2
  exit 64
}

legacy_db=
target_db=
session_id=
report_file=
while (($#)); do
  case "$1" in
    --legacy-db) legacy_db=${2:-}; shift 2 ;;
    --target-db) target_db=${2:-}; shift 2 ;;
    --session-id) session_id=${2:-}; shift 2 ;;
    --report) report_file=${2:-}; shift 2 ;;
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
  --raw
  --skip-column-names
)

temporary_dir=$(mktemp -d)
trap 'rm -rf "$temporary_dir"' EXIT
sed -e "s/__LEGACY_DB__/$legacy_db/g" \
    -e "s/__TARGET_DB__/$target_db/g" \
    -e "s/__SESSION_ID__/$session_id/g" \
    "$repo_root/migration/sql/02_verify.sql" > "$temporary_dir/verify.sql"

mysql "${mysql_args[@]}" < "$temporary_dir/verify.sql" > "$temporary_dir/report.tsv"
if [[ -n "$report_file" ]]; then
  mkdir -p "$(dirname "$report_file")"
  cp "$temporary_dir/report.tsv" "$report_file"
fi

cat "$temporary_dir/report.tsv"
blockers=$(mysql "${mysql_args[@]}" "$target_db" -e \
  "SELECT COUNT(*) FROM migration_verification_results
    WHERE session_id='$session_id' AND severity='BLOCKER' AND status='FAIL'")
if [[ "$blockers" != 0 ]]; then
  echo "verification session=$session_id status=FAILED blockers=$blockers" >&2
  exit 2
fi
echo "verification session=$session_id status=SUCCESS blockers=0"
