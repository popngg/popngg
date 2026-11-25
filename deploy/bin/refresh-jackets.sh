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
case "${1:-}" in
  "") apply=() ;;
  --apply) apply=(--apply) ;;
  *) echo "usage: bash deploy/bin/refresh-jackets.sh [--apply]" >&2; exit 64 ;;
esac

lock_dir=${DEPLOY_LOCK_DIR:-"$repo_root/.deploy.lock"}
if ! mkdir "$lock_dir" 2>/dev/null; then
  echo "another deployment or jacket maintenance is running" >&2; exit 75
fi
trap 'rmdir "$lock_dir"' EXIT
compose=(docker compose "${compose_env[@]}" -f "$repo_root/deploy/compose.yml")
if [[ ${#apply[@]} -gt 0 && -n "$("${compose[@]}" ps --status running -q api)" ]]; then
  echo "Stop API writes and the API container before applying jacket changes; restart afterward." >&2
  exit 75
fi
source_dir="$repo_root/deploy/catalog/2026-10-13-jam-and-fizz/jackets"
report_dir=${JACKET_REPORT_DIR:-"$repo_root/output/jacket-maintenance"}
mkdir -p "$report_dir"
"${compose[@]}" run --rm --no-deps --user "$(id -u):$(id -g)" \
  --entrypoint java -v "$source_dir:/jackets:ro" -v "$report_dir:/reports" api \
  -Dloader.main=gg.popn.infra.storage.JacketMaintenanceCli \
  -cp /app/app.jar org.springframework.boot.loader.launch.PropertiesLauncher \
  /jackets "/reports/jackets-$(date -u +%Y%m%dT%H%M%SZ).json" --normalize-existing "${apply[@]}"
