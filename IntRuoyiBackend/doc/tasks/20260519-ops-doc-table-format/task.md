# Task: Refine OPS And README Into Compact Tables

## Goal

Improve the readability of the operations documentation by converting the main `OPS.md` and `README.md` operations sections into compact, scan-friendly tables.

## Scope

- Confirm the latest same-repository backend task is explicitly completed before starting this documentation refinement task.
- Record BDD for the documentation format change before editing the docs.
- Update `OPS.md` so the key launcher, environment, and command examples are easier to scan.
- Update the `README.md` operations entry to use a more compact summary format.
- Keep the change documentation-only and aligned to the already verified operations scripts.

## Previous Task Check

- Previous backend task: `doc/tasks/20260519-readme-ops-entry/task.md`
- Status before this task: completed.
- Impact: the README operations entry task is already closed, so this documentation refinement can proceed independently.

## Milestones

- [x] M1: Confirm the previous backend task is closed and create this documentation refinement task package.
- [x] M2: Record BDD and refine `OPS.md` into compact tables.
- [x] M3: Refine the `README.md` operations summary.
- [x] M4: Verify closeout preview and prepare a task-scoped commit.

## Expected Verification

- Manual review of `OPS.md` and `README.md`
- `Select-String -Path D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\OPS.md -Pattern '|'`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260519-ops-doc-table-format --mode preview`

## Current Status

Completed on 2026-05-19. `OPS.md` and the `README.md` operations summary now use compact tables for the primary launcher, command, and environment information.

## Final Verification Result

- PASS: `Select-String -Path D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\OPS.md -Pattern '|'`
- PASS: `Select-String -Path D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\README.md -Pattern 'Operations','OPS.md','运维工具.bat'`
- PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260519-ops-doc-table-format --mode preview`

## Blocker And Impact

- Blocker: none.
- Impact: none.
