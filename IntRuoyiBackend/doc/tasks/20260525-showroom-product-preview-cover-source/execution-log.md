# Execution Log: 20260525-showroom-product-preview-cover-source

BDD: 产品前台预览图直接使用封面 -> Given 已发布产品存在 `cover_image` / When 发布展厅前台配置 / Then 产品卡片和详情的 `previewImageUrl` 来自该封面。

BDD: 产品缺封面不得使用 preview asset 兜底 -> Given 已发布产品没有 `cover_image` 但存在 PRODUCT preview asset / When 发布展厅前台配置 / Then 发布失败并提示产品 `cover_image` 缺失。

BDD: 产品版本中心不要求单独 preview asset -> Given 已发布产品有封面和双语讲解 / When 创建产品版本 bundle / Then bundle 可创建且 `releasePreviewAssetVersionId` 为空。

INFO: 前端仓库存在 in-progress 任务 `20260525-showroom-company-editable-fields` 与未提交改动，本任务不触碰前端文件。

RED: `mvn -pl yudao-module-showroom "-Dtest=ShowroomReleasePublisherServiceTest,ShowroomVersionCenterServiceTest" test` -> FAIL, 期望失败原因：产品缺 `cover_image` 但仍可发布、产品 bundle 仍写入 PRODUCT preview asset version。

GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomReleasePublisherServiceTest,ShowroomVersionCenterServiceTest" test` -> PASS, 12 tests。

GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomReleaseSourceSnapshotSelectionTest,ShowroomProductCoverBatchTaskServiceTest" test` -> PASS, 10 tests。

REGRESSION: `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest" test` -> FAIL, 失败原因：旧集成测试夹具仍假定产品 preview asset 可代替 `cover_image`，且部分发布成功路径缺少版本 bundle 必需的双语讲解。处理方式：按新契约显式补充 `cover_image` 和发布成功路径的双语讲解测试数据，不改业务 fallback。

GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest" test` -> PASS, 68 tests。

GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomReleasePublisherServiceTest,ShowroomVersionCenterServiceTest,ShowroomReleaseSourceSnapshotSelectionTest,ShowroomProductCoverBatchTaskServiceTest,ShowroomReleaseAdminPublishIntegrationTest" test` -> PASS, 27 tests。

REGRESSION: `mvn -pl yudao-module-showroom test` -> FAIL, 218 tests run, 19 errors。失败阻塞不属于本任务改动：`ShowroomAppConfigCompanyFieldsContractTest` 等精简 Spring 测试缺少 `ShowroomImagePromptVersionService` 或 `ShowroomVersionBundleService` Bean；`ShowroomVersionCenterBackfillContractTest` 在模块工作目录读取 `sql/showroom/20260523_showroom_version_center_backfill.sql` 失败。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260525-showroom-product-preview-cover-source/backend-api-evidence.md` -> PASS。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260525-showroom-product-preview-cover-source --mode preview` -> PASS, keep task.md / execution-log.md / backend-api-evidence.md, delete none, blocked none。
