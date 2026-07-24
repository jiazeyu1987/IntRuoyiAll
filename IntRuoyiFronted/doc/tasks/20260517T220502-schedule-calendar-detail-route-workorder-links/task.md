# Task: Schedule calendar day-detail route and work-order links

## Goal

Update the MES schedule-calendar right-side day-detail panel so it no longer renders workshop and line cards. Instead, it groups tasks by route, lets users click a work-order code to open the work-order page, and lets users click a route title to open the route page.

## Scope

- Update the schedule-calendar frontend day-detail view and the route-page handoff logic only.
- Keep the month grid, scheduling rules, auto-schedule flow, lock actions, and shortage dialog behavior intact.
- Depend on real backend `routeId` / `routeName` fields and do not add fallback labels to hide missing data.

## Previous Task Check

- Previous frontend task: `doc/tasks/20260517-workorder-list-hide-type-unit-add-finish-time/task.md`
- Status before this task: completed for code delivery.
- Impact: no unfinished frontend task blocked this schedule-calendar detail update.

## Milestones

- [x] M1: Create the frontend task package before production changes.
- [x] M2: Record BDD scenarios and RED evidence for the missing route / link behavior.
- [x] M3: Implement the route-group detail view and clickable route / work-order handoff.
- [x] M4: Run type-check and real Playwright verification.
- [x] M5: Update task status, evidence, and closeout notes.

## Expected Verification

- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session schedule-calendar-detail-route-workorder-links-green run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260517T220502-schedule-calendar-detail-route-workorder-links\scripts\verify-schedule-calendar-detail-route-workorder-links.mjs`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260517T220502-schedule-calendar-detail-route-workorder-links --mode preview`

## Current Status

Completed. The day-detail panel now groups task cards by route, and both route and work-order click-through flows have green evidence.

## Final Verification Result

- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json` -> PASS
- `npx.cmd --yes --package @playwright/cli playwright-cli --session schedule-calendar-detail-route-workorder-links-green run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260517T220502-schedule-calendar-detail-route-workorder-links\scripts\verify-schedule-calendar-detail-route-workorder-links.mjs` -> PASS
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260517T220502-schedule-calendar-detail-route-workorder-links --mode preview` -> PASS

## Blocker And Impact

- None for code delivery.
- Live data note: current runtime data still references logically deleted route `900020`, so that card title falls back to `工艺流程#900020` until the route record is repaired.
