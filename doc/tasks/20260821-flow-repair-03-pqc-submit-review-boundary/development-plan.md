# Development Plan

## 1. Objective And Target State

本专项把一线 PQC 到 PQC 组长复核限定为“过程检验正式来源事实生产链”，不承担最终单据生成。

目标链路：

1. 正式 PQC 任务由活跃订单冻结的路线工序与 QA 规程版本生成，任务锁定检验类型、轮次、计划数量和项目规则。
2. 一线 PQC 按任务提交所有逐件/逐项明细、所选设备快照和电子签名；提交只形成一个不可变的待复核来源版本。
3. PQC 组长只读取同一来源版本并确认或退回，不能改写检验事实。确认在同一事务生成唯一结构化汇集版本；退回保留原始审计并要求一线 PQC 创建新修订。
4. `CONFIRMED` 只表示来源事实已确认，不表示正式过程检验单已生成，也不表示活跃订单完成、批次执行已创建或产品已放行。
5. 活跃订单生产/检验双进度 100% 且点击完成时，由流程 4 消费唯一确认版本，在同一业务节点回填批记录、正式过程检验单和实际有损耗时的损耗单；流程 4 输出 `formalProcessInspectionDocumentId`。
6. 活跃订单分支由流程 6 消费流程 4 的完整 `completionBackfillReceipt` 创建或复用批次；排产、手工、独立或独立 PQC 分支则由流程 6 消费流程 9 校验后的 canonical `IndependentBatchPrerequisiteReceipt`，不得伪造 `activeOrderId` 或流程 3 aggregate。
7. 批次创建后，流程 7 先建立 Origin/TraceLink，并在 PQC 适用时将流程 3 来源及流程 4 正式过程检验单映射为批次执行过程检验记录；只有放行前映射及来源 hash 校验成功后，流程 8 才接受四份材料，流程 10 才能最终放行，流程 11 只负责总体验证与迁移门禁。

## 2. Current Code Facts

| 事实 | 当前行为 | 结论 |
| --- | --- | --- |
| PQC 任务状态 | `PENDING / SUBMITTED / CONFIRMED`，保存活跃订单、工单、路线版本、工序、QA 工序、规程版本、类型、规则、班次、轮次、数量、内容哈希和事件 ID | 已有正式任务骨架；生产代码尚未实现/验证本文档冻结的显式 `RETURNED` 与受控修订合同 |
| 逐件明细 | 保存样本序号、检验项目、方法、标准、设备 ID/编码/名称/编号、限值、单位、精度、结果类型、实测值和判定 | 基本符合结构化正式来源 |
| 组长复核 | 复核 PQC 事件时校验人员范围；批准即调用汇集服务 | 批准可以生成确认来源，但必须禁止解释为最终单据回填 |
| 汇集事务 | 校验 record/event/task/piece details，CAS 更新记录和任务，再批量写 aggregate detail | 原子性和并发 fail-fast 基础符合要求 |
| 汇集来源 | 冻结 tenant、event、review、task、activeOrder、workOrder、路线/工序/规程/轮次、逐件值和设备快照 | 可作为完成节点唯一输入 |
| 映射能力 | `PQC_AGGREGATE_DETAIL` 已作为批记录单元格可选来源 | 证明汇集是来源，不是正式过程检验单 |
| 重复复核 | 当前代码未完整执行终态普通命令幂等/冲突与独立受控修订 | 设计合同已冻结，待流程 3 生产实现和测试验证 |
| 完成消费 | 本专项未找到“流程 4 冻结消费某个 aggregate version 并生成正式过程检验单”的完整生产实现 | 设计合同已冻结；流程 4 负责正式单，流程 7 负责批次映射 |

### Audited Code References

- `MesTeamLeaderSubmissionReviewServiceImpl`：PQC 审批路径、人员范围、已有复核处理和批准后汇集。
- `MesPqcProcessInspectionAggregationServiceImpl`：来源校验、CAS、任务确认和汇集明细创建。
- `MesPqcInspectionTaskDO`、`MesPqcInspectionPieceDetailDO`、`MesPqcProcessInspectionAggregateDetailDO`：正式任务、逐件事实和确认汇集模型。
- `MesProBatchRecordCellLinkServiceImpl`：`PQC_AGGREGATE_DETAIL` 来源目录及映射。

## 3. Root Cause

根因不是缺少逐件或设备数据，而是修订前的业务状态边界没有形成端到端唯一所有权；下列根因现已转化为冻结合同，后续缺口均属于实现/验证：

- “组长确认来源”和“最终过程检验单已回填”容易被同一个 `CONFIRMED` 词义混淆。
- 复核记录、PQC 任务、PQC record 和 aggregate detail 分别有状态，但缺少一个被所有下游共同接受的“唯一有效来源版本”合同。
- 修订前终态重复复核/退回/修订缺少明确命令与状态转换，可能出现审计记录与已汇集来源并存但含义冲突。
- 后续完成节点尚未被强制要求绑定精确 aggregate/source IDs 和内容哈希，存在重查当前配置或重复回填风险。
- 跨线程此前侧重各自节点，缺少统一的 expectedVersion、idempotencyKey、payloadHash、阻断码和追溯身份。

## 4. Change Boundary For Future Implementation

### Owned By Flow Repair 3

- PQC 任务提交命令、逐件明细冻结、设备快照冻结、签名归属和提交内容哈希。
- PQC 组长确认/退回命令、人员范围、复核审计、状态机、并发 CAS 和确认汇集。
- 唯一有效来源版本及供下游读取的只读来源合同。
- 对 raw payload、旧 IPQC、当前 QA/设备配置、生产提交事实替代 PQC 来源的硬阻断。

### Explicitly Not Owned

- 流程 4：活跃订单双 100% 完成节点统一回填批记录、正式过程检验单、实际有损耗时的损耗单，并提交不可变 `completionBackfillReceipt`；生产事实复核由流程 2 提供，流程 4 不拥有该复核。
- 流程 4：活跃订单双 100% 完成节点统一回填三类适用单据；输出 `formalProcessInspectionDocumentId` 和不可变 `completionBackfillReceiptId/completionBackfillReceiptHash/completionVersion/sourceSnapshotHash`。
- 流程 6：按合法前置 receipt 创建/复用批次；活跃订单分支使用流程 4 receipt，独立场景分支使用流程 9 receipt。
- 流程 7：放行前 Origin/TraceLink、适用的 PQC 过程检验映射，以及放行后追溯读模型/查询；输出适用的 `batchExecutionProcessInspectionRecordId`。
- 流程 8：来料检报告、灭菌报告、成品检报告、成品检记录四份材料上传与有效性门禁。
- 流程 9：多入口创建批次执行/放行的前置条件、状态所有者和幂等契约。
- 流程 10：只拥有最终放行状态、管理者代表签名、CAS 和放行审计；不拥有追溯状态或追溯读模型。
- 流程 11：BDD/TDD、迁移、回滚和总体验证门禁。
- QA 规程配置和 PQC 任务生成算法本身；本专项只消费其冻结结果。

## 5. Data Design

### 5.1 Aggregate Root And Identity

建议以 `PqcInspectionTask` 为提交状态所有者，以 `PqcSourceRevision`（可由现有 task/record/event 组合扩展，不要求另建平行状态机）表达不可变来源版本。

必须冻结：

- `tenantId`, `activeOrderId`, `workOrderId`
- `routeId`, `routeVersionId`, `routeProcessId`, `processId`, `qaProcessId`
- `regulationVersionId`, `inspectionType`, `ruleKey`, `businessDate`, `shift`, `round`
- `plannedInspectionQuantity`, `actualInspectionQuantity`
- `pqcTaskId`, `sourceRevision`, `submitEventId`, `pqcRecordId`
- 提交人、签名 ID/快照、提交时间、`payloadHash`
- 每条 `pieceDetailId`、样本序号、项目身份/名称/方法/标准、限值/单位/精度、值/判定
- 提交时选择的设备 ID/编码/名称/编号；无设备项目按正式 `equipmentRequired=false` 表达，不伪造设备。

数据库 Long ID 经 JSON 和前端必须保持十进制字符串或无精度损失的既有 Long 序列化合同，不得先转 JavaScript Number。

### 5.2 Review And Aggregate

复核保存：`reviewId`, `pqcTaskId`, `sourceRevision`, `submitEventId`, `payloadHash`, `decision`, `reason`, `reviewerUserId`, `reviewerSignatureId/snapshot`, `reviewedAt`, `expectedVersion`, `idempotencyKey`。

确认汇集保存精确来源：`aggregateVersionId`, `sourcePqcRecordId`, `sourcePieceDetailId`, `eventId`, `reviewId`, `pqcTaskId`, `sourceRevision`, `payloadHash` 以及完整逐件/设备快照。

`productionSubmitEventId` 如保留只能是可选关联证据，不得成为 PQC 提交、确认或汇集的前置条件，也不得代替 PQC 自身来源身份。

### 5.3 Uniqueness And Constraints

- 同一 `tenantId + pqcTaskId + sourceRevision` 只能有一个正式提交版本。
- 同一来源版本只能有一个有效终态复核。
- 同一确认复核只能生成一个 aggregate version；汇集逐件行按 `aggregateVersionId + sourcePieceDetailId` 唯一。
- 幂等记录按 `tenantId + operation + actorUserId + idempotencyKey` 唯一，并保存 `requestHash` 和 result identity。
- 流程 4 的正式回填链接按 `tenantId + activeOrderId + routeProcessId + completionAttempt` 唯一，绑定 `aggregateVersionId` 并输出 `formalProcessInspectionDocumentId`；流程 7 的批次映射链接按 `tenantId + batchExecutionId + processInspectionBackfillSlot` 唯一，输出 `batchExecutionProcessInspectionRecordId`。

## 6. State Machine

| 状态 | 所有者 | 允许命令 | 下一状态 | 禁止事项 |
| --- | --- | --- | --- | --- |
| `PENDING` | PQC task | frontline submit | `SUBMITTED` | 缺任务快照、样本或签名不得提交 |
| `SUBMITTED` | PQC task | leader confirm / return | `CONFIRMED` 或 `RETURNED`（若沿用 record 状态则必须有等价显式状态） | 组长不得修改逐件事实；不得写最终过程检验单 |
| `RETURNED` | source revision | frontline revise | 新 revision `SUBMITTED` | 不得原地覆盖原 revision；旧版本不得被下游消费 |
| `CONFIRMED` | PQC task/source revision | completion consume | 保持 `CONFIRMED`，另写消费链接 | 不能被普通复核命令退回或重写；修订必须走单独受控命令 |
| `CONSUMED`（派生状态） | completion link | query only | 无 | 不建议改写 PQC task 状态；由正式回填链接派生，避免双状态所有者 |

关键裁定：现有 `CONFIRMED` 保留为“来源确认完成”。终态后普通复核只能同命令幂等或冲突，受控修订必须走独立命令、新 revision、原因和下游未消费校验；该合同已冻结，剩余问题是生产代码未实现/未验证。正式过程检验单状态与 `formalProcessInspectionDocumentId` 归流程 4；批次执行过程检验记录、Origin/TraceLink 与追溯读模型归流程 7；最终 `RELEASED`、签名、CAS 和放行审计归流程 10；不得在 PQC task 上增加这些下游含义。

## 7. API And Command Contracts

### 7.1 Frontline Submit

`SubmitPqcSourceCommand`

- 输入：`pqcTaskId`, `expectedVersion`, `idempotencyKey`, 完整逐件明细、设备选择、签名凭据。
- 服务端补充：tenant、当前登录人、正式任务快照、签名 actor；客户端不得提交审核身份或派生来源 ID。
- 成功返回：`pqcTaskId`, `sourceRevision`, `submitEventId`, `pqcRecordId`, `payloadHash`, `status=SUBMITTED`, `version`。
- 同键同内容返回同一结果；同键不同内容返回 `IDEMPOTENCY_PAYLOAD_CONFLICT`。

### 7.2 Leader Review

`ReviewPqcSourceCommand`

- 输入：`submitEventId`, `pqcTaskId`, `sourceRevision`, `payloadHash`, `decision`, `reason`, `expectedVersion`, `idempotencyKey`, 签名凭据。
- 前置：当前用户在 PQC 组长正式范围；来源为同租户 `SUBMITTED`；所有身份与 hash 完全一致。
- 确认返回：`reviewId`, `aggregateVersionId`, `status=CONFIRMED`, `sourceRevision`, `payloadHash`, `version`。
- 退回返回：`reviewId`, `status=RETURNED`, `sourceRevision`, `version`；不得创建 aggregate detail。
- 终态后普通 review 命令必须返回幂等结果或 `STATE_CONFLICT`，不得新增相反终态记录。受控修订另立命令并要求原因、新 revision 和下游未消费校验。

### 7.3 Read-Only Source Query

`GetConfirmedPqcSource(activeOrderId, routeProcessId)` 只返回：唯一 `aggregateVersionId`、确认状态、精确追溯身份、逐件汇集明细、设备快照、payload hash 和 source version。

若 0 个、多个有效版本、跨租户、身份不一致、逐件数不一致或结构不完整，返回稳定结构化 blocker code，不返回空成功，不按中文错误文案分支。

## 8. Idempotency, Concurrency And Transaction Rules

- submit 与 review 各有独立幂等空间；键由客户端为一次用户动作生成，服务端保存 request hash。
- 所有状态写使用 `expectedVersion` CAS；影响行数不为 1 即回滚并返回版本冲突。
- 确认事务必须原子包含：锁定/校验来源、写复核审计、推进来源状态、写 aggregate header/details、写幂等结果。
- 任一明细写入、数量校验或唯一约束失败，整个确认事务回滚，不得留下 `CONFIRMED` 但无完整 aggregate 的状态。
- 流程 4 在完成事务中锁定并验证 aggregate version；流程 7 在批次执行映射事务中再次锁定并验证流程 4 输出的正式单及来源 hash，预检结果不能代替最终事务复核。

## 9. Stable Blocker Contract

至少定义以下机器可读码：

- `PQC_TASK_NOT_FOUND_OR_OUT_OF_SCOPE`
- `PQC_TASK_SNAPSHOT_INCOMPLETE`
- `PQC_SOURCE_STATE_CONFLICT`
- `PQC_SOURCE_VERSION_CONFLICT`
- `PQC_SOURCE_PAYLOAD_HASH_MISMATCH`
- `PQC_PIECE_DETAIL_INCOMPLETE`
- `PQC_EQUIPMENT_SNAPSHOT_INVALID`
- `PQC_REVIEW_SCOPE_DENIED`
- `PQC_AGGREGATE_NOT_UNIQUE`
- `PQC_AGGREGATE_STRUCTURE_INCOMPLETE`
- `IDEMPOTENCY_PAYLOAD_CONFLICT`
- `PQC_SOURCE_MIGRATION_BLOCKER`
- `BACKFILL_RECEIPT_REQUIRED`
- `SOURCE_SNAPSHOT_MISMATCH`
- `ENTRY_PREREQUISITE_MISSING`
- `ENTRY_SOURCE_INVALID`
- `TRACE_MAPPING_BLOCKED`
- `TRACE_SOURCE_CONFLICT`
- `RELEASE_GATE_BLOCKED`
- `RELEASE_SNAPSHOT_MISMATCH`

前述 PQC 码用于流程 3 命令/查询；八个 receipt/trace/release 码是对外跨线程稳定合同，不得以内部细分码替代。错误响应必须携带相关业务身份和缺失项列表，但不得泄漏签名凭据或秘密。

## 10. Cross-Flow Interface Contracts

### Flow Repair 4: Active Order Completion And Formal Backfill

- 流程 4 输入流程 2 已复核生产来源、流程 3 的唯一确认 aggregate、双 100% 状态、损耗事实和正式批记录绑定；在同一业务节点回填三类单据并输出 `formalProcessInspectionDocumentId` 及完整 `completionBackfillReceipt`。
- 流程 4 只消费冻结 PQC 来源和生产正式来源；不得从 raw payload、旧 IPQC、当前 QA/设备配置、formBindings 或生产提交事实推算 PQC 明细。三类回填任一失败必须整体回滚。
- 与流程 3 仅通过 `activeOrderId + workOrderId + routeVersionId + routeProcessId` 在流程 4 汇合；流程 4 产出的正式单再由流程 7 映射到批次执行，生产来源与 PQC 来源相互独立验证。

### Flow Repair 6: Batch Execution Create Or Reuse

- 流程 6 只接受两个互斥合法分支之一的正式前置 receipt，并输出 `batchExecutionId/status/version`；同键同内容幂等、同键异内容冲突，两个分支同时出现或均缺失都必须阻断。
- 活跃订单分支输入流程 4 不可变的 `completionBackfillReceiptId`、`completionBackfillReceiptHash`、`completionVersion`、`sourceSnapshotHash`、正式来源关系和 `idempotencyKey`；receipt 必须证明批记录、正式过程检验单和实际有损耗时损耗单均按适用性成功回填。缺 receipt 返回 `BACKFILL_RECEIPT_REQUIRED`，来源快照不一致返回 `SOURCE_SNAPSHOT_MISMATCH`。`formalProcessInspectionDocumentId` 只是 receipt 结果字段，不能替代完整 receipt。
- 排产、手工、独立或独立 PQC 分支输入流程 9 签发的 `IndependentBatchPrerequisiteReceiptId/hash/version/scenarioType/sourceSnapshotHash/idempotencyKey` 及场景正式来源关系；缺凭证返回 `ENTRY_PREREQUISITE_MISSING`，无效或来源不一致返回 `ENTRY_SOURCE_INVALID`；不得伪造 `activeOrderId`、流程 4 receipt 或流程 3 aggregate。
- 流程 6 不直接消费流程 3 aggregate，不承担 PQC 组长确认、退回或修订状态；流程 3 的 PQC 来源只在活跃订单分支经流程 4 消费，并在适用场景由流程 7 建立映射。

### Flow Repair 7: Batch Execution Process Inspection Mapping And Traceability

- 输入：流程 6 已存在的 `batchExecutionId`、建批前置 receipt 类型与来源关系；活跃订单/PQC 适用分支还必须输入流程 4 的 `formalProcessInspectionDocumentId`、流程 3 唯一 `aggregateVersionId` 及其 `payloadHash/sourceSnapshotHash`。
- PQC 过程检验映射只使用 `aggregateVersionId` 对应的结构化明细与设备快照；不得读取 raw payload、当前 QA/设备配置、旧 IPQC、`formBindings` 或生产提交推算。
- 流程 7 对所有合法批次先写不可变 Origin/TraceLink；PQC 适用时再把流程 4 正式单和流程 3 确认来源映射到批次执行过程检验记录。它不生成或覆盖流程 4 正式单，不负责三类回填、批次创建或最终放行。
- 输出 `preReleaseMappingStatus=READY`、Origin/TraceLink identity、`sourceSnapshotHash` 和映射幂等键；PQC 适用时还必须包含 `batchExecutionProcessInspectionRecordId`、`formalProcessInspectionDocumentId`、`aggregateVersionId`、`payloadHash`。Origin/TraceLink 或适用映射缺失统一传递 `TRACE_MAPPING_BLOCKED`；来源 hash 冲突统一传递 `TRACE_SOURCE_CONFLICT`；非适用场景伪造 PQC 映射同样阻断。
- 流程 7 独占放行后追溯读模型/查询；流程 10 只提供放行审计事实作为读模型输入，不持有追溯状态。

### Flow Repair 8/9/10/11: Materials, Entry, Release And Overall Gate

- 流程 9 对排产、手工、独立或独立 PQC 建批入口校验各自正式来源并签发 canonical `IndependentBatchPrerequisiteReceipt`；活跃订单入口不走该凭证，而是使用流程 4 `completionBackfillReceipt`。所有入口只适配到流程 6 的互斥 receipt 合同，不伪造另一分支身份。
- 流程 8 仅在流程 7 `preReleaseMappingStatus=READY` 后接受并校验来料检报告、灭菌报告、成品检报告、成品检记录；流程 7 blocker 必须原样传递 `TRACE_MAPPING_BLOCKED`/`TRACE_SOURCE_CONFLICT`，四份缺一、无效或把成品检报告与成品检记录互代统一返回 `RELEASE_GATE_BLOCKED`。流程 10 发现最终放行快照与已验证快照不一致时返回 `RELEASE_SNAPSHOT_MISMATCH`，其余 gate 未满足返回 `RELEASE_GATE_BLOCKED`。
- 批次详情、PQC/生产申请、管理者代表批准、独立批次放行等合法入口只能适配统一的流程 7 pre-release gate、流程 8 材料 gate 和流程 10 finalization；流程 10 独占最终 `RELEASED`、管理者代表签名、CAS 与放行审计。流程 11 只负责 BDD/TDD、迁移、回滚和总体验证。
- 放行只引用流程 4 正式单、流程 7 批次执行过程检验记录及其 aggregate source link；不得重新聚合 PQC 数据或补写检验事实。
- 追溯至少返回：放行记录 -> 批次执行 -> 活跃订单 -> 生产工单/领料单 -> 正式过程检验单 -> aggregate version -> review -> PQC task -> piece details/equipment snapshot；同时关联一线生产、生产复核和损耗来源。

## 11. Migration Boundary

- 先只读盘点所有 `SUBMITTED/CONFIRMED` task、PQC record、review、aggregate header/detail 和正式过程检验单/批次执行链接。
- 只有 tenant、任务、事件、复核、逐件明细、设备快照、数量、路线/规程版本和 payload hash 可完整对账的数据才可建立明确消费链接。
- 历史存在多个相反终态复核、确认无汇集、汇集缺明细、设备来源无法证明、来源版本不唯一、或批次执行早于有效完成回填的数据统一标记 `PQC_SOURCE_MIGRATION_BLOCKER`。
- 禁止用最新配置、名称、排序、当前设备、旧 IPQC、默认值或人工猜测自动修复；需单独人工核验/重建方案。

## 12. Rollback Boundary

- 上线应以状态机/约束、读合同、写命令、完成节点消费顺序分阶段，但不得允许新旧来源逻辑并行 fallback。
- 在正式单据尚未由流程 4 消费前，可通过受控业务命令撤销新 revision/确认，保留完整审计；不得物理删除来源。
- 一旦已被正式过程检验单或放行记录引用，不允许回滚覆盖 PQC 来源，只能走受监管修订链并建立新版本关联。
- 若迁移或约束校验失败，回滚本次 schema/code 部署并保持旧数据只读，禁止部分启用新完成链路。

## 13. Implementation Sequence

1. 按已冻结业务语义把共享字段映射为统一 DTO/事件名称，并保留状态/阻断码、Long ID 和幂等合同。
2. 用测试暴露重复终态复核、幂等冲突、缺明细/快照、并发 CAS 和下游消费歧义。
3. 收敛提交 revision 与复核状态唯一所有者；保持现有结构化明细，不引入平行来源。
4. 使确认事务生成唯一 aggregate version 并提供严格只读查询。
5. 由流程 4 接入精确 aggregate version 并产生正式过程检验单 ID；由流程 9 实现独立场景前置 receipt；流程 6 按两个互斥合法分支创建/复用批次。
6. 流程 7 建立所有批次 Origin/TraceLink 和适用的 PQC 过程检验映射；流程 8/10 分别接入四材料门禁与最终放行，流程 11 执行总体验证、迁移和回滚门禁。
7. 完成迁移审计、真实角色 E2E 和失败恢复验证后才允许发布。

## 14. Executable Milestones

### 里程碑 1：PQC 提交来源版本
目标：实现一线 PQC 提交的结构化来源校验、不可变 revision、设备/签名快照、payload hash 和幂等冲突。
涉及文件：
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool`
交付物：
- 提交命令在重复请求和 hash 冲突时返回稳定结果。
- 任务状态与来源身份的定向测试。

### 里程碑 2：PQC 组长确认/退回与聚合
目标：实现组长只对同一来源版本确认或退回，确认事务原子生成唯一 aggregate，终态普通复核不可反转。
涉及文件：
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team`
交付物：
- 确认/退回状态机、CAS、幂等和结构化 aggregate 测试。
- 明确 `CONFIRMED` 不产生正式过程检验单、批次或放行。

### 里程碑 3：只读来源合同与回归验证
目标：提供下游消费所需的唯一确认来源查询，并完成 Flow3 定向回归和任务证据记录。
涉及文件：
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool`
- `doc/tasks/20260821-flow-repair-03-pqc-submit-review-boundary`
交付物：
- 唯一 aggregate/sourceRevision/payloadHash 查询合同。
- RED/GREEN/REGRESSION 结果、阻塞项和验证报告。

## 15. Current Implementation Evidence

- 已实现 P1 回执身份字段：`sourceRevision` 映射现有不可变 `submittedEventId`，`payloadHash` 映射任务冻结的 `submittedContentHash`；提交、重试和只读回执保持同一身份。
- 已实现 P2 终态边界：同一终态复核的同命令重试返回原 `reviewId`，不重复签名或 aggregate；相反决定保持终态冲突；aggregate 异常继续传播到事务边界。
- 主代码编译证据：`mvn.cmd -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am -Dmaven.test.skip=true package` -> PASS（测试跳过）。
- 定向测试证据：`mvn.cmd -f IntRuoyiBackend/yudao-module-mes/pom.xml -Dtest=MesFrontlinePqcSubmissionConcurrencyTest,MesFrontlinePqcContextServiceTest,MesFrontlinePqcSubmitReceiptControllerTest,MesTeamLeaderSubmissionReviewServiceTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，4 个测试类共 27 tests，0 failures、0 errors；为修复问题 1 补齐冻结工序快照夹具并清理已删除 QA 测试字段引用，未放宽生产校验。
- 本专项没有数据库迁移、服务启动或写入型 E2E；P3 只读来源验证、独立验证、task-owned commit、受保护融合和主工作树复验仍待执行。
