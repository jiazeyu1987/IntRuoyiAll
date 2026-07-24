# Task: Auto schedule calendar-context slice

## Goal

Implement the first mandatory remaining-gap slice from the auto-schedule checklist:

- make schedule calendar rules and simulation state drive auto-schedule availability
- bind preview and apply to the same effective calendar context

## Scope

- Backend only in `D:\wt\intsched-be` for scheduling logic, API contract, and tests.
- Frontend only in `D:\ProjectPackage\Int\IntRuoyi-worktrees\auto-schedule-first-loop\yudao-ui-admin-vue3` for preview/apply token flow and simulated-calendar UI cues.
- Do not implement replan, lock/unlock operations, or material/BOM deepening in this task.
- Current backend slice owner in this repository is limited to `preview/apply` calendar-context binding and schedule-calendar-driven auto-schedule availability.

## Previous Task Check

- `doc/tasks/20260513-auto-schedule-gap-checklist/task.md` is completed.
- `doc/tasks/20260513-pro-schedule-calendar-followups/task.md` remains explicitly blocked by missing non-MySQL validation environments and is treated as a separate bounded follow-up.

## Milestones

- [x] M1: Previous task state checked and current slice boundary chosen from the reviewed checklist.
- [x] M2: Task documents created before production code changes.
- [x] M3: RED backend verification written for calendar-driven preview/apply and stale-context rejection.
- [x] M4: Backend calendar-context token and schedule-calendar-driven scheduling logic implemented.
- [x] M5: Frontend preview/apply token flow and simulated-calendar state cues implemented in the separate UI repository.
- [x] M6: Targeted verification passes and evidence updated.
- [x] M7: Backend slice changes committed in `D:\wt\intsched-be`; frontend follow-up remains in the separate UI repository.

## Expected Verification

- Preview reflects schedule calendar rules and simulation state, not only holiday/plan/shift defaults.
- Apply fails when the effective calendar context changed after preview.
- Frontend sends the previewed calendar context token back on apply and blocks stale publish paths.

## Current Status

Completed for the backend scope on `feature/auto-schedule-first-loop`.
Backend slice execution in `D:\wt\intsched-be` is complete:

- RED/GREEN tests for calendar-driven preview changes.
- RED/GREEN tests for stale calendar-context rejection on apply.
- Backend API contract changes for preview token echo and transactional apply validation.
- Shared calendar-rule semantics now come from the extracted helper used by schedule-calendar and auto-schedule.
- Targeted backend verification passed, including `MesProAutoScheduleServiceImplTest`, `MesProScheduleCalendarServiceImplTest`, and reactor compile through `yudao-server`.
- Frontend slice is completed in `D:\ProjectPackage\Int\IntRuoyi-worktrees\auto-schedule-first-loop\yudao-ui-admin-vue3` and committed as `2280b6e1`.
- The local replay script is updated for the new preview/apply token contract and is verified against the current branch backend running on `http://127.0.0.1:48092/admin-api`.
