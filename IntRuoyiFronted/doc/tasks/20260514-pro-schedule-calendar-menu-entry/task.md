# Task: Route production schedule calendar through scheduling management

## Goal

Update the frontend entry so the production scheduling page routes to the
unified `排班管理 -> 生产排程日历` menu entry instead of the old hidden route.

## Scope

- Keep the schedule calendar page implementation intact
- Change the production scheduling page button to route to the new visible menu route
- Keep other page behavior unchanged

## Previous Task Check

- `doc/tasks/20260514-vite-dev-restart/task.md` is completed.

## Milestones

- [x] F1: Previous frontend task checked complete before new work.
- [x] F2: Frontend task document created before production code changes.
- [x] F3: Entry update implemented.
- [x] F4: Focused verification passes.
- [x] F5: Task evidence updated and change committed.

## Expected Verification

- The production scheduling page `排程日历` button opens the new unified menu route.

## Current Status

Completed on `feature/auto-schedule-first-loop`. The production scheduling page
now routes `排程日历` to the unified scheduling-management entry.
