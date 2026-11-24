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
