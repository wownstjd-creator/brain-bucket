#!/usr/bin/env bash
set -euo pipefail
BB_ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
BB_CONFIG="${BB_COMPOSE_FILE:-$BB_ROOT/compose.yaml}"
[[ "$BB_CONFIG" = /* ]] || BB_CONFIG="$BB_ROOT/$BB_CONFIG"
[[ -f "$BB_ROOT/.env" ]] || { echo 'Create the root .env first.' >&2; exit 1; }
BB_DC=(docker compose --env-file "$BB_ROOT/.env" -f "$BB_CONFIG")
"${BB_DC[@]}" config --quiet
