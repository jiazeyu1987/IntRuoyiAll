# Task: Merge DCC IntAuth position sync into int_main

## Goal

Bring the DCC IntAuth-backed approval-position sync behavior into `int_main` on top of the existing DCC backend baseline, without introducing fallback behavior or disturbing unrelated local work.

## Scope

- Backend repository only.
- Port the DCC IntAuth position-sync delta into `int_main`.
- Keep the current `int_main` DCC module baseline intact.
- Add only the configuration, service, error-code, and regression-test changes required by the approval-position sync slice.

## Previous Task Check

- Previous backend task: `doc/tasks/20260514-erp-kingdee-config-page/task.md`
- Status before this task: completed.
- Impact: no open backend blocker from the previous `int_main` task prevents this merge task.

## Milestones

- [x] M1: Inspect the source DCC branch and confirm the missing-source blocker is resolved.
- [x] M2: Identify the minimal DCC position-sync delta needed on `int_main`.
- [x] M3: Apply the position-sync delta on `int_main` without broad DCC history replay.
- [x] M4: Run targeted compile and regression verification on `int_main`.
- [x] M5: Record evidence and prepare a scoped backend commit.

## Expected Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc clean compile -DskipTests`
- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -Dtest=DccApprovalPositionAdminServiceImplTest,DccIntAuthPositionClientImplTest -Dsurefire.failIfNoSpecifiedTests=false test`

## Current Status

Completed. `int_main` now contains the DCC approval-position IntAuth sync delta and passes targeted DCC module compile plus position-sync regression verification.
