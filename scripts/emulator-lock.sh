#!/usr/bin/env bash
#
# Locks an emulator or device to one checkout of the repo, so two people or tools testing at once (from different
# worktrees) don't install over each other's build, clear each other's data, or restart each other's emulator.
#
#   scripts/emulator-lock.sh acquire [device]          Take the lock, or keep it if this checkout already has it
#   scripts/emulator-lock.sh release [device] [--force] Give it back (--force releases another checkout's lock)
#   scripts/emulator-lock.sh status                    List the locks and who holds them
#
# A device is an AVD name (Pixel_9a) or a serial (emulator-5554, or a phone's). Serials of emulators are turned into
# their AVD name, since emulator serials swap when emulators restart. With no device, it's ANDROID_SERIAL's, or the
# only connected one.
#
# emulator-cold-boot.sh, emulator-db.sh, emulator-perf.sh, and Gradle's install tasks take or check the lock before
# they touch a device, and stop when another checkout holds it. Release it when you're done testing.
#
# The lock is the directory <device>.lock in ~/.android/emulator-locks (EMULATOR_LOCK_DIR to change it), holding a
# file named owner with the lines owner=<the checkout's top folder, from git rev-parse --show-toplevel> and
# since=<when it was taken>. Set EMULATOR_LOCK_OWNER to use another owner name. A lock whose owner folder no longer
# exists (its worktree was removed) is taken over.
#
# Works in Git Bash on Windows, macOS, and Linux. Other scripts source this file for its lock_* functions.

# Git Bash would otherwise rewrite paths passed to adb into Windows paths
export MSYS_NO_PATHCONV=1

EMULATOR_LOCK_DIR="${EMULATOR_LOCK_DIR:-$HOME/.android/emulator-locks}"

_lock_die() { echo "Error: $*" >&2; exit 1; }

# The checkout taking locks: EMULATOR_LOCK_OWNER, or the repo's top folder
lock_owner() {
    if [[ -n "${EMULATOR_LOCK_OWNER:-}" ]]; then echo "$EMULATOR_LOCK_OWNER"; return; fi
    git -C "$(dirname "${BASH_SOURCE[0]}")" rev-parse --show-toplevel 2>/dev/null \
        || _lock_die "Not in a git checkout, set EMULATOR_LOCK_OWNER."
}

_lock_adb() {
    if command -v adb >/dev/null 2>&1; then adb "$@"; return; fi
    local dir
    for dir in "${ANDROID_HOME:-}" "${ANDROID_SDK_ROOT:-}" "${LOCALAPPDATA:-}/Android/Sdk" "$HOME/Library/Android/sdk" "$HOME/Android/Sdk"; do
        [[ -n "$dir" ]] || continue
        if [[ -x "$dir/platform-tools/adb" ]]; then "$dir/platform-tools/adb" "$@"; return; fi
        if [[ -x "$dir/platform-tools/adb.exe" ]]; then "$dir/platform-tools/adb.exe" "$@"; return; fi
    done
    _lock_die "adb not found. Install the Android SDK platform-tools or add them to the PATH."
}

# The serial of the device scripts would use: ANDROID_SERIAL, or the only connected device
lock_target_serial() {
    if [[ -n "${ANDROID_SERIAL:-}" ]]; then echo "$ANDROID_SERIAL"; return; fi
    local serials
    serials="$(_lock_adb devices | awk -F'\t' 'NR > 1 && $2 != "" { print $1 }')"
    case "$(grep -c . <<< "$serials")" in
        0) _lock_die "No device connected (check 'adb devices')." ;;
        1) echo "$serials" ;;
        *) _lock_die "More than one device is connected. Set ANDROID_SERIAL to one from 'adb devices'." ;;
    esac
}

# The lock name for a serial or AVD name: an emulator's AVD name, otherwise the name as given
lock_name() {
    local device="$1" avd
    if [[ "$device" == emulator-* ]]; then
        avd="$(_lock_adb -s "$device" emu avd name 2>/dev/null | head -1 | tr -d '\r')"
        [[ -n "$avd" ]] || _lock_die "Can't tell which AVD $device is (is it running?)."
        echo "$avd"
    else
        echo "$device"
    fi
}

_lock_field() { sed -n "s/^$2=//p" "$1/owner" 2>/dev/null | head -1; }

# Takes the lock for a device (AVD name or serial), or stops the script when another checkout holds it
lock_acquire() {
    local name owner dir holder since
    name="$(lock_name "$1")"
    owner="$(lock_owner)"
    dir="$EMULATOR_LOCK_DIR/$name.lock"
    mkdir -p "$EMULATOR_LOCK_DIR"

    if ! mkdir "$dir" 2>/dev/null; then
        holder="$(_lock_field "$dir" owner)"
        since="$(_lock_field "$dir" since)"
        if [[ "$holder" == "$owner" ]]; then
            return 0
        elif [[ -n "$holder" && ! -d "$holder" ]]; then
            echo "==> Taking over the lock on $name from $holder, which no longer exists" >&2
        elif [[ -z "$holder" && -n "$(find "$dir" -maxdepth 0 -mmin -1 2>/dev/null)" ]]; then
            # Another checkout is taking it right now and hasn't written its owner file yet
            _lock_die "$name is being locked by another checkout. Try again in a moment."
        elif [[ -n "$holder" ]]; then
            _lock_die "$name is locked by $holder (since $since). Wait for it to be released, use another device," \
                "or if it's no longer in use: scripts/emulator-lock.sh release $name --force"
        fi
    else
        echo "==> Locked $name for $owner. Release it when done: scripts/emulator-lock.sh release $name" >&2
    fi
    printf 'owner=%s\nsince=%s\n' "$owner" "$(date '+%Y-%m-%d %H:%M:%S')" > "$dir/owner"
}

# Takes the lock for the device scripts would use (see lock_target_serial)
lock_acquire_target() { lock_acquire "$(lock_target_serial)"; }

lock_release() {
    local name dir holder force="${2:-}"
    name="$(lock_name "$1")"
    dir="$EMULATOR_LOCK_DIR/$name.lock"
    [[ -d "$dir" ]] || { echo "==> $name isn't locked"; return 0; }
    holder="$(_lock_field "$dir" owner)"
    if [[ "$holder" != "$(lock_owner)" && "$force" != "--force" ]]; then
        _lock_die "$name is locked by $holder, not this checkout. Add --force to release it anyway."
    fi
    rm -rf "$dir"
    echo "==> Released $name"
}

lock_status() {
    local dir found=""
    for dir in "$EMULATOR_LOCK_DIR"/*.lock; do
        [[ -d "$dir" ]] || continue
        found=1
        echo "$(basename "$dir" .lock): $(_lock_field "$dir" owner) (since $(_lock_field "$dir" since))"
    done
    [[ -n "$found" ]] || echo "No devices are locked"
}

if [[ "${BASH_SOURCE[0]}" == "$0" ]]; then
    set -euo pipefail
    case "${1:-}" in
        acquire) lock_acquire "${2:-$(lock_target_serial)}" ;;
        release)
            if [[ "${2:-}" == --force ]]; then lock_release "$(lock_target_serial)" --force
            else lock_release "${2:-$(lock_target_serial)}" "${3:-}"; fi
            ;;
        status) lock_status ;;
        *) sed -n '3,22p' "$0" | sed 's/^# \{0,1\}//'; exit 1 ;;
    esac
fi
