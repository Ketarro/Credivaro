#!/usr/bin/env bash
# Runs on disposable CI emulators only. No physical devices or personal data.
set -euo pipefail
pkg=io.github.ketarro.credivaro
report=compatibility/reports
mkdir -p "$report"
tools="$ANDROID_HOME/build-tools/35.0.0"
api="$(adb shell getprop ro.build.version.sdk | tr -d '\r')"
printf 'api=%s\nabi=%s\n' "$api" "$(adb shell getprop ro.product.cpu.abilist | tr -d '\r')" | tee "$report/device.txt"
[[ "$api" == "$EXPECTED_API" ]]
[[ "$(adb get-serialno)" == emulator-* ]]
fingerprint=d26244a7cdf0207f6bf0f5c54ad557c89b2e1ae4357bb07ae070085de66c22c8

check_one() (
  set -euo pipefail
  local_kind="$1"
  apk="$2"
  require_signed="$3"
  dest="$report/$local_kind"
  mkdir -p "$dest"
  # Collect diagnostics before emulator teardown, including exact PackageManager errors.
  trap 'rc=$?; adb logcat -d -v threadtime > "$dest/logcat.txt" 2>&1 || true; printf "%s\n" "$rc" > "$dest/exit-code.txt"; exit "$rc"' EXIT
  sha256sum "$apk" | tee "$dest/sha256.txt"
  unzip -t "$apk" > "$dest/zip-test.txt"
  "$tools/aapt2" dump badging "$apk" | tee "$dest/badging.txt"
  "$tools/zipalign" -c -v 4 "$apk" > "$dest/alignment.txt"
  "$tools/apksigner" verify --verbose --print-certs --min-sdk-version "$api" --max-sdk-version "$api" "$apk" | tee "$dest/signature.txt"
  grep -F "package: name='$pkg'" "$dest/badging.txt"
  if [[ "$require_signed" == true ]]; then
    grep -Fx "Signer #1 certificate SHA-256 digest: $fingerprint" "$dest/signature.txt"
  fi
  # API 26 may report boot_completed before logd is fully ready.
  # Never interpret a log-clearing failure as an APK installation failure.
  cleared=false
  for attempt in 1 2 3 4 5; do
    if adb logcat -b main -b crash -c >> "$dest/logcat-clear.txt" 2>&1; then
      cleared=true
      break
    fi
    sleep 2
  done
  if [[ "$cleared" != true ]]; then
    echo "INFRASTRUCTURE: logcat buffers could not be cleared" | tee "$dest/result.txt"
    exit 2
  fi
  # Force a clean install on this isolated emulator only.
  if adb shell pm path "$pkg" | grep -q '^package:'; then adb uninstall "$pkg"; fi
  adb install "$apk" 2>&1 | tee "$dest/install.txt"
  grep -q '^Success' "$dest/install.txt"
  adb shell pm path "$pkg" | tee "$dest/package-path.txt"
  adb shell am start -W -n "$pkg/.MainActivity" | tee "$dest/launch.txt"
  grep -Eq '^Status: ok' "$dest/launch.txt"
  sleep 5
  adb shell pidof "$pkg" | tee "$dest/pid.txt"
  test -s "$dest/pid.txt"
  adb logcat -b crash -d -v brief > "$dest/crash.txt"
  if grep -Fq "Process: $pkg," "$dest/crash.txt"; then
    echo "::error::Credivaro crashed on API $api ($local_kind)"
    exit 1
  fi
  echo "PASS $local_kind API $api" | tee "$dest/result.txt"
)
# Execute each specimen independently so an original failure does not hide current results.
# Each child shell has its own errexit; do not call check_one in an if/|| context.
export pkg report tools api fingerprint
export -f check_one
status=0
for kind in original current; do
  mapfile -t apks < <(find "compatibility/$kind" -type f -name '*.apk' 2>/dev/null || true)
  if [[ "${#apks[@]}" == 0 && "$kind" == original ]]; then
    echo "Original artifact not available" | tee "$report/original-unavailable.txt"
    continue
  fi
  if [[ "${#apks[@]}" != 1 ]]; then
    echo "::error::Expected exactly one $kind APK"
    status=1
    continue
  fi
  signed="$CURRENT_SIGNED"
  [[ "$kind" != original ]] || signed=true
  set +e
  bash -c 'check_one "$@"' _ "$kind" "${apks[0]}" "$signed"
  rc=$?
  set -e
  if [[ "$rc" != 0 ]]; then status=1; fi
done
exit "$status"
