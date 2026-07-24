# 任务：展厅产品一键讲解返回当前执行产品状态（后端）

## Goal

为 `showroom/product` 的一键讲解任务状态接口补充“当前正在执行哪个产品”的真实状态字段，并在批量任务执行过程中随着处理项变化即时持久化，供前端实时展示。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\controller\admin\ShowroomAdminController.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\controller\ShowroomApiRuntime.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\test\java\cn\iocoder\yudao\module\showroom\integration\ShowroomProductNarrationRegressionTest.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-product-narration-current-product-status\**`

## Non-Scope

- 不修改一键讲解的筛选规则与补齐规则。
- 不新增 WebSocket、SSE 或新的状态接口。
- 不改动一键语音、一键封面任务状态模型。
- 不引入 mock 成功、默认值掩盖或降级返回。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-product-batch-narration-script-recovery\task.md`
- Status before this task: `Blocked`
- Impact: 旧任务已显式记录测试/提交阻塞，不影响本次继续补齐“当前执行产品状态”字段链路。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Current state: 仓库存在并行未提交改动。
- Impact: 本任务仅修改一键讲解任务状态 VO、运行态持久化点、定向测试与任务文档，不覆盖无关改动。

## Milestones

1. 建立任务文档并锁定“状态返回当前执行产品”“处理项切换时即时更新状态”的可观察行为。
2. 先补 RED，锁定任务状态响应字段与批量循环中的更新时机。
3. 最小实现状态字段、运行态持久化与响应映射。
4. 跑定向后端回归并更新证据。
5. 执行 closeout preview。

## Expected Verification

- `mvn -pl yudao-module-showroom "-Dtest=ShowroomProductNarrationRegressionTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-showroom-product-narration-current-product-status --mode preview`

## Current Status

Completed.

## Completed Work

- 为一键讲解任务状态响应补充 `currentProduct`，后端会返回当前运行产品的 ID、产品编码和中文名。
- 在批量讲解循环中新增运行态持久化：进入每个产品时先写入当前产品，处理后写入最新统计，完成后清空当前产品。
- 保持原有续跑语义不变：成功生成语言数可累计，失败数按每轮续跑重新统计，恢复已有回归用例预期。
- 已完成后端定向回归与 closeout preview。

## Verification Result

- PASS: `mvn -pl yudao-module-showroom "-Dtest=ShowroomProductNarrationRegressionTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-showroom-product-narration-current-product-status --mode preview`

## Remaining Blockers

- 无
