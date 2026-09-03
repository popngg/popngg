#!/usr/bin/env bash
set -euo pipefail

base_url=${BASE_URL:-"http://127.0.0.1:${API_PORT:-8080}"}
curl_args=(--fail --silent --show-error --connect-timeout 3 --max-time 10)
body_file=$(mktemp)
trap 'rm -f "$body_file"' EXIT

check() {
  local label=$1 path=$2 metrics exit_code=0
  shift 2
  echo "smoke start=$label"
  metrics=$(curl "${curl_args[@]}" --output "$body_file" \
    --write-out 'http=%{http_code} first_byte=%{time_starttransfer}s total=%{time_total}s' \
    "$@" "$base_url$path") || exit_code=$?
  echo "smoke request=$label $metrics curl_exit=$exit_code"
  if (( exit_code != 0 )); then return "$exit_code"; fi
}

check health /health
grep -q '"status":"UP"' "$body_file" || { echo 'smoke health=FAIL'; exit 1; }
check songs '/api/v1/songs?page=0&size=1'
check rankings '/api/v1/users/rankings?page=0&size=1'
check users_clear_level_first '/api/v1/users?sort=clearLevel&order=desc&page=1&size=20'
check users_clear_level_repeat '/api/v1/users?sort=clearLevel&order=desc&page=1&size=20'

if [[ -n "${SMOKE_POPTOMO_ID:-}" ]]; then
  check profile "/api/v1/users/$SMOKE_POPTOMO_ID"
  check playdata "/api/v1/users/$SMOKE_POPTOMO_ID/playdata"
fi

if [[ -n "${SMOKE_LOGIN_PASSWORD:-}" && -n "${SMOKE_POPTOMO_ID:-}" ]]; then
  echo "authenticated login and playdata import must use a dedicated smoke account" >&2
  check login /api/v1/auth/login -H 'Content-Type: application/json' \
    --data "{\"poptomoId\":\"$SMOKE_POPTOMO_ID\",\"password\":\"$SMOKE_LOGIN_PASSWORD\"}"
fi

echo "smoke health=PASS songs=PASS rankings=PASS users_clear_level=PASS"
