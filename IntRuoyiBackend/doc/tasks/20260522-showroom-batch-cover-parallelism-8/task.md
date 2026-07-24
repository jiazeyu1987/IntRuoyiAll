# 任务：展厅一键封面批量生成默认并发提升到 8（后端）

## Goal

将 `showroom/product` 的一键封面批量生成默认并发能力提升为“最多 8 个 Codex CLI 同时为不同产品生成封面”，并保持：

- 待生成产品数量大于等于 8 时，最多并发 8 个；
- 待生成产品数量少于 8 时，并发数等于待生成产品数量；
- 并发数配置仍必须大于 0，非法值继续 fail-fast；
- 不引入 fallback、mock 成功或静默降级。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\cover\ShowroomProductCoverBatchTaskService.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\cover\ShowroomProductCoverImageService.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-ai\src\main\java\cn\iocoder\yudao\module\ai\framework\ai\config\YudaoAiProperties.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\test\java\cn\iocoder\yudao\module\showroom\cover\ShowroomProductCoverBatchTaskServiceTest.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\test\java\cn\iocoder\yudao\module\showroom\cover\ShowroomProductCoverImageServiceTest.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-batch-cover-parallelism-8\**`

## Non-Scope

- 不修改一键封面前端交互。
- 不修改单产品 `AI生成` 入口。
- 不修改 Codex CLI 命令本身的图片生成提示词。
- 不扩大到一键讲解或一键语音的并发策略。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-batch-audio-token-fix-retry\task.md`
- Status before this task: `Completed`
- Impact: 上一任务已完成 token 修复后复测，不阻塞本次批量封面并发上限调整。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Current state: 仓库存在大量并行未提交改动，含 showroom / DCC / MES 在途修改。
- Impact: 本任务仅允许修改一键封面并发默认值、相关定向测试与任务文档，不覆盖无关改动。

## Milestones

1. 建立任务文档并锁定“默认最多 8 并发、少于 8 按实际数量”的可观察行为。
2. 先补 RED，锁定默认并发上限与批任务执行并发选择规则。
3. 最小实现默认并发从 3 提升到 8，并保持非法配置 fail-fast。
4. 跑定向回归并更新证据。
5. 执行 closeout preview，并按任务边界提交。

## Expected Verification

- `mvn -pl yudao-module-showroom "-Dtest=ShowroomProductCoverBatchTaskServiceTest,ShowroomProductCoverImageServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-showroom-batch-cover-parallelism-8 --mode preview`

## Current Status

Completed.

## Completed Work

- 已将 Codex CLI 并发默认值从 `3` 提升到 `8`：
  - `YudaoAiProperties.CodexCli.parallelism`
  - `ShowroomProductCoverImageService` 的默认并发回退值
- 已在 `ShowroomProductCoverImageService.resolveBatchParallelism()` 中增加上限裁剪，确保即使配置值大于 8，实际也只会取到 8。
- 已在 `ShowroomProductCoverBatchTaskService` 中显式通过 `resolveExecutionParallelism(configuredMaximumParallelism, waitingItemCount)` 计算本轮并发，锁定 `min(配置上限, 待处理数量)` 规则。
- 已补充/更新定向测试：
  - `ShowroomProductCoverImageServiceTest`：默认值改为 8，配置值大于 8 时仍裁剪到 8，非法值继续 fail-fast
  - `ShowroomProductCoverBatchTaskServiceTest`：显式断言 `min(8, waitingItemCount)` 规则

## Verification Result

- PASS: `mvn -pl yudao-module-showroom -am "-Dmaven.test.skip=true" compile`
- PASS: `mvn -pl yudao-module-showroom -am clean "-Dtest=ShowroomProductCoverBatchTaskServiceTest,ShowroomProductCoverImageServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-showroom-batch-cover-parallelism-8 --mode preview`

## Current Assessment

- 需求“最多 8 个 Codex CLI 并发、少于 8 按实际数量”已在代码中实现。
- 定向测试与生产代码编译均已通过。
