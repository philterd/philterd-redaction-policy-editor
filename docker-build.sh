#!/usr/bin/env bash
#
# Build the Philterd Redaction Policy Editor Docker image.
#
# Tags the image as both philterd/philterd-redaction-policy-editor:<version> and :latest.
# The version is taken from the Maven project version unless overridden via the VERSION
# environment variable or the first argument.
#
# Usage:
#   ./docker-build.sh            # version from pom.xml
#   ./docker-build.sh 1.2.0      # explicit version
#   VERSION=1.2.0 ./docker-build.sh
#
set -euo pipefail

IMAGE="${IMAGE:-philterd/philterd-redaction-policy-editor}"

# Run from the repository root (where this script lives) regardless of the invocation directory.
cd "$(dirname "$0")"

VERSION="${1:-${VERSION:-$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)}}"

if [[ -z "${VERSION}" ]]; then
  echo "Error: could not determine the version to tag." >&2
  exit 1
fi

echo "Building ${IMAGE}:${VERSION} and ${IMAGE}:latest"
docker build -t "${IMAGE}:${VERSION}" -t "${IMAGE}:latest" .

echo "Done. Built:"
echo "  ${IMAGE}:${VERSION}"
echo "  ${IMAGE}:latest"
