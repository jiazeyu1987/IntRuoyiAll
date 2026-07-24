# 任务：展厅产品一键讲解定时续跑（后端）

## Goal

为 `showroom/product` 增加“批量讲解稿生成 + 10 分钟自动续跑”的后端能力，并确保：

- 点击 `一键讲解` 后立即以当前筛选条件快照启动异步任务；
- 当前版本已有中文则只补英文，已有英文则只补中文，双语都已有则整条跳过；
- 活动任务状态持久化到 `infra` 参数配置表，重启后仍能继续；
- 10 分钟自动检查只处理仍有缺口的同一批次，全部完成后自动停止；
- 同一时刻只允许一个活动中的一键讲解任务。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\controller\ShowroomApiRuntime.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\controller\admin\ShowroomAdminController.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\job\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\service\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\test\java\cn\iocoder\yudao\module\showroom\integration\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-product-batch-narration-script-recovery\**`

## Non-Scope

- 不自动生成语音。
- 不自动发布讲解稿。
- 不把范围改成全量产品或当前页产品。
- 不新增 fallback、静默吞错、mock 成功或兼容分支。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-product-batch-audio-auto-check\task.md`
- Status before this task: `Blocked due scope switch`
- Impact: 旧任务已显式暂停，不阻塞本次一键讲解后端交付。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Current state: 仓库存在多组 showroom / AI / SQL 在途改动。
- Impact: 本任务仅允许修改一键讲解相关代码、定向测试与本任务文档，不覆盖无关改动。

## Milestones

1. 建立任务文档并锁定批量讲解状态字段、跳过规则、停止条件与并发语义。
2. 先补 RED，锁定启动接口、状态查询、按语言补缺、重启续跑、自动停止与重入保护。
3. 最小实现状态持久化、共享执行器、10 分钟调度器与异步启动。
4. 扩展控制器返回类型与失败明细。
5. 跑定向回归、更新证据并执行 closeout preview。

## Expected Verification

- `mvn -pl yudao-module-showroom "-Dtest=ShowroomProductNarrationRegressionTest,ShowroomHttpApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-showroom-product-batch-narration-script-recovery --mode preview`

## Current Status

Blocked.

## Completed Work

- Added `POST /showroom/product/batch-generate-narration-script/start` and `GET /showroom/product/batch-generate-narration-script/status`.
- Implemented current-version bilingual narration draft gap filling, active-task scope pinning, and 10-minute scheduled resume.
- Added `ShowroomProductBatchNarrationScriptAutoCheckScheduler`.
- Passed targeted `ShowroomProductNarrationRegressionTest`.
- Rebuilt `yudao-server.jar` from current source and restored live backend startup after applying the missing local runtime table SQL for `showroom_product_cover_batch_task`.

## Blockers

- `ShowroomHttpApiIntegrationTest` timed out twice within 300000ms, so broader integration status is still unknown.
- The repository currently has many overlapping uncommitted showroom changes, so a task-only clean commit cannot be created safely.
