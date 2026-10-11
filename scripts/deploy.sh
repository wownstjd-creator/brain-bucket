#!/usr/bin/env bash
set -euo pipefail
export BB_COMPOSE_FILE=deploy/compose.yaml
source "$(dirname -- "$0")/compose-common.sh"
"${BB_DC[@]}" up -d --build
"${BB_DC[@]}" ps
printf 'Read docs/deployment.md for HTTPS, smoke checks and backup commands.\n'
