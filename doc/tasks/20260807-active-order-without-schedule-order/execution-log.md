# 执行日志

## 用户意图与范围

- 用户要求“不需要排产工单的限制”，并确认继续实施完整后端链路。
- 保留单一有效排产工单的原有正式行为；仅在有效排产工单数量为零时启用产品路线绑定 + 当前 ACTIVE 路线版本的正式模式。
- 多个有效排产工单仍属于数据冲突并阻塞，不静默任选一条。

## BDD

- BDD: 无排产工单可加入活跃订单 -> Given 已确认生产工单具有正数 ERP 数量、ERP 计划开工时间、唯一产品路线绑定、该路线唯一 ACTIVE 版本及完整发布工序/PQC 规程，When 生产组长搜索候选并加入订单，Then 候选可加入且系统按 ACTIVE 版本快照生成活跃订单工序快照和正式 PQC 任务。
- BDD: 无排产正式来源缺失时阻塞 -> Given 已确认生产工单没有有效排产工单，且产品路线绑定、ACTIVE 版本、发布工序、数量系数、ERP 计划开工时间或 PQC 规程任一缺失，When 搜索候选或加入订单，Then 系统明确返回不可加入原因且不写入活跃订单、工序快照或 PQC 任务。
- BDD: 单一有效排产工单行为保持不变 -> Given 已确认生产工单存在一条完整有效排产工单，When 搜索候选并加入订单，Then 系统继续使用排产路线、版本、工序计划数量和计划日期生成快照及 PQC 任务。
- BDD: 多个有效排产工单继续阻塞 -> Given 已确认生产工单存在多条有效排产工单，When 搜索候选或加入订单，Then 系统拒绝选择并提示有效排产不唯一。

## 命令意图与证据

- READ: 已读取根 `AGENTS.md`、`docs/backend-development.md`、`docs/task-closeout-rules.md`、`docs/powershell-memory.md`、`docs/powershell-encoding.md` 和 `docs/experience-index.md` 的适用门禁。
- READ: 已读取 `backend-api-delivery`、`behavior-driven-development` 及 backend evidence contract。
- SOURCE: `mes_pro_work_order.quantity` 作为 ERP 固定数量；无排产模式的业务日期使用明确的 `mes_pro_work_order.planned_start_time` 日期部分，缺失即阻塞，不切换到需求日期或当前日期。
- SOURCE: 无排产路线使用 `mes_pro_route_product` 唯一正式绑定、`mes_pro_route_version` 唯一 ACTIVE 版本及其 `route_snapshot_json.configSnapshots.flowGraph.nodes/scheduleUseConfigs`。
- BASELINE: 开始本任务时发现并发任务 `doc/tasks/20260807-production-leader-process-loss-reasons-random/execution-log.md` 仍有未提交改动；将按共享分支规则单独保存，不纳入本任务实现提交。

## 里程碑状态

- M1 completed：现有新增链路完全依赖排产路线、排产工序数量系数/计划数量/计划日期；生产工单可提供 ERP 数量和 ERP 计划开工时间，ACTIVE 路线发布快照可提供正式工序与排产用途数量系数。
- M2 in_progress：准备新增无排产成功与正式来源缺失测试并执行 RED。

## 阻塞项

- 当前无业务实现阻塞。

