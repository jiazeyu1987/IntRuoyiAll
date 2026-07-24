# Task: Merge DCC backend history into int_main

## Goal

Integrate the compile-clean DCC backend history from `feature/dcc-v1-backend` into `int_main` in a safe order, while skipping the rejected IntAuth runtime file-category and position sync commits.

## Scope

- Backend repository only.
- Cherry-pick the safe DCC backend commit sequence into `int_main`.
- Exclude rejected IntAuth runtime sync commits from the integration path.
- Add the minimal follow-up compile fix needed after replaying the selected DCC history.
- Verify the merged DCC module on real `int_main`.

## Previous Task Check

- Previous backend task: `doc/tasks/20260514-erp-kingdee-config-page/task.md`
- Status before this task: completed.
- Impact: no open backend blocker from the previous `int_main` task prevented this merge task.

## Milestones

- [x] M1: Check the latest backend task status before starting the merge.
- [x] M2: Create this merge task document before Git changes.
- [x] M3: Inspect branch divergence and choose a safe merge strategy for DCC history.
- [x] M4: Integrate the safe DCC branch history into `int_main`.
- [x] M5: Run targeted verification on the merged DCC module.
- [x] M6: Record evidence and complete the merge with a scoped commit.

## Expected Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc clean compile -DskipTests`
- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -Dtest=DccFileCategoryMapperTest,DccCategoryPermissionAdminServiceImplTest,DccControlledFileWorkflowServiceImplTest,DccControlledFileFinalizationServiceImplTest,DccControlledFileQueryServiceTest,DccControlledFileTaskActionApiTest,DccTrainingAssignmentAckServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`

## Current Status

Completed. `int_main` now contains the DCC backend history through `任务: 补齐DCC模块缺失源码`, and the branch compiles after one minimal local follow-up patch that restores DCC-specific error codes `1_080_000_022` through `1_080_000_028`.

## Blocker And Impact

- Blocker: none remaining for this merge task.
- Impact: `int_main` now contains the compile-clean DCC module. The runtime file-category database import was handled separately and did not require additional Git history here.

## Final Verification Result

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc clean compile -DskipTests`
- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -Dtest=DccFileCategoryMapperTest,DccCategoryPermissionAdminServiceImplTest,DccControlledFileWorkflowServiceImplTest,DccControlledFileFinalizationServiceImplTest,DccControlledFileQueryServiceTest,DccControlledFileTaskActionApiTest,DccTrainingAssignmentAckServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`

## Completion Status

- `int_main` cherry-picked DCC commits through `7d9d2897af` (`任务: 补齐DCC模块缺失源码`).
- This task also added one local follow-up commit for the missing DCC error codes after cherry-pick verification.
