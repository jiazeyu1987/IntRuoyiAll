# Verification Report

## Scope

- Updated the real `PQC填写` page under `FrontlineFixedTemplatePanel.vue`.
- Kept API clients, backend, DTO/schema, route permissions, database, employee/process data source, and formal PQC submit behavior unchanged.

## Commands

- `node tests/e2e/edhr-frontline-pqc-html-alignment-static.spec.cjs` -> PASS.
- `node tests/e2e/edhr-frontline-fill-tabs-static.spec.cjs` -> PASS.
- `node src/views/mes/pro/feedback/frontline-template-render.spec.cjs` -> PASS.
- `node src/views/mes/pro/feedback/frontline-template-switch.spec.cjs` -> PASS.
- `pnpm ts:check` -> PASS.
- `node --check tests/e2e/edhr-frontline-pqc-html-alignment-real.e2e.cjs` -> PASS.
- Login preflight for local `http://127.0.0.1:8081` -> PASS, credentials redacted.
- `node tests/e2e/edhr-frontline-pqc-html-alignment-real.e2e.cjs` -> PARTIAL: main layout assertions and screenshot passed; piece interaction blocked by missing formal process data.

## Assertions

- PQC page exposes four target entries: `length / appearance / seal / pressure`.
- Appearance and seal expose three actions: `全部合格 / 全部不良 / 逐件选择`.
- Piece dialog exposes `data-pqc-piece-modal` and `data-pqc-piece-list`.
- Piece grid uses five columns at the target 1920px layout.
- Length default is `32.5` with `0.1` step; pressure default is `50` with `1` step.
- Local piece values are isolated by selected process, inspection type, patrol round, and item key.
- Footer exposes `重填 / 提交`; reset clears only current PQC local inspection state.
- Existing PQC formal submit fail-fast message remains present.

## Real Browser Evidence

- Frontend `http://127.0.0.1:8081/` -> HTTP 200.
- Backend `http://127.0.0.1:48081/actuator/health` -> `UP`.
- Screenshot: `IntRuoyiFronted/output/playwright/20260731-frontline-pqc-html-alignment/pqc-main-1920.png`.
- MES write requests during real visual probe: none before the missing-process blocker.

## Blocker

- Full real piece-dialog interaction requires at least one selectable formal frontline process for the local login identity.
- Current local `芋道源码/admin` path returned no selectable process in the process picker.
- No fallback was used: no mock data, no direct SQL fixture, no fake process, and no API/data-source changes.

## Result

- Implementation and static/typing verification are complete.
- Real browser visual layout is verified.
- Real piece interaction remains blocked by missing formal process data and should be rerun after a test process/employee fixture is available.
- Closeout remains `ready_for_closeout` because `git push origin int_main` failed with GitHub 443 connection failure via `127.0.0.1`.
