# Build16 — финансовые события
Статус: код подготовлен, CI ожидается. Только синтетические данные. [Чек-лист Build15](BUILD-15.ru.md) сохраняется. Без удаления прежней установки; несовместимый временный сертификат не исправлять удалением. Не устанавливать неподтверждённый signed artifact.

## Автоматические проверки
24 JUnit: leap/non-leap 29–31, CLAMP/SKIP, last day, intervals, weekly multiple weekdays, explicit date IDs, half-open window, overrides move/cancel across boundaries, stable IDs, rule versions, invalid inputs, bank zone, unknown money, keep/upsert/retire/conflicts, old terms mapping.

## Проверка на устройстве
- [ ] C01: версия Build16; Настройки → О приложении → Автотестирование → Запустить → скопировать полный отчёт 32/32, complete=true. CAL01 конец месяца/високосный, CAL02 повторы/unknown, CAL03 перенос/отмена, CAL04 ID/история, CAL05 bankZone/границы, CAL06 суммы/валидация.
- [ ] C02: повторить 32 проверки, убедиться в неизменности синтетических записей и отсутствии дублей. Убедиться, что вкладки и Build15 формы/календарные поля работают.
- [ ] C03: проверить чистую установку/подписанное обновление только после соответствующего допуска, force-stop/reboot, синтетический CRUD и платежи, RU/EN, темы, два устройства.
- [ ] C04: прислать модель/API, CLEAN/UPDATE, PASS/FAIL/SKIP/BLOCKED и копию отчёта. Не считать отсутствующий полноценный календарь или уведомления ошибкой этого ограниченного этапа: они ещё не реализованы.

Для следующего этапа: транзакционное хранение actual rule revisions, events, exceptions, links и outbox; review/recovery и migration gate при изменении схемы. Полный CAL-01–04/T08–T12 не принят.

CI #68 Build14 PR SUCCESS (debug, API26/36; sign/compatibility skipped). CI #70 Build15 debug SUCCESS, API26 app run 6 tests with 1 failure: StorageUiTest.discardUnsavedHomeForm attempted performScrollTo on the now-fixed header Back button without a scrollable parent. API36 still running when inspected. Build15 fix 040e80a5 and same fix in Build16: replace the two stale scroll actions with assertIsDisplayed before click; discard dialog, retained draft, and no saved record assertions remain. No product code changes or weakened criteria. Re-run clean CI; signing push results still unverified.
