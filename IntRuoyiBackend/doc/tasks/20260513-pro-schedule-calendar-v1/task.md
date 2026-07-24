# Task: IntPP-style schedule calendar v1 backend

## Goal

Implement a production schedule calendar v1 for IntRuoyi MES that behaves like the IntPP schedule calendar at the rule and detail level, while reusing existing IntRuoyi calendar and scheduling master data.

## Scope

- Add backend singleton rule and simulation state persistence for the production schedule calendar.
- Add month and day-detail APIs under `/mes/pro/schedule-calendar`.
- Reuse existing holiday, plan, shift, line, workstation, task, dependency, and issue data as the schedule calendar source.
- Keep one current schedule only. Do not implement schedule version save/load/compare.
- Keep legacy `mes/cal/*` APIs unchanged.

## Previous Task Check

- `doc/tasks/20260513-intpp-auto-schedule-first-loop/task.md` is completed.
- `doc/tasks/20260513-auto-schedule-replay-automation/task.md` is completed.

## Milestones

- [x] M1: Previous backend tasks checked complete before new work.
- [x] M2: Backend task documentation created before production code changes.
- [x] M3: RED backend tests added for rules, simulation, month aggregation, and day-detail aggregation.
- [x] M4: Rule/simulation persistence and APIs implemented.
- [x] M5: Month/day-detail aggregation implemented using existing MES master data.
- [x] M6: Targeted backend verification passes.
- [x] M7: Evidence updated and backend task marked completed.
- [x] M8: Backend changes committed on `feature/auto-schedule-first-loop`.

## Expected Verification

- Rules can be read and updated.
- Simulation date can advance by one day, by 30 days, and reset.
- Month API returns per-day schedule calendar summary using current formal tasks.
- Day-detail API returns workshop/line/task/material detail for the selected day.
- Missing required scheduling master data fails fast instead of silently degrading.

## Current Status

Completed on `feature/auto-schedule-first-loop`. Backend rules, simulation,
month/day-detail APIs, fail-fast checks, SQL updates, browser-backed frontend
integration, and task evidence are complete.
