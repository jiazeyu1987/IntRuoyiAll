# Task: Add Interactive Menu To IntRuoyi Test Publish Bat

## Goal

Upgrade the existing IntRuoyi test publish `.bat` wrapper so operators can either run preset publish modes from a simple Chinese menu or keep using explicit command-line arguments.

## Scope

- Confirm the latest same-repository backend task is explicitly completed before starting this menu-enhancement task.
- Record BDD for menu-driven publish entry behavior before changing the wrapper.
- Add a failing script test for the desired `.bat` menu and preset command mapping.
- Implement only the minimal `.bat` changes needed for a menu-based operator experience.
- Re-run wrapper verification to ensure direct publish still works after the menu enhancement.

## Previous Task Check

- Previous backend task: `doc/tasks/20260518-publish-int-ruoyi-bat-wrapper/task.md`
- Status before this task: completed.
- Impact: the current `.bat` wrapper task is already closed, so the menu-enhancement task can proceed independently.

## Milestones

- [x] M1: Confirm the previous backend task is closed and create this menu-enhancement task package.
- [x] M2: Record BDD and RED evidence for the new `.bat` menu behavior.
- [x] M3: Implement the menu and preset mapping with the minimal wrapper changes.
- [x] M4: Reverify the `.bat` wrapper against the real test publish flow.

## Expected Verification

- `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q`
- `cmd /c D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\publish-int-ruoyi-to-test.bat default`

## Current Status

Completed on 2026-05-18. The `.bat` wrapper now supports both menu mode and direct preset mode, and the `default` preset has been reverified with a full real test-server publish run.

## Final Verification Result

- PASS: `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q`
- PASS: `cmd /c D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\publish-int-ruoyi-to-test.bat default`
- PASS: wrapper-driven `default` publish output ended with:
  - `Publish completed.`
  - `Frontend: http://172.30.30.58:8081`
  - `Backend health: http://172.30.30.58:48081/actuator/health`

## Blocker And Impact

- Blocker: none.
- Impact: none.
