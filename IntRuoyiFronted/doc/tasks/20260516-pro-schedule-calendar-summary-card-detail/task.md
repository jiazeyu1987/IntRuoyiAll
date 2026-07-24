# Task: Schedule Calendar Day-Detail Summary Cards Open Detail

## Goal

Make every non-zero summary card in the production schedule calendar day-detail
panel clickable and show the corresponding detail view after the click.

## Scope

- Check the latest frontend task in this repository before starting new work.
- Create this task directory and task documents before production code changes.
- Keep the existing schedule calendar route, APIs, and overall page layout.
- Implement clickable detail behavior only for the day-detail summary cards:
  `任务`, `工单`, `白班`, `夜班`, `短缺`, and `锁定`.
- Do not add fallback branches, mock data, or unrelated visual redesign.

## Previous Task Check

- Previous frontend task: `doc/tasks/20260516-workorder-erp-bom-sync/task.md`
- Status before this task: blocked.
- Impact: the old task is documentation-only and can be resumed later, so it does
  not block this summary-card detail fix.

## Milestones

- [x] M1: Block the unfinished previous frontend task and create this task package.
- [x] M2: Record BDD scenarios and RED verification for non-clickable summary cards.
- [x] M3: Implement the minimal clickable-card and detail-dialog behavior.
- [x] M4: Run focused verification, update evidence, and mark the task completed.
- [x] M5: Commit only the frontend files produced by this task.

## Expected Verification

- `npx.cmd --yes --package @playwright/cli playwright-cli --session schedule-calendar-summary-card-detail run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-pro-schedule-calendar-summary-card-detail\scripts\verify-summary-card-detail.mjs`
- `pnpm exec eslint src/views/mes/pro/task/calendar/index.vue`

## Current Status

Completed. The day-detail summary cards now open detail dialogs for every
non-zero metric, while zero-value cards remain non-interactive.

## Final Verification Result

- `pnpm exec eslint src/views/mes/pro/task/calendar/index.vue` -> PASS
- `npx.cmd --yes --package @playwright/cli playwright-cli --session schedule-calendar-summary-card-detail run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-pro-schedule-calendar-summary-card-detail\scripts\verify-summary-card-detail.mjs` -> PASS
- Verified live behavior on `http://127.0.0.1:8081/mes/pro/schedule-calendar`:
  - non-zero `任务`, `工单`, and `白班` cards each opened a detail dialog with rows
  - zero-value `夜班`, `短缺`, and `锁定` cards remained disabled
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260516-pro-schedule-calendar-summary-card-detail --mode preview` -> READY
  - preview would keep `task.md` and `execution-log.md`
  - preview would delete `frontend-feature-evidence.md` and `scripts/verify-summary-card-detail.mjs`
