# Task: Add Showroom Entry On Home Page

## Goal

Add visible, clickable Showroom entry points to the logged-in home page so users can enter the Showroom frontstage and back-office pages through normal UI operations instead of typing route URLs manually.

## Scope

- Add home-page quick entries for:
  - `数字展厅前台` -> `/showroom/display/home`
  - `展厅后台` -> `/showroom-admin/company`
- Preserve the current home page visual style and existing content.
- Add a focused regression test for the new home entry behavior.

## Non-Scope

- Do not change Showroom backend APIs.
- Do not change existing Showroom route definitions.
- Do not add mock data or fallback route behavior.
- Do not touch unrelated AI, MES, DCC, or task artifact files.

## Milestones

- [x] M1: Confirm Showroom routes exist but no home-page operation entry exists.
- [x] M2: Add failing regression test for home-page Showroom entries.
- [x] M3: Implement minimal home-page entry UI and route handlers.
- [x] M4: Run focused verification and record evidence.
- [x] M5: Commit only task-scoped files.

## Expected Verification

- `node --test scripts/home-showroom-entry.test.mjs`
- `pnpm exec eslint src/views/Home/Index.vue`
- `node --test scripts/showroom-admin-frontend.test.mjs scripts/showroom-frontstage.test.mjs`

## Current Status

Completed.

## Final Verification Result

- PASS: `node --test scripts/home-showroom-entry.test.mjs`
- PASS: `pnpm exec eslint src/views/Home/Index.vue`
- PASS: `node --test scripts/showroom-admin-frontend.test.mjs scripts/showroom-frontstage.test.mjs`
- PASS: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260519-showroom-home-entry/frontend-feature-evidence.md`

## Cleanup Keep

- `doc/tasks/20260519-showroom-home-entry/task.md`
- `doc/tasks/20260519-showroom-home-entry/execution-log.md`
- `doc/tasks/20260519-showroom-home-entry/frontend-feature-evidence.md`
- `scripts/home-showroom-entry.test.mjs`
- `src/views/Home/Index.vue`
