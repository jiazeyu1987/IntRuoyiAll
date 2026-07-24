# Task: Add README Entry For OPS Toolkit

## Goal

Add a clear operations entry in `README.md` so collaborators can quickly discover the verified `OPS.md` guide and the unified `运维工具.bat` launcher.

## Scope

- Confirm the latest same-repository backend task is explicitly completed before starting this documentation-entry task.
- Record BDD for the README entry behavior before changing the document.
- Add a concise README section that points to `OPS.md` and `运维工具.bat`.
- Keep the change documentation-only and aligned to the currently verified operations toolkit.

## Previous Task Check

- Previous backend task: `doc/tasks/20260519-ops-md-guide/task.md`
- Status before this task: completed.
- Impact: the OPS guide task is already closed, so this README entry task can proceed independently.

## Milestones

- [x] M1: Confirm the previous backend task is closed and create this documentation-entry task package.
- [x] M2: Record BDD and add the README operations entry.
- [x] M3: Verify the new README pointers and prepare a task-scoped commit.

## Expected Verification

- Manual doc review of the new `README.md` operations section
- `Select-String -Path D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\README.md -Pattern 'Operations','OPS.md','运维工具.bat'`

## Current Status

Completed on 2026-05-19. `README.md` now exposes a clear `Operations` entry that points to the verified `OPS.md` guide and the unified launcher.

## Final Verification Result

- PASS: `Select-String -Path D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\README.md -Pattern 'Operations','OPS.md','运维工具.bat'`
- PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260519-readme-ops-entry --mode preview`

## Blocker And Impact

- Blocker: none.
- Impact: none.
