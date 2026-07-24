# 执行日志：MES 工单无阻断但未生成排产任务跟进 后端实现

BDD: 无阻断时工单应真正进入排产结果 -> Given 工单排产前置已满足且当前范围允许生成任务 / When 执行自动排产预览 / Then 返回 generatedTaskCount 大于 0 或明确暴露新的真实前置原因。

BDD: 仍有隐藏前置时必须继续暴露 -> Given 工单没有 blocking issue 但仍无法生成任务 / When 查询真实预览与主数据 / Then 系统或排查结果必须定位出导致 generatedTaskCount=0 的真实根因。

GREEN: previous-task-check -> PASS，上一后端任务 `20260628-mes-work-order-clear-all-blockers` 已完成。
GREEN: experience-index-hit -> PASS，已命中并读取 `docs/powershell-memory.md`。
GREEN: experience-index-hit-login -> PASS，已命中并读取 `docs/login-access.md`。
GREEN: experience-preflight -> PASS，本次先在本机运行态执行真实库只读核查与真实预览复验；未确认根因前不做库写入。
GREEN: zero-generated-repro -> PASS，真实 `/admin-api/mes/pro/auto-schedule/preview` 对 `scheduleOrderId=48` 返回 `generatedTaskCount=0`、`blockingIssueCount=0`，并包含 `LATEST_START` warning。
GREEN: apply-repro -> PASS，真实 `/admin-api/mes/pro/auto-schedule/apply` 返回业务失败 `最晚开工约束导致未生成任何任务，禁止发布：SCH-TESTERPA9ED2D417434-20260628-0001`。
GREEN: root-cause-located -> PASS，代码与真实库联合核对确认 `LATEST_START` 会在预览阶段丢弃候选生成步骤；`mes_pro_schedule_order.id=48` 当前 `promise_date=2026-06-20` 早于真实候选排程窗口结束日，属于过期交期前置。
GREEN: schedule-order-update -> PASS，已通过正式接口 `/admin-api/mes/pro/schedule-order/update` 调整 `scheduleOrderId=48 / promiseDate=2026-08-25`。
GREEN: post-fix-preview-apply -> PASS，调整后真实 `preview` 返回 `generatedTaskCount=67 / blockingIssueCount=0`，真实 `apply` 返回 `applied=true`、`createdTaskIds=67`。
GREEN: post-fix-readonly-check -> PASS，`mes_pro_schedule_order.id=48` 当前已落下新的计划区间与 `latest_start_time`；`mes_pro_task` 当前存在 `67` 条活动任务。
