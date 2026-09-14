# 开发计划：多入口双场景前置合同

## 1. 设计原则

统一的是领域门禁和状态 owner，不是所有入口的输入凭证。流程修复 9 只按 `entryType` 校验对应正式前置，并把建批请求交给流程修复 6；入口不得创建或修改回填 receipt、材料状态、批次状态或最终放行状态。

批次创建与最终放行分离：流程修复 6 返回已经创建/复用的 `batchExecutionId`；流程修复 8 在该批次上收集四份材料并执行 gate；流程修复 10 消费该 `batchExecutionId` 和材料 manifest，唯一写入 `RELEASED`。

## 2. 正式凭证合同

### 2.1 活跃订单链路：`CompletionBackfillReceipt`

`CompletionBackfillReceipt` 只能由流程修复 4 产生，并且只有 `status=BACKFILL_SUCCEEDED` 才能被活跃订单入口消费。`ACTIVE_ORDER_COMPLETION`、属于活跃订单的 `SCHEDULED`、活跃订单关联的 PQC 和 `MANUAL_CONTROLLED_RETRY` 都是消费方，不得创建、修改或重新回填 receipt。

必需事实包括：

- `tenantId`、`activeOrderId`、`workOrderId`、`batchCode`、`routeId`、`routeVersionId`；
- 流程修复 1 的 `pickListBindingId`、`pickListId`、`sourceSnapshotHash`、`bindingVersion`；
- `productionProgress=100`、`inspectionProgress=100`、`completionVersion`、`completionEventId`；
- 批记录、过程检验和损耗结论：`hasActualLoss=true` 时必须有正式损耗单，`false` 时必须是 `NO_LOSS` 正式事实；
- receipt version/hash、`status=BACKFILL_SUCCEEDED`、幂等键和审计事件。

### 2.2 独立链路：`IndependentBatchPrerequisiteReceipt`

canonical 名称与流程修复 6 一致。允许的独立 entryType 为 `MANUAL`、`SCHEDULED`、`PQC_INDEPENDENT`。凭证由后端受控签发，调用方不能自定有效期、签发人或撤销状态。至少冻结以下字段：

| 类别 | 字段 |
|---|---|
| 标识/租户 | `receiptId`、`tenantId`、`entryType` |
| 业务对象 | `workOrderId`、`workOrderCode`、`routeId`、`routeVersion`、`batchCode` |
| 正式来源 | `sourceRelationId`/`batchExecutionSourceRelation`、正式 source IDs、`sourceSnapshotHash` |
| 理由与签发 | `businessReason`、`issuerSystem`、`issuerUserId`、`issuerUserRole`、`issuedAt` |
| 生命周期 | `expiresAt`、`revokedAt`、`revocationReason`、`credentialVersion` |
| 完整性/审计 | `payloadHash`、签名、`auditEventId`、幂等键 |

其中正式 source relation 必须列出 source object、material source、路线版本、租户和追溯根；不能只传工单号/批号/路线号。有效期由后端根据 entryType 生成并在调用时校验；过期、撤销、签名/hash/version 不一致均阻断。

`PQC_INDEPENDENT` 是合法独立创建场景，但只有流程 9 允许的 entryType、有效 `IndependentBatchPrerequisiteReceipt` 和正式 source relation 才能调用流程 6。缺凭证必须阻断。PQC 关联活跃订单时只能消费流程 4 已生成的 completion receipt；PQC 批准不能产生回填，也不能自行先建批。

## 3. 统一建批命令（交给流程 6）

建议接口：`BatchExecutionProvisionResult createOrReuse(BatchExecutionProvisionCommand command)`。

命令至少包含：

- `entryType`、`entryBusinessId`；
- `sourceCredentialType`、`sourceCredentialId`、`sourceContextHash`；
- 可选 `activeOrderId`、`workOrderId`、`pickListBindingId`、`pickListId`；
- `routeVersionId`、`idempotencyKey`、`expectedSourceVersion`、`payloadHash`；
- `tenantId`、actor 和权限从安全上下文取得，不信任客户端覆盖。

服务按 entryType 强制选择凭证：活跃订单类型必须验证流程 4 receipt；`MANUAL/SCHEDULED/PQC_INDEPENDENT` 必须验证流程 6 canonical 独立 receipt。active order 字段只允许出现在活跃链路；独立入口无 activeOrderId 合法，带 activeOrderId 却缺完整活跃 receipt 必须 `ENTRY_SCENARIO_MISMATCH`。

成功输出至少包含：`batchExecutionId`、`created/reused`、`batchExecutionSourceRelationId`、`sourceContextHash`、`entryType`、`credentialId`（独立时为 receiptId）、`auditEventId`。流程 9 不写批次状态，流程 6 才拥有创建/复用结果。

## 4. 创建入口矩阵

| 入口 | 场景与权限 | 状态 owner | 必需输入 | 成功输出 | 幂等键 | 失败码 | 追溯 |
|---|---|---|---|---|---|---|---|
| 活跃订单完成 | 完成节点适配；生产负责人范围 | 流程 4 拥有完成/receipt；流程 9 只适配 | activeOrderId、workOrderId、流程 1 绑定字段、双 100%、流程 4 receipt | 交流程 6，返回 batchExecutionId created/reused | `entryType+entryBusinessId+receiptId+payloadHash` | `ACTIVE_ORDER_CREDENTIAL_REQUIRED`、`BACKFILL_NOT_SUCCEEDED` | receipt -> active/work/pick-list -> source relation -> batch |
| 排产完成 | 活跃订单排产只能消费流程 4 receipt；独立排产使用 `SCHEDULED` 独立 receipt | 排产只拥有排产状态；流程 6 拥有批次状态 | schedule ID、对应 credential、expectedSourceVersion | 流程 6 建批结果 | `entryType+scheduleId+receiptId+payloadHash` | `ENTRY_SCENARIO_MISMATCH`、`CREDENTIAL_REQUIRED`、`SOURCE_VERSION_CONFLICT` | schedule -> receipt -> source relation -> batch |
| PQC 批准联动 | 活跃订单 PQC 只消费流程 4 receipt；独立 PQC 使用 `PQC_INDEPENDENT` receipt | PQC 只拥有批准事实 | application/task、entryType、对应 receipt、source hash | 凭证通过后交流程 6；不得先建批 | `entryType+applicationId+receiptId+payloadHash` | `BACKFILL_NOT_SUCCEEDED`、`INDEPENDENT_CREDENTIAL_REQUIRED`、`ENTRY_SCENARIO_MISMATCH` | PQC event -> receipt -> source relation -> batch |
| 页面手工/受控重试 | 专用权限、原因；活跃消费流程 4 receipt，独立使用 `MANUAL` receipt | 页面不拥有业务状态 | entryType、receipt ID、source hash、reason、expected version | 原建批结果或明确 blocker | `entryType+entryBusinessId+receiptId+payloadHash` | `CREDENTIAL_REQUIRED`、`IDEMPOTENCY_PAYLOAD_CONFLICT` | user/audit -> receipt -> batch |
| 独立批次创建 | 独立业务权限；可无 activeOrderId | 流程 6 拥有批次；后端签发方拥有 receipt | canonical receipt 全字段、正式 source relation、route/version | 独立 batchExecutionId 与 relation | `entryType+sourceRelationId+receiptId+payloadHash` | `INDEPENDENT_CREDENTIAL_REQUIRED`、`SOURCE_RELATION_REQUIRED`、`RECEIPT_EXPIRED` | source IDs/material relation -> batch relation -> release |

入口差异是触发时机和正式场景，不是自有前置规则。入口不能用 PQC 批准替代活跃完成/回填，不能用工单+批号+路线直建，不能 warning 后继续。

## 5. 放行入口矩阵（消费既有 batchExecutionId）

| 入口 | 场景 | 权限/owner | 必需输入 | 统一结果 | 追溯 |
|---|---|---|---|---|---|
| 批次详情提交 | 活跃或独立批次 | 申请角色只拥有申请事实；流程 8 拥有材料 gate | 已创建/复用的 batchExecutionId、source relation、四材料 manifest | 流程 8 gate；不能建批或 RELEASED | batch -> relation -> manifest |
| PQC/生产放行申请 | 两类来源均适用 | 只拥有申请/复核事实 | batchExecutionId、四材料 gate request、expected version | `MATERIALS_READY` 后交流程 10 | application -> batch -> manifest |
| 管理者代表批准 | 两类来源均适用 | 流程 10 唯一拥有最终 RELEASED | batchExecutionId、流程 8 gate result、manifest hash、签名、expectedVersion | 流程 10 唯一写 `RELEASED`；重复返回同一结果 | release -> manifest -> batch relation -> active/independent source |

批次执行创建后才上传固定四份材料：来料检报告、灭菌报告、成品检报告、成品检记录。流程 8 要求四份当前有效、hash/version 与 manifest 一致；缺件或过期阻断，流程 10 才能消费 gate 结果并最终放行。

## 6. 幂等、复用、迁移和回滚

- 同一入口+业务 ID+同一 receipt+同一 payload hash 重试返回原建批结果。
- receipt、sourceContextHash、source relation 或 payload 不同，返回冲突，不覆盖原批次。
- 不同合法入口仅在 source relation/context、租户、路线版本和业务规则完全一致时复用同一批次；否则创建独立批次。
- 无正式 receipt/source relation 的历史批次保持 `BLOCKED_LEGACY`，不能猜测认领。
- 迁移只允许绑定可验签、未过期/未撤销、hash/version 可复算的正式 receipt；未知记录不自动放行。
- 回滚只撤销未写入正式 release decision 的建批尝试；已放行事实只能用有权限的反向业务命令和审计，不覆盖原事实。

## 7. 跨线程契约

| 线程 | 提供 | 流程 9 使用边界 |
|---|---|---|
| 1 | pick-list binding 四字段 | 仅活跃链路；独立入口不伪造 |
| 4 | 唯一产生 completionBackfillReceipt、完成版本、三类回填结果 | 活跃入口只消费 `BACKFILL_SUCCEEDED` |
| 5 | `hasActualLoss`、`NO_LOSS` 或正式损耗单事实 | 由流程 4 纳入 receipt；流程 9 不写损耗 |
| 6 | canonical `IndependentBatchPrerequisiteReceipt`、统一建批服务、batchExecutionId、created/reused | 流程 9 只传递合约；流程 6 拥有批次状态 |
| 7 | 批次完整映射和追溯根 | 建批成功后建立完整来源映射和放行后追溯 |
| 8 | 四材料节点、manifest、hash/version、gate 结果 | 两类批次统一消费硬门禁 |
| 10 | 已创建 batchExecutionId 的最终 release decision、签名、`RELEASED` | 流程 9 不调用建批替代放行；10 唯一写终态 |
| 11 | BDD/TDD、回归、迁移和总体验证门禁 | 验证分流、幂等、四材料、放行和追溯 |

## 8. 错误码与实施顺序

建议错误码：`ENTRY_SCENARIO_MISMATCH`、`ACTIVE_ORDER_CREDENTIAL_REQUIRED`、`INDEPENDENT_CREDENTIAL_REQUIRED`、`SOURCE_RELATION_REQUIRED`、`RECEIPT_EXPIRED`、`RECEIPT_REVOKED`、`RECEIPT_SIGNATURE_INVALID`、`SOURCE_CONTEXT_CONFLICT`、`BACKFILL_NOT_SUCCEEDED`、`MATERIAL_MANIFEST_NOT_READY`、`FINAL_RELEASE_OWNER_REQUIRED`、`BLOCKED_LEGACY`。

实施顺序：先冻结双 receipt 和 source relation；再写 entryType/场景混用/幂等 RED；接入流程 6 建批；接入流程 8 四材料 gate；由流程 10 消费 batchExecutionId 唯一放行；最后由流程 11 执行全链路回归和迁移门禁。

## Verification Status

文档设计完成；生产实现、RED、GREEN、REGRESSION、迁移和 E2E 均 `NOT RUN`。

### 主流程冻结核验
独立凭证由后端签发，至少含 receiptId、tenantId、entryType、工单/路线/批号、来源关系/IDs、sourceSnapshotHash、理由、签发者、issuedAt/expiresAt、撤销、版本、payloadHash、签名审计和幂等键；PQC 按活跃 receipt/独立凭证分流到流程6统一建批。
