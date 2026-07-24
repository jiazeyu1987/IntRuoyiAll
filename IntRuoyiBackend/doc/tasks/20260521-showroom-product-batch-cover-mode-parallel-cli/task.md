# 任务：展厅产品批量封面支持模式选择与多 Codex CLI 并发

## Goal

扩展展厅产品批量封面接口，使 `一键生成所有封面` 支持两种明确模式：

- `ALL`：重新生成所有已发布产品封面
- `MISSING_ONLY`：只处理当前没有封面的已发布产品

同时按用户要求，把批量封面从当前串行执行改为有界并发，允许一次批量中同时跑多个本地 Codex CLI 进程生成封面，并继续保留失败汇总、未发布跳过和 fail-fast 校验语义。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-ai\src\main\java\cn\iocoder\yudao\module\ai\framework\ai\config\YudaoAiProperties.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\controller\admin\ShowroomAdminController.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\controller\ShowroomApiRuntime.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\cover\ShowroomProductCoverImageService.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\test\java\cn\iocoder\yudao\module\showroom\cover\ShowroomProductCoverImageServiceTest.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\test\java\cn\iocoder\yudao\module\showroom\integration\ShowroomHttpApiIntegrationTest.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-product-batch-cover-mode-parallel-cli\**`

## Non-Scope

- 不改动单产品封面生成 prompt 结构。
- 不新增数据库 schema 或 fallback 兼容分支。
- 不恢复其他图片平台，也不绕过本地 Codex CLI。
- 不顺带修复与公司字段翻译无关的其他在途后端任务。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-cover-product-002-image-v3\task.md`
- Status before this task: `Blocked on 2026-05-21`
- Impact: 上一同仓图片任务已显式暂停，不再占用当前线程的本地图片生成额度；本次可继续处理批量封面代码行为。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Current state: 仓库存在公司字段翻译相关的在途 controller / runtime / integration test 改动。
- Impact: 本任务只允许在目标后端文件上叠加批量封面模式与并发逻辑，必须与现有并行改动兼容，不能回退无关内容。

## Milestones

- [x] M1: 创建任务文档并补齐上一同仓未收口任务状态。
- [x] M2: 先补 RED，锁定 `ALL / MISSING_ONLY`、已有封面跳过统计和多 CLI 并发汇总行为。
- [x] M3: 完成批量封面请求/响应契约、模式过滤和有界并发实现。
- [x] M4: 跑通定向后端测试并更新执行日志与证据。
- [x] M5: 执行 closeout preview，并评估本仓提交边界。

## Expected Verification

- `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomProductCoverImageServiceTest,ShowroomApiRuntimeBatchCoverModeTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-product-batch-cover-mode-parallel-cli\backend-api-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260521-showroom-product-batch-cover-mode-parallel-cli --mode preview`

## Current Status

- Status: Completed with commit-boundary blocker on 2026-05-21
- Completed work:
  - 已为批量封面请求新增 `coverGenerationMode`，支持 `ALL / MISSING_ONLY`。
  - 已为批量封面结果新增 `skippedExistingCount`。
  - 已把批量封面从串行改为有界并发，默认并发数走 `CodexCli.parallelism = 3`。
  - 已补齐后端单测，验证 `MISSING_ONLY` 跳过已有封面产品，且配置并发数为 `2` 时两个封面任务可同时启动。
- Remaining blockers:
  - `ShowroomAdminController.java`、`ShowroomApiRuntime.java` 当前同时承载公司字段翻译任务的在途改动，无法在不混入并行任务内容的前提下形成纯后端任务提交。

## Final Verification Result

- PASS: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomProductCoverImageServiceTest,ShowroomApiRuntimeBatchCoverModeTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-product-batch-cover-mode-parallel-cli\backend-api-evidence.md`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260521-showroom-product-batch-cover-mode-parallel-cli --mode preview`
