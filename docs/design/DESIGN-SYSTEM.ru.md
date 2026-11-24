# Credivaro — дизайн-система Signal Frame

Статус: проект, подлежит проверке на устройствах. Версия приложения v0.1.0-alpha.

## 1. Бренд и цвет
Сохраняем утверждённую геометрию Signal Frame и палитру бренда. Цвета бренда не равны семантическим ролям интерфейса. Предлагаемые пары для Material 3:

| Role | Light | Dark |
|---|---|---|
| primary | #0C5A5A | #80D5CA |
| onPrimary | #FFFFFF | #102D36 |
| primaryContainer | #C5ECE6 | #17504F |
| onPrimaryContainer | #102D36 | #E0F8F3 |
| secondary | #385E60 | #A9D1CE |
| onSecondary | #FFFFFF | #102D36 |
| tertiary / progress accent | #176B3A | #48E27A |
| onTertiary | #FFFFFF | #102D36 |
| background | #F5F7FA | #102D36 |
| onBackground | #102D36 | #F5F7FA |
| surface | #FFFFFF | #17343D |
| onSurface | #102D36 | #F5F7FA |
| surfaceVariant | #E8EFF0 | #28464F |
| onSurfaceVariant | #425B63 | #D9DEE2 |
| outline | #647C83 | #A9BABF |
| error | #B3261E | #FFB4AB |
| onError | #FFFFFF | #690005 |
| warning text | #805300 | #FFD180 |
| success text | #176B3A | #48E27A |

Other Material 3 roles must be completed in implementation; never default to unrelated purple palette. Pale Gray #D9DEE2 and Muted Slate #A9BABF are supporting colors, not low-contrast body text on white. Highlight Green #48E27A may be used with dark foreground, not white small text. Dark/light switch and follow-system preference. Dynamic colors optional only after brand contrast review, off initially. Use semantics: error/warning/success with icon + text, not color alone.

## 2. Typography / geometry
Android Roboto or system font; logo uses original vector wordmark from brand kit when imported. Material 3 text roles, default sizes in sp: displaySmall 36, headlineSmall 24, titleLarge 22, titleMedium 16, bodyLarge 16, bodyMedium 14, labelLarge 14, labelMedium 12. Main financial amount 30–36 sp, tabular figures where font supports; currency kept visible, wrap rather than clip. Locale-dependent formatting; avoid silent abbreviations for exact sums.
Spacing grid: 4/8/12/16/24/32 dp. Outer padding 16 dp on compact screens; adaptive wider container on tablets. Cards radius 16–20 dp; small chips 8–12 dp; standard Material controls. Touch targets >=48 dp including icons. Use WindowInsets; keyboard must not hide submit action. Elevation subtle, avoid gradient-heavy surfaces. No rigid card heights for large font.

## 3. Components
- BalanceSummary: labeled amount by currency, source badge (bank-confirmed vs estimate), as-of date, unknown values, privacy toggle. Never show 0 for unknown.
- ObligationCard: user title, lender, type icon, status, known balance, next event date. No full card numbers or implied bank logos. Archived item distinct.
- EventRow: due date, type, related obligation, amount if known, status and next action. Nonmonetary events have no fake amount. Minimum and grace targets grouped.
- MoneyField: locale-aware decimal input, currency, clear validation and precision; nullable values allowed where domain permits. No Float/Double conversion.
- DateField: accessible text plus picker, bank zone hint; monthly day selector and missing-day behavior.
- SourceLabel: confirmed by user from bank / estimated / unknown with explanatory text, not just color.
- StatusChip: planned, partial, recorded, possible overdue, canceled. Text labels; avoid asserting bank-confirmed payment.
- NotificationRuleEditor: offset, time, enabled, quiet-hour and privacy options.
- EmptyState, ErrorState, PermissionNotice: specific remedy without misleading metrics.
- DestructiveConfirmation: distinguish archive and permanent delete, mention history; cancel safe by default.

## 4. Accessibility and privacy
Target WCAG AA contrast as design baseline (4.5:1 normal text, 3:1 large text and meaningful non-text controls), verify all actual pairs and states. Support TalkBack semantics, 200% font scaling without inaccessible content, logical focus order, landscape and compact/expanded widths. Avoid redundant announcements; decorative icon contentDescription null, interactive icons localized. Use reduced-motion preference when applicable; no mandatory animations. Mask sums on dashboard when enabled; no financial data in generic notifications or task previews when protection enabled. Compose previews and screenshots contain only synthetic data.

## 5. Localization
All user-facing strings in resources RU/EN with plurals, dates and currency formatting by locale. No concatenated sentences that prevent translation. User title/note not translated. Unknown value: «Не указано» / «Not set». Bank-confirmed source phrasing explains manual reconciliation, not bank integration.

## 6. Review
Review light/dark, empty/error/loading, long names, multi-currency, large text, keyboard, permission denial and privacy. Theme values are proposals, not implemented resources. Test screenshots and actual contrast before acceptance.
