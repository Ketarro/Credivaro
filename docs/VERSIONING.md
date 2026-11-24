# Versioning and release policy / Политика версий

## Initial application version
- First planned application version: **v0.1.0-alpha**.
- Proposed Android versionName: `0.1.0-alpha`; Git tag convention: `v0.1.0-alpha`. Initial versionCode: 1 when the Android project is initialized; increase for each distributed build.
- This is a planned version, not an existing APK, tag or release. Document versions (requirements v1.0, architecture v1.0, branding v1.0) are independent.

## Iterative development
Alpha is the normal development state. Build, test, gather maintainer feedback, refine features and repeat. Scope, priorities, UI and architecture can change. Suggested later pre-release versions include v0.1.1-alpha for fixes and v0.2.0-alpha for significant additions; numbered pre-release iterations (e.g. v0.1.0-alpha.1) are also possible if useful. Select the next version based on actual changes, not calendar deadlines. Update CHANGELOG and record test results for each distributed build.

## Release authority
Only the repository maintainer decides that the application is ready to leave alpha and explicitly approves promotion or publication. Beta and release candidate are optional, not mandatory milestones. A first stable version such as v1.0.0 is a possible future choice, not a commitment. Automated checks, passing acceptance tests, security reviews and roadmap completion inform the decision but never replace explicit approval. No automatic stable tags, release publishing or promotion.

## Alpha safety
Alpha builds may have incomplete functionality, migrations and defects. Clearly label builds as experimental and document known limitations. Use synthetic data until data protection is implemented and verified; do not expose real financial records or suggest alpha as a sole source of payment deadlines. Before stable release, review calculation correctness, reminder reliability, migration and recovery, privacy, accessibility, localization, brand rights and distribution requirements.

## По-русски
Начинаем с v0.1.0-alpha. Постепенно дорабатываем приложение, исправляем ошибки и проверяем функции на практике. Остаёмся в предварительных версиях столько, сколько необходимо. Решение о готовности к стабильному релизу принимает владелец проекта; ни окончание этапа, ни успешная сборка сами по себе не являются разрешением на публикацию.
