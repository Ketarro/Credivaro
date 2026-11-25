# Credivaro — дорожная карта первого выпуска v0.1.0-alpha

Актуально: 2026-11-25. Build16 — feat/build16-event-engine, versionCode 16; Build15 и Build14 сохранены отдельно. CI/signing Build16 SUCCESS; Build17 preparation on separate branch.

## Объём выпуска
Согласованный владельцем объём: обязательства, защищённое хранение, платежи, сверка, календарь, напоминания, настройки, базовая сводка/аналитика и контроль качества. Полные прогнозы, сравнительные стратегии (лавина, снежный ком и др.), защищённый переносимый backup/export/import, интеграции банков, AI-импорт и облако — отдельный backlog после 0.1.0-alpha; включать только по согласованному расширению карты. Базовая аналитика R07 — ограниченное дополнение к MVP, не полноценные стратегии. Владелец может изменять приоритеты и объём; нельзя переносить обязательные условия безопасности без явного решения.

## Следующая версия — исследование (вне alpha)
По решению владельца 2026-11-25: исследовать доступность официальных API банков для кредитных данных физлиц, условия доступа, разрешения и тестовые среды. Реализация только после подтверждения доступности и отдельного согласования; текущий объём alpha и 68 обязательных задач не меняются.

## Build 17 — подготовка хранения событий
2026-11-25: Build 16 baseline c5aaad22 — CI [push](https://github.com/Ketarro/Credivaro/actions/runs/45536421242) SUCCESS, debug/storage API26/36, persistent signing, compatibility/upgrade API26/28/29/30/36, final signed artifact 14987124149. Physical acceptance, independent review and populated recovery remain open. Build 17 preparation: feat/build17-event-storage, R05.03, additive candidate DDL and isolated encrypted fixture test; NOT wired to Room, versionCode remains 16, no new APK authorized. Next: typed codec/DAO, transactional reconciliation and link validation, actual v3 fixture and compiler-exported v4 schema, migration/recovery evidence and independent review. No checklist closure.

2026-11-25 — R05.03 / Build17 preparation: added versioned typed EventCodec for all recurrence shapes and event payloads, exact minor units as decimal strings, canonical UUID/date decoding, unknown version rejection; Android instrumentation roundtrip and invalid-payload tests added. Previous candidate CI 45538015908 in progress at inspection; new tests not yet verified. No Room activation or versionCode change. Next: transactional repository, same-owner payment links, actual v3 fixture and migration schema. 17/68 unchanged.

2026-11-25 — R05.03: isolated transactional event repository candidate added (append/replay rules, generation/reconciliation, soft retirement, explicit same-owner/same-currency mandatory-payment links; corrected payments rejected, protected conflicts stop writes). Synthetic encrypted repository test covers replay, overlap/conflicting revision rejection, linking and reopen. No production Room registration, no outbox or manual override editing yet, no versionCode bump. CI 45538427301 was in progress; 45538015908 cancelled by subsequent push. Tests for this commit pending. Roadmap remains 17/68. Next: actual v3 fixture, Room mapping, migration, correction-aware link lifecycle, override editing and outbox.

2026-11-25 — R05.03/R03.06: added separate Room v4 compiler/fixture target EventMigrationCandidateDatabase, five typed entities and explicit additive migration 3→4; adjusted DDL to named unique indexes matching Room. Instrumentation test builds populated actual v3 using existing Room implementation and same synthetic key, migrates, checks prior obligation/payment/zero, integrity/FK, event replay/link and reopen. Production StorageDatabase remains v3; versionCode remains 16. New Room export must be captured from compiler (schema guard may fail until tracked); guard unchanged. Fresh Room schema lacks SQL CHECK constraints present in migrated candidate, so repository enforcement/parity needs review before activation. CI 45539025297 in progress at inspection; earlier CI cancelled by subsequent pushes. No verified new APK. 17/68. Next: capture compiler schema, validate migration/recovery and parity, finish override/outbox and only then activate v4.

2026-11-25 — CI [45539272598](https://github.com/Ketarro/Credivaro/actions/runs/45539272598) FAIL on 68928ba. ROADMAP consistency PASS. Debug and API36 failed :core:database:compileDebugAndroidTestKotlin: EventMigrationCandidateTest lines 32/42 used Closeable.use on RoomDatabase which does not implement that receiver. Fixed explicit try/finally close for both handles; assertions unchanged. API26 failed dependency resolution of KSP 2.3.6 before compilation; retry unchanged dependencies (same KSP resolved in debug/API36), underlying repository/network cause unconfirmed. No instrumentation execution or signing; schema guard not reached. New CI required, do not interrupt it. Production DB/versionCode unchanged. 17/68, no acceptance. Compiler-generated v4 export still needs tracking.

2026-11-25 — CI [45540911782](https://github.com/Ketarro/Credivaro/actions/runs/45540911782) on df6966c: debug Check and build PASS, roadmap PASS, API26 and API36 storage jobs PASS; debug failed at schema guard because candidate v4 export untracked. Extracted actual compiler export from debug job 138958911349; identityHash 7a286fb7d862b063c029a85b65101087, all five preexisting v3 entities structurally identical, five added event entities. Existing schema files unchanged by this commit. Guard intact, rerun required. Production DB stays v3/versionCode16; no distribution; full recovery, independent review, override/outbox and correction lifecycle open. 17/68.

2026-11-25 — R05.03: baseline CI [45541750692](https://github.com/Ketarro/Credivaro/actions/runs/45541750692) SUCCESS (debug/schema guard, API26/36 storage, signing and API26/28/29/30/36 compatibility; artifact 14990286480 is Build16, not Build17). Added isolated transactional explicit override edit/move/cancel/clear with optimistic snapshot, canonical slot regeneration, future 12-month bound, linked/historical/retired protection, atomic override and occurrence update; no balance changes. Expanded encrypted fixture test for stale edits, invalid past move, cancel/link rejection, clear, linked rejection and reopen. New CI pending; no local Android build. Outbox, correction lifecycle, parity/recovery, independent review and production activation remain open. No versionCode bump, 17/68.

2026-11-25 — R05.03: CI [45602820517](https://github.com/Ketarro/Credivaro/actions/runs/45602820517) SUCCESS on 02c2a0e (debug, API26/36 storage, signing and compatibility matrix). Added isolated correction-aware audit link projection: ACTIVE/CANCELLED/REPLACED with explicit replacement ID; no implicit transfer or deletion, corrected historical links continue protecting occurrences. Synthetic encrypted test covers replacement, explicit relink, subsequent cancellation, reopen and protected edit. No schema or production change. New CI pending, no local Android build/checker execution. Next: integrate atomic correction/outbox lifecycle in candidate, schema parity and recovery; independent review remains open. 17/68, versionCode16.

## Подготовка Build 12 (отдельная ветка)
Build 12 — подготовительный этап R04.01 в отдельной ветке `feat/build12-payment-core`: по поручению владельца ручная проверка Build 11 отложена, R03.05 не закрыт. Добавлены доменные типы платежа и распределения, 11 сценариев локального JVM smoke (PASS); JUnit/Android CI не запускались. Room v1, UI, versionCode, подпись и CI без изменений. APK Build 12 ещё не создан. Платёжная персистентность и миграция требуют отдельного безопасного допуска и тестов; никаких действий с реальными данными. 17/68 (25.0%).

Повторная автоматическая проверка 2026-11-24 (код 6d240ab): локальный JVM 32/32 исходных тестовых методов через независимый упрощённый runner (не JUnit runtime), дополнительно 20 008/20 008 синтетических сценариев. Git blob SHA всех 7 файлов Kotlin совпали; текстовые инварианты ROADMAP/HANDOFF проверены, 17/68. Android CI, миграция, APK, физическая приёмка not_run. [Отчёт](docs/testing/BUILD-12.ru.md).

Build 12 / этап хранения (R03.06/R04.01): подготовлены PaymentRow, DAO, транзакционный PaymentRepository и добавочная миграция Room v1→v2 в отдельной ветке. SQL на синтетической обычной SQLite 16/16 PASS, в том числе rollback, FK/unique и reopen. Android SQLCipher/Room, KSP и подписанный upgrade не запускались; экспорт 2.json ещё не получен от Room compiler. versionCode 11, UI/ключи/CI неизменны. Не собирать и не распространять APK до проверки миграции, предупреждения об удалении истории и независимого review. R03.05 отложен по поручению владельца; 17/68 без изменений.

CI #45479966371 (SHA 0097fc30): Gradle compilation/lint/JVM tests PASS, instrumented storage jobs API26/API36 PASS. Общий debug job FAIL на schema guard: новая 2.json не отслеживалась Git. Экспорт Room v2 извлечён из журнала компилятора; старый экспорт v1 совпал побайтно, три старые таблицы совпадают структурно, добавлена payment. Guard не изменён. Повторный CI требуется; APK не распространялся, versionCode 11, 17/68.

Актуальная проверка кандидата Build 12: [Android CI #52](https://github.com/Ketarro/Credivaro/actions/runs/45482462046), SHA d25024cdc9e319d9ddea37dc8c1e54c5de2d0b01 — SUCCESS. Gradle check/build (JVM tests, lint, APK и androidTest compilation), штатная проверка ROADMAP и Room schema guard PASS. Инструментальные тесты API26 и API36: по 7 storage + 3 app, всего 20/20 выполнений PASS. Временный debug-APK создан, versionCode остаётся 11; постоянная подпись и compatibility/upgrade matrix SKIPPED по правилам PR. Не устанавливать на рабочие данные и не считать подписанным Build 12. Независимый review, полный migration failure/recovery, UI платежей, подписанный upgrade и физическая приёмка остаются открытыми; R03.05/R03.06/R04.01 не закрыты, 17/68 (25.0%).

Build 12 UI candidate (R04.02/R04.03): журнал фактических платежей в карточке обязательства, форма даты/суммы/частей/примечания RU/EN; остаток явно unallocated, банковский снимок не меняется. Повтор после ошибки использует тот же operationId; hard-delete предупреждает об удалении платежей. Добавлены 10 JVM unit tests и 1 UI instrumentation test; CI ещё не получен. Корректировки, события, расчётный остаток, recovery, исполнение UI instrumentation и review впереди; версия 11, подписанного Build 12 нет. Ручная приёмка отложена. 17/68.

CI #54 (45485443922, e968bf9): debug compile/JVM/lint/schema PASS, API36 PASS; API26 7 storage tests PASS, 2/4 app UI PASS. PaymentUiTest: Home node absent after saving payment; StorageUiTest: timeout 30s, exact wait not identified in log. Likely IME transition and conditional dashboard Add (leftover synthetic fixture), not proven root causes. Candidate fix: clear focus/hide IME on valid payment save and cancel, wait for navigation after dialog dismissal, start both UI tests through obligations nav without assuming empty DB. All original assertions and 30s limits retained. Re-run required; R09.03 open. 17/68, no signed APK.

CI #55 (45489811714, 8f1c179): SUCCESS, debug compilation/JVM/lint/schema/ROADMAP PASS; API26/36 each 7 storage + 4 app, total 22/22. Compatibility and persistent signing skipped in PR. R09.03 remains open pending full matrix. Next candidate R04.04: append-only correction/cancellation model, transactional repository, active-only query plus full history query, additive Room migration 2→3 retaining v1/v2, 2 additional encrypted instrumentation tests. UI actions/status not yet wired. New v3 export must be captured from compiler; schema guard expected to fail until checked in, never disable it. CI pending. No real DB migration, signed APK, version bump, or merge. 17/68.

CI #56 (45515399942, e46dbd4): Gradle compilation/JVM/lint/ROADMAP PASS, API26/API36 по 9 storage + 4 app, всего 26/26 PASS, включая два новых теста корректировки. Debug FAIL только на schema guard: экспорт 3.json не отслеживался Git. Добавлен фактический экспорт Room v3 из журнала компилятора (identityHash 6ab97d3eb255241f1eac1d6f426ec257); экспорты v1/v2 и четыре прежние таблицы неизменны, добавлена payment_correction. Guard сохранён. Повторный CI необходим. UI отмены/исправления, отдельная заполненная v2 fixture, injected rollback, независимый review и подписанный upgrade ещё впереди. versionCode 11, релиза нет, 17/68.

Текущий кандидат Build 12: [CI #57](https://github.com/Ketarro/Credivaro/actions/runs/45516514318) SUCCESS на 8af6c05; debug, API26/36 PASS, compatibility/signing skipped. R04 фактически в работе (0/7 принято); сводка ниже вычисляется по принятым чекбоксам. UI коррекции, recovery/review, signed upgrade и ручная приёмка открыты.

Build 12 diagnostic candidate (R03.05–08/R04.01–04/R09.07): по запросу владельца versionCode 12, расширена одна кнопка самопроверки до 26 сценариев (10 FIN, 8 DB, 5 PAY, 3 MIG). Синтетические изолированные базы и ключи, 1000 генераций внутри FIN10, повтор/конкуренция, коррекции, fixture v1/v2→v3, DDL rollback/retry, отсутствие envelope/alias; отчёт с моделью/API/ABI/page size и статусами. Не считать ещё пройденными: CI pending; UI коррекции не добавлен, disk-full/kill/recovery/review и реальный signed upgrade открыты. [Ручной чек-лист](docs/testing/BUILD-12-MANUAL.ru.md) 44 сценария; физическая проверка по отдельному допуску. Не распространять непроверенный APK; 17/68.

CI #61 (45518679220, 9c247cc): первая попытка debug (JVM/lint/compile/schema guard) и API36 PASS, API26: 9 storage PASS, PaymentUiTest аварийно завершился с Compose Modifier.Node.runDetachLifecycle IllegalStateException при уничтожении Activity; оставшиеся app tests не завершены. Повтор только упавшего job 138889016198 на том же SHA: 9 storage + 4 app PASS, BUILD SUCCESSFUL 5m20s; итог #61 SUCCESS. API36 9+4 PASS; SelfCheckTest на каждом API подтвердил все 26 диагностических сценариев (52 выполнения). Первопричина нестабильности не доказана, R09.03 открыт. По запросу владельца 2026-11-25 запрошена полная чистая перекомпиляция Build 12: Gradle clean --no-build-cache в debug и обоих storage jobs, без изменения тестов, зависимостей, schema guard, trusted signing policy или versionCode. Новый PR CI должен заново собрать и проверить приложение, результаты ожидаются. Signed upgrade/compatibility в PR skipped, независимый review/recovery и ручная приёмка открыты. 17/68.

CI #62 (45521114232, beeac785) SUCCESS, clean --no-build-cache, debug/schema/roadmap PASS, 9 storage + 4 app на каждом API26/36, 26/26 self-check на каждом, 52 выполнения; прежний Compose crash не повторился, причина не установлена, R09.03 открыт. Временный APK artifact 14980042131, signing/compatibility skipped. Независимый review/recovery и signed upgrade открыты.

2026-11-25 — частичный ручной отчёт владельца Xiaomi 17 / Android 16 / 0.1.0-alpha Build 12. CLEAN: удалил старую установку и поставил временный APK из-за несовместимости подписи; APK hash/API числом не сообщены, прежние данные не сравнивались. M01 PASS по сообщённой подготовке (синтетическая запись, наличие прежних реальных данных не уточнено), M02 CLEAN, M03/M05 не проверены (upgrade blocked), M04 PASS, M06 запущен, отчёт ожидается, M07 не проверен, M08 функционально PASS (назад, вкладки, без зависания), но навигация субъективно неудобна, M09 PASS по сообщению: B12 loan, Тестбанк, RUB, остаток 10000, плановый платёж 1050 (отличие от примера допустимо), ставка 12.5, дата введена (значение не сообщено), примечание как в мануале, карточка сохранилась, журнал пуст. M10–M44 не проверены. Четыре PASS, без закрытия задач ROADMAP. UX-задача предложена отдельно, не регрессия и не утверждённый редизайн. Не удалять приложение повторно, только синтетика; сохранность при обновлении не доказана. Следующее: получить копируемый self-check, продолжить M10, согласовать UX.
[Предложение UX](docs/checkpoints/R09.06.md).

UX: расширенное сравнение финансовых приложений, три концепции «Сегодня» / «Мой кошелёк» / «План месяца», рекомендуемый гибрид и критерии удобства — [R09.06](docs/checkpoints/R09.06.md), ожидают выбора владельца; без реализации/закрытия задач.

2026-11-25: владелец утвердил гибрид UX и отдельную сборку для теста. Ветка feat/build13-navigation от c8e20289 (CI #64 SUCCESS); versionCode 13. Главная с быстрым добавлением и записью платежа, ближайшая известная будущая дата без прогнозирования/сверки, раздельные валюты; список с поиском по названию/банку, фильтрами типа/валюты и быстрым платежом; карточка Обзор/Платежи/Условия, меню действий, независимая прокрутка, сохранение route/фильтров, подтверждение выхода из изменённой формы. Форма создания: базовые поля и раскрываемые условия; платёж: сумма, дата, выбранное обязательство, необязательные детали; после сохранения история, исправление/отмена с причиной, прежние факты и банковский снимок сохранены. RU/EN, темы и 26 диагностических случаев сохранены. Добавлен тест отказа от несохранённого ввода, расширены StorageUiTest/PaymentUiTest (создание с главной, восстановление, история коррекции/отмены); CI pending. Схема/репозиторий/ключи/подпись не менялись. Полный календарный движок, аналитика и архивирование не реализованы; не добавлены фиктивные вкладки. Нет подтверждения сохранения черновика платежа после process death, физической доступности и удобства одной рукой. 17/68, не закрывать R04.04/R09.06/R09.07/R09.03. Ветка Build 12 остаётся для приёмки.

CI #65 — SUCCESS, 2026-11-25, проверенный SHA 47b99c8ccaa270bffc1a5881febc77b9d81bd5a4, [run](https://github.com/Ketarro/Credivaro/actions/runs/45531415532). Debug 138927391880: clean compile, unit/lint/schema guard/roadmap PASS, BUILD SUCCESSFUL 2m55s; API26 138927392229: 9 storage + 5 app PASS (5m17s); API36 138927392371: 9+5 PASS (3m36s). Всего 28/28 instrumentation, SelfCheckTest проверяет 26 диагностических сценариев на каждом API. Ошибок в выполненных job нет. [Временный APK](https://github.com/Ketarro/Credivaro/actions/runs/45531415532/artifacts/14985461987) существует, не истёк, только для чистой установки на отдельном тестовом устройстве. Постоянная подпись и install/upgrade matrix skipped; независимый review/recovery и физическая приёмка не выполнены. Не удалять существующее приложение ради установки, не использовать реальные данные. R09.03/R09.06/R04.04/R09.07 не закрывать; 17/68. Следующее: ручная проверка UX и отчёт автотестирования; безопасный подписанный upgrade отдельно.

2026-11-25: владелец явно согласовал постоянную подпись для любых веток и улучшение выбора дат/форм. Build 14 (versionCode 14, versionName 0.1.0-alpha) в feat/build13-navigation / PR #10; база 023b14d, CI #65/#66 SUCCESS. CalendarField Material3 вместо ручных дат в обязательстве и платеже, локализованная дата, необязательную дату можно убрать, сегодня/вчера в банковской зоне, фактическая дата ограничена открытием и сегодня; UTC round-trip и границы в 2 unit tests, UI открытие/отмена/подтверждение и shortcuts. Валюта с названием, подтверждение очистки введённых сумм при смене, без конверсии; видимый расчёт unallocated в раскрываемых деталях. Финансовый валидатор, схема, репозиторий, applicationId, ключ неизменны. Подписание в обновлённом workflow на push/dispatch любой ветки собственного репозитория после debug+storage, никогда PR; отдельный job без checkout/Gradle, секреты только в шаге подписания, прежний fingerprint, fail-closed при отсутствии ключа; candidate проверяется в матрице API26/28/29/30/36 и только при успехе создаётся финальный signed test artifact. Baseline Build 11 artifact 14934959781 проверен доступным, actual signed upgrade script теперь запускается для нового кандидата и не принимает пропуск baseline. Это проверяет установку, готовность DB, сохранение sandbox marker и wrapped key, но не полную сохранность заполненной реальной базы и не независимый recovery/review. Исторические ветки без нового workflow не изменены; при создании от обновлённого кода новая политика наследуется. PR не получает ключ, repo secrets и права редактирования workflow требуют доверенных contributors; отдельное защищённое signing environment рекомендуется, не настроено. CI pending; физическая приёмка, миграционный review/recovery открыты. 17/68.

2026-11-25: Build 15 по запросу владельца, отдельная ветка feat/build15-form-ergonomics от 9d2481c (Build14 CI #68 ещё выполнялся при проверке; постоянная подпись Build14 не подтверждена). versionCode 15. Список ранее введённых кредиторов из локальных записей, ручное добавление сохраняется; визуальная группировка разрядов и явная валюта, raw decimal неизменён, без округления; ошибки названия, сумм, ставки, превышения распределения, периодичности; фиксированная нижняя кнопка сохранения/отмены в редакторе, платёж остаётся диалогом с фиксированными действиями; календарь Build14 сохранён. Для кредита/рассрочки явная периодичность unknown/one-time/weekly/biweekly/monthly, для месячной день 1–31 и CLAMP/SKIP; явная первая дата обязательна, старые правила без даты сохраняются, карточка показывает условия. Для карт не считать минимум фиксированным повторением. TermsCodec v1 добавочные nullable поля, Room schema, ключ, applicationId и финансовые расчёты не изменены; старый APK может не знать новые поля, downgrade не допускается. Генерация событий/идентификаторов, регулярный календарь и напоминания — следующие R05/R06, не реализованы; не выдавать сохранённую периодичность за график. Добавлены unit tests форматирования/validation, encrypted roundtrip/legacy payload и UI regression. CI ещё не подтверждён. Новый workflow подписывает push любой ветки собственного repo после проверок, матрица 5 API, baseline signed Build11; PR без ключа. Signed upgrade/physical review/recovery и ручная приёмка остаются открытыми. 17/68, все критерии сохранены.

2026-11-25 — владелец согласовал Build16: движок финансовых событий. Отдельная ветка feat/build16-event-engine от 75ff62d (Build15 CI #70 выполнялся, Build14 #68 debug/API26 PASS, API36 выполнялся при проверке; signing не подтверждён), versionCode 16. Основа: согласованное R01.06, docs/architecture-v1: docs/architecture/EVENT-ENGINE-v1.ru.md, docs/product-requirements-v1: CAL-01–04/T08–T12. Чистое Kotlin-ядро EventRule/FinancialEvent/OccurrenceKey, 8 типов, monetary roles и nullable amount, UUID series/revision и детерминированный UUID occurrence из series+nominal slot, без Android/DB/network. Разовые, ежемесячные 1–31 CLAMP/SKIP, последний день, недельные с несколькими днями/интервалом, каждые N дней, явный график со стабильным UUID записи. EventWindow [from,until) не больше 12 месяцев; clock+bankZone; эффективные интервалы версий [from,to) по номинальной дате до override (решение реализации для стабильности идентичности; перенос может пересечь границы версии/окна). Пересекающиеся revisions/неоднозначные слоты/дубли/невалидные суммы fail-closed. Manual move/cancel keyed slot; перенос в окно из соседнего периода не дублирует, неизвестный slot/перенос вне окна сообщается как конфликт, не удаляется. Reconciler возвращает keep/upsert/retire/conflicts, защищает прошлые, фактические, связанные, overridden и вне окна; это план, не транзакционное хранилище. knownPaymentRecurrence маппит явно известную дату/частоту из Build15, первый известный день имеет приоритет в своём месяце, карта только известная разовая дата, без неизвестной даты нет правила; не выводит срок/число оставшихся платежей, caller задаёт известный конец договора. Не угадывать выходные/праздники, не вычислять долг/льготу/просрочку, не создавать payment link без хранилища. 24 новых JUnit методов + 6 CAL01–06 в SelfCheck, всего 32 диагностики; прежние 26 сохранены. Никаких изменений схемы, ключа, signing workflow, существующего UI/финансовых операций, только диагностика RU/EN. CI pending. R05.01–04 частично реализованы, durable storage/transactional reconciliation/outbox, actual rule revision source, event link, полная материализация 8 типов из данных, календарь, reminder/DST и monetary aggregation ещё открыты; не закрывать критерии. 17/68.

CI #68 Build14 PR SUCCESS (debug, API26/36; sign/compatibility skipped). CI #70 Build15 debug SUCCESS, API26 app run 6 tests with 1 failure: StorageUiTest.discardUnsavedHomeForm attempted performScrollTo on the now-fixed header Back button without a scrollable parent. API36 still running when inspected. Build15 fix 040e80a5 and same fix in Build16: replace the two stale scroll actions with assertIsDisplayed before click; discard dialog, retained draft, and no saved record assertions remain. No product code changes or weakened criteria. Re-run clean CI; signing push results still unverified.

## Прогресс
Обязательные пункты: **17/68 (25.0%)**.
Завершённые этапы: **0/10**.
Это доля принятых проверяемых пунктов с одинаковым весом, а не оценка оставшегося времени или трудоёмкости. Отметка [x] допускается только с доказательством для конкретного пункта; частичная реализация остаётся [ ]. При регрессии снять отметку, сохранить историю и причину.

| ID | Этап | Готово | Статус |
|---|---|---:|---|
| R01 | Основа проекта и поставка APK | 5/6 | В работе |
| R02 | Управление обязательствами | 2/7 | В работе |
| R03 | Постоянное хранилище и миграции | 4/8 | В работе |
| R04 | Платежи и сверка задолженности | 0/7 | Запланирован |
| R05 | Финансовые события и календарь | 0/7 | Запланирован |
| R06 | Напоминания и уведомления | 0/7 | Запланирован |
| R07 | Сводка и базовая аналитика | 1/6 | В работе |
| R08 | Приватность, безопасность и восстановление | 2/7 | В работе |
| R09 | Интерфейс, совместимость и стабилизация | 3/7 | В работе |
| R10 | Подготовка первого выпуска v0.1.0-alpha | 0/6 | Запланирован |

Текущий приоритет: **TEST-B16-001 / R05.01–04** — проверить чистое ядро событий и 32 диагностики; затем durable rule/occurrence storage, R04.05–06 и R05.05–07, R06, R07. [Checkpoint](docs/checkpoints/TEST-B16-001.md).

Build 10: диагностика и кандидаты исправления #7/#8, R09.03; CI #39/#40 успешны (14/14 инструментальных тестов, 5/5 signed upgrade с ключом и готовностью БД). Два замера владельца: databaseOpen 1955/380 мс из storage.ready 2084/426 мс; владелец подтвердил замер 18:10 после reboot (2084 мс) и исчезновение мерцания; условия замера 18:08 не уточнены. Push CI #37: API29 upgrade PID check failed после успешной установки/старта, причина неизвестна; PR #38 зелёный. R09.02 сохраняет историческое доказательство Build 9 CI #31, окончательная регрессия R09.07 открыта. Подробнее [Build 10](docs/testing/BUILD-10.ru.md).

Build 11 этап 1: R03.05/R03.07/R09.07 — встроенная самопроверка (10 сценариев, синтетическая отдельная БД), CI #49/#50 успешны, физический self-check 10/10 по записи владельца (TEST-B11-001), полная приёмка впереди. R03.06/R04.01 остаются следующими. [План проверки](docs/testing/BUILD-11.ru.md).

Перед задачей проверить зависимости, права, критерии, откат и вести отдельную контрольную точку в docs/checkpoints/.

Базовые решения утверждены владельцем 2026-11-24: [R01.06](docs/checkpoints/R01.06.md). R01.06/R09.04 закрыты с доказательствами, 68 критериев сохранены. Реальная миграция и распространение требуют отдельного допуска.

## Правила работы с картой (обязательны для всех чатов)
1. До любого существенного изменения прочитать [HANDOFF.md](HANDOFF.md), этот ROADMAP, [AGENTS.md](AGENTS.md), проверить HEAD, PR, актуальный CI и отчёты. Выбрать ID этапа и пункта, сообщить его владельцу. При конфликте с более старым планом приоритет у актуального ROADMAP; не игнорировать технические ограничения требований.
2. После изменения/теста обновить чекбоксы только по доказательствам, пересчитать прогресс и синхронизировать HANDOFF, историю итераций и отчёт. Ссылки: SHA/CI job/issue/ручная приёмка, версия, дата. При незавершённом пункте записать причину и следующий шаг. В новом чате не начинать работу вслепую.
3. Не смешивать «реализовано», «проверено автоматически», «проверено вручную» и «принято». Если обязательный критерий включает несколько видов проверки, закрыть его лишь после всех. Ошибки, блокеры и ограничения не скрывать. Прогресс рассчитывается из чекбоксов; `python3 scripts/check-roadmap.py` проверяет согласованность с HANDOFF и сводкой.
4. Изменение обязательного объёма — явное решение владельца, зафиксированное в журнале изменений карты; не удалять открытые пункты ради 100%. ID стабильны. Не переходить в beta/RC/stable автоматически.
5. Завершение всех пунктов и 100% карты — условие выпуска первой версии **v0.1.0-alpha**. После подтверждения готовности владельцем выпустить именно эту версию (не стабильную) с проверенным APK, тегом и release notes. Фактическую публикацию записать отдельно: дата, тег, URL, SHA, versionCode. Пока 100% нет, не публиковать релиз и не объединять PR без разрешения.

## Обязательные этапы

### R01 — Основа проекта и поставка APK

- [x] R01.01 Название, Signal Frame, репозиторий, Kotlin/Compose/Material 3; зафиксировать версию alpha. Доказательство: PR #1/#5; Build 9, CI #31.
- [x] R01.02 RU/EN, темы, логотип, навигация и экран фактической версии. Доказательство: Build 8, ручной отчёт 21/21.
- [x] R01.03 Автоматическая сборка, lint, JVM и инструментальные тесты в CI. Доказательство: CI #31.
- [x] R01.04 Постоянная подпись, SHA-256 APK, чистая установка и обновление 8→9 на пяти API. Доказательство: CI #31; sandbox marker, не миграция финансовой БД.
- [ ] R01.05 Добавить и проверить Gradle Wrapper, воспроизводимый bootstrap, документацию сборки.
- [x] R01.06 Согласовать требования MVP, ADR, архитектуру и границы alpha с владельцем; устранить расхождения PR #1–#5. Доказательство: явное утверждение итоговой редакции 1 (424bca4, fingerprint), сверка ADR/UX и приоритет согласованного уточнения над конфликтующими предложениями PR #1–4 в [R01.06](docs/checkpoints/R01.06.md). PR не объединены.

### R02 — Управление обязательствами

- [x] R02.01 Модели денег (minor units), кредита, карты, рассрочки и прочего обязательства; базовая валидация. Доказательство: Build 8/9, CI #31.
- [x] R02.02 Добавление, просмотр, редактирование, удаление с подтверждением, раздельные валюты и неизвестный остаток. Доказательство: Build 8, 21/21; Build 9 UI smoke.
- [ ] R02.03 Довести поля OB-02–OB-06 до согласованного MVP: даты, сроки, исходная сумма, график/комиссии, выписки, несколько льготных групп, доступный и использованный лимиты; неизвестное не угадывать.
- [ ] R02.04 Закрытие, архивирование и восстановление; сохранение истории, отмена будущих событий/напоминаний.
- [ ] R02.05 Поиск и фильтры, детализация условий, платежей и событий; ручные правила для прочих обязательств.
- [ ] R02.06 Версии условий с датой вступления в силу и неизменностью прошлых фактов.
- [ ] R02.07 Приёмка T01–T03, OB-01–07 на финальной сборке.

### R03 — Постоянное хранилище и миграции

- [x] R03.01 Room v1, DAO, репозиторий Flow, точные суммы, nullable значения и транзакции. Доказательство: CI #29/#31.
- [x] R03.02 SQLCipher и обёртывание случайного ключа Android Keystore AES-GCM; тесты неверного/утраченного ключа и повреждённого контейнера. Доказательство: CI #31, 5 storage tests на API 26/36.
- [x] R03.03 Подключить CRUD и сводку к постоянному хранилищу; загрузка и ошибки без пустой подмены; UI smoke. Доказательство: CI #31/#32, 1 UI test на каждом API.
- [x] R03.04 Зафиксировать сгенерированную Room схему v1 и CI-проверку схемы. Доказательство: коммит 3081543; push CI #33.
- [ ] R03.05 Ручная приёмка Build 9: force-stop, перезагрузка, редактирование, удаление и сохранение всех полей (20 проверок). В Build 10 владелец подтвердил все основные сценарии хранения на Xiaomi 17, включая сохранность заполненной БД после обновления 9→10; 28/31 по расширенному чек-листу. Остались системные режимы, уточнение лицензии и другой телефон, исходный критерий не закрыт. [Отчёт](docs/testing/BUILD-10.ru.md). #8 — наблюдение.
- [ ] R03.06 При изменении схемы: явные миграции с реальной зашифрованной БД v1, сохранение ключа/истории и rollback; без destructive migration.
- [ ] R03.07 Проверить аварийное завершение, конкурирующие операции, недостаток места, ошибки чтения/записи; не терять данные и не сообщать ложный успех.
- [ ] R03.08 Приёмка T22, реальное обновление с сохранённой финансовой БД, ARM и 16 KiB совместимость. Build 10: сохранность заполненной базы после физического обновления 9→10 подтверждена владельцем; миграции и полная матрица ещё впереди.

### R04 — Платежи и сверка задолженности

- [ ] R04.01 Добавить модели, миграцию, DAO и журнал фактических платежей: дата, сумма, валюта, связь с обязательством/событием.
- [ ] R04.02 Редактор платежей и история; частичный, полный, досрочный платёж, переплата.
- [ ] R04.03 Распределение на основной долг, проценты и комиссии; неизвестное распределение не уменьшает весь основной долг.
- [ ] R04.04 Корректировка/отмена без переписывания фактов, атомарность, защита от двойного учёта.
- [ ] R04.05 Ручная сверка с банком: датированный подтверждённый снимок отдельно от расчётного остатка.
- [ ] R04.06 Явная привязка к событиям, остаток к оплате, статус и валютная политика; запись в приложении не банковский перевод.
- [ ] R04.07 Приёмка T04–T07, PAY-01–05 и примеры FINANCIAL-RULES.

### R05 — Финансовые события и календарь

- [ ] R05.01 Чистое Kotlin-ядро событий; устойчивые ID правил, экземпляров и версий, типы CAL-01.
- [ ] R05.02 Повторения: разовые, ежемесячные, последний день, интервалы, явный график; CLAMP/SKIP, високосные годы и банковская зона.
- [ ] R05.03 Генерация на согласованный ограниченный горизонт, идемпотентный пересчёт, ручные исключения; сохранение фактической истории.
- [ ] R05.04 Платёж, выписка, новый цикл, окончание льготы, обновление лимита снятия, проценты/комиссии и ручные события.
- [ ] R05.05 Месячный календарь и список, фильтры, детали, связь с платежами, просрочка и неизвестные суммы.
- [ ] R05.06 Минимальный платёж и цель льготы не суммировать дважды; неденежные события не меняют долг.
- [ ] R05.07 Приёмка T08–T12, CAL-01–04; тесты дат, ручных исключений и отмены.

### R06 — Напоминания и уведомления

- [ ] R06.01 Настройки смещений, времени, типов, обязательств, тихих часов и приватности; приоритеты правил.
- [ ] R06.02 Durable outbox, уникальные job ID, WorkManager/AlarmManager по допустимым разрешениям; best effort без обещания доставки.
- [ ] R06.03 Системные каналы, запрос разрешения в контексте, диагностика отказов и ограничений энергопотребления.
- [ ] R06.04 Перепланирование после reboot, time/zone/DST, изменений и отмены событий, при восстановлении процесса.
- [ ] R06.05 Действия открыть/отложить/отметить; частичная оплата не отменяет нужные напоминания.
- [ ] R06.06 Приватный текст на экране блокировки по умолчанию, минимум данных в job payload, отсутствие дублей.
- [ ] R06.07 Приёмка T13–T17, REM-01–05; реальные устройства, в том числе ограниченный фон.

### R07 — Сводка и базовая аналитика

- [x] R07.01 Сводка задолженности отдельно по валютам, количество неизвестных остатков, точные суммы и переполнение. Доказательство: Build 8/9, ручная проверка Build 8 + CI #31.
- [ ] R07.02 Показывать подтверждённые остатки с датой отдельно от расчётных оценок; предупреждения о неполноте.
- [ ] R07.03 Ближайшие платежи, просрочки и обязательные расходы за месяц; переход к событиям.
- [ ] R07.04 Базовый обзор: остатки по типам и валютам, фактические платежи, известные проценты/комиссии; без фиктивных данных.
- [ ] R07.05 Явная политика округления, отсутствие двойного учёта, контрольные примеры финансовых правил и тесты крайних значений.
- [ ] R07.06 Приёмка T23–T24 и UI-02; базовые расчёты сверены с синтетическими сценариями.

### R08 — Приватность, безопасность и восстановление

- [x] R08.01 Локальное хранение без обязательного аккаунта, сети и банковских интеграций в текущем коде. Доказательство: Build 9 manifest/реализация; T20/T21 ещё требуют приёмки.
- [x] R08.02 Backup/device-transfer exclusions настроены, лицензия SQLCipher включена в About. Доказательство: Build 9 manifest и res/xml/data_extraction_rules.xml; фактические проверки впереди.
- [ ] R08.03 Утвердить модель угроз, остаточные риски, жизненный цикл ключей, действия при утрате и повреждении; никакого автоматического удаления.
- [ ] R08.04 Реализовать согласованную опциональную блокировку системной аутентификацией, таймаут, deep link и безопасный предпросмотр; не обещать защиту на скомпрометированной ОС.
- [ ] R08.05 Проверить backup/transfer и отсутствие утечек в БД/journal/temp/logcat, уведомлениях, компонентах и CI; проверка разрешений и зависимостей/лицензий.
- [ ] R08.06 Утвердить политику восстановления/удаления и предупредить об отсутствии переносимого backup; экспорт/импорт отдельным этапом после alpha, если владелец не расширит объём.
- [ ] R08.07 Приёмка T20–T21, SEC-01–03; проверка на физических устройствах и отсутствие критических/высоких дефектов безопасности.

### R09 — Интерфейс, совместимость и стабилизация

- [x] R09.01 Базовый Signal Frame UX, адаптивные иконки, RU/EN, светлая/тёмная/системная темы, клавиатура и форма Build 8. Доказательство: 21/21 Build 8; регрессия #7 устранена по сообщению владельца Build 10 после reboot, CI #39/#40; [отчёт](docs/testing/BUILD-10.ru.md). Расширенная регрессия R09.06/R09.07 впереди.
- [x] R09.02 Матрица CI API 26/28/29/30/36, установочные диагностики и проверка обновления. Доказательство: CI #31.
- [ ] R09.03 Воспроизвести и устранить нестабильный UI-тест API 26 (PR CI #34: Failed to inject touch input); не маскировать ошибки, подтвердить повторными зелёными прогонами.
- [x] R09.04 Диагностировать Samsung Galaxy A30s Issue #6 (Android, SHA-256, ADB); проверить исправление или явно согласовать ограничение поддержки до выпуска. Доказательство: [отчёт совместимости](docs/testing/ANDROID-COMPATIBILITY.md) — владелец подтвердил установку через «Мои файлы»; явное утверждение допустимого обходного пути в [R01.06](docs/checkpoints/R01.06.md). Закрыто по альтернативному критерию согласованного ограничения, не исправлению Telegram. Android/API, SHA-256 фактического файла и ADB причина остаются неизвестными; полная физическая приёмка R03.05/R09.05–07 впереди.
- [ ] R09.05 Расширить CI и ручную матрицу до согласованных версий Android, ARM/16 KiB, разных оболочек/размеров экрана, жестовой и трёхкнопочной навигации.
- [ ] R09.06 TalkBack, масштаб текста, контраст, доступность ошибок, RU/EN всех новых экранов; не показывать неработающие элементы.
- [ ] R09.07 Пройти T18–T19 и регрессию всех функций на финальном APK, устранить критические/высокие дефекты, описать остальные.

### R10 — Подготовка первого выпуска v0.1.0-alpha

- [ ] R10.01 Все R01–R09 завершены, обязательные T01–T24 имеют ссылки на фактические результаты; новые критерии также пройдены.
- [ ] R10.02 Подтверждены CI, ручная приёмка и безопасность; финальный signed APK, versionCode, SHA-256, сертификат и исходный SHA проверены.
- [ ] R10.03 Проверить права на название и бренд, лицензии зависимостей и условия распространения; утвердить поддерживаемые устройства.
- [ ] R10.04 Подготовить README RU/EN, CHANGELOG, инструкции установки/обновления, описание функций, приватности, ограничений и отсутствия банковского перевода.
- [ ] R10.05 Согласовать объединение PR, ветку выпуска, проверку финального кандидата и способ доставки; не менять main без разрешения.
- [ ] R10.06 Подготовить release notes и пакет выпуска; финальное решение владельца о публикации именно v0.1.0-alpha после проверки 100% карты.

## Доказательства текущей контрольной точки
- [Build 8 — ручная проверка](docs/testing/BUILD-8.ru.md), 21/21 по сообщению владельца; первоначально на Samsung A30s установка из Telegram не удалась; позднее владелец подтвердил успешную установку через «Мои файлы».
- [Build 9 — CI #31](https://github.com/Ketarro/Credivaro/actions/runs/45399939307), [PR CI #32](https://github.com/Ketarro/Credivaro/actions/runs/45399954654): 8/8 push jobs, 12/12 инструментальных тестов на API 26/36; чистая установка и подписанное обновление 8→9 (маркер sandbox, не миграция финансовых записей) на API 26/28/29/30/36.
- [Push CI #33](https://github.com/Ketarro/Credivaro/actions/runs/45401601140) успешен; [PR CI #34](https://github.com/Ketarro/Credivaro/actions/runs/45401613445) неуспешен: API 26 StorageUiTest failed to inject touch input; причина нестабильности ещё не установлена. Открытый пункт R09.03. Не выдавать весь CI за зелёный.
- [Build 9 manual](docs/testing/BUILD-9.ru.md): частичная ручная проверка Xiaomi 17; обновление и сохранение после reboot подтверждены, открыты #7/#8. CI #35/#36 зелёные, проверка карты прошла; единичный сбой #34 остаётся R09.03 до проверки стабильности. [Storage](docs/STORAGE-BUILD9.md), [совместимость](docs/testing/ANDROID-COMPATIBILITY.md), [Issue #6](https://github.com/Ketarro/Credivaro/issues/6). Xiaomi 17 wrong APK — владелец подтвердил ошибку выбора файла, не регрессия.

## Исходные документы и согласование
- PR #1 [исходная общая карта](https://github.com/Ketarro/Credivaro/blob/docs/project-foundation/ROADMAP.md) и [версионирование](https://github.com/Ketarro/Credivaro/blob/docs/project-foundation/docs/VERSIONING.md). Эта рабочая карта детализирует прежнюю; при объединении PR сохранить новую версию ROADMAP/AGENTS и согласовать изменения, не перезаписывать более новую карту старой.
- PR #2 [ТЗ](https://github.com/Ketarro/Credivaro/blob/docs/product-requirements-v1/docs/requirements/PRODUCT-SPEC-v1.ru.md), [приёмка T01–T26](https://github.com/Ketarro/Credivaro/blob/docs/product-requirements-v1/docs/requirements/ACCEPTANCE-v1.ru.md), [финансовые правила](https://github.com/Ketarro/Credivaro/blob/docs/product-requirements-v1/docs/requirements/FINANCIAL-RULES-v1.ru.md). T01–T24 обязательны для MVP; T25–T26 относятся к расширенным стратегиям.
- PR #3 [ADR](https://github.com/Ketarro/Credivaro/blob/docs/architecture-v1/docs/architecture/DECISIONS-v1.ru.md), [движок событий](https://github.com/Ketarro/Credivaro/blob/docs/architecture-v1/docs/architecture/EVENT-ENGINE-v1.ru.md), [безопасность](https://github.com/Ketarro/Credivaro/blob/docs/architecture-v1/docs/architecture/SECURITY-v1.ru.md).
- PR #4 [alpha scope](https://github.com/Ketarro/Credivaro/blob/docs/ui-ux-alpha/docs/design/ALPHA-SCOPE.ru.md). Документы всё ещё в открытых PR, не считать их утверждёнными только по факту существования.

## Журнал изменений карты
| Дата | Изменение | Прогресс |
|---|---|---|
| 2026-11-24 | Создана измеримая карта первого alpha-выпуска по запросу владельца; предложен MVP-объём, учтены Build 9 и PR CI #34 | 15/68 (22.1%) |

| 2026-11-24 | Частичная ручная приёмка Build 9; R09.01 открыт повторно из-за регрессии #7, R03.05 остаётся открыт (#8). Объём не менялся | 14/68 (20.6%) |

| 2026-11-24 | Build 10: диагностика и кандидаты исправления #7/#8, CI ожидается; без закрытия пунктов | 14/68 (20.6%) |

| 2026-11-24 | Build 10: CI #39/#40 успешны, диагностические замеры внесены; причины #7/#8 не установлены, приёмка ожидается | 14/68 (20.6%) |

| 2026-11-24 | Build 10: владелец подтвердил исчезновение мерцания после reboot и замер 2084 мс; R09.01 восстановлен, R03.05 остаётся открыт | 15/68 (22.1%) |

| 2026-11-24 | Build 10: ручная проверка Xiaomi 17, 28/31 PASS, 2 частично, 1 уточнение; хранение подтверждено на основном телефоне, другие устройства не проверены | 15/68 (22.1%) |

| 2026-11-24 | Samsung A30s: установка через файлы подтверждена, Telegram не работает; R09.04 открыт до согласования способа распространения | 15/68 (22.1%) |

| 2026-11-24 | Build 11 этап 1: добавлена автономная самопроверка, CI ожидается; платежи и миграция следующие | 15/68 (22.1%) |

| 2026-11-24 | Аудит проекта и подготовка согласования; критерии и статусы сохранены | 15/68 (22.1%) |

| 2026-11-24 | TEST-B11-001: Build 11 self-check 10/10 на физическом устройстве; частичная визуальная проверка; без закрытия пунктов и без изменения допуска | 15/68 (22.1%) |

| 2026-11-24 | Согласованы объём, архитектура и CI | 15/68 (22.1%) |

| 2026-11-24 | Согласованы принципы платежей, миграции и безопасности | 15/68 (22.1%) |

| 2026-11-24 | Приняты решения; R01.06/R09.04 закрыты, R03.05 начат | 17/68 (25.0%) |

| 2026-11-25 | Самостоятельные правила, перенос контрольных точек и актуализация состояния; без закрытия задач | 17/68 (25.0%) |

## Публикация
Статус: **не опубликовано**. Публикация производится после завершения карты и финального подтверждения владельца; не является ещё одним чекбоксом в знаменателе, чтобы избежать кругового условия. Фактический тег/URL/SHA/versionCode/дата — пока отсутствуют.
