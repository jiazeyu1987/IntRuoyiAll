# Task: Auto schedule replan and task-lock slice

## Goal

Implement the next mandatory remaining-gap slice from the auto-schedule checklist:

- add first-class bounded `/replan` behavior
- add explicit task lock/unlock operations that automatic scheduling must respect

## Scope

- Backend only in `D:\wt\intsched-be` for replan API, lock/unlock API, scheduling rules, and tests.
- Frontend only in `D:\ProjectPackage\Int\IntRuoyi-worktrees\auto-schedule-first-loop\yudao-ui-admin-vue3` for replan entry points, task lock/unlock actions, and impacted-task confirmation UX.
- Do not implement BOM/material deepening, split scheduling, or quantityScheduled redefinition in this task.

## Previous Task Check

- `doc/tasks/20260513-auto-schedule-calendar-context-slice/task.md` is completed.
- `doc/tasks/20260513-pro-schedule-calendar-followups/task.md` remains explicitly blocked by missing non-MySQL validation environments and is treated as a separate bounded follow-up.

## Milestones

- [x] M1: Previous task state checked and current slice boundary chosen from the reviewed checklist.
- [x] M2: Task documents created before production code changes.
- [x] M3: RED verification written for lock/unlock and backend replan behavior.
- [x] M4: Backend replan and lock/unlock behavior implemented.
- [ ] M5: Frontend replan and lock/unlock interaction implemented.
- [ ] M6: Targeted verification passes and evidence updated.
- [ ] M7: Slice changes committed separately in backend and frontend repos.

## Expected Verification

- Users can replan only an explicit work-order/date scope instead of recomputing unrelated schedule.
- Replan respects protected MANUAL/locked/finished tasks and reports impacted preserved tasks before write.
- Planners can explicitly lock/unlock tasks and the next preview/apply/replan respects that state.

## Current Status

In progress on `feature/auto-schedule-first-loop`.
Progress in this turn:

- Backend explicit task lock/unlock API and service behavior are implemented and target-tested.
- Backend replan preview/apply API and protected-task preview behavior are implemented and target-tested.
- Frontend calendar day-detail lock/unlock interaction is implemented and real-browser verified.
- Frontend bounded replan interaction is still pending.
