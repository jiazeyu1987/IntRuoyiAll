# 流程修复 4 实施设计

## 1. 设计结论

活跃订单完成命令是生产完成、Tx-A 三类资料物化及不可变完成/回填 receipt 的唯一状态所有者。它不是“申请放行”的别名，也不接受前端传入的完成进度为权威事实。receipt 成功后由流程修复 6 在后继 Tx-B 创建或复用批次执行；旧申请放行命令应改为读取完成回执和下游材料状态，不能继续触发或要求提前回填。

## 2. 目标流程与状态

```text
来源事实已形成
  -> 生产进度=100 且 检验进度=100
  -> 组长提交完成命令
  -> 锁定订单 + 重算/校验正式来源
  -> Tx-A 原子三类回填成功（损耗可为 NOT_REQUIRED）
  -> 生成不可变完成/回填 receipt
  -> 流程修复 6（含流程9合法入口）后继 Tx-B createOrReuse 批次执行
  -> 流程修复 6 独占 BATCH_PROVISIONING / BATCH_PROVISIONING_RETRYABLE / BATCH_PROVISIONING_BLOCKED / BATCH_READY 与 batchExecutionId
  -> 流程修复 7 pre-release 建立并校验 Origin/TraceLink（工单、正式领料、批记录、过程检验、适用损耗或 NO_LOSS）
  -> 流程修复 8 上传并校验四份材料，返回 MATERIALS_READY
  -> 流程修复 10 唯一最终放行或退回
  -> 流程修复 7 post-release 追溯
```

完成前状态绝不允许写入最终批记录、过程检验单、损耗单或正式批次执行。组长复核后的工序记录可标示“来源已确认”，但不得使用 `BACKFILL_SUCCESS` 表示最终回填完成；双 100% 之前三类回填均禁止启动。

## 3. 当前实现替换范围

| 当前行为 | 问题 | 正式替换 |
| --- | --- | --- |
| 工序完成后 `completeAndBackfill` | 订单未完成即物化批记录 | 工序层只固化可回填的来源事实 |
| `/active-order/release/apply` 检查工序已回填 | 将回填作为申请前置 | 完成命令负责回填；申请只消费完成回执 |
| 工序级独立回填事务 | 订单未完成即物化，无法形成统一完成事实 | 由订单完成 Tx-A 原子编排三类回填；建批属于流程6后继 Tx-B |
| 申请级幂等 | 无法覆盖建批和完成状态 | 新增订单完成级幂等回执及来源哈希 |

现有 `MesTeamLeaderOrderProcessCompletionService`、`MesTeamLeaderBatchRecordBackfillServiceImpl`、`MesTeamLeaderActiveOrderReleaseGenerationService`、过程检验/损耗 writer 和批次执行创建服务是后续改造审计对象。不得在未完成第 1 里程碑的 TDD 前修改。

## 4. 后端 API 合同

### 4.1 完成命令

`POST /mes/pro/process-pool/team/active-order/complete`

请求体：

```json
{
  "activeOrderId": "Long 字符串",
  "expectedVersion": 0,
  "idempotencyKey": "客户端生成且不可复用"
}
```

服务端按当前用户生产组长负责范围授权，锁定活跃订单并计算 `sourceSnapshotHash`。该哈希覆盖冻结路线/工序、正式 BATCH 报表绑定、生产和 PQC 已确认明细、设备快照、工单、唯一审核领料单及实际损耗。禁止接受请求中的进度、资料 ID、批次 ID 或来源哈希。

成功回执：

```json
{
  "activeOrderId": "Long 字符串",
  "completionReceiptId": "Long 字符串",
  "activeOrderVersion": 0,
  "backfill": {
    "batchRecord": "SUCCESS",
    "processInspection": "SUCCESS",
    "lossReportStatus": "SUCCESS|NOT_REQUIRED",
    "hasActualLoss": true,
    "lossQuantity": "Decimal 字符串"
  },
  "provisionHandoff": "PENDING_FLOW6"
}
```
`provisionHandoff` 只是完成命令交接结果，不是流程6的持久化批次状态；完成回执不包含 `batchExecutionId`，也不在 Tx-B 重试中更新。

同幂等键、同规范载荷与同来源哈希返回同一回执；同键但任一值不同返回 `ACTIVE_ORDER_COMPLETION_IDEMPOTENCY_CONFLICT`。版本不符返回 `ACTIVE_ORDER_VERSION_CONFLICT`。所有 ID 在 JSON 中以字符串传输。

### 4.2 结构化 blocker

失败响应必须保留稳定的 `code`、`blockerCode`、`message` 和可安全展示的 `data`；不能以 HTTP 成功码包失败、中文文案分支或吞异常替代。至少冻结：

- `ACTIVE_ORDER_PROGRESS_NOT_COMPLETE`
- `ACTIVE_ORDER_NOT_OWNED_BY_TEAM_LEADER`
- `ACTIVE_ORDER_COMPLETION_SOURCE_MISSING`
- `ACTIVE_ORDER_BATCH_RECORD_BINDING_MISSING`
- `ACTIVE_ORDER_PICK_LIST_RELATION_INVALID`
- `ACTIVE_ORDER_PROCESS_INSPECTION_SOURCE_UNCONFIRMED`
- `ACTIVE_ORDER_LOSS_MAPPING_MISSING`
- `ACTIVE_ORDER_BATCH_EXECUTION_CREATE_FAILED`
- `ACTIVE_ORDER_HISTORY_BATCH_EXECUTION_MIGRATION_REQUIRED`

### 4.3 下游内部端口

流程修复 6 必须提供后继 Tx-B 的同步、按完成 receipt 幂等的 `createOrReuse(CompletionReceipt)` 端口。输入包含完成回执 ID、订单 ID、来源哈希、三类回填结果和所有稳定来源 ID；输出返回唯一批次执行 ID，并由流程6独占批次状态。Tx-B 失败保留不可变 Tx-A receipt，返回流程6的可重试或 blocker 状态，不得重新触发三类回填，也不得由异步审计事件、轮询任务或前端二次调用替代该端口。流程9的合法入口只能调用流程6。

## 5. 正式来源与回填设计

### 批记录

- 每个冻结工序必须有正式 `BATCH` 绑定，且从 `batch_record_report_id -> mes_pro_batch_record_report.report_id` 解析定义/版本。
- 仅使用 `RECORD_CATEGORY_BATCH_RECORD`、一线生产正式事件、生产工单和已核对的唯一审核领料单。
- 禁止 `formBindings`、默认 `MAIN`、工序开始上传人、空工序、首条绑定或前端值补齐。

### 过程检验单

- 仅来自已确认的 PQC 汇集明细；不得使用未复核 PQC、旧 IPQC 资料或运行时拼装。
- 设备字段必须保存提交/汇集明细中的 `selectedEquipmentId/Code/Name/Number` 快照，不得反查 QA 版本或当前设备主数据。

### 损耗单

- 逐工序损耗状态只允许 `REQUIRED`、`NO_LOSS`、`BLOCKED`；`BLOCKED` 不生成成功 receipt，也不能驱动流程6。
- 订单/工序级必须显式保存 `hasActualLoss` 和 `lossQuantity`。正损耗要求 `hasActualLoss=true` 且 `lossQuantity>0`，由流程5提供正式损耗单映射，receipt 使用 `lossReportStatus=SUCCESS`。
- 无损耗必须有正式零损耗确认快照，要求 `hasActualLoss=false`、`lossQuantity=0`、receipt 使用 `lossReportStatus=NOT_REQUIRED`；不生成损耗单或零损耗报告。缺失 `lossRecordId` 不得推断为无损耗。

## 6. 事务、并发和幂等

**Tx-A：完成节点原子回填事务**

1. 按订单 ID 锁定活跃订单，检查组长授权和 `expectedVersion`。
2. 重算双 100%，读取并验证全部正式来源与来源关系；写入前出现任何缺口立即失败。
3. 计算规范请求哈希与来源快照哈希，查询订单完成 receipt。
4. 依次写批记录、过程检验单、实际损耗单（仅有实际损耗）或 `NOT_REQUIRED` 事实。
5. 写不可变完成/回填 receipt、订单完成状态和递增版本后提交。

步骤 2 至 5 属于同一数据库事务。任一回填、约束或 receipt 持久化失败均回滚三类回填和订单状态；损耗 `BLOCKED` 直接失败。禁止 `REQUIRES_NEW`、部分成功补偿、mock 成功或后台继续执行。

**Tx-B：receipt 成功后的批次执行事务**

流程修复 6 消费成功 receipt，在独立后继事务中按 receipt 唯一键创建或复用批次执行并建立来源映射。Tx-B 失败不得回滚已提交的 Tx-A，不得重复回填；应保留 receipt 并进入可重试或结构化 blocker。完成编排可以立即调用 Tx-B，但两者不是同一数据库事务。

## 7. 数据模型与完整性

以一次性迁移为前提，推荐新增订单完成回执/资料映射实体或在既有申请表扩展时指定唯一迁移所有者。以下语义必须可持久化：

| 字段/约束 | 用途 |
| --- | --- |
| `active_order_id` + 完成业务作用域唯一 | 一个活跃订单只拥有一个有效完成回执 |
| `request_idempotency_key` 唯一 | 重试返回同一回执 |
| `request_payload_hash`、`source_snapshot_hash` | 识别同键冲突和来源漂移 |
| `expected_version`、`completed_version` | 乐观锁与审计 |
| 三类回填状态、正式资料 ID、资料来源哈希 | 建批与追溯的正式证据 |
| `completion_backfill_receipt_id` | Tx-A 不可变完成/回填 receipt |
| `lossReportStatus`、`hasActualLoss`、`lossQuantity` | 统一损耗合同；区分 `SUCCESS` 与 `NOT_REQUIRED`，并保留订单/工序实际损耗事实 |
| `lossRecordId`（有损耗时必填）及零损耗确认快照 | 流程5正式来源证据；缺失不得推断无损耗 |
| `provision_relation_id`、`provision_state` | Tx-B 后继建批关联和重试状态；由流程6持有，不写回不可变 receipt |
| `batch_execution_id` | 仅由流程6批次执行实体/关联持有，流程4 receipt 不持有可变批次状态 |
| 完成时间、操作人、迁移审计编码 | 合规审计，不替代业务状态 |

跨阶段共用申请表、待办或聚合版本时，必须在实施前指定唯一 schema 迁移所有者。消费阶段只能使用 DTO/内部端口，禁止并行添加同名列、平行关联列或待办版本列。

## 8. 前端设计边界

- 活跃订单页仅在服务端返回的生产/检验进度均为 100% 时展示可点击完成动作；前端可提示但不能自行判定完成。
- 点击后展示不可逆确认，提交期间禁止重复点击；使用一次性幂等键。
- 成功后刷新订单和批次执行摘要；失败展示后端 blocker，不能把失败显示为已完成。
- 前端静态合同覆盖按钮可见性、确认、重复提交保护、刷新和 blocker 展示；不允许提供“回填”“建批”独立绕过按钮。

## 9. 与其它流程修复线程的接口契约

| 流程 | 其状态所有者 | 本流程的依赖/输出 | 禁止越界 |
| --- | --- | --- | --- |
| 修复 2：生产提交/复核 | 一线生产签名与生产组长复核事实 | 提供正式生产、工单、领料来源 | 不得提前写三类最终资料、损耗单或建批 |
| 修复 3：PQC 提交/复核 | 一线 PQC 签名与 PQC 组长复核事实 | 提供已确认 PQC 汇集明细及设备快照 | 不得提前写过程检验单或建批 |
| 修复 4：本任务 | 活跃订单完成、Tx-A 三类回填与不可变 receipt | 输出完成 receipt 和交给流程6的 Tx-B 请求 | 不得负责材料、最终放行、完整追溯或流程11总门禁 |
| 修复 5：条件损耗 | 有实际损耗时的损耗事实与映射 | 提供损耗条件、来源和单据稳定映射 | 无损耗不得创建损耗单或零损耗报告 |
| 修复 6：批次执行创建/复用 | receipt 后继 Tx-B 建批状态 | 接收完成 receipt，按唯一键 `createOrReuse`，独占 BATCH_* 状态和 batchExecutionId | 不得重新触发三类回填 |
| 修复 7：批次映射与放行后追溯 | pre-release Origin/TraceLink、post-release 追溯 | 建批后先映射并校验工单、领料、批记录、过程检验、损耗/NO_LOSS hash/version；放行后提供追溯 | 不得从前端拼装事实或跳过 pre-release |
| 修复 8：四份材料 | 来料检报告、灭菌报告、成品检报告、成品检记录上传及硬门禁 | 在批次执行创建/复用后收齐四份材料 | 缺任一材料不得进入放行门禁 |
| 修复 10：最终放行 | 放行状态、放行角色和放行审计 | 消费流程8材料齐套状态 | 不得绕过材料硬门禁 |
| 修复 11：质量总门禁 | BDD/TDD、回归和迁移总门禁 | 汇总各流程测试与迁移证据 | 不得以文档声明替代测试证据 |

说明：已有模拟 Stage2/3/5/6 文档中的职责拆分须按上述编号修订；流程4只提交 receipt，流程6负责后继建批，流程8/10/7/11分别负责材料、最终放行、追溯和总门禁。

## 10. 迁移、回滚和失败边界

- 上线前只读盘点历史“完成前建批”或“回填前建批”记录。只有能证明与有效完成回执、三类资料证据和相同幂等键关联的记录可作为重复请求结果复用。
- 任何无法证明关联的历史批次执行返回迁移 blocker；不得自动认领、自动删除、自动补资料或重新绑定。
- 数据修复必须另立任务、获明确批准并提供备份/回滚计划；本任务不执行数据修复。
- 代码回滚只允许撤回尚未启用的完成入口。已提交的完成事务不可通过删除资料“回滚”；应由批准的受控业务纠正流程处理。

## 11. 实施里程碑

1. 写 RED：锁定双 100%、完成前禁回填/建批、三类资料、Tx-A 失败回滚、Tx-B 失败保留 receipt、幂等/版本冲突和正式来源的单元测试。
2. 交付订单完成命令与订单级事务，移除/隔离逐工序最终回填入口。
3. 与流程6对接 receipt 驱动的批次执行 `createOrReuse` 和来源映射，完成 schema/迁移预检。
4. 交付前端完成操作和正式 blocker 展示。
5. 流程11统筹真实用户路径和回归；流程8、10、7分别覆盖四份材料、最终放行和放行后追溯。

## 2026-08-25 implementation evidence

- Tx-A 已在活跃订单完成服务中锁定订单、重算生产/PQC 双 100%、校验正式来源快照和签名快照，并在同一事务中写入批记录、过程检验记录、实际损耗或正式 `NO_LOSS` 事实。
- 三类正式结果写入均返回持久化记录 ID；只有三类写入成功后才插入不可变 `BACKFILL_SUCCEEDED` completion receipt。receipt 包含 `batchRecordId`、`processInspectionId`、损耗结果 ID/`NO_LOSS` 决策、来源快照和哈希，不持有 `batchExecutionId` 或 `BATCH_*` 状态。
- Flow6 读取端口按租户、receiptId、receiptHash、完成版本、三类回填状态和损耗分支再次校验；缺失、篡改、跨租户或不完整 receipt 均阻断，不从原始报工/PQC 补造。
- 定向服务、回填适配器、Flow6 receipt 端口和 schema 合同共 `37/37` 通过；MES 模块 compile 通过；完整 reactor 在无关 `yudao-server` MDEP-98 阻断，数据库 apply/rollback 和真实数据 E2E 未运行。

## 12. 实施 blocker（合同已冻结）

1. 流程6/9/8/10/7/11 的职责、状态和接口合同已在对应任务文档冻结；当前生产代码尚未实现这些合同。
2. 流程6 Tx-B 的 receipt 消费、BATCH_* 状态、provision relation 和 batchExecutionId 尚未实现或取得测试证据。
3. 流程7 pre-release Origin/TraceLink hash/version 校验、流程8 MATERIALS_READY、流程10唯一放行和流程7 post-release追溯尚未实现或取得测试证据。
4. 流程5 REQUIRED/NO_LOSS/BLOCKED、hasActualLoss、lossQuantity、lossRecordId 和正式零损耗快照尚未实现或取得测试证据。
5. 流程11总门禁尚未完成生产代码回归、迁移预检和真实数据证据；本任务不启动这些写入验证。
