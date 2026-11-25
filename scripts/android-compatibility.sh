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
  expected_code="$4"
  mode="${5:-clean}"
  dest="$report/$local_kind"
  mkdir -p "$dest"
  # Collect diagnostics before emulator teardown, including exact PackageManager errors.
  trap 'rc=$?; adb logcat -d -v threadtime > "$dest/logcat.txt" 2>&1 || true; printf "%s\n" "$rc" > "$dest/exit-code.txt"; exit "$rc"' EXIT
  sha256sum "$apk" | tee "$dest/sha256.txt"
  unzip -t "$apk" > "$dest/zip-test.txt"
  "$tools/aapt2" dump badging "$apk" | tee "$dest/badging.txt"
  "$tools/zipalign" -c -v 4 "$apk" > "$dest/alignment.txt"
  "$tools/apksigner" verify --verbose --print-certs --min-sdk-version "$api" --max-sdk-version "$api" "$apk" | tee "$dest/signature.txt"
  grep -F "package: name='$pkg'" "$dest/badging.txt" | grep -F "versionCode='$expected_code'"
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
  # Clean installs only on this isolated emulator; upgrade preserves the sandbox.
  if [[ "$mode" == upgrade ]]; then
    adb install -r "$apk" 2>&1 | tee "$dest/install.txt"
  else
    if adb shell pm path "$pkg" | grep -q '^package:'; then adb uninstall "$pkg"; fi
    adb install "$apk" 2>&1 | tee "$dest/install.txt"
  fi
  grep -q '^Success' "$dest/install.txt"
  adb shell pm path "$pkg" | tee "$dest/package-path.txt"
  adb shell am start -W -n "$pkg/.MainActivity" | tee "$dest/launch.txt"
  grep -Eq '^Status: ok' "$dest/launch.txt"
  # Retain evidence even if the process vanishes; a failed PID check must not hide the crash log.
  sleep 5
  adb logcat -b crash -d -v brief > "$dest/crash.txt" 2>&1 || {
    echo "::error::Unable to read crash buffer"; exit 2;
  }
  if grep -Fq "Process: $pkg," "$dest/crash.txt"; then
    echo "::error::Credivaro crashed on API $api ($local_kind)"
    exit 1
  fi
  if ! adb shell pidof "$pkg" > "$dest/pid.txt" 2>&1 || ! grep -Eq '[0-9]+' "$dest/pid.txt"; then
    adb shell dumpsys activity processes > "$dest/processes.txt" 2>&1 || true
    echo "::error::Credivaro process not confirmed alive on API $api ($local_kind)"
    exit 1
  fi
  cat "$dest/pid.txt"
  if [[ "$expected_code" -ge 10 ]]; then
    # Ensure this diagnostic build really opens encrypted storage, not just a live activity.
    ready=false
    for attempt in $(seq 1 30); do
      adb logcat -d -s CredivaroStartup:D > "$dest/startup.txt" 2>&1
      if grep -Fq "storage.failed" "$dest/startup.txt"; then
        echo "::error::Storage failed on API $api ($local_kind)"; exit 1
      fi
      if grep -Fq "storage.ready" "$dest/startup.txt"; then ready=true; break; fi
      sleep 1
    done
    [[ "$ready" == true ]] || { echo "::error::Storage readiness not observed"; exit 1; }
    adb shell pidof "$pkg" > "$dest/pid-ready.txt"
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
  expected="$EXPECTED_VERSION_CODE"
  [[ "$kind" != original ]] || expected="$BASELINE_VERSION_CODE"
  bash -c 'check_one "$@"' _ "$kind" "${apks[0]}" "$signed" "$expected"
  rc=$?
  set -e
  if [[ "$rc" != 0 ]]; then status=1; fi
done
# Update the known persistent baseline certificate to the current signed APK.
# No physical user data exists in this CI sandbox.
if [[ "$status" == 0 && "$CURRENT_SIGNED" == true && "$EXPECTED_VERSION_CODE" == 11 ]]; then
  mapfile -t originals < <(find compatibility/original -type f -name '*.apk' 2>/dev/null || true)
  mapfile -t currents < <(find compatibility/current -type f -name '*.apk' 2>/dev/null || true)
  if [[ "${#originals[@]}" == 1 && "${#currents[@]}" == 1 ]]; then
    bash -c 'check_one "$@"' _ upgrade-baseline "${originals[0]}" true "$BASELINE_VERSION_CODE"
    # Wait for the baseline's real encrypted database and wrapped key to exist.
    baseline_ready=false
    for attempt in $(seq 1 30); do
      if adb shell "run-as $pkg sh -c 'test -s databases/credivaro.db && test -s no_backup/credivaro.db.key.v1'"; then
        baseline_ready=true; break
      fi
      sleep 1
    done
    [[ "$baseline_ready" == true ]] || { echo "::error::Baseline database not initialized"; exit 1; }
    adb shell am force-stop "$pkg"
    adb shell "run-as $pkg sha256sum no_backup/credivaro.db.key.v1" | tr -d '\r' > "$report/baseline-key-sha256.txt"
    adb shell "run-as $pkg sh -c 'mkdir -p files && echo ci-update-marker > files/ci-update-marker'"
    bash -c 'check_one "$@"' _ upgrade "${currents[0]}" true "$EXPECTED_VERSION_CODE" upgrade
    adb shell "run-as $pkg cat files/ci-update-marker" | tr -d '\r' | grep -Fx ci-update-marker
    adb shell "run-as $pkg sha256sum no_backup/credivaro.db.key.v1" | tr -d '\r' > "$report/upgrade-key-sha256.txt"
    cmp "$report/baseline-key-sha256.txt" "$report/upgrade-key-sha256.txt"
    echo "PASS signed update, retained sandbox and wrapped key, storage ready API $api" | tee "$report/upgrade/result.txt"
  else
    echo "Update test skipped: baseline unavailable" | tee "$report/upgrade-skipped.txt"
  fi
fi
exit "$status"
