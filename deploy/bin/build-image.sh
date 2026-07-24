#!/usr/bin/env bash
set -euo pipefail

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

repo_root=$(git -C "$(dirname "$0")" rev-parse --show-toplevel)
docker build --pull --label "org.opencontainers.image.revision=$image_tag" \
  --tag "$image_repository:$image_tag" "$repo_root"
echo "image=$image_repository:$image_tag status=built"
