# First device feedback — UI iteration, v0.1.0-alpha (build 2)

CI run #4 on c06d51348 completed successfully. Maintainer reported installation, launch, navigation, light/dark and Russian screens working; English switching was missing. UI was not accepted visually. This iteration addresses:
- Explicit System / Russian / English app locale selector with AppCompatDelegate and automatic locale storage on Android <=32, platform locale config for newer versions.
- Persisted appearance preference (only nonfinancial preferences); distinct Settings and About screens.
- Material 3 surface/secondary roles, explicit navigation colors, vector icons, smaller spacing and compact alpha notice.
- Four new JVM preference mapping tests; CI publishes test/lint reports. No device results claimed for this iteration.

VersionName remains 0.1.0-alpha, versionCode 2. No release/tag. No financial storage or input yet. Test cold restart, system language changes, explicit RU/EN, back navigation, both themes, contrast and font scaling on real hardware. CI-generated debug APKs can have different signing certificates: if update install fails, uninstall prior preview (contains no financial records), then install fresh. Stable debug signing strategy is a follow-up; never commit signing secrets.

## Build 3 — second device feedback
Build 2 CI run #6 succeeded. Maintainer screenshots show RU/EN and light/dark UI, build 2 and launcher icon. Reported visible flicker when switching language, dissatisfaction with bottom navigation, and undersized launcher mark. Screenshots remain private. No visual acceptance yet.

Changes: Compose activity explicitly handles locale/layoutDirection configuration changes (Android documentation https://developer.android.com/guide/topics/resources/runtime-changes); AppCompat locale API remains the source of truth, with no-op guard, observable configuration and selection state. Window background tracks actual appearance and system bar updates run on theme changes, not every recomposition. Compact 64 dp minimum navigation row with top accent, vector gear, selected semantics and system navigation inset; no heavy pill. Separate adaptive icon layers on API 26+, monochrome layer on API 33+, foreground mark approximately 51 dp inside 108 dp canvas, within 66 dp safety zone; visual size depends on launcher mask. Separate transparent light/dark header mark. VersionName 0.1.0-alpha, versionCode 3.

Verify on device: switch RU/EN/system repeatedly without leaving Settings or visible flashes; system locale change, same-language selection, cold restart, light/dark; check navigation with gesture and three-button modes, large text/TalkBack, and launcher circle/squircle/themed icons. No claim of zero flicker until tested. CI must compile and pass tests; no new financial functionality, release or tag.
