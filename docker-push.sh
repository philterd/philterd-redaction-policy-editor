#!/usr/bin/env bash
#
# Push the Philterd Redaction Policy Editor Docker image to its registry (DockerHub by default).
#
# Pushes both philterd/philterd-redaction-policy-editor:<version> and :latest. The version is taken
# from the Maven project version unless overridden via the VERSION environment variable or the first
# argument. Make sure you have run scripts/docker-build.sh and `docker login` first.
#
# Usage:
#   ./docker-push.sh             # version from pom.xml
#   ./docker-push.sh 1.2.0       # explicit version
#   VERSION=1.2.0 ./docker-push.sh
#
set -euo pipefail

IMAGE="${IMAGE:-philterd/philterd-redaction-policy-editor}"

# Run from the repository root (where this script lives) regardless of the invocation directory.
cd "$(dirname "$0")"

VERSION="${1:-${VERSION:-$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)}}"

if [[ -z "${VERSION}" ]]; then
  echo "Error: could not determine the version to push." >&2
  exit 1
fi

echo "Pushing ${IMAGE}:${VERSION} and ${IMAGE}:latest"
docker push "${IMAGE}:${VERSION}"
docker push "${IMAGE}:latest"

echo "Done. Pushed:"
echo "  ${IMAGE}:${VERSION}"
echo "  ${IMAGE}:latest"
