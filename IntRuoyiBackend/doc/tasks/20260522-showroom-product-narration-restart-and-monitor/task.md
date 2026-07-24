# 任务：展厅产品一键讲解重启并观察推进（后端）

## Goal

在修复 Codex CLI 卡住问题后，通过真实前端入口重新启动一轮一键讲解任务，并持续观察状态接口与运行态，确认任务能够正常推进。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-product-narration-restart-and-monitor\**`

## Non-Scope

- 不新增后端业务代码修改。
- 不改动任务筛选、补齐策略或审批语义。
- 不用 mock 结果替代真实运行态。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-product-narration-stuck-running-diagnosis\task.md`
- Status before this task: `Completed`
- Impact: 上一任务已完成卡住根因修复与运行态重启，不阻塞本次继续启动新任务并观察推进。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Current state: 仓库存在并行未提交改动。
- Impact: 本任务只新增任务文档和运行证据，不覆盖无关改动。

## Milestones

1. 通过真实页面启动新一轮任务。
2. 连续读取状态接口，确认新任务进入活动态。
3. 持续观察直到出现真实推进证据或新的阻塞信号。
4. 更新证据并执行 closeout preview。

## Expected Verification

- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-showroom-product-narration-restart-and-monitor --mode preview`

## Current Status

Completed.

## Completed Work

- 使用测试租户真实鉴权重新启动了一轮一键讲解任务。
- 启动响应显示本轮初始缺口已从旧任务的 `171` 降到 `47`。
- 连续轮询状态接口，确认任务在 30 秒内出现真实推进，不再是旧的假死 `running=true` 无变化状态。

## Verification Result

- PASS: `POST /admin-api/showroom/product/batch-generate-narration-script/start`
  - returned `active=true`、`remainingCount=47`、`startedAt=1779427290332`
- PASS: status polling confirms progress
  - poll1: `running=true`、`remainingCount=47`、`generatedLanguageCount=0`、`currentProduct=product_125 / 无菌抽吸管路`
  - poll2 after 30s: `running=true`、`remainingCount=46`、`generatedLanguageCount=1`、`currentProduct=product_126 / 斑马导丝`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-showroom-product-narration-restart-and-monitor --mode preview`

## Remaining Blockers

- 无当前阻塞；新任务已正常推进。
