# Task: Auto schedule replan and task-lock slice frontend

## Goal

Support the replan and task lock/unlock slice in the frontend:

- expose bounded replan from task and calendar surfaces
- let planners lock and unlock tasks explicitly
- show protected-task impact before confirming replan

## Scope

- Update task-page and schedule-calendar-page flows to support replan requests with explicit scope.
- Add task lock/unlock actions and visible locked-state cues where planners manage schedule.
- Do not redesign the broader production scheduling UI in this task.

## Previous Task Check

- `doc/tasks/20260513-auto-schedule-calendar-context-slice/task.md` is completed.

## Milestones

- [x] M1: Previous frontend task checked complete before new work.
- [x] M2: Frontend slice task documentation created before production code changes.
- [x] M3: Verification written for the lock/unlock half of the slice; replan verification remains pending.
- [x] M4: Frontend lock/unlock interaction implemented; replan interaction remains pending.
- [x] M5: Focused verification for the lock/unlock half passed; replan verification remains pending.
- [ ] M6: Frontend slice changes committed separately.

## Expected Verification

- The task page and/or calendar page can trigger bounded replan.
- The UI shows which protected tasks will be preserved before replan confirm.
- A planner can lock/unlock a task and see the locked state reflected in the scheduling surfaces.

## Current Status

In progress on `feature/auto-schedule-first-loop`.
Progress in this turn:

- Calendar day-detail task cards now expose explicit `锁定/解锁` actions.
- Real browser verification passed for lock/unlock state refresh against backend `48092`.
- Bounded replan UI and protected-impact confirmation are still pending.
