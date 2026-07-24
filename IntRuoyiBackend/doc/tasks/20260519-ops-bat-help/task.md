# Task: Add Help Page To Unified Ops Bat

## Goal

Add a help page and command quick reference to the repository-root `运维工具.bat` so operators can see the supported direct command modes without opening the script source.

## Scope

- Confirm the latest same-repository backend task is explicitly completed before starting this help-page task.
- Record BDD for help behavior before changing the launcher.
- Add a failing script test for the help route and help menu entry.
- Implement only the minimal launcher changes needed to expose a read-only help page.
- Verify the help route in `cmd` without triggering any runtime action.

## Previous Task Check

- Previous backend task: `doc/tasks/20260519-ops-bat-grouped-menu/task.md`
- Status before this task: completed.
- Impact: the grouped-menu task is already closed, so the help-page task can proceed independently.

## Milestones

- [x] M1: Confirm the previous backend task is closed and create this help-page task package.
- [x] M2: Record BDD and RED evidence for the help route.
- [x] M3: Implement the help page and menu entry.
- [x] M4: Verify regression tests and the direct help path.

## Expected Verification

- `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q`
- `cmd /c D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\运维工具.bat help`

## Current Status

Completed on 2026-05-19. The unified launcher now exposes a help page and direct command quick reference through both `help` and a menu entry.

## Final Verification Result

- PASS: `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q`
- PASS: `cmd /c "\"D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\运维工具.bat\" help"`
- PASS: `cmd /c D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\运维工具.bat cancel`
- PASS: help output includes direct command quick reference for:
  - publish
  - restart
  - status
  - cancel
  and now expands the current bat filename to `运维工具.bat`

## Blocker And Impact

- Blocker: none.
- Impact: none.
