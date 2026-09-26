#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "usage: $0 --legacy-db NAME --target-db NAME --expected-ready-count N --backup-file ABSOLUTE_PATH --apply" >&2
  exit 64
}

legacy_db=
target_db=
expected_ready=
backup_file=
apply=false
while (($#)); do
  case "$1" in
    --legacy-db) legacy_db=${2:-}; shift 2 ;;
    --target-db) target_db=${2:-}; shift 2 ;;
    --expected-ready-count) expected_ready=${2:-}; shift 2 ;;
    --backup-file) backup_file=${2:-}; shift 2 ;;
    --apply) apply=true; shift ;;
    *) usage ;;
  esac
done

[[ "$legacy_db" =~ ^[A-Za-z][A-Za-z0-9_]{0,63}$ ]] || usage
[[ "$target_db" =~ ^[A-Za-z][A-Za-z0-9_]{0,63}$ ]] || usage
[[ "$legacy_db" != "$target_db" ]] || usage
[[ "$expected_ready" =~ ^[1-9][0-9]*$ ]] || usage
[[ "$backup_file" = /* ]] || usage
[[ "$apply" == true ]] || usage

repo_root=$(git -C "$(dirname "$0")" rev-parse --show-toplevel)
backup_parent=$(realpath -m "$(dirname "$backup_file")")
backup_resolved="$backup_parent/$(basename "$backup_file")"
case "$backup_resolved" in
  "$repo_root"/*) echo "error: backup must be outside the repository" >&2; exit 64 ;;
esac
[[ -d "$backup_parent" && ! -e "$backup_resolved" ]] || {
  echo "error: backup parent must exist and backup file must not exist" >&2
  exit 64
}

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
sed -e "s/__LEGACY_PLAYDATA__/$legacy_db.playdata/g" \
    -e "s/__TARGET_DB__/$target_db/g" \
    "$repo_root/migration/sql/05_stage_medals_for_restore.sql" \
    > "$temporary_dir/stage.sql"

umask 077
cat "$temporary_dir/stage.sql" > "$temporary_dir/backup.sql"
cat >> "$temporary_dir/backup.sql" <<'SQL'
SELECT new_playdata_id, current_medal, expected_medal
  FROM medal_restore_audit WHERE audit_status = 'READY'
 ORDER BY new_playdata_id;
SQL
mysql "${mysql_args[@]}" --skip-column-names \
  < "$temporary_dir/backup.sql" > "$backup_resolved"
actual_ready=$(wc -l < "$backup_resolved" | tr -d ' ')
[[ "$actual_ready" == "$expected_ready" ]] || {
  echo "error: ready count changed (expected=$expected_ready actual=$actual_ready); no records updated" >&2
  exit 2
}

sed -e "s/__TARGET_DB__/$target_db/g" \
    -e "s/__EXPECTED_READY__/$expected_ready/g" \
    "$repo_root/migration/sql/06_apply_legacy_medal_restore.sql" \
    > "$temporary_dir/apply.sql"
cat "$temporary_dir/stage.sql" "$temporary_dir/apply.sql" \
  | mysql "${mysql_args[@]}"
echo "medal restore complete; backup=$backup_resolved"
