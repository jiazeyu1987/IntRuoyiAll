# 任务：展厅一键封面 10 分钟后台续跑（后端）

## Goal

为 `POST /showroom/product/batch-generate-cover-image` 增加“后台续跑任务”能力：

- 首轮请求仍立即执行一轮批量封面生成；
- 同时持久化本次点击对应的筛选条件、模式和待生成产品快照；
- 如果首轮后仍有未完成产品，则由 showroom 模块内定时任务每 10 分钟继续重试，直到全部成功后自动停止；
- 如果当前已存在未完成后台续跑任务，则拒绝再次发起新的 `一键封面`。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\showroom\20260519_showroom_v1_schema.sql`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\mysql\**` 中与本任务直接相关的 showroom 增量脚本
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\cover\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\controller\ShowroomApiRuntime.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\controller\admin\ShowroomAdminController.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\test\**` 中与本任务直接相关的 showroom 测试
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-product-batch-cover-auto-resume\**`

## Non-Scope

- 不新增后台任务管理页面或独立查询接口。
- 不改产品单条 `AI生成` 入口。
- 不引入 Quartz 动态任务或 fallback 兼容链路。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-product-bilingual-tabs\task.md`
- Status before this task: `Blocked on 2026-05-21`
- Impact: 上一后端任务已显式记录编译与提交边界阻塞；本次继续在 showroom 模块上叠加批量封面后台续跑，不回退既有双语与封面生成链路。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Current state: showroom 相关 schema、controller、cover service 与测试文件存在未提交并行改动。
- Impact: 本任务只允许修改批量封面续跑所需 schema、cover 持久化与调度、接口返回、定向测试与本任务文档，不能覆盖无关并行改动。

## Milestones

1. 创建任务文档并确认上一同仓任务状态。
2. 先补 RED，锁定批量封面后台续跑任务、活动任务冲突和定时续跑行为。
3. 最小实现 schema、DO/Mapper、任务服务、调度恢复与接口返回扩展。
4. 跑通 showroom 定向单测 / 集成测试、证据校验与 closeout preview。
5. 按任务边界提交当前后端仓库改动。

## Expected Verification

- `mvn --% -pl yudao-module-showroom -am "-Dtest=ShowroomProductCoverBatchTaskServiceTest,ShowroomApiRuntimeBatchCoverModeTest,ShowroomHttpApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-product-batch-cover-auto-resume\backend-api-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-product-batch-cover-auto-resume\database-schema-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-showroom-product-batch-cover-auto-resume --mode preview`

## Current Status

Completed with commit-boundary blocker on 2026-05-22.

## Completed Work

- 已新增 `showroom_product_cover_batch_task` 与 `showroom_product_cover_batch_task_item` 两张表及其 DO/Mapper、正式 schema、测试 schema、MySQL 增量脚本。
- 已新增 `ShowroomProductCoverBatchTaskService` 与 `ShowroomProductCoverBatchResumeScheduler`，实现任务快照持久化、首轮执行、启动恢复和每 10 分钟续跑。
- 已将 `batchGenerateProductCoverImage(...)` 改为“筛选快照 -> 创建任务 -> 立即跑首轮 -> 返回任务元数据”。
- 已扩展 `ProductBatchGenerateRespVO`，补齐 `taskId / taskStatus / remainingPendingCount / nextCheckAt`。
- 已补齐 showroom 定向单测、runtime mode 单测、foundation contract、单方法 HTTP 集成验证，并顺手收口当前仓库中阻塞编译的若干 showroom 测试 / mock 漂移问题。

## Final Verification Result

- PASS: `mvn --% -pl yudao-module-showroom -am -DskipTests compile`
- PASS: `mvn --% -pl yudao-module-showroom -am "-Dtest=ShowroomProductCoverBatchTaskServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS: `mvn --% -pl yudao-module-showroom -am "-Dtest=ShowroomApiRuntimeBatchCoverModeTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS: `mvn --% -pl yudao-module-showroom -am "-Dtest=ShowroomHttpApiIntegrationTest#batchGenerateProductCoverImageShouldCreatePublishedRevisionAndSummarizeSkippedProducts" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-product-batch-cover-auto-resume\backend-api-evidence.md`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-product-batch-cover-auto-resume\database-schema-evidence.md`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-showroom-product-batch-cover-auto-resume --mode preview`

## Blockers And Impact

- Blocker: `ShowroomApiRuntime.java`、`ShowroomAdminController.java`、`ShowroomProductCoverImageService.java`、`sql/showroom/20260519_showroom_v1_schema.sql` 及多份 showroom 相关测试文件当前混入并行任务未提交改动，无法安全切出只包含本任务的独立后端 commit。
- Impact: 后端功能与验证已完成，但本次不执行仓库提交，避免把无关在途改动一并带入。
