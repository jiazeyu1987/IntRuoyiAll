# P0 生产执行主闭环追溯矩阵

## Purpose and Scope

本文档把 P0 审计问题拆成系统事实、来源表或服务、BDD 场景和 TDD 验证。矩阵用于避免后续开发只做页面展示而没有正式数据链路。

## Evidence Reviewed

- `docs/acceptance/production-execution-main-loop/bdd-scenarios.md`
- `docs/acceptance/production-execution-main-loop/tdd-plan.md`
- `docs/acceptance/production-execution-main-loop/e2e-plan.md`
- 当前工序池、生产组长、批记录回填和 trace 相关服务代码。

## Traceability Matrix

| 审计问题 | 必须返回的结构化事实 | 正式来源 | BDD 场景 | TDD 覆盖 |
| --- | --- | --- | --- | --- |
| 谁提交 | 实际员工 ID、姓名、设备登录账号、签名员工 | 工序池提交事件、电子签名记录 | 生产员工一次提交形成主事件 | P0-T02 |
| 在哪台设备 | 设备 ID、设备编号、工作站 ID | 工序池提交事件、班组设备配置 | 生产员工一次提交形成主事件 | P0-T02 |
| 做了哪个工序 | 路线 ID、路线工序 ID、MES 工序 ID、工序名称 | 工序池提交事件、路线工序 | 生产员工一次提交形成主事件 | P0-T02 |
| 做了多少 | 完成数量、损耗数量、确认数量、分配数量 | 原始 payload、分配明细、订单工序完成 | FIFO 分配、组长确认后累计完成订单工序 | P0-T07 |
| 质量结果怎样 | PQC 任务、规程版本、逐件明细、质量结论、质量可分配状态 | PQC 任务、PQC 明细、工序池 PQC 事件 | PQC 正式提交进入工序池质量链路 | P0-T03、P0-T04 |
| 签名是谁 | 提交签名、PQC 签名、复核签名、签名员工和签名快照 | 电子签名字段和签名服务 | 生产员工提交、PQC 提交、班组长复核 | P0-T02、P0-T03、P0-T06 |
| 进入哪个生产工单 | 活跃订单 ID、生产工单 ID、生产工单编号、分配明细 | 活跃订单、分配明细 | 生产工单 FIFO 分配只消耗活跃订单 | P0-T07 |
| 班组长是否复核 | 复核状态、复核说明、复核人、复核时间、复核签名 | 提交复核表 | 班组长复核必须签名且不改写原始提交 | P0-T05、P0-T06 |
| 如何进入批记录追溯 | 批记录执行 ID、正式报表 ID、定义 ID、版本 ID、字段审计 batch、字段审计明细 | 批记录执行、字段审计、正式绑定、字段映射 | 工序完成后回填正式批记录 | P0-T08、P0-T10 |
| 一键闭环视图 | 以上所有节点、状态、阻塞原因和正式 ID | 统一 trace 服务 | 统一闭环 trace 回答 P0 审计问题 | P0-T09、P0-T10 |

## Missing-Link Rules

- 缺少任何一个关键节点时，trace 必须返回 `BLOCKED` 或等价明确状态，说明缺失对象和影响。
- 若 PQC 任务存在但没有工序池 PQC 事件，质量链路视为未闭环。
- 若复核记录存在但没有复核电子签名，复核链路视为未闭环。
- 若订单工序完成但批记录字段审计投影缺失，批记录追溯视为未闭环。
- 若批记录来源不是正式逐工序批记录表单绑定，trace 必须阻塞。
- 若按生产工单 + 工序查询命中多条事件，trace 必须返回候选列表或要求选择事件；不得合并成单条完成链路。
- `complete=true` 或等价完成状态只能在提交、质量、复核、分配、完成和批记录全部拥有正式结构化 ID 时返回。

## Trace Completion Contract

- 每个 trace 分组必须包含 `status`、`sourceIds`、`blockers` 和 `lastUpdatedAt`；`status` 只能来自后端枚举，不得来自前端文案。
- `sourceIds` 至少包含当前节点的正式 ID，并在适用时包含 `processPoolEventId`、`productionSubmitEventId`、`pqcEventId`、`reviewId`、`allocationId`、`orderProcessId`、`batchRecordExecutionId`、`fieldAuditBatchId`。
- `blockers` 必须包含机器可读 `code`、用户可读 `message`、缺失对象类型和解除条件；不得只返回空数组或“暂无数据”。
- trace 接口必须只读；调用 trace 不得触发复核、分配、订单工序完成、批记录回填或任何补偿写入。

## E2E Mapping

- E2E-P0-01：一线员工从真实前端入口提交生产执行事件，验证报工、记录本、提交签名和工序池主事件。
- E2E-P0-02：PQC 员工从真实前端入口提交质量结果，验证 PQC 任务、逐件明细、质量签名和工序池质量事件。
- E2E-P0-03：班组长从真实工作台复核并确认 FIFO 分配，验证复核签名、活跃生产工单、订单工序完成和批记录回填。
- E2E-P0-04：审计用户从 trace 入口按 `processPoolEventId` 查看闭环，验证所有 P0 审计问题均有正式来源或明确阻塞原因。

## Report Shape

统一 trace 建议至少返回以下分组：

- `submitEvent`：工序池事件、报工来源、记录本来源、原始 payload 摘要、提交签名。
- `quality`：PQC 任务、逐件明细摘要、质量结论、可分配状态。
- `review`：生产组长 / PQC 组长复核状态、复核签名和说明。
- `allocation`：活跃订单、生产工单、FIFO 或手工分配明细、确认数量。
- `completion`：订单工序目标数量、累计确认数量、完成状态、完成时间。
- `batchRecord`：正式批记录执行、字段审计 batch、字段审计 item、字段映射来源。
- `blockers`：缺失正式前置、越权、质量不可分配、签名缺失、字段映射缺失。
