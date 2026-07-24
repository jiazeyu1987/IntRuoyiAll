# Task: Add Unified Ops Bat Entry

## Goal

Add a single repository-root `运维工具.bat` entrypoint similar to the RagflowAuth maintenance launcher so operators can choose test or production publish flows from one place.

## Scope

- Confirm the latest same-repository backend task is explicitly completed before starting this unified launcher task.
- Record BDD for the unified launcher behavior before changing deployment tooling.
- Add a failing script test for the new unified launcher contract.
- Implement only the minimal unified `.bat` launcher needed to route to the existing test and production wrappers.
- Verify the safe cancel path so the unified launcher does not accidentally trigger a release during validation.

## Previous Task Check

- Previous backend task: `doc/tasks/20260518-publish-int-ruoyi-to-prod-server/task.md`
- Status before this task: completed.
- Impact: the production release task is already closed, so the unified launcher task can proceed independently.

## Milestones

- [x] M1: Confirm the previous backend task is closed and create this unified launcher task package.
- [x] M2: Record BDD and RED evidence for the unified launcher contract.
- [x] M3: Implement the unified launcher and routing behavior.
- [x] M4: Verify the cancel path and regression tests.

## Expected Verification

- `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q`
- `cmd /c D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\运维工具.bat cancel`

## Current Status

Completed on 2026-05-18. The repository root now has a unified `运维工具.bat` launcher that routes to the existing test and production publish wrappers.

## Final Verification Result

- PASS: `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q`
- PASS: `cmd /c D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\运维工具.bat cancel`
- PASS: the unified launcher exposes:
  - test route
  - production route
  - safe cancel path

## Blocker And Impact

- Blocker: none.
- Impact: none.
