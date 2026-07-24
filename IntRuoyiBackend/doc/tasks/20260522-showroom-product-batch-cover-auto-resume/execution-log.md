# Execution Log: 展厅一键封面 10 分钟后台续跑（后端）

BDD: 首轮后仍有未完成产品时必须创建后台续跑任务 -> Given 用户触发 `一键封面` 且本次快照里仍有产品未成功生成封面 / When 后端返回首轮批量结果 / Then 必须持久化后台续跑任务与产品项快照，返回任务编号、任务状态、剩余未完成数量和下一次检查时间。

BDD: 已有活动中的后台续跑任务时必须拒绝再次发起一键封面 -> Given 当前存在状态为 `WAITING` 或 `RUNNING` 的一键封面后台任务 / When 用户再次调用 `POST /showroom/product/batch-generate-cover-image` / Then 后端必须显式失败并返回活动任务冲突错误，不得创建第二个任务。

BDD: 定时续跑只应重试未成功项并在全部成功后自动停止 -> Given 后台续跑任务中存在 `WAITING` 产品项且部分产品已在前一轮成功 / When 定时检查触发下一轮续跑 / Then 后端只应重试未成功项，并在全部产品项完成后将任务状态切为 `COMPLETED` 且停止后续扫描。

BDD: 应用重启后必须回收中断中的后台续跑任务 -> Given 后台在上一轮执行中意外停止，任务状态残留为 `RUNNING` / When 应用重新启动且 showroom 后台续跑恢复逻辑执行 / Then 中断任务必须被回收为 `WAITING`，等待下一次 10 分钟检查继续执行。

RED: `mvn --% -pl yudao-module-showroom -am "-Dtest=ShowroomProductCoverBatchTaskServiceTest,ShowroomHttpApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，新增 RED 已要求 `ProductBatchGenerateRespVO` 暴露后台续跑元数据、`ShowroomProductCoverBatchTaskService` 与对应持久化类型存在；当前这些能力尚未实现。同时本仓当前脏改动下还暴露出 companion 编译缺口：`ShowroomAdminController` 调用的 `runtime.getProductBatchGenerateNarrationAudioState()` 在本轮编译阶段未成功解析，需在实现阶段一并收口并恢复 showroom 模块可编译状态。

GREEN: `mvn --% -pl yudao-module-showroom -am -DskipTests compile` -> PASS。
GREEN: `mvn --% -pl yudao-module-showroom -am "-Dtest=ShowroomProductCoverBatchTaskServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。
GREEN: `mvn --% -pl yudao-module-showroom -am "-Dtest=ShowroomApiRuntimeBatchCoverModeTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。
GREEN: `mvn --% -pl yudao-module-showroom -am "-Dtest=ShowroomHttpApiIntegrationTest#batchGenerateProductCoverImageShouldCreatePublishedRevisionAndSummarizeSkippedProducts" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。
GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-product-batch-cover-auto-resume\backend-api-evidence.md` -> PASS。
GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-product-batch-cover-auto-resume\database-schema-evidence.md` -> PASS。
GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-showroom-product-batch-cover-auto-resume --mode preview` -> PASS。
