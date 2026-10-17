#!/usr/bin/env bash
# Installs an APK on the running emulator, launches it, and fails if it
# crashes. Leaves smoke-screen.png and smoke-logcat.txt for the CI artifact.
#
#   tools/smoke.sh app/build/outputs/apk/release/app-release.apk
set -uo pipefail
apk="$1"
adb install -r "$apk" || exit 1
adb logcat -c
adb shell am start -W -n app.katachi/.MainActivity
sleep 20
adb exec-out screencap -p > smoke-screen.png
adb logcat -d > smoke-logcat.txt
pid="$(adb shell pidof app.katachi | tr -d '\r')"
if [ -z "$pid" ] || grep -qE "FATAL EXCEPTION|Fatal signal" smoke-logcat.txt; then
    echo "Katachi crashed or is not running:"
    grep -A60 -E "FATAL EXCEPTION|Fatal signal" smoke-logcat.txt | head -120
    exit 1
fi
echo "Katachi is running (pid $pid)"
