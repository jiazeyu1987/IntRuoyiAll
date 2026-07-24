# Task: Schedule calendar shortage risk and daily material summary

## Goal

Update the MES production schedule calendar and auto-schedule frontend so material shortage no longer blocks publish by itself, and clicking a calendar date shows per-material daily usage, remaining available quantity, shortage quantity, and affected work-order count.

## Scope

- Block the previous same-repository frontend task before starting this work.
- Preserve the already-paused route / work-order drill-down changes in the same area.
- Record BDD scenarios and strict TDD evidence for publish gating and inline material-summary rendering.
- Extend the schedule-calendar API types with the new summary fields.
- Verify the real frontend path `http://localhost:8081` with Playwright.

## Previous Task Check

- Previous frontend task: `doc/tasks/20260518-md-item-erp-bom-sync-button/task.md`
- Status before this task: blocked by user priority switch.
- Impact: the paused ERP BOM sync frontend task remained isolated and did not block this schedule-calendar slice.

## Milestones

- [x] M1: Block the previous same-repository frontend task and create this task package first.
- [x] M2: Record BDD scenarios and add RED verification for shortage-warning publish behavior and selected-day material summary rendering.
- [x] M3: Implement the minimal frontend API/type updates, publish gating change, and inline day-detail material summary UI.
- [x] M4: Run targeted frontend verification, real Playwright verification, and update evidence.
- [x] M5: Preview closeout artifacts and prepare a task-scoped frontend commit.

## Expected Verification

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260518-schedule-calendar-shortage-risk-daily-material-summary\scripts\verify-schedule-calendar-shortage-source.mjs`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session schedule-calendar-shortage-risk run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260518-schedule-calendar-shortage-risk-daily-material-summary\scripts\verify-schedule-calendar-shortage-real-e2e.mjs`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260518-schedule-calendar-shortage-risk-daily-material-summary/frontend-feature-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260518-schedule-calendar-shortage-risk-daily-material-summary --mode preview`

## Current Status

Completed. Frontend implementation, source verification, type-check, live Playwright verification, evidence validation, and closeout preview are complete.

## Final Verification Result

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260518-schedule-calendar-shortage-risk-daily-material-summary\scripts\verify-schedule-calendar-shortage-source.mjs` -> PASS
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json` -> PASS
- `npx.cmd --yes --package @playwright/cli playwright-cli --session schedule-calendar-shortage-risk run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260518-schedule-calendar-shortage-risk-daily-material-summary\scripts\verify-schedule-calendar-shortage-real-e2e.mjs` -> PASS
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260518-schedule-calendar-shortage-risk-daily-material-summary/frontend-feature-evidence.md` -> PASS
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260518-schedule-calendar-shortage-risk-daily-material-summary --mode preview` -> PASS

## Blocker And Impact

- None currently.
