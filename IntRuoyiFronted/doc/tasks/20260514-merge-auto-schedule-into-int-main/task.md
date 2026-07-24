# Task: Merge auto-schedule branch into int_main frontend

## Goal

Merge the committed frontend history from `feature/auto-schedule-first-loop` into
`int_main` so the main frontend branch includes the production schedule calendar
and related auto-schedule UI flow.

## Scope

- Frontend repository only
- Merge committed frontend history from `feature/auto-schedule-first-loop` into `int_main`
- Exclude unrelated untracked files already present in the `int_main` worktree
- Run focused frontend verification after merge

## Previous Task Check

- Previous frontend task: `doc/tasks/20260514-erp-kingdee-config-page/task.md`
- Status before this task: completed.

## Milestones

- [x] F1: Previous frontend task checked complete before new work.
- [x] F2: Merge task document created before Git changes.
- [x] F3: Merge committed feature history into `int_main`.
- [x] F4: Run focused frontend verification on merged result.
- [x] F5: Record evidence and complete the merge commit on `int_main`.

## Expected Verification

- `git merge --no-ff feature/auto-schedule-first-loop`
- `pnpm exec eslint src/api/mes/pro/scheduleCalendar/index.ts src/views/mes/pro/task/index.vue src/views/mes/pro/task/calendar/index.vue src/views/mes/pro/workorder/index.vue src/router/modules/remaining.ts`

## Current Status

Completed on `int_main`. The committed frontend auto-schedule and schedule
calendar history from `feature/auto-schedule-first-loop` has been integrated.
