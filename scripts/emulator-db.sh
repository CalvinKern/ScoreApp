#!/usr/bin/env bash
#
# Saves, restores, and seeds the debug app's database on a connected emulator or device, so test data survives
# reinstalls and migrations that wipe the database.
#
#   scripts/emulator-db.sh backup [label]     Save the database to db-backups/
#   scripts/emulator-db.sh restore [file]     Put a backup back (the newest one when no file is given)
#   scripts/emulator-db.sh seed               Replace the data with scripts/test-data/seed.sql (backs up first)
#   scripts/emulator-db.sh list               List the backups
#
# Works with debug builds only (it uses run-as), and in Git Bash on Windows, macOS, and Linux. Needs adb and
# sqlite3, which both come with the Android SDK platform-tools. Set ANDROID_SERIAL to pick a device when more
# than one is connected, and PACKAGE to use another build.
#
# A backup from an older schema can be restored into a newer app, which then runs its migrations on launch.
# A backup from a newer schema can't be, since Room doesn't downgrade.

set -euo pipefail

# Git Bash would otherwise rewrite device paths like /data/local/tmp into Windows paths
export MSYS_NO_PATHCONV=1

PACKAGE="${PACKAGE:-com.seakernel.scorepad.debug}"
ACTIVITY="com.seakernel.android.scoreapp.ui.MainActivity"
DB_NAME="score_app"
DEVICE_DB="databases/$DB_NAME" # Relative to the app's data directory, where run-as starts
DEVICE_TMP="/data/local/tmp/$DB_NAME.restore"

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKUP_DIR="${BACKUP_DIR:-$REPO_ROOT/db-backups}"
SEED_SQL="$REPO_ROOT/scripts/test-data/seed.sql"
SCHEMA_DIR="$REPO_ROOT/app/schemas/com.seakernel.android.scoreapp.database.AppDatabase"

die() { echo "Error: $*" >&2; exit 1; }
info() { echo "==> $*"; }

# Finds a platform-tools binary on the PATH or in the usual SDK locations
find_tool() {
    local name="$1" dir
    if command -v "$name" >/dev/null 2>&1; then command -v "$name"; return; fi
    for dir in "${ANDROID_HOME:-}" "${ANDROID_SDK_ROOT:-}" "${LOCALAPPDATA:-}/Android/Sdk" "$HOME/Library/Android/sdk" "$HOME/Android/Sdk"; do
        [[ -n "$dir" ]] || continue
        for candidate in "$dir/platform-tools/$name" "$dir/platform-tools/$name.exe"; do
            if [[ -x "$candidate" ]]; then echo "$candidate"; return; fi
        done
    done
    die "$name not found. Install the Android SDK platform-tools or add them to the PATH."
}

ADB="$(find_tool adb)"
SQLITE="$(find_tool sqlite3)"

adb() { "$ADB" "$@"; }
run_as() { adb shell run-as "$PACKAGE" "$@"; }

# sqlite3 can be a native Windows program that can't read Git Bash paths, so it's always run on a file
# name inside the file's own folder
sqlite_in() {
    local file="$1"; shift
    (cd "$(dirname "$file")" && "$SQLITE" "$(basename "$file")" "$@")
}

# The newest schema the app knows, from Room's exported schemas
app_schema_version() {
    ls "$SCHEMA_DIR" 2>/dev/null | sed -n 's/^\([0-9]*\)\.json$/\1/p' | sort -n | tail -1
}

check_device() {
    if [[ -z "${ANDROID_SERIAL:-}" && "$(adb devices | grep -c $'\tdevice')" -gt 1 ]]; then
        die "More than one device is connected. Set ANDROID_SERIAL to one from 'adb devices'."
    fi
    [[ "$(adb get-state 2>/dev/null)" == "device" ]] || die "No device connected (check 'adb devices', or set ANDROID_SERIAL)."
    run_as true 2>/dev/null || die "Can't access $PACKAGE. Is the debug build installed?"
}

# Closes the database by stopping the app, so the files aren't changing while they're copied
stop_app() { adb shell am force-stop "$PACKAGE"; }

device_has_db() { run_as test -f "$DEVICE_DB"; }

# Opens the app so Room creates the database, then waits for the file
create_db() {
    info "Opening the app to create its database"
    adb shell am start -n "$PACKAGE/$ACTIVITY" >/dev/null
    for _ in $(seq 30); do
        if device_has_db; then sleep 2; stop_app; return; fi
        sleep 1
    done
    die "The app didn't create its database."
}

# Copies the database off the device into a single file, folding in the write-ahead log
pull_db() {
    local out="$1" work suffix
    work="$(mktemp -d)"
    stop_app
    for suffix in "" "-wal" "-shm"; do
        if run_as test -f "$DEVICE_DB$suffix"; then
            adb exec-out run-as "$PACKAGE" cat "$DEVICE_DB$suffix" > "$work/$DB_NAME$suffix"
        fi
    done
    [[ -s "$work/$DB_NAME" ]] || { rm -rf "$work"; die "The database is empty or missing on the device."; }

    rm -f "$out"
    sqlite_in "$work/$DB_NAME" "VACUUM INTO '$DB_NAME.single'"
    mv "$work/$DB_NAME.single" "$out"
    rm -rf "$work"
}

# Replaces the device's database with a file. The app's old log files are removed so they aren't replayed.
push_db() {
    local file="$1" local_size device_size
    stop_app
    (cd "$(dirname "$file")" && adb push "$(basename "$file")" "$DEVICE_TMP" >/dev/null 2>&1)
    # One string, since adb joins its arguments and the device shell would run anything after && outside run-as
    adb shell "run-as $PACKAGE sh -c 'mkdir -p databases && cat $DEVICE_TMP > $DEVICE_DB && rm -f $DEVICE_DB-wal $DEVICE_DB-shm $DEVICE_DB-journal'"
    adb shell rm -f "$DEVICE_TMP"

    local_size="$(wc -c < "$file" | tr -d ' ')"
    device_size="$(run_as stat -c %s "$DEVICE_DB" | tr -d '\r')"
    [[ "$local_size" == "$device_size" ]] || die "Copy failed ($local_size bytes here, $device_size on the device)."
}

db_version() { sqlite_in "$1" "PRAGMA user_version;" | tr -d '\r'; }

db_summary() {
    sqlite_in "$1" "SELECT (SELECT COUNT(*) FROM games) || ' games, ' || (SELECT COUNT(*) FROM players) || ' players, '
        || (SELECT COUNT(*) FROM rounds) || ' rounds, ' || (SELECT COUNT(*) FROM scores) || ' scores';" | tr -d '\r'
}

cmd_backup() {
    local label="${1:-}" version file
    check_device
    device_has_db || die "The app has no database yet, open it once first."
    mkdir -p "$BACKUP_DIR"

    file="$BACKUP_DIR/$DB_NAME-$(date +%Y%m%d-%H%M%S).db"
    pull_db "$file"
    version="$(db_version "$file")"
    local named="${file%.db}-v$version${label:+-$label}.db"
    mv "$file" "$named"
    info "Saved $(basename "$named") (schema v$version: $(db_summary "$named"))"
}

cmd_restore() {
    local file="${1:-}" version app_version
    if [[ -z "$file" ]]; then
        file="$(ls -1 "$BACKUP_DIR"/*.db 2>/dev/null | sort | tail -1 || true)"
        [[ -n "$file" ]] || die "No backups in $BACKUP_DIR."
    elif [[ ! -f "$file" && -f "$BACKUP_DIR/$file" ]]; then
        file="$BACKUP_DIR/$file"
    fi
    [[ -f "$file" ]] || die "No backup at $file."
    check_device

    version="$(db_version "$file")"
    app_version="$(app_schema_version)"
    if [[ -n "$app_version" && "$version" -gt "$app_version" ]]; then
        die "$(basename "$file") is schema v$version, newer than this checkout's v$app_version. Room can't downgrade it."
    fi

    push_db "$file"
    info "Restored $(basename "$file") (schema v$version: $(db_summary "$file"))"
    if [[ -n "$app_version" && "$version" -lt "$app_version" ]]; then
        info "The app will migrate it from v$version to v$app_version when it opens."
    fi
}

cmd_seed() {
    local seed_version work
    [[ -f "$SEED_SQL" ]] || die "No seed data at $SEED_SQL."
    check_device
    if device_has_db; then
        cmd_backup "before-seed"
    else
        create_db
    fi

    work="$(mktemp -d)"
    pull_db "$work/$DB_NAME.db"
    # Windows checkouts can have \r line endings
    seed_version="$(tr -d '\r' < "$SEED_SQL" | sed -n 's/^-- schema-version: \([0-9]*\)$/\1/p')"
    [[ "$(db_version "$work/$DB_NAME.db")" == "$seed_version" ]] ||
        die "The app's database is schema v$(db_version "$work/$DB_NAME.db"), but the seed data is for v$seed_version. Update $SEED_SQL."

    cp "$SEED_SQL" "$work/seed.sql"
    sqlite_in "$work/$DB_NAME.db" ".bail on" ".read seed.sql"
    push_db "$work/$DB_NAME.db"
    info "Seeded the test data ($(db_summary "$work/$DB_NAME.db"))"
    rm -rf "$work"
}

cmd_list() {
    local file
    ls "$BACKUP_DIR"/*.db >/dev/null 2>&1 || { echo "No backups in $BACKUP_DIR."; return; }
    for file in "$BACKUP_DIR"/*.db; do
        printf '%s  (v%s: %s)\n' "$(basename "$file")" "$(db_version "$file")" "$(db_summary "$file")"
    done
}

case "${1:-}" in
    backup) shift; cmd_backup "$@" ;;
    restore) shift; cmd_restore "$@" ;;
    seed) shift; cmd_seed "$@" ;;
    list) shift; cmd_list "$@" ;;
    *) sed -n '3,17p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; exit 1 ;;
esac
