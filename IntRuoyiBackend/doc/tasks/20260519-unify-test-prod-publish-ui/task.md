# Task: Unify Test Publish And Test-To-Prod Promotion UI

## Goal

Place the test publish flow and the production promotion flow in one operator UI so an operator can choose either:

- publish the current local workspace to the test server
- promote the current test-server runtime to the production server

## Scope

- Confirm the latest same-repository backend task is explicitly completed before starting this UI unification task.
- Record BDD and strict TDD evidence for the unified publish UI and the test-to-production promotion path.
- Keep the existing test publish wrapper available.
- Change the production publish path so it promotes the tested release from `172.30.30.58` to `172.30.30.57` instead of rebuilding locally for production.
- Update the repository-root `运维工具.bat` publish UI so operators choose between test publish and test-to-production promotion in one place.
- Update operator-facing documentation to describe the new publish flow.

## Previous Task Check

- Previous backend task: `doc/tasks/20260519-clean-showroom-commit-mixup/task.md`
- Status before this task: completed.
- Impact: the previous cleanup task is already closed, so this UI and promotion change can proceed independently.

## Milestones

- [x] M1: Confirm the previous backend task is closed and create this task package.
- [x] M2: Record BDD and RED evidence for the unified UI and promotion wrapper.
- [x] M3: Implement the unified publish UI and the test-to-production promotion script/wrapper.
- [x] M4: Verify the wrapper/tooling behavior, update operator docs, and run closeout preview.

## Expected Verification

- `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q`
- `cmd /c D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\publish-int-ruoyi-to-prod.bat cancel`
- `cmd /c D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\运维工具.bat help`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260519-unify-test-prod-publish-ui --mode preview`

## Current Status

Completed on 2026-05-19. The repository-root launcher now exposes one publish UI with both `test publish` and `test-to-production promotion`, and the production wrapper now promotes the current tested runtime from the test server instead of rebuilding locally for production.

## Final Verification Result

- PASS: `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q`
- PASS: PowerShell parse validation of `script\deploy\promote-int-ruoyi-test-to-prod.ps1`
- PASS: `cmd /c D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\publish-int-ruoyi-to-prod.bat cancel`
- PASS: `cmd /c D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\运维工具.bat help`
- PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260519-unify-test-prod-publish-ui --mode preview`

## Blocker And Impact

- Blocker: none.
- Impact: none.
