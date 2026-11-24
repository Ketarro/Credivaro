# Build 10 — стабилизация и диагностика
Статус: CI #39/#40 успешны; получены два диагностических замера, ручная приёмка не завершена. v0.1.0-alpha, versionCode 10. Только вымышленные данные. R09.01/R09.03/R03.05/R03.07. Issues [#7](https://github.com/Ketarro/Credivaro/issues/7), [#8](https://github.com/Ketarro/Credivaro/issues/8), [#6](https://github.com/Ketarro/Credivaro/issues/6) отложен.
- [ ] Установить signed Build 10 поверх Build 9 без удаления; проверить сборку 10, тему/язык и все сохранённые записи.
- [ ] Перезагрузить Xiaomi 17, разблокировать, сразу открыть Credivaro; записать время до данных и время с разблокировки.
- [ ] Первый переход RU→EN или EN→RU; записать направление, тему и наличие мерцания. Повторить обратное переключение и системный режим.
- [ ] Открыть Настройки → О приложении → Диагностика запуска; передать только строки времени (можно скриншот именно этого окна), не финансовые записи. Повторить после force-stop и повторного reboot. Сравнить storage.keystoreEnvelope, nativeLoad, roomBuilder, databaseOpen, firstQuery, ready.
- [ ] Проверить сохранённые записи, редактирование, удаление и итоги после force-stop/reboot.
- [ ] Проверить оригинал лицензии и пояснение RU/EN.
- [ ] Проверить пустое название, -100, 12.345 USD, невозможную дату 2026-02-30, затем допустимые значения; некорректная запись не сохраняется.
- [ ] Проверить на другом доступном устройстве, указать Android/оболочку. Samsung отдельная задача.

Диагностика только в тестовой DEBUG сборке, в оперативной памяти и logcat, не передаётся автоматически; локальные длительности не содержат финансовых данных. Нет гарантии исправления мерцания/ускорения до замеров. CI semantic UI test не заменяет физические жесты. Схема Room v1, шифрование и ключи сохранены.

## CI and first physical timing samples — 2026-11-24
[Push #39](https://github.com/Ketarro/Credivaro/actions/runs/45415653884) and [PR #40](https://github.com/Ketarro/Credivaro/actions/runs/45415673746) successful for `0c83ac7f51e9b5e9103649f2f3ce900211baf2fd`. Push 8/8 jobs; 5 storage + 2 UI tests each on API 26/36 (14/14). Signed Build 9→10 update, retained sandbox and wrapped key hash, storage readiness passed on API 26/28/29/30/36 (5/5); not proof of preservation of populated financial records or physical reboot. [Signed Build 10](https://github.com/Ketarro/Credivaro/actions/runs/45415653884/artifacts/14931893030), APK SHA-256 `777900af69ebb2bdaaddd252163ca0c65e8a82f4460ef4d0dd861dff9090a404`.
Owner supplied two Build 10 diagnostics screenshots (device assumed Xiaomi 17 from preceding context; exact launch conditions unconfirmed). Only numerical transcriptions retained, no screenshots committed.

| Stage | 18:10 ms | 18:08 ms |
|---|---:|---:|
| keystoreEnvelope | 23 | 14 |
| nativeLoad | 16 | 5 |
| roomBuilder | 44 | 5 |
| databaseOpen | 1955 | 380 |
| firstQuery | 44 | 21 |
| storage.ready (duration) | 2084 | 426 |
| activity.storageReadyFrame (relative timestamp) | 2189 | 484 |

DB open accounts for ~94% / 89% of storage duration. It includes SQLCipher/SQLite open and Room checks; internal KDF vs IO vs other causes not isolated. 18:08 locale request 15386→15392 return→15397 config changed (11 ms); 18022→18025→18030 (8 ms); third request at 29904, following entries not visible. No new activity.create visible for these first two switches; this cannot prove no visual flicker. No ten-second delay reproduced in these samples; whether either followed reboot or force-stop and whether flicker persists remain unknown. Do not weaken crypto or claim resolution. Ask owner which run followed reboot and whether first switch flickered; compare repeated cold/warm timings. R03.05, R03.07, R09.01, R09.03 remain open; no progress change. No code/version changes in this follow-up.

## Уточнение владельца — 2026-11-24
Замер 18:10 сделан после перезагрузки телефона: storage.ready 2084 мс, databaseOpen 1955 мс, кадр готовности 2189 мс. Владелец подтвердил, что мерцание пропало. Для замера 18:08 условия не уточнены. Предыдущие ~10 с Build 9 — визуальная оценка, не сопоставимый инструментальный benchmark. R09.01 восстановлен по подтверждению исправления наблюдавшейся регрессии; расширенная проверка других устройств и всех комбинаций RU/EN/тем остаётся R09.06/R09.07. Issue #7 закрыт по исходному симптому; повторно открыть при регрессии. Issue #8 остаётся открытым для повторных замеров и проверки на других устройствах, но дальнейшее изменение криптографии не требуется по текущим данным. R03.05 и R03.07 не завершены, остальные пункты ручной проверки не засчитаны. Версия и код без изменений. Прогресс 15/68 (22.1%).
