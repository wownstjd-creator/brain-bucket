#!/usr/bin/env bash
set -euo pipefail
[[ $# == 2 && "$2" == --confirm-replace ]] || { echo 'Usage: bash scripts/restore.sh BACKUP_DIR --confirm-replace (replaces current DB/uploads)' >&2; exit 2; }
source "$(dirname -- "$0")/compose-common.sh"
BB_BACKUP="$(cd -- "$1" && pwd)"
[[ -s "$BB_BACKUP/database.sql" && -s "$BB_BACKUP/uploads.tgz" && -f "$BB_BACKUP/SHA256SUMS" ]] || { echo 'Incomplete backup.' >&2; exit 1; }
(cd -- "$BB_BACKUP" && sha256sum -c SHA256SUMS)
# Accept only the uploads tree and reject traversal in an archive.
tar tzf "$BB_BACKUP/uploads.tgz" | awk '$0 !~ /^uploads(\/|$)/ || $0 ~ /(^|\/)\.\.(\/|$)/ {bad=1} END {exit bad}'
"${BB_DC[@]}" stop app
# On failure leave the app stopped, so a partial restore is not served.
"${BB_DC[@]}" exec -T db sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" exec mysql --user="$MYSQL_USER" --default-character-set=utf8mb4 "$MYSQL_DATABASE"' < "$BB_BACKUP/database.sql"
"${BB_DC[@]}" run --rm --no-deps --entrypoint sh -v "$BB_BACKUP:/backup:ro" app -c 'find /app/uploads -mindepth 1 -maxdepth 1 -exec rm -rf -- {} + && tar xzf /backup/uploads.tgz -C /app'
"${BB_DC[@]}" start app
printf 'Restore complete. Check login, posts and images before reopening service.\n'
