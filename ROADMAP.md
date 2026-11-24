# Roadmap

Status: planning. First application version: **v0.1.0-alpha** (planned, not published). Dates and scope are not promises. All development phases may be revisited during alpha; completing a phase does not authorize a stable release. The maintainer determines release readiness. See [versioning policy](docs/VERSIONING.md).

## Phase 0 — Foundation
- [x] Product concept and Credivaro name selected.
- [x] Signal Frame identity selected.
- [x] Public GitHub repository created.
- [ ] Finish brand asset import and name clearance.
- [ ] Review initial project documentation.

## Phase 1 — Requirements and design
- [ ] Specify product types, data fields, recurring financial events and acceptance criteria.
- [ ] Define calculation rules and edge cases.
- [ ] Design storage, privacy threat model, navigation and accessible RU/EN UX.
- [ ] Record architecture decisions and Android compatibility.

## Phase 2 — Android foundation
- [ ] Initialize Kotlin/Compose project, Room and migrations.
- [ ] Set up CI for lint, tests and debug APK artifacts.
- [ ] Integrate Material 3 theme and Android icon resources.

## Phase 3 — MVP
- [ ] CRUD obligations, record payments, distinguish actual and estimated balances.
- [ ] Dashboard, calendar and flexible reminders.
- [ ] Offline storage, notification settings and backup/export design.
- [ ] Verify financial correctness and reminder behavior on real devices.

## Phase 4 — Extended features
- [ ] Repayment forecasting and strategy comparison.
- [ ] Advanced card cycles, limits and grace-period rules.
- [ ] Import/export, document parsing and optional integrations after privacy review.

## Phase 5 — Iterative stabilization and release review
- [ ] Accessibility, localization, security and regression testing.
- [ ] Confirm branding rights and distribution requirements.
- [ ] Iterate alpha builds, collect maintainer feedback, fix defects and revise scope as needed.
- [ ] Obtain explicit maintainer approval before a beta, release candidate or stable transition; beta/RC are optional.
- [ ] Publish documented, tested builds and release notes only with appropriate approval.
