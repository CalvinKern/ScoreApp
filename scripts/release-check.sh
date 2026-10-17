#!/usr/bin/env bash
#
# Builds the release app the way Google Play gets it (R8 and resource shrinking) and installs it on an emulator, to
# check before a release that R8 didn't strip or rename something the app needs.
#
#   scripts/release-check.sh                Build, install, and open the release app
#   scripts/release-check.sh --no-build     Install and open the last release build again
#   scripts/release-check.sh uninstall      Remove the release app from the device
#
# Releases are signed in Android Studio, so Gradle's release APK is unsigned. A copy is signed with the debug key
# (~/.android/debug.keystore) to install it. It's com.seakernel.scorepad, next to the debug app, and starts with no
# data: run-as only works on debug builds, so emulator-db.sh can't load test data into it. Building it doesn't upload
# Crashlytics' mapping file (see app/build.gradle).
#
# Use an emulator. A phone with Score Pad from Google Play refuses the install (a different signature), and the
# script stops rather than uninstall that copy and its games.
#
# Takes the device's lock first (see emulator-lock.sh), and stops if another checkout holds it. Set ANDROID_SERIAL to
# pick a device when more than one is connected. Works in Git Bash on Windows, macOS, and Linux.

set -euo pipefail

PACKAGE="com.seakernel.scorepad"
ACTIVITY="com.seakernel.android.scoreapp.ui.MainActivity"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APK_DIR="$REPO_ROOT/app/build/outputs/apk/release"
KEYSTORE="${DEBUG_KEYSTORE:-$HOME/.android/debug.keystore}"

die() { echo "Error: $*" >&2; exit 1; }
info() { echo "==> $*"; }

sdk_dirs() {
    local dir
    for dir in "${ANDROID_HOME:-}" "${ANDROID_SDK_ROOT:-}" "${LOCALAPPDATA:-}/Android/Sdk" "$HOME/Library/Android/sdk" "$HOME/Android/Sdk"; do
        [[ -n "$dir" && -d "$dir" ]] && echo "$dir"
    done
}

# Finds a platform-tools binary on the PATH or in the usual SDK locations
find_tool() {
    local name="$1" dir candidate
    if command -v "$name" >/dev/null 2>&1; then command -v "$name"; return; fi
    while read -r dir; do
        for candidate in "$dir/platform-tools/$name" "$dir/platform-tools/$name.exe"; do
            if [[ -x "$candidate" ]]; then echo "$candidate"; return; fi
        done
    done < <(sdk_dirs)
    die "$name not found. Install the Android SDK platform-tools or add them to the PATH."
}

# Finds zipalign or apksigner in the newest build-tools
find_build_tool() {
    local name="$1" dir version candidate
    while read -r dir; do
        for version in $(ls "$dir/build-tools" 2>/dev/null | sort -rV); do
            for candidate in "$dir/build-tools/$version/$name" "$dir/build-tools/$version/$name.exe" "$dir/build-tools/$version/$name.bat"; do
                if [[ -f "$candidate" ]]; then echo "$candidate"; return; fi
            done
        done
    done < <(sdk_dirs)
    die "$name not found. Install the Android SDK build-tools."
}

# Windows programs can't read Git Bash paths, and emulator-lock.sh turns off Git Bash's conversion of them
native_path() { if command -v cygpath >/dev/null 2>&1; then cygpath -m "$1"; else echo "$1"; fi; }

ADB="$(find_tool adb)"
adb() { "$ADB" "$@"; }
source "$(dirname "${BASH_SOURCE[0]}")/emulator-lock.sh"

check_device() {
    # Before touching the device, so it's never one another checkout is testing on
    lock_acquire_target
    if [[ -z "${ANDROID_SERIAL:-}" && "$(adb devices | grep -c $'\tdevice')" -gt 1 ]]; then
        die "More than one device is connected. Set ANDROID_SERIAL to one from 'adb devices'."
    fi
    [[ "$(adb get-state 2>/dev/null)" == "device" ]] || die "No device connected (check 'adb devices', or set ANDROID_SERIAL)."
}

build() {
    info "Building the release app"
    (cd "$REPO_ROOT" && ./gradlew :app:assembleRelease -q)
}

sign() {
    local unsigned="$APK_DIR/app-release-unsigned.apk" aligned="$APK_DIR/app-release-aligned.apk"
    SIGNED="$APK_DIR/app-release-debug-signed.apk"
    [[ -f "$unsigned" ]] || die "No release build at $unsigned. Run this without --no-build."
    [[ -f "$KEYSTORE" ]] || die "No debug keystore at $KEYSTORE. Build and run the debug app once to create it."

    info "Signing a copy with the debug key"
    "$(find_build_tool zipalign)" -f -p 4 "$(native_path "$unsigned")" "$(native_path "$aligned")"
    "$(find_build_tool apksigner)" sign --ks "$(native_path "$KEYSTORE")" --ks-pass pass:android \
        --key-pass pass:android --ks-key-alias androiddebugkey --out "$(native_path "$SIGNED")" "$(native_path "$aligned")"
    rm -f "$aligned" "$SIGNED.idsig"
}

install_and_open() {
    local result
    info "Installing $PACKAGE"
    result="$(adb install -r "$(native_path "$SIGNED")" 2>&1 || true)"
    case "$result" in
        *Success*) ;;
        *INSTALL_FAILED_UPDATE_INCOMPATIBLE*)
            die "This device has a $PACKAGE signed with another key (Score Pad from Google Play?). Use an emulator, or" \
                "uninstall it yourself if losing its data is fine." ;;
        *) die "Install failed: $result" ;;
    esac
    adb shell am force-stop "$PACKAGE"
    adb shell am start -n "$PACKAGE/$ACTIVITY" >/dev/null 2>&1
    info "Opened the release app. Check what R8 can break: adding players, scores and rounds, finishing a game with"
    info "confetti and the standings, the graph, settings, and the game still there after closing the app. Crashes and"
    info "missing classes show in: adb logcat -b crash"
    info "Remove it when done: scripts/release-check.sh uninstall"
}

case "${1:-}" in
    "") check_device; build; sign; install_and_open ;;
    --no-build) check_device; sign; install_and_open ;;
    uninstall) check_device; adb uninstall "$PACKAGE" ;;
    *) sed -n '3,19p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; exit 1 ;;
esac
