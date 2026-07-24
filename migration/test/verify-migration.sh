#!/usr/bin/env bash
set -euo pipefail

repo_root=$(git -C "$(dirname "$0")" rev-parse --show-toplevel)
container="popngg-migration-verification-$RANDOM-$$"
port=$((23306 + RANDOM % 1000))
mysql_password="local-test-only"
report=$(mktemp)
trap 'docker rm -f "$container" >/dev/null 2>&1 || true; rm -f "$report"' EXIT

docker run -d --name "$container" -e MYSQL_ROOT_PASSWORD="$mysql_password" \
  -p "127.0.0.1:$port:3306" mysql:8.0 >/dev/null
for _ in {1..60}; do
  if docker exec -e MYSQL_PWD="$mysql_password" "$container" \
      mysql -uroot -Nse "SELECT 1" >/dev/null 2>&1; then
    break
  fi
  sleep 1
done
docker exec -e MYSQL_PWD="$mysql_password" "$container" mysql -uroot -Nse "SELECT 1"
docker exec -e MYSQL_PWD="$mysql_password" "$container" mysql -uroot -e \
  "CREATE DATABASE legacy; CREATE DATABASE target;"
docker exec -i -e MYSQL_PWD="$mysql_password" "$container" mysql -uroot legacy \
  < "$repo_root/migration/test/legacy-fixture.sql"

for migration in "$repo_root"/popngg-infra/src/main/resources/db/migration/V*.sql; do
  docker exec -i -e MYSQL_PWD="$mysql_password" "$container" mysql -uroot target < "$migration"
done

MYSQL_HOST=127.0.0.1 MYSQL_PORT="$port" MYSQL_USER=root MYSQL_PWD="$mysql_password" \
  "$repo_root/migration/bin/migrate-legacy.sh" \
  --legacy-db legacy --target-db target --session-id verification-test

if MYSQL_HOST=127.0.0.1 MYSQL_PORT="$port" MYSQL_USER=root MYSQL_PWD="$mysql_password" \
  "$repo_root/migration/bin/verify-migration.sh" \
  --legacy-db legacy --target-db target --session-id verification-test --report "$report"; then
  echo "error: verifier did not detect the intentionally missing potential popclass" >&2
  exit 1
fi

docker exec -e MYSQL_PWD="$mysql_password" "$container" mysql -uroot target -e "
  UPDATE user_profiles p
  JOIN (
    SELECT user_id, FLOOR(SUM(chart_popclass) / 50) AS expected_value
    FROM (
      SELECT p.user_id,
             GREATEST(0, FLOOR((c.level * 10000 + p.all_time_score - 50000
               + CASE WHEN p.medal_code BETWEEN 1 AND 4 THEN 5000
                      WHEN p.medal_code BETWEEN 5 AND 8 THEN 3000 ELSE 0 END) / 54.4))
               AS chart_popclass
      FROM playdata p JOIN charts c ON c.chart_id=p.chart_id
      WHERE c.is_deleted=FALSE
    ) values_by_chart GROUP BY user_id
  ) expected ON expected.user_id=p.user_id
  SET p.potential_popclass=expected.expected_value;"

MYSQL_HOST=127.0.0.1 MYSQL_PORT="$port" MYSQL_USER=root MYSQL_PWD="$mysql_password" \
  "$repo_root/migration/bin/verify-migration.sh" \
  --legacy-db legacy --target-db target --session-id verification-test --report "$report"
grep -q $'potential_popclass_mismatch\tPOPCLASS\tBLOCKER\t0\t0\tPASS' "$report"
echo "migration verification integration test passed"
