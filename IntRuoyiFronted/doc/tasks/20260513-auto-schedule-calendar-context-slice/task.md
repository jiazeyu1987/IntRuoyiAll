# Task: Auto schedule calendar-context slice frontend

## Goal

Support the schedule-calendar-driven auto-schedule slice in the frontend:

- preserve the previewed calendar context for publish
- surface simulated calendar usage clearly enough for planners
- keep the task page usable for real waiting work orders

## Scope

- Update the task-page and schedule-calendar-page auto-schedule flows to carry the previewed calendar-context token.
- Add focused UI cues for simulated calendar usage and stale preview invalidation.
- Fix the task-page runtime regression that blocked real waiting work-order rendering.
- Do not implement broader replan or lock-management UI in this task.

## Previous Task Check

- `doc/tasks/20260513-pro-schedule-calendar-v1/task.md` is completed.

## Milestones

- [x] M1: Previous frontend task checked complete before new work.
- [x] M2: Frontend slice task documentation created before production code changes.
- [x] M3: RED frontend verification written for preview-token replay and stale-context blocking.
- [x] M4: Frontend token flow and simulated-calendar cues implemented.
- [x] M5: Focused verification passes and evidence updated.
- [x] M6: Frontend slice changes committed separately.
- [x] M7: Real browser verification passed after fixing the task-page runtime regression.

## Expected Verification

- The preview result stores a calendar-context token.
- Apply sends the preview token back.
- UI makes stale preview invalidation and simulated calendar usage visible.
- The task page renders waiting work orders and can issue real preview/apply requests.

## Current Status

Completed on `feature/auto-schedule-first-loop`. Frontend token-flow work,
task evidence, and the scoped slice commit are finished. A post-commit runtime
verification pass found and fixed a missing `handleTree` import in
`src/views/mes/pro/task/index.vue`, which had blocked the waiting work-order
grid and real auto-schedule UI verification. Real browser verification now
passes against frontend `3102` and backend `48092`.
