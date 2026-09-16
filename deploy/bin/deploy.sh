#!/usr/bin/env bash
set -euo pipefail

repo_root=$(git -C "$(dirname "$0")" rev-parse --show-toplevel)
env_file=${ENV_FILE:-"$repo_root/.env"}
compose_env=()
if [[ -f "$env_file" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$env_file"
  set +a
  compose_env=(--env-file "$env_file")
fi
lock_dir=${DEPLOY_LOCK_DIR:-"$repo_root/.deploy.lock"}
if ! mkdir "$lock_dir" 2>/dev/null; then
  echo "another deployment is already running" >&2
  exit 75
fi
trap 'rmdir "$lock_dir"' EXIT

: "${IMAGE_REPOSITORY:?IMAGE_REPOSITORY is required}"
: "${IMAGE_TAG:?IMAGE_TAG is required}"
[[ "$IMAGE_TAG" != latest ]] || {
  echo "latest is not an allowed deployment tag" >&2
  exit 64
}

compose=(docker compose "${compose_env[@]}" -f "$repo_root/deploy/compose.yml")
docker volume create api-logs 2>/dev/null || true
"${compose[@]}" run --rm --no-deps api-logs-init
"${compose[@]}" up --no-deps --wait mysql
"${compose[@]}" run --rm migration
"${compose[@]}" run --rm --no-deps catalog-migration
if ! "${compose[@]}" up -d --no-deps redis; then
  echo "warning: Redis could not start; API will use DB fallback" >&2
fi
"${compose[@]}" up -d --no-deps --wait api
"$repo_root/deploy/bin/smoke-test.sh"

wait_for_loki() {
  local port=${LOKI_PORT:-3100}
  local attempt
  for attempt in {1..30}; do
    if curl --fail --silent "http://127.0.0.1:${port}/ready" >/dev/null; then
      return 0
    fi
    sleep 2
  done
  echo "Loki did not become ready on 127.0.0.1:${port}" >&2
  return 1
}

monitoring_status=skipped
if [[ -n "${GRAFANA_ADMIN_PASSWORD:-}" ]]; then
  monitoring=(docker compose "${compose_env[@]}"
    -f "$repo_root/deploy/compose.yml"
    -f "$repo_root/deploy/compose.monitoring.yml")
  if "${monitoring[@]}" run --rm --no-deps loki-init \
    && "${monitoring[@]}" up -d --no-deps loki \
    && wait_for_loki \
    && "${monitoring[@]}" build incident-bot \
    && "${monitoring[@]}" up -d --wait prometheus alloy grafana incident-bot; then
    monitoring_status=healthy
  else
    monitoring_status=failed
    echo "warning: monitoring failed to start; API remains healthy" >&2
  fi
else
  echo "warning: monitoring skipped because GRAFANA_ADMIN_PASSWORD is not set" >&2
fi

echo "deployment image=$IMAGE_REPOSITORY:$IMAGE_TAG status=healthy monitoring=$monitoring_status"
