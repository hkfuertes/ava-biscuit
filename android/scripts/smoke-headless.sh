#!/usr/bin/env bash
# Normal-app smoke check for the CM12.1 voice appliance.
set -euo pipefail

PKG="${AVA_PACKAGE:-net.mfuertes.biscuit.ava}"
# ponytail: source namespace stays com.example.ava (project convention); only the installed
# package ($PKG) is runtime-configurable via applicationId.
RECEIVER="com.example.ava.receivers.AvaControlReceiver"
PORT="${AVA_PORT:-6503}"
ADB_BIN="${ADB:-adb}"
SERIAL="${ANDROID_SERIAL:-}"
APK=""
REBOOT=0
WAIT_SECONDS=8
FAILS=0

adb_cmd() {
    if [ -n "$SERIAL" ]; then
        "$ADB_BIN" -s "$SERIAL" "$@"
    else
        "$ADB_BIN" "$@"
    fi
}

adb_shell() {
    adb_cmd shell "$@"
}

pass() { printf '[PASS] %s\n' "$*"; }
warn() { printf '[WARN] %s\n' "$*" >&2; }
fail() { printf '[FAIL] %s\n' "$*" >&2; FAILS=$((FAILS + 1)); }

usage() {
    cat <<'EOF'
Usage: android/scripts/smoke-headless.sh [options]

Installs (optionally) and checks the headless appliance without a launcher UI.
It starts VoiceSatelliteService through the package-derived control action and
verifies its ESPHome TCP listener.

Options:
  --apk <path>       Install this normal APK before checking
  --port <port>      ESPHome listener port (default: AVA_PORT or 6503)
  --serial <serial>  Target an ADB device
  --reboot           Reboot and repeat the service/listener checks
  --wait <seconds>   Service/boot wait (default: 8)
  --self-check       Validate this script only
  -h, --help         Show this help
EOF
}

self_check() {
    bash -n "$0"
    [[ "$PORT" =~ ^[0-9]+$ ]] && [ "$PORT" -gt 0 ] && [ "$PORT" -lt 65536 ]
    [[ "$WAIT_SECONDS" =~ ^[0-9]+$ ]] && [ "$WAIT_SECONDS" -gt 0 ]
    printf 'self-check PASS\n'
}

while [ "$#" -gt 0 ]; do
    case "$1" in
        --apk) APK="${2:?missing APK path}"; shift 2 ;;
        --port) PORT="${2:?missing port}"; shift 2 ;;
        --serial) SERIAL="${2:?missing serial}"; shift 2 ;;
        --reboot) REBOOT=1; shift ;;
        --wait) WAIT_SECONDS="${2:?missing wait}"; shift 2 ;;
        --self-check) self_check; exit 0 ;;
        -h|--help) usage; exit 0 ;;
        *) printf 'Unknown option: %s\n' "$1" >&2; usage >&2; exit 2 ;;
    esac
done

if ! [[ "$PORT" =~ ^[0-9]+$ ]] || [ "$PORT" -le 0 ] || [ "$PORT" -ge 65536 ]; then
    printf 'Invalid port: %s\n' "$PORT" >&2
    exit 2
fi

if ! [[ "$WAIT_SECONDS" =~ ^[0-9]+$ ]] || [ "$WAIT_SECONDS" -le 0 ]; then
    printf 'Invalid wait: %s\n' "$WAIT_SECONDS" >&2
    exit 2
fi

command -v "$ADB_BIN" >/dev/null 2>&1 || {
    printf 'ADB not found: %s\n' "$ADB_BIN" >&2
    exit 2
}

if [ -n "$APK" ]; then
    [ -f "$APK" ] || { printf 'APK not found: %s\n' "$APK" >&2; exit 2; }
    adb_cmd install -r "$APK"
fi

package_path="$(adb_shell "pm path '$PKG'" 2>/dev/null || true)"
if [[ "$package_path" == package:* ]]; then
    pass "normal package installed: $PKG"
else
    fail "package not installed: $PKG"
fi

# pm resolve-activity isn't reliably present/correct on CM12.1's API22 pm(1); monkey's launcher
# resolution is available since Android 1.x and works the same way there.
monkey_output="$(adb_shell "monkey -p '$PKG' -c android.intent.category.LAUNCHER 1" 2>&1 || true)"
if grep -Fq 'No activities found to run' <<<"$monkey_output"; then
    pass "no launcher activity"
else
    fail "launcher activity is still exposed: $monkey_output"
fi

# Android 5 grants this at install time; newer test devices need an explicit grant.
adb_shell "pm grant '$PKG' android.permission.RECORD_AUDIO" >/dev/null 2>&1 || \
    warn "RECORD_AUDIO grant was not changed (already granted or device policy blocked it)"

# Explicit component (-n) instead of -p: CM12.1/API22 am(1) does not reliably support
# broadcast package-restriction, and explicit targeting bypasses that entirely.
start_output="$(adb_shell "am broadcast -a '${PKG}.ACTION_START_SERVICE' -n '$PKG/$RECEIVER'" 2>&1 || true)"
if grep -qE 'Broadcast completed: result=[0-9]+' <<<"$start_output"; then
    pass "package-derived service action sent"
else
    fail "could not send service action: ${start_output:-no output}"
fi

wait_for_boot() {
    local waited=0
    while [ "$waited" -lt "$WAIT_SECONDS" ]; do
        if [ "$(adb_shell getprop sys.boot_completed 2>/dev/null || true)" = "1" ]; then
            return 0
        fi
        sleep 1
        waited=$((waited + 1))
    done
    return 1
}

check_service_and_listener() {
    sleep "$WAIT_SECONDS"

    local services tcp port_hex
    services="$(adb_shell "dumpsys activity services '$PKG'" 2>/dev/null || true)"
    if grep -Fq 'VoiceSatelliteService' <<<"$services"; then
        pass 'VoiceSatelliteService is running'
    else
        fail 'VoiceSatelliteService is absent from dumpsys'
    fi

    # A LISTEN socket is the observable ESPHome reachability contract; no HA instance is needed.
    port_hex="$(printf '%04X' "$PORT")"
    tcp="$(adb_shell 'cat /proc/net/tcp /proc/net/tcp6 2>/dev/null' 2>/dev/null || true)"
    if awk -v port="$port_hex" '
        toupper($2) ~ ":" port "$" && toupper($4) == "0A" { found = 1 }
        END { exit found ? 0 : 1 }
    ' <<<"$tcp"; then
        pass "ESPHome listener reachable on TCP $PORT"
    else
        fail "ESPHome listener is not visible on TCP $PORT"
    fi
}

check_service_and_listener

if [ "$REBOOT" -eq 1 ]; then
    adb_cmd reboot
    if wait_for_boot; then
        pass 'device rebooted'
        check_service_and_listener
    else
        fail "device did not boot within ${WAIT_SECONDS}s"
    fi
fi

if [ "$FAILS" -eq 0 ]; then
    printf 'RESULT: PASS\n'
else
    printf 'RESULT: FAIL (%s check(s))\n' "$FAILS" >&2
    exit 1
fi
