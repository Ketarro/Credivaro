# Build 8 — отчёт о ручном тестировании

Дата: 2026-11-24. Версия 0.1.0-alpha, versionCode 8. Проверенный исходный коммит: `a94aba638ed3046107a4b7d606732209185ba9d2`.

Владелец проекта сообщил 21/21 успешных проверок на своём Android-устройстве. Новых дефектов не сообщил; запросил HANDOFF для перехода между чатами. Это отчёт владельца, не независимое инструментальное тестирование.

- [x] Обновление поверх Build 7 без удаления приложения
- [x] Сохранение языка и темы
- [x] Отсутствие мерцания при смене языка
- [x] Новая финансовая сводка
- [x] Раздельные итоги по валютам
- [x] Отображение неизвестных остатков
- [x] Новые карточки обязательств
- [x] Добавление кредита
- [x] Добавление кредитной карты
- [x] Добавление рассрочки
- [x] Дополнительные условия
- [x] Выбор валюты
- [x] Редактирование обязательства
- [x] Удаление обязательства
- [x] Автоматическая прокрутка при вводе
- [x] Доступность нижнего поля при открытой клавиатуре
- [x] Доступность кнопки сохранения
- [x] Валидация сумм и дат
- [x] Компоновка настроек языка
- [x] Светлая и тёмная темы
- [x] Русская и английская локализации

## CI
- [Push #17](https://github.com/Ketarro/Credivaro/actions/runs/45325287905): success, lint/JVM tests/assembleDebug and persistent signature steps succeeded.
- [PR #18](https://github.com/Ketarro/Credivaro/actions/runs/45325293847): success; signing intentionally skipped for PR events.
- [Signed APK](https://github.com/Ketarro/Credivaro/actions/runs/45325287905/artifacts/14889192816); [reports](https://github.com/Ketarro/Credivaro/actions/runs/45325287905/artifacts/14889626802). Artifacts expire 2026-12-08 UTC; verify availability before sharing. Use signed, not temporary-debug, for updates.

No additional claim about device model, Android version, accessibility coverage, encrypted storage or release readiness. Financial records remain memory-only. Synthetic data only. No screenshots or personal data attached.

## Дополнительное устройство — 2026-11-24
Samsung Galaxy A30s: установка Build 8 из Telegram не удалась («Не удалось обработать пакет»). [Issue #6](https://github.com/Ketarro/Credivaro/issues/6). Версия Android и фактический SHA-256 файла неизвестны. 21/21 относится только к прежнему устройству; успешная подпись CI не подтверждает целостность переданного файла. Открытый дефект, причина не установлена.

Дополнительная совместимость: Xiaomi 17 и PILOT 3 (Android 16, Doke OS 5) успешно устанавливаются через Telegram по сообщению владельца. На PILOT 3 показан экран настроек, не полный тест. Добавлена диагностическая матрица CI; результаты ожидаются. См. [ANDROID-COMPATIBILITY.md](ANDROID-COMPATIBILITY.md).
