# 执行日志：提交 Showroom 后端当前代码

BDD: 只提交已验证的 showroom 后端当前代码 -> Given 后端仓库同时存在 showroom 已验证改动与 MES 在途改动, When 本次执行 Git 提交, Then 只能提交 showroom 相关源码、测试与 SQL，且不混入 MES、图片产物或无关 task 残留

INFO: 2026-05-22 已将本次后端提交范围锁定为 showroom 相关代码线
RED: `mvn -pl yudao-module-showroom,yudao-module-ai -am "-Dtest=ShowroomPersistentContentServiceTest,ShowroomApiRuntimeProductCoverPersistenceTest,ShowroomProductCoverImageServiceTest,ShowroomFoundationContractTest,ShowroomAppConfigCompanyFieldsContractTest,ShowroomHttpApiIntegrationTest,ShowroomProductExcelImportExportIntegrationTest,ShowroomProductNarrationRegressionTest,ShowroomApiRuntimeBatchCoverModeTest,ShowroomProductCoverBatchTaskServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，提交前该集合曾因 `websiteConfig` 契约与前台 payload 断言不一致而未通过
GREEN: `mvn -pl yudao-module-showroom,yudao-module-ai -am "-Dtest=ShowroomPersistentContentServiceTest,ShowroomApiRuntimeProductCoverPersistenceTest,ShowroomProductCoverImageServiceTest,ShowroomFoundationContractTest,ShowroomAppConfigCompanyFieldsContractTest,ShowroomHttpApiIntegrationTest,ShowroomProductExcelImportExportIntegrationTest,ShowroomProductNarrationRegressionTest,ShowroomApiRuntimeBatchCoverModeTest,ShowroomProductCoverBatchTaskServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS
GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260522-commit-showroom-backend-current-code --mode preview` -> PASS
