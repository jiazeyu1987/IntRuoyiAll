# Task: Group Unified Ops Menu By Action Type

## Goal

Refine the repository-root `运维工具.bat` menu so publish, restart, status, and cancel actions are grouped clearly, making the launcher easier to scan without changing any underlying deployment behavior.

## Scope

- Confirm the latest same-repository backend task is explicitly completed before starting this menu-grouping task.
- Record BDD for grouped-menu behavior before changing the launcher.
- Add a failing script test for the grouped menu copy and structure.
- Implement only the minimal menu-display changes needed to group the existing routes.
- Re-verify the safe cancel path after the menu update.

## Previous Task Check

- Previous backend task: `doc/tasks/20260519-ops-bat-status-modes/task.md`
- Status before this task: completed.
- Impact: the status-mode task is already closed, so the grouped-menu task can proceed independently.

## Milestones

- [x] M1: Confirm the previous backend task is closed and create this grouped-menu task package.
- [x] M2: Record BDD and RED evidence for the grouped menu structure.
- [x] M3: Implement the grouped menu layout with the minimal launcher changes.
- [x] M4: Verify regression tests and the cancel path.

## Expected Verification

- `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q`
- `cmd /c D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\运维工具.bat cancel`

## Current Status

Completed on 2026-05-19. The unified launcher now uses a two-level grouped menu: first choose `Publish / Restart / Status / Cancel`, then choose `Test / Production` for the selected action.

## Final Verification Result

- PASS: `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q`
- PASS: `cmd /c D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\运维工具.bat cancel`
- PASS: grouped menu headings now expose:
  - root level `Publish / Restart / Status / Cancel`
  - nested target selection `Test / Production / Cancel`

## Blocker And Impact

- Blocker: none.
- Impact: none.
