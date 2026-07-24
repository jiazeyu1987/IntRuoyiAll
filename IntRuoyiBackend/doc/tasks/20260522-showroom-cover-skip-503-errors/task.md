# 任务：展厅一键封面遇到 503 错误直接跳过（后端）

## Goal

将一键封面批任务中的 OpenAI native `image_generation` `503 Service temporarily unavailable` 视为可跳过错误：遇到该错误时直接把当前产品记为失败完成，不再回到 `WAITING` 反复续跑。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\cover\ShowroomProductCoverBatchTaskService.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\test\java\cn\iocoder\yudao\module\showroom\cover\ShowroomProductCoverBatchTaskServiceTest.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-cover-skip-503-errors\**`

## Non-Scope

- 不修改 OpenAI 上游服务。
- 不新增 503 自动重试。
- 不修改一键封面前端交互。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-cover-503-error-message-hardening\task.md`
- Status before this task: `Completed`
- Impact: 上一任务已把 503 错误信息收敛干净，不阻塞本次继续把 503 归类为“直接跳过”。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Current state: 仓库存在并行未提交改动。
- Impact: 本任务仅修改封面批任务 503 错误分类、定向测试与任务文档，不覆盖无关改动。

## Milestones

1. 建立任务文档并锁定“503 错误直接跳过”的可观察行为。
2. 先补 RED，构造封面生成返回 503 错误文本的批任务场景。
3. 最小实现 503 错误分类为非重试失败。
4. 跑定向回归并更新证据。
5. 执行 closeout preview 并按任务边界提交。

## Expected Verification

- `mvn -pl yudao-module-showroom -am clean "-Dtest=ShowroomProductCoverBatchTaskServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-showroom-cover-skip-503-errors --mode preview`

## Current Status

Completed.

## Completed Work

- 已将一键封面批任务中的 `503 Service temporarily unavailable` 归类为“非重试失败”。
- 遇到这类错误时，任务项现在会直接标记为 `FAILED`，不再回到 `WAITING` 反复续跑。
- 已补充定向回归测试，锁定“503 直接跳过”的行为。

## Verification Result

- PASS: `mvn -pl yudao-module-showroom -am clean "-Dtest=ShowroomProductCoverBatchTaskServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-showroom-cover-skip-503-errors --mode preview`

## Final Assessment

- 你要求的“503 正常错误，遇到就跳过”已落地。
- 当前封面批任务后续再遇到 `503 Service temporarily unavailable`，会直接跳过该产品，不再因为同一类 503 一直重试占着队列。
