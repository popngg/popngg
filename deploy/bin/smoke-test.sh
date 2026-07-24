#!/usr/bin/env bash
set -euo pipefail

base_url=${BASE_URL:-"http://127.0.0.1:${API_PORT:-8080}"}
curl_args=(--fail --silent --show-error --connect-timeout 3 --max-time 10)

curl "${curl_args[@]}" "$base_url/actuator/health" | grep -q '"status":"UP"'
curl "${curl_args[@]}" "$base_url/api/v1/songs?page=0&size=1" >/dev/null
curl "${curl_args[@]}" "$base_url/api/v1/users/rankings?page=0&size=1" >/dev/null

if [[ -n "${SMOKE_POPTOMO_ID:-}" ]]; then
  curl "${curl_args[@]}" "$base_url/api/v1/users/$SMOKE_POPTOMO_ID" >/dev/null
  curl "${curl_args[@]}" "$base_url/api/v1/users/$SMOKE_POPTOMO_ID/playdata" >/dev/null
fi

if [[ -n "${SMOKE_LOGIN_PASSWORD:-}" && -n "${SMOKE_POPTOMO_ID:-}" ]]; then
  echo "authenticated login and playdata import must use a dedicated smoke account" >&2
  curl "${curl_args[@]}" -H 'Content-Type: application/json' \
    --data "{\"poptomoId\":\"$SMOKE_POPTOMO_ID\",\"password\":\"$SMOKE_LOGIN_PASSWORD\"}" \
    "$base_url/api/v1/auth/login" >/dev/null
fi

echo "smoke health=PASS songs=PASS rankings=PASS"
