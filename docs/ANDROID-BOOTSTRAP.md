# Android bootstrap — v0.1.0-alpha

Experimental source foundation; not a release. Initial versionName 0.1.0-alpha, versionCode 1, applicationId io.github.ketarro.credivaro. Min SDK 26 (Android 8.0) proposed for review; compile/target SDK 36 for development, not a claim of store eligibility.

Toolchain: JDK 17, Gradle 8.13, AGP 8.13.2, Kotlin 2.3.0 (matching Compose compiler plugin), Compose BOM 2025.10.00. References: https://developer.android.com/build/releases/agp-8-13-0-release-notes and https://developer.android.com/build/kotlin-support.

Use Android Studio with compatible AGP, JDK 17 and Android SDK 36. The Gradle wrapper files/JAR are not committed in this bootstrap; CI uses explicitly installed Gradle 8.13. With installed Gradle 8.13, run:
```sh
gradle :core:model:test :app:lintDebug :app:testDebugUnitTest :app:assembleDebug
```
Generate and validate the official Gradle wrapper in a follow-up with a trusted Gradle installation (`gradle wrapper --gradle-version 8.13`); commit all generated files after verification. No hand-written wrapper substitutes.

CI uploads a debug APK only after checks succeed; this is a CI artifact, not a GitHub Release. No signed stable build or tag. CI run and device test status must be checked separately.

Implemented scope: launchable-screen source, Home/About navigation, system/light/dark theme for current activity session, system-locale English/Russian resources, preliminary Signal Frame vector icon, exact minor-unit arithmetic with five JVM test cases. No storage, obligations, payments, reminders or forecast yet. No INTERNET permission. Backup disabled/exclusion rules provided as defense in depth; these do not prove storage security. No real financial data. Full brand kit import, adaptive launcher icon, persisted theme, Room encryption design, and feature implementation remain open.

Next: verify CI, add official wrapper and review initial screen on device, then implement the typed obligation vertical slice. Maintainer alone authorizes releases and version promotion.
