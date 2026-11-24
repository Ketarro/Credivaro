# First device feedback — UI iteration, v0.1.0-alpha (build 2)

CI run #4 on c06d51348 completed successfully. Maintainer reported installation, launch, navigation, light/dark and Russian screens working; English switching was missing. UI was not accepted visually. This iteration addresses:
- Explicit System / Russian / English app locale selector with AppCompatDelegate and automatic locale storage on Android <=32, platform locale config for newer versions.
- Persisted appearance preference (only nonfinancial preferences); distinct Settings and About screens.
- Material 3 surface/secondary roles, explicit navigation colors, vector icons, smaller spacing and compact alpha notice.
- Four new JVM preference mapping tests; CI publishes test/lint reports. No device results claimed for this iteration.

VersionName remains 0.1.0-alpha, versionCode 2. No release/tag. No financial storage or input yet. Test cold restart, system language changes, explicit RU/EN, back navigation, both themes, contrast and font scaling on real hardware. CI-generated debug APKs can have different signing certificates: if update install fails, uninstall prior preview (contains no financial records), then install fresh. Stable debug signing strategy is a follow-up; never commit signing secrets.
