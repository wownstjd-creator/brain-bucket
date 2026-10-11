#!/usr/bin/env bash
set -euo pipefail
source "$(dirname -- "$0")/compose-common.sh"
umask 077
BB_DEST="${1:-$BB_ROOT/backups/$(date -u +%Y%m%dT%H%M%SZ)}"
mkdir -p -- "$BB_DEST"
BB_DEST="$(cd -- "$BB_DEST" && pwd)"
[[ ! -e "$BB_DEST/SHA256SUMS" && ! -e "$BB_DEST/database.sql" && ! -e "$BB_DEST/uploads.tgz" ]] || { echo 'Choose a new empty backup directory.' >&2; exit 1; }
[[ $("${BB_DC[@]}" ps --status running --services app) == app ]] || { echo 'Start the app before using this coordinated backup.' >&2; exit 1; }
BB_PAUSED=0
cleanup() { local result=$?; if [[ $BB_PAUSED == 1 ]]; then "${BB_DC[@]}" start app || result=1; fi; exit "$result"; }
trap cleanup EXIT
"${BB_DC[@]}" stop app
BB_PAUSED=1
"${BB_DC[@]}" exec -T db sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" exec mysqldump --user="$MYSQL_USER" --single-transaction --no-tablespaces --set-gtid-purged=OFF --default-character-set=utf8mb4 "$MYSQL_DATABASE"' > "$BB_DEST/database.sql.tmp"
[[ -s "$BB_DEST/database.sql.tmp" ]] || { echo 'Empty DB dump.' >&2; exit 1; }
mv -- "$BB_DEST/database.sql.tmp" "$BB_DEST/database.sql"
"${BB_DC[@]}" run --rm --no-deps --entrypoint tar -v "$BB_DEST:/backup" app czf /backup/uploads.tgz -C /app uploads
[[ -s "$BB_DEST/uploads.tgz" ]] || { echo 'Missing upload archive.' >&2; exit 1; }
(cd -- "$BB_DEST" && sha256sum database.sql uploads.tgz > SHA256SUMS)
printf 'Backup complete: %s\n' "$BB_DEST"
