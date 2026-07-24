# Task: Auto schedule local replay automation

## Goal

Turn the first-loop local replay steps into an executable PowerShell helper so the schema patch, demo data reset, and demo data verification can be rerun without manually copying shell commands.

## Scope

- Add one PowerShell helper under `script/shell/` for schema apply, demo-data seed, demo-data cleanup, and demo-data verification.
- Reuse the committed SQL files under `sql/mysql/`.
- Verify the helper against the local `int-ruoyi-mysql` container.
- Do not change backend scheduling logic or frontend behavior in this task.

## Previous Task Check

- `doc/tasks/20260513-intpp-auto-schedule-first-loop/task.md` is completed and already committed.

## Milestones

- [x] M1: Previous backend task checked and completed before follow-up work.
- [x] M2: Follow-up task documentation created before script changes.
- [x] M3: Replay helper script implemented.
- [x] M4: Targeted helper verification passes against local container.
- [x] M5: Evidence updated and task marked completed.
- [x] M6: Helper changes committed on `feature/auto-schedule-first-loop`.

## Expected Verification

- The helper can apply schema SQL into `int-ruoyi-mysql`.
- The helper can clean and reseed the `900xxx` demo data.
- The helper can print a concise verification summary showing the demo work order, current formal task count, and `quantityScheduled`.

## Current Status

Completed on `feature/auto-schedule-first-loop`. The replay helper now supports schema apply, clean, seed, verify, and end-to-end API replay against the local container stack.
