# Task: Add Production Publish Bat Wrapper

## Goal

Add a dedicated Windows `.bat` entrypoint for publishing IntRuoyi to the production server, reusing the verified PowerShell publish script while forcing production-target parameters and an explicit production confirmation step.

## Scope

- Confirm the latest same-repository backend task is explicitly completed before starting this production-wrapper task.
- Record BDD for production-wrapper behavior before changing deployment tooling.
- Add a failing script test for the expected production `.bat` wrapper contract.
- Implement only the minimal production `.bat` wrapper needed to target the production server safely.
- Verify the wrapper itself in a non-destructive way through a safe `cancel` path.

## Previous Task Check

- Previous backend task: `doc/tasks/20260518-publish-int-ruoyi-bat-menu/task.md`
- Status before this task: completed.
- Impact: the test-wrapper menu task is already closed, so the production-wrapper task can proceed independently.

## Milestones

- [x] M1: Confirm the previous backend task is closed and create this production-wrapper task package.
- [x] M2: Record BDD and RED evidence for the production `.bat` wrapper contract.
- [x] M3: Implement the production wrapper with explicit confirmation and target parameters.
- [x] M4: Verify the wrapper cancel path and regression tests.

## Expected Verification

- `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q`
- `cmd /c D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\publish-int-ruoyi-to-prod.bat cancel`

## Current Status

Completed on 2026-05-18. A dedicated production-target `.bat` wrapper has been added with explicit confirmation and a safe `cancel` verification path.

## Final Verification Result

- PASS: `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q`
- PASS: `cmd /c D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\publish-int-ruoyi-to-prod.bat cancel`
- PASS: production wrapper pins:
  - host `172.30.30.57`
  - remote dir `/opt/intruoyi/runtime`
  - frontend port `8081`
  - backend port `48081`
  - explicit `PROD` confirmation before any real publish

## Blocker And Impact

- Blocker: none.
- Impact: none.
