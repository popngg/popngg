#!/usr/bin/env bash
set -euo pipefail

repo_root=$(git -C "$(dirname "$0")" rev-parse --show-toplevel)
env_file=${ENV_FILE:-"$repo_root/.env"}
if [[ -f "$env_file" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$env_file"
  set +a
fi

image_repository=${IMAGE_REPOSITORY:-}
image_tag=${IMAGE_TAG:-}
[[ "$image_repository" =~ ^[a-zA-Z0-9._/-]+$ ]] || {
  echo "IMAGE_REPOSITORY is required" >&2
  exit 64
}
[[ "$image_tag" =~ ^[A-Za-z0-9][A-Za-z0-9._-]{6,127}$ && "$image_tag" != latest ]] || {
  echo "IMAGE_TAG must be an immutable commit SHA or release tag, not latest" >&2
  exit 64
}

revision=$(git -C "$repo_root" rev-parse HEAD)
build_epoch=$(date +%s)
build_time=$(date -u -d "@$build_epoch" +%Y-%m-%dT%H:%M:%SZ)
# POSIX TZ works even when the host has no IANA zoneinfo database (e.g. Git Bash).
release_version="$(TZ=KST-9 date -d "@$build_epoch" +%Y.%m.%d.%H%M%S)-${revision:0:7}"
docker build --pull \
  --build-arg "POPNGG_RELEASE_VERSION=$release_version" \
  --build-arg "POPNGG_GIT_SHA=$revision" \
  --build-arg "POPNGG_BUILD_TIME=$build_time" \
  --label "org.opencontainers.image.revision=$revision" \
  --label "org.opencontainers.image.version=$release_version" \
  --label "org.opencontainers.image.created=$build_time" \
  --tag "$image_repository:$image_tag" \
  --tag "$image_repository:$release_version" "$repo_root"
echo "image=$image_repository:$image_tag release=$release_version revision=$revision status=built"
