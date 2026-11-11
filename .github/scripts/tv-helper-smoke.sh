#!/usr/bin/env bash
set -euo pipefail
apk="$(find app/build/outputs/apk/debug -name '*.apk' -print -quit)"
helper="${RUNNER_TEMP}/skyadb-helper.dex"
labels="${RUNNER_TEMP}/skyadb-labels.txt"
remote=/data/local/tmp/skyadb-helper-smoke.dex
unzip -p "$apk" assets/classes.dex > "$helper"
test -s "$helper"
adb push "$helper" "$remote"
trap 'adb shell rm -f "$remote"' EXIT
adb shell "chmod 444 '$remote' && CLASSPATH='$remote' app_process /system/bin com.sky22333.skyadb.helper.TvAppLabels" | tr -d '\r' | tee "$labels"
grep -q '^SKYEND$' "$labels"
grep -q $'^SKYAPP\tcom.android.settings\t' "$labels"
# Installing/launching catches manifest or startup regressions too.
adb install -r "$apk"
adb shell am start -W -n com.sky22333.skyadb/.MainActivity
sleep 3
! adb logcat -d -b crash | grep -q 'Process: com.sky22333.skyadb'
adb shell cat /proc/meminfo | grep MemTotal
adb shell dumpsys meminfo | grep 'Total PSS by process:'
