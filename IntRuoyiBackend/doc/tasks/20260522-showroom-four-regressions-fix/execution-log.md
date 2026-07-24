# 执行日志：修复 Showroom 四个回归问题

BDD: 单个产品封面生成回归 -> Given 已审批通过且允许生成封面的产品, When 调用单个产品封面生成接口, Then 系统必须生成并保存封面文件记录，且不能因为缺少上传记录而失败
BDD: 产品字段中译英访问回归 -> Given 符合业务预期且可编辑产品的登录用户, When 调用产品中文字段翻译为英文接口, Then 请求应成功写入英文草稿，而不是被误判为无权访问
BDD: 批量封面任务冲突回归 -> Given 测试场景准备了可批量生成封面的产品集合, When 调用批量封面生成接口, Then 系统应基于当前数据正确创建或复用任务，不得被错误残留任务阻塞
BDD: 公司中译英空指针回归 -> Given 公司修订数据存在空字段或缺省字段, When 调用公司中文字段翻译为英文相关流程, Then 系统应安全处理空值并完成翻译流程，而不是抛出空指针

INFO: 2026-05-22 已确认最近后端任务文档均已完成，可开始当前修复任务
RED: `mvn -pl yudao-module-showroom,yudao-module-ai -am "-Dtest=ShowroomHttpApiIntegrationTest#productCoverImageShouldGenerateAndUploadAiCoverForApprovedProduct+productFieldTranslationShouldTranslateChineseFieldsAndNarrationIntoEnglishDrafts+batchGenerateProductCoverImageShouldCreatePublishedRevisionAndSummarizeSkippedProducts+companyNarrationGenerateAndPublishShouldAllowLoggedInUserWithoutChangingCompanyRevision" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，最初稳定暴露 3 个错误：封面上传记录缺失、产品中译英访问被拒、公司中译英空指针；在更大集合下还暴露批量封面活动任务残留冲突
INFO: 根因1 -> 封面集成测试夹具仍按旧的 `fileApi + imageModel` 路径建模，未适配当前 `FileService + 本地 Codex CLI` 契约
INFO: 根因2 -> `ShowroomAdminController.translateProductFieldsToEn(...)` 错误要求产品编辑权限，导致纯翻译辅助接口被误拦截
INFO: 根因3 -> 公司讲解集成测试缺失 `development_history` 翻译桩，触发 `Map.copyOf(...)` 的空值异常
INFO: 根因4 -> 批量封面测试最初走真实 Codex CLI，且并发模拟脚本共用同一 stdin 文件；同时 showroom 测试清库脚本未删除封面后台任务表，导致活动任务跨用例残留
GREEN: `mvn -pl yudao-module-showroom,yudao-module-ai -am "-Dtest=ShowroomHttpApiIntegrationTest#companyNarrationGenerateAndPublishShouldAllowLoggedInUserWithoutChangingCompanyRevision" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS
GREEN: `mvn -pl yudao-module-showroom,yudao-module-ai -am "-Dtest=ShowroomHttpApiIntegrationTest#productCoverImageShouldGenerateAndUploadAiCoverForApprovedProduct+batchGenerateProductCoverImageShouldCreatePublishedRevisionAndSummarizeSkippedProducts" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS
GREEN: `mvn -pl yudao-module-showroom,yudao-module-ai -am "-Dtest=ShowroomHttpApiIntegrationTest#productCoverImageShouldGenerateAndUploadAiCoverForApprovedProduct+productFieldTranslationShouldTranslateChineseFieldsAndNarrationIntoEnglishDrafts+batchGenerateProductCoverImageShouldCreatePublishedRevisionAndSummarizeSkippedProducts+companyNarrationGenerateAndPublishShouldAllowLoggedInUserWithoutChangingCompanyRevision" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS
GREEN: `mvn -pl yudao-module-showroom,yudao-module-ai -am "-Dtest=ShowroomProductCoverImageServiceTest,ShowroomApiRuntimeBatchCoverModeTest,ShowroomProductCoverBatchTaskServiceTest,ShowroomHttpApiIntegrationTest#productCoverImageShouldGenerateAndUploadAiCoverForApprovedProduct+productFieldTranslationShouldTranslateChineseFieldsAndNarrationIntoEnglishDrafts+batchGenerateProductCoverImageShouldCreatePublishedRevisionAndSummarizeSkippedProducts+companyNarrationGenerateAndPublishShouldAllowLoggedInUserWithoutChangingCompanyRevision" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS
BLOCKED: `mvn -pl yudao-module-showroom,yudao-module-ai -am "-Dtest=ShowroomPersistentContentServiceTest,ShowroomApiRuntimeProductCoverPersistenceTest,ShowroomProductCoverImageServiceTest,ShowroomFoundationContractTest,ShowroomAppConfigCompanyFieldsContractTest,ShowroomHttpApiIntegrationTest,ShowroomProductExcelImportExportIntegrationTest,ShowroomProductNarrationRegressionTest,ShowroomApiRuntimeBatchCoverModeTest,ShowroomProductCoverBatchTaskServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，仓库当前另有 websiteConfig 相关测试与 `ShowroomDisplayController.getWebsiteConfig()` 方法不一致，不属于本次 4 个回归修复范围
GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-four-regressions-fix\bug-regression-evidence.md` -> PASS
GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260522-showroom-four-regressions-fix --mode preview` -> PASS
