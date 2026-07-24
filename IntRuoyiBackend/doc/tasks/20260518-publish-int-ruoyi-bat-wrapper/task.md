# Task: Add Bat Wrapper For IntRuoyi Test Publish Script

## Goal

Wrap the existing verified PowerShell test-publish script with a Windows `.bat` entrypoint so the release can be started directly from `cmd` or by double-clicking.

## Scope

- Confirm the latest same-repository backend task is explicitly completed before starting this wrapper task.
- Record BDD for the wrapper behavior before changing any deployment tooling.
- Add the minimal `.bat` launcher that forwards arguments to the verified PowerShell publish script.
- Add a script-level regression test for the `.bat` wrapper.
- Verify the wrapper by running the real test publish flow through the `.bat` entrypoint.

## Previous Task Check

- Previous backend task: `doc/tasks/20260518-publish-script-reverify/task.md`
- Status before this task: completed.
- Impact: the verified publish-script task is already closed, so the wrapper task can proceed independently.

## Milestones

- [x] M1: Confirm the previous backend task is closed and create this wrapper task package.
- [x] M2: Record BDD and add RED verification for the bat wrapper.
- [x] M3: Implement the bat wrapper and add the regression test.
- [x] M4: Verify that the bat wrapper can publish successfully against the real test server.

## Expected Verification

- `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q`
- `cmd /c D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\publish-int-ruoyi-to-test.bat`

## Current Status

Completed on 2026-05-18. The `.bat` wrapper now launches the verified PowerShell publish script and has been proven with a full real test-server release run.

## Final Verification Result

- PASS: `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q`
- PASS: `cmd /c D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\publish-int-ruoyi-to-test.bat`
- PASS: wrapper-driven publish output ended with:
  - `Publish completed.`
  - `Frontend: http://172.30.30.58:8081`
  - `Backend health: http://172.30.30.58:48081/actuator/health`

## Blocker And Impact

- Blocker: none.
- Impact: none.
