#!/usr/bin/env bash
#
# Measures how smoothly the debug app scrolls on a connected emulator or device, so performance fixes can be
# compared before and after.
#
#   scripts/emulator-perf.sh scroll ["game name"]     Scroll a game up and down (the game list when no name is given)
#   scripts/emulator-perf.sh sideways "game name"     Scroll a game's score table left and right
#
# Prints the frame stats from dumpsys gfxinfo and where the main thread spent its time, from a system trace saved
# to perf-traces/ (open it at https://ui.perfetto.dev for the details). Emulators are slower than phones, so
# compare runs on the same device instead of reading the numbers on their own.
#
# Works with debug builds only (the app's trace sections need a debuggable app), and in Git Bash on Windows,
# macOS, and Linux. Set ANDROID_SERIAL to pick a device when more than one is connected, PACKAGE to use another
# build, and SWIPES to change how many swipes go each way (6 by default).

set -euo pipefail

# Git Bash would otherwise rewrite device paths like /sdcard into Windows paths
export MSYS_NO_PATHCONV=1

PACKAGE="${PACKAGE:-com.seakernel.scorepad.debug}"
ACTIVITY="com.seakernel.android.scoreapp.ui.MainActivity"
SWIPES="${SWIPES:-6}"
DEVICE_UI_DUMP="/sdcard/emulator-perf-ui.xml"

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TRACE_DIR="${TRACE_DIR:-$REPO_ROOT/perf-traces}"

die() { echo "Error: $*" >&2; exit 1; }

# Finds adb on the PATH or in the usual SDK locations
find_adb() {
    local dir candidate
    if command -v adb >/dev/null 2>&1; then command -v adb; return; fi
    for dir in "${ANDROID_HOME:-}" "${ANDROID_SDK_ROOT:-}" "${LOCALAPPDATA:-}/Android/Sdk" "$HOME/Library/Android/sdk" "$HOME/Android/Sdk"; do
        [[ -n "$dir" ]] || continue
        for candidate in "$dir/platform-tools/adb" "$dir/platform-tools/adb.exe"; do
            if [[ -x "$candidate" ]]; then echo "$candidate"; return; fi
        done
    done
    die "adb not found. Install the Android SDK platform-tools or add them to the PATH."
}

ADB="$(find_adb)"
adb() { "$ADB" "$@"; }

check_device() {
    if [[ -z "${ANDROID_SERIAL:-}" && "$(adb devices | grep -c $'\tdevice')" -gt 1 ]]; then
        die "More than one device is connected. Set ANDROID_SERIAL to one from 'adb devices'."
    fi
    [[ "$(adb get-state 2>/dev/null)" == "device" ]] || die "No device connected (check 'adb devices', or set ANDROID_SERIAL)."
    adb shell pm path "$PACKAGE" >/dev/null 2>&1 || die "$PACKAGE isn't installed."
}

# Screen size in pixels, so swipes land in the same places on any device
read_screen_size() {
    local size
    size="$(adb shell wm size | tail -1 | tr -d '\r' | sed 's/.*: //')"
    SCREEN_W="${size%x*}"
    SCREEN_H="${size#*x}"
}

# The screen's views, one per line
ui_nodes() {
    adb shell uiautomator dump "$DEVICE_UI_DUMP" >/dev/null 2>&1 || true
    adb exec-out cat "$DEVICE_UI_DUMP" | tr '>' '\n'
}

# The vertical middle of the game list row with this name, empty when it isn't on screen. The dump is read in
# full first, since a search that stops early (or finds nothing) would fail the pipeline and end the script.
game_row_y() {
    local name nodes
    name="$(printf '%s' "$1" | sed 's/&/\&amp;/g; s/</\&lt;/g; s/>/\&gt;/g; s/"/\&quot;/g')"
    nodes="$(ui_nodes)"
    printf '%s\n' "$nodes" | grep -F "text=\"$name\"" | grep -F "gameNameHolder" |
        sed -n 's/.*bounds="\[[0-9]*,\([0-9]*\)\]\[[0-9]*,\([0-9]*\)\]".*/\1 \2/p' |
        awk 'NR == 1 { print int(($1 + $2) / 2) }' || true
}

# Restarts the app on the game list, then opens the game with this name when one is given
open_app() {
    local name="${1:-}" y
    adb shell am force-stop "$PACKAGE"
    adb shell am start -n "$PACKAGE/$ACTIVITY" >/dev/null
    for _ in $(seq 15); do
        [[ "$(ui_nodes)" == *gameNameHolder* ]] && break
        sleep 1
    done
    [[ -n "$name" ]] || return 0

    for _ in $(seq 10); do
        y="$(game_row_y "$name")"
        if [[ -n "$y" ]]; then
            adb shell input tap $((SCREEN_W / 2)) "$y"
            sleep 3 # Let the game load and settle before measuring
            return
        fi
        swipe 50 70 50 40 800 # Slow, so it moves a set distance instead of flinging past the game
    done
    die "No game named \"$name\" in the game list."
}

# Swipes between two points given as percentages of the screen, quickly (a fling) unless a duration is given
swipe() {
    adb shell input swipe $((SCREEN_W * $1 / 100)) $((SCREEN_H * $2 / 100)) $((SCREEN_W * $3 / 100)) $((SCREEN_H * $4 / 100)) "${5:-150}"
}

swipes_vertical() {
    for _ in $(seq "$SWIPES"); do swipe 50 30 50 50; done
    for _ in $(seq "$SWIPES"); do swipe 50 50 50 30; done
}

# The score table's middle, below the player names and above the totals
swipes_sideways() {
    for _ in $(seq "$SWIPES"); do swipe 80 35 20 35; done
    for _ in $(seq "$SWIPES"); do swipe 20 35 80 35; done
}

# Sums how long each traced section took on the app's main thread, from atrace's text output
summarize_trace() {
    local trace="$1" pid="$2"
    awk -v tid="$pid" '
        $0 !~ /tracing_mark_write: [BE]\|/ { next }
        {
            match($0, /-[0-9]+ +\(/); thread = substr($0, RSTART + 1, RLENGTH - 1); gsub(/[ (]/, "", thread)
            if (thread != tid) next
            match($0, /[0-9]+\.[0-9]+: tracing_mark_write/); time = substr($0, RSTART, RLENGTH); sub(/:.*/, "", time)
            if ($0 ~ /tracing_mark_write: B\|/) {
                name = $0; sub(/.*tracing_mark_write: B\|[0-9]+\|/, "", name); sub(/\r$/, "", name)
                depth++; start[depth] = time; names[depth] = name
            } else if (depth > 0) {
                total[names[depth]] += (time - start[depth]) * 1000; count[names[depth]]++; depth--
            }
        }
        END { for (name in total) if (count[name] > 1) printf "%10.1f ms %7d  %s\n", total[name], count[name], name }
    ' "$trace" | sort -rn | grep -vE "Choreographer#doFrame [0-9]|deliverInputEvent|ViewPostImeInputStage" | head -15 || true
}

# Runs the swipes while recording frame stats and a trace, then prints both
measure() {
    local label="$1" swipes="$2" trace pid
    mkdir -p "$TRACE_DIR"
    trace="$TRACE_DIR/$(date +%Y%m%d-%H%M%S)-$label.trace"

    # A first pass isn't measured, since a freshly started app is still compiling code and creating views
    "$swipes"
    sleep 1

    adb shell dumpsys gfxinfo "$PACKAGE" reset >/dev/null
    adb shell atrace --async_start -b 32768 -a "$PACKAGE" view gfx res dalvik >/dev/null
    "$swipes"
    adb exec-out atrace --async_stop | sed -n '/^# tracer/,$p' > "$trace"

    echo "==> Frames"
    adb shell dumpsys gfxinfo "$PACKAGE" | tr -d '\r' |
        grep -E "Total frames rendered|Janky frames:|(50|90|99)th percentile|Number Slow UI thread" | sed 's/^/    /' || true
    echo "==> Main thread (total time, times, section)"
    pid="$(adb shell pidof "$PACKAGE" | tr -d '\r')"
    summarize_trace "$trace" "$pid"
    echo "==> Trace saved to $trace"
}

label_for() { printf '%s' "${1:-game-list}" | tr -c 'A-Za-z0-9' '-' | tr 'A-Z' 'a-z' | sed 's/--*/-/g; s/-$//'; }

case "${1:-}" in
    scroll)
        check_device; read_screen_size
        open_app "${2:-}"
        measure "scroll-$(label_for "${2:-}")" swipes_vertical
        ;;
    sideways)
        [[ -n "${2:-}" ]] || die "Name the game to scroll sideways."
        check_device; read_screen_size
        open_app "$2"
        measure "sideways-$(label_for "$2")" swipes_sideways
        ;;
    *) sed -n '3,16p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; exit 1 ;;
esac
