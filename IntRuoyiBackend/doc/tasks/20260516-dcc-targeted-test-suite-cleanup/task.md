# Task: DCC Targeted Test Suite Cleanup

## Goal

Stabilize the `ruoyi-vue-pro` DCC module test suite just enough to run the
targeted regression command for the protected controlled-file viewer backend
contract without being blocked by unrelated compile drift in newly added DCC
tests.

## Scope

- Work only in `ruoyi-vue-pro/yudao-module-dcc` task-owned source and tests.
- Repair compile and assertion drift that blocks:
  `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileQueryServiceTest,DccControlledFilePreviewDownloadApiTest" test`
- Prefer the minimal production fixes only when the compile failure is in
  current source and not only in tests.
- Update or block unrelated new tests when they reference removed fields or
  missing production APIs.
- Do not introduce fallback behavior or mock-success shortcuts.
- Do not implement the separate upload-name/version-linkage feature itself in
  this task unless it becomes the only path to unblock the targeted regression.

## Previous Task Check

- Previous DCC backend task:
  `doc/tasks/20260516-dcc-upload-name-version-linkage/task.md`
- Status before this task: blocked by explicit user reprioritization.
- Impact: that feature slice remains paused so this task can isolate DCC test
  suite cleanup work.

## BDD Scenarios

- BDD: targeted controlled-file preview regressions can compile and run ->
  Given the DCC module contains unrelated new tests and drifted fixtures, When
  the targeted controlled-file regression command runs, Then unrelated compile
  drift no longer blocks `DccControlledFileQueryServiceTest` and
  `DccControlledFilePreviewDownloadApiTest`.
- BDD: cleanup preserves current protected preview behavior -> Given the live
  protected preview route is already verified, When cleanup changes production
  code or tests, Then the DCC preview watermark contract and preview/download
  behavior stay covered by the targeted regression pair.
- BDD: fail fast on out-of-scope missing feature tests -> Given newly added
  tests depend on production API that does not exist in the checked-out main
  code, When cleanup reaches those tests, Then the task records them as a
  separate blocker instead of silently inventing the missing feature.

## Milestones

1. [x] M1: Create this task package and explicitly block the previous paused DCC task.
2. [x] M2: Record BDD plus RED evidence for the current targeted Maven blockers.
3. [x] M3: Apply minimal compile/test cleanup for training/signature/distribution drift.
4. [x] M4: Re-run targeted Maven verification until it is either green or blocked only by separate missing-feature tests.
5. [x] M5: Update final evidence and commit only task-scoped files if verification fully passes.

## Expected Verification

- `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileQueryServiceTest,DccControlledFilePreviewDownloadApiTest" test`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260516-dcc-targeted-test-suite-cleanup\backend-api-evidence.md`

## Current Status

Completed and committed. After minimal cleanup plus resuming the
upload-name/version-linkage feature slice, the targeted DCC backend regression
set runs green and the verified DCC changes have been committed in the service
repository.

## Blocker And Impact

- No remaining blocker for the targeted Maven scope.

## Final Verification Result

- `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileQueryServiceTest,DccControlledFilePreviewDownloadApiTest,DccControlledFileUploadNameOptionApiTest,DccControlledFileUploadNameOptionQueryServiceTest" test` -> PASS
