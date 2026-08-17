#!/usr/bin/env bash
set -euo pipefail

readonly max_attempts=3
readonly wait_seconds=30

for attempt in $(seq 1 "${max_attempts}"); do
  if timeout "${wait_seconds}s" adb wait-for-device \
    && [ "$(adb get-state 2>/dev/null || true)" = "device" ] \
    && [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; then
    exit 0
  fi

  echo "Android device was not ready (attempt ${attempt}/${max_attempts}); reconnecting adb" >&2
  adb reconnect offline >/dev/null 2>&1 || true
  adb reconnect device >/dev/null 2>&1 || true
done

adb devices -l >&2 || true
echo "Android device did not become ready after ${max_attempts} attempts" >&2
exit 1
