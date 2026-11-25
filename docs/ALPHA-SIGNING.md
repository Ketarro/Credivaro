# Persistent alpha APK signing — build 4

Status: configuration proposed in PR #5; no signing secrets configured or signed artifact verified by this change alone.

## Problem
Android updates require a compatible signing certificate and the same applicationId. Ephemeral CI debug keys can produce incompatible APKs. Maintainer reports repeated package conflict on update. Confirm with apksigner verify --print-certs on prior APKs if available. Do not use uninstall as a normal update process once financial data exists.

## Owner setup (one time)
On a trusted local computer with JDK installed, create a dedicated private alpha key. Keep a secure offline backup. Use a strong unique password; PKCS12 uses the same password for store and key here. Do not share key or password in chat, issues, commits, logs, or artifacts.

```sh
keytool -genkeypair -keystore credivaro-alpha.p12 -storetype PKCS12 -alias credivaro-alpha -keyalg RSA -keysize 3072 -validity 10000
```

Enter the password and certificate identity interactively. The key must be kept private. Use non-sensitive certificate identity as it may be public. With GitHub CLI authenticated locally (Linux/macOS):
```sh
base64 < credivaro-alpha.p12 | tr -d '\n' | gh secret set ALPHA_KEYSTORE_B64 --repo Ketarro/Credivaro
gh secret set ALPHA_KEYSTORE_PASSWORD --repo Ketarro/Credivaro
```
Second command prompts for the password. Alternatively add the Base64-encoded file and password via repository Settings > Secrets and variables > Actions > New repository secret. Base64 is not encryption; protect the encoded data just like the private key. Never commit the p12 file (add ignore if needed). 

## CI behavior
Regular pull_request CI never receives signing credentials. Trusted pushes and manual dispatches on main or feat/android-alpha-bootstrap can sign the tested APK using the Android SDK apksigner, with temporary key file removed on exit. The workflow verifies the resulting APK and prints only public certificate fingerprints, then uploads a separately named signed artifact. If neither secret is present, temporary debug build is available, clearly labeled; if only one is present or signing fails, signing fails rather than silently producing a persistent artifact. Review all code on trusted branches before running with signing secrets; do not grant contributors permission to modify trusted workflows or read secrets. Configure branch protections and consider a restricted GitHub Actions environment before broader collaboration.

## Transition and test
The first persistently signed APK may still need one clean install because old builds were signed with temporary keys. Current build 3 has no financial records. Uninstalling removes local settings; do not do this with real data. Thereafter install only persistently signed artifacts with increasing versionCode and unchanged applicationId. Compare SHA-256 certificate digests across at least two signed CI builds, test updating without uninstall, and confirm preferences survive. Do not claim update issue fixed until actual device test passes. A debug APK is still experimental even with a persistent signature. Do not enter real financial data until storage protection is verified. Decide stable release and Google Play signing strategy separately with owner, including continuity of app ID and signing key; never reuse this key automatically for a public release.

References: https://developer.android.com/studio/publish/app-signing ; https://developer.android.com/tools/apksigner ; https://docs.github.com/en/actions/how-tos/write-workflows/choose-what-workflows-do/use-secrets

## Verified baseline and continuity check
Build 4 CI run #9 attempt 2 successfully signed and verified the APK using v2/v3. Public signer certificate SHA-256: `d26244a7cdf0207f6bf0f5c54ad557c89b2e1ae4357bb07ae070085de66c22c8`. Maintainer confirms signed Build 4 runs. Build 5 CI compares the verified signer digest to this baseline and rejects mismatches before signed artifact upload. This is the certificate fingerprint, not the APK or ZIP hash, and contains no private key. Update test on a real device remains required. Keep Build 4 installed; select non-default appearance and language, install signed Build 5 over it, confirm Build 5 and both settings remain. If install fails, report error and do not uninstall as a workaround.

Build 5 CI #11 (45320460175) confirmed matching certificate and signed artifact; maintainer verified updating Build 4 to Build 5 without uninstall or loss of preferences. Signing continuity accepted for this pair. Keep the certificate guard for subsequent builds.

## Политика постоянной подписи — согласовано владельцем 2026-11-25
Каждая тестовая сборка из любой ветки собственного репозитория с обновлённым workflow использует один существующий alpha key и неизменный applicationId, при успешных debug и storage tests. Не выдавать временный APK как обычную сборку для обновления. PR (включая fork) без signing credentials; push/dispatch в собственном репозитории — отдельный sign job без checkout/Gradle, использует точный APK данного run, существующие секреты и проверяет fingerprint Build 4; отсутствие ключа или mismatch = failure. Signed candidate для матрицы, итоговый signed artifact только после успешной матрицы и проверки SHA-256. Не публиковать релиз/тег/merge. Доверие к авто-подписанию любой ветки предполагает доверенных авторов с правом push/workflow edits; repo secrets не изолируют произвольный изменённый workflow. Защищённый environment с required reviewer и ограничение прав — рекомендуемое отдельное усиление, не выполнено. Не показывать секреты. Исторические ветки сохраняются, новый порядок действует в ветках с этой редакцией. Baseline Build11 #49, пока нет более нового verified signed artifact; проверять доступность и обновлять baseline только после фактического подтверждения. Матрица не доказывает полный populated upgrade/recovery или физическое принятие.
