# Execution Log

## User Intent

用户要求对岗位需求分解矩阵代码分析发现的不符合项进行修复。当前修复切片聚焦可直接闭环的代码问题：MES 调拨手工写入口、活跃订单确认态校验、活跃订单班组长范围隔离。

## BDD Scenarios

- BDD: 调拨写入口禁用 -> Given MES 调拨应由 ERP/正式库存链路生成，When 用户调用调拨单、调拨行或调拨明细的 create/update/delete/submit/confirm/stock/finish/cancel 写接口，Then 后端必须返回手工操作禁止错误且不得调用写服务。
- BDD: 活跃订单必须来自已确认生产工单 -> Given 班组长加入活跃订单，When 生产工单未达到确认状态，Then 服务必须调用确认态校验并 fail fast，不得仅验证工单存在后入池。
- BDD: 活跃订单按当前班组长隔离 -> Given 当前登录班组长查询活跃订单，When 其它班组长也有活跃订单，Then 服务只能从 `selectActiveListByLeader(leaderUserId)` 读取当前班组长范围。

## TDD Evidence

- RED: pending -> 预计旧代码仍调用 `validateWorkOrderExists`、`selectActiveList`，且调拨写接口调用写服务。
- GREEN: pending

## Command Log

- 读取技能：bug-regression-fix-loop、backend-api-delivery、frontend-feature-delivery、bdd-tdd-acceptance-planner。
- 读取规则：task-closeout-rules、backend-development、frontend-development、database-rules、powershell-encoding、powershell-memory、experience-index。

## Blockers

- 当前仓库启动时已存在非本任务改动：`doc/tasks/20260805-job-matrix-compliance/non-compliance-analysis.md` 修改、`doc/tasks/20260805-ac-m21-process-inspection-aggregation-fix/` 未跟踪；本任务不触碰这些文件。
- 分支启动时已 `ahead 4`，最终提交/推送需单独处理既有 ahead 状态，不能把并行任务改动混入本任务。
