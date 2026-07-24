# Execution Log: Merge DCC backend history into int_main

BDD: int_main gains the compile-clean DCC backend module -> Given `int_main` initially lacks a compile-clean `yudao-module-dcc`, When the safe DCC backend commit sequence is replayed into `int_main`, Then `int_main` contains the DCC module and passes targeted compile plus regression verification.

BDD: merge path skips rejected IntAuth runtime sync commits -> Given the user rejected current-system IntAuth runtime coupling for file-category behavior, When DCC backend history is integrated into `int_main`, Then the merge path excludes the IntAuth runtime file-category and position sync commits.

BDD: merge does not rely on fallback file recreation -> Given the DCC backend history exists as committed Git history plus one missing-source completion commit, When the integration is performed, Then `int_main` is updated by replaying committed history and one minimal local compile fix rather than by ad hoc file copying.

RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\tmp-dcc-feature-clean\pom.xml -pl yudao-module-dcc -Dtest=DccFileCategoryMapperTest -Dsurefire.failIfNoSpecifiedTests=false test` on clean detached `feature/dcc-v1-backend` HEAD -> FAIL, the committed branch history still referenced non-committed DCC source files such as `DccControlledFileMasterDO`, `DccFileCategoryDistributionRuleDO`, related mappers, and DCC status enums.

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\.worktrees\dcc-v1-backend\pom.xml -pl yudao-module-dcc clean compile -DskipTests` -> PASS after `bbdc00de5c` (`任务: 补齐DCC模块缺失源码`) completed the source branch.

GREEN: cherry-picked DCC commits `e9cde42e97` through `7ff4a8f370` plus `bbdc00de5c` into a detached `int_main` snapshot, skipped `a024f4e334`, `89b8861cbd`, `5762cf4304`, and `3e9e25aac7`, then added the minimal `ErrorCodeConstants` follow-up patch for error codes `22-28`.

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\tmp-dcc-merge-int-main\pom.xml -pl yudao-module-dcc clean compile -DskipTests` -> PASS

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\tmp-dcc-merge-int-main\pom.xml -pl yudao-module-dcc -Dtest=DccFileCategoryMapperTest,DccCategoryPermissionAdminServiceImplTest,DccControlledFileWorkflowServiceImplTest,DccControlledFileFinalizationServiceImplTest,DccControlledFileQueryServiceTest,DccControlledFileTaskActionApiTest,DccTrainingAssignmentAckServiceTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 47 tests passed.

GREEN: replayed the same selected cherry-pick sequence plus the same minimal error-code patch onto the real `int_main` branch, and the same compile plus 47-test verification passed there as well.
