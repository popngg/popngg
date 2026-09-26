#!/usr/bin/env bash
set -euo pipefail
umask 077

usage() {
  echo "usage: $0 --legacy-db NAME --target-db NAME [--report FILE] [--review-file ABSOLUTE_PATH]" >&2
  exit 64
}

legacy_db=
target_db=
report_file=
review_file=
while (($#)); do
  case "$1" in
    --legacy-db) legacy_db=${2:-}; shift 2 ;;
    --target-db) target_db=${2:-}; shift 2 ;;
    --report) report_file=${2:-}; shift 2 ;;
    --review-file) review_file=${2:-}; shift 2 ;;
    *) usage ;;
  esac
done

[[ "$legacy_db" =~ ^[A-Za-z][A-Za-z0-9_]{0,63}$ ]] || usage
[[ "$target_db" =~ ^[A-Za-z][A-Za-z0-9_]{0,63}$ ]] || usage
[[ "$legacy_db" != "$target_db" ]] || usage

repo_root=$(git -C "$(dirname "$0")" rev-parse --show-toplevel)
if [[ -n "$review_file" ]]; then
  [[ "$review_file" = /* ]] || usage
  review_parent=$(realpath -m "$(dirname "$review_file")")
  review_file="$review_parent/$(basename "$review_file")"
  case "$review_file" in
    "$repo_root"/*) echo "error: review file must be outside the repository" >&2; exit 64 ;;
  esac
  [[ -d "$review_parent" && ! -e "$review_file" ]] || {
    echo "error: review parent must exist and review file must not exist" >&2
    exit 64
  }
fi

mysql_args=(
  --host="${MYSQL_HOST:-127.0.0.1}"
  --port="${MYSQL_PORT:-3306}"
  --user="${MYSQL_USER:-root}"
  --database="$target_db"
  --protocol=tcp
  --batch
  --raw
)
temporary_dir=$(mktemp -d)
trap 'rm -rf "$temporary_dir"' EXIT

prepare_sql() {
  local result_file=$1
  local query_file=$2
  cat "$repo_root/migration/sql/05_stage_ambiguous_medals.sql" "$query_file" \
    | sed -e "s/__LEGACY_DB__/$legacy_db/g" \
          -e "s/__TARGET_DB__/$target_db/g" > "$result_file"
}

prepare_sql "$temporary_dir/report.sql" \
  "$repo_root/migration/sql/05_report_ambiguous_medals.sql"
mysql "${mysql_args[@]}" < "$temporary_dir/report.sql" > "$temporary_dir/report.tsv"
cat "$temporary_dir/report.tsv"

if [[ -n "$report_file" ]]; then
  mkdir -p "$(dirname "$report_file")"
  install -m 600 "$temporary_dir/report.tsv" "$report_file"
fi

if [[ -n "$review_file" ]]; then
  prepare_sql "$temporary_dir/review.sql" \
    "$repo_root/migration/sql/05_review_ambiguous_medals.sql"
  mysql "${mysql_args[@]}" < "$temporary_dir/review.sql" > "$temporary_dir/review.tsv"
  install -m 600 "$temporary_dir/review.tsv" "$review_file"
  echo "review file saved outside repository: $review_file" >&2
fi
