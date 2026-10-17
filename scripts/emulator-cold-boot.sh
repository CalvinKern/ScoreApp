#!/usr/bin/env bash
#
# Cold boots an emulator (starts it without its saved snapshot), so a test run starts on an emulator that hasn't
# slowed down from days of use. adb reboot doesn't do this, it only restarts Android inside the same emulator.
#
#   scripts/emulator-cold-boot.sh [avd]     Cold boot the AVD with this name (Pixel_9a when none is given)
#
# Finds the running emulator by its AVD name, since emulator-55xx serials can swap when emulators restart, closes
# it, starts it again with -no-snapshot-load, and waits until Android has finished booting. An AVD that isn't
# running is just started. Prints the emulator's serial when it's ready, for ANDROID_SERIAL.
#
# Works in Git Bash on Windows, macOS, and Linux. Set BOOT_TIMEOUT to change how many seconds to wait for the
# boot (300 by default).

set -euo pipefail

# Git Bash would otherwise rewrite paths passed to adb into Windows paths
export MSYS_NO_PATHCONV=1

AVD="${1:-Pixel_9a}"
BOOT_TIMEOUT="${BOOT_TIMEOUT:-300}"

die() { echo "Error: $*" >&2; exit 1; }
info() { echo "==> $*"; }

# Finds a tool on the PATH or in the usual SDK locations, e.g. find_tool adb platform-tools
find_tool() {
    local name="$1" subdir="$2" dir candidate
    if command -v "$name" >/dev/null 2>&1; then command -v "$name"; return; fi
    for dir in "${ANDROID_HOME:-}" "${ANDROID_SDK_ROOT:-}" "${LOCALAPPDATA:-}/Android/Sdk" "$HOME/Library/Android/sdk" "$HOME/Android/Sdk"; do
        [[ -n "$dir" ]] || continue
        for candidate in "$dir/$subdir/$name" "$dir/$subdir/$name.exe"; do
            if [[ -x "$candidate" ]]; then echo "$candidate"; return; fi
        done
    done
    die "$name not found. Install the Android SDK's $subdir or add it to the PATH."
}

ADB="$(find_tool adb platform-tools)"
EMULATOR="$(find_tool emulator emulator)"
adb() { "$ADB" "$@"; }

# The serial of the running emulator with this AVD name, empty when it isn't running
avd_serial() {
    local serial
    for serial in $(adb devices | awk '/^emulator-[0-9]+\tdevice$/ { print $1 }'); do
        if [[ "$(adb -s "$serial" shell getprop ro.boot.qemu.avd_name 2>/dev/null | tr -d '\r')" == "$AVD" ]]; then
            echo "$serial"
            return
        fi
    done
}

"$EMULATOR" -list-avds | tr -d '\r' | grep -qx "$AVD" || die "No AVD named $AVD (see 'emulator -list-avds')."

serial="$(avd_serial)"
if [[ -n "$serial" ]]; then
    info "Closing $AVD ($serial)"
    adb -s "$serial" emu kill >/dev/null
    for _ in $(seq 60); do
        [[ -z "$(avd_serial)" ]] && break
        sleep 1
    done
    [[ -z "$(avd_serial)" ]] || die "$AVD didn't close."
    sleep 3 # Let the emulator process finish exiting before starting it again
fi

info "Starting $AVD without its snapshot"
# Started on its own, so it keeps running after this script ends
case "$(uname -s)" in
    MINGW* | MSYS* | CYGWIN*)
        powershell.exe -NoProfile -Command \
            "Start-Process -FilePath '$(cygpath -w "$EMULATOR")' -ArgumentList '-avd','$AVD','-no-snapshot-load' -WindowStyle Minimized"
        ;;
    *)
        nohup "$EMULATOR" -avd "$AVD" -no-snapshot-load >/dev/null 2>&1 &
        disown
        ;;
esac

info "Waiting for Android to boot"
serial=""
for _ in $(seq "$((BOOT_TIMEOUT / 2))"); do
    serial="$(avd_serial)"
    if [[ -n "$serial" && "$(adb -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == "1" ]]; then
        break
    fi
    serial=""
    sleep 2
done
[[ -n "$serial" ]] || die "$AVD didn't finish booting in $BOOT_TIMEOUT seconds."

info "$AVD is ready as $serial ($(adb -s "$serial" shell uptime | tr -d '\r' | sed 's/^ *//'))"
echo "export ANDROID_SERIAL=$serial"
