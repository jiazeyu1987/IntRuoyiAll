# 流程修复 7：批次执行完整映射和放行后追溯 PRD

## Purpose and Scope

本 PRD 冻结流程修复 7 的产品行为：批次执行必须能够从正式来源追溯到活跃订单、生产工单、领料单及分录、一线生产、一线 PQC、损耗与完成/回填凭证，并在实际放行后追加放行决定关系。产品目标是可审计、可重放、不可篡改的来源图，不改变流程 1/2/3/4/5/6/8/9/10/11 的状态所有权。

范围包括批次来源根、来源链接、快照/hash、批次 manifest、详情/列表/追溯查询、来源捕获幂等、放行决定后置关系、权限、历史记录、迁移分类和阻断提示。

## Evidence Reviewed

- `docs/product/production-role-system-operations.md`：正式来源、角色与放行操作边界。
- `docs/backend-development.md`：活跃订单申请放行资料只能使用正式来源。
- 流程修复 1/2/3/4/5/6/8/9/10/11 任务合同及本任务的当前代码审计。
- 当前批次执行、放行申请、来源回填、SQL 迁移草案和流程 7 定向测试证据。

## Product Summary

用户在批次执行详情中应看到一条有根、有版本、有 hash 的来源链。活跃订单入口的链条以 `activeOrderId + completionBackfillReceiptId/hash + flow1 pickListBindingId/sourceSnapshotHash + flow6 batchExecutionId/provisionReceipt` 为核心；`releaseApplicationId` 只在流程 10 实际产生放行决定后作为 `RELEASE_DECISION` 后置关系追加。独立、手工、排产入口各自携带正式来源凭证，不得借用活跃订单或放行申请关系。

## Target Users

- 生产组长：查看活跃订单、工单、领料和完成回填来源，确认批次来源可追溯。
- PQC 组长/质量人员：查看 PQC 正式事实、批次材料 manifest 和追溯阻断原因。
- 管理者代表：在流程 8 材料齐套后通过流程 10 产生最终放行决定。
- 审计/质量追溯人员：按批次、正式来源 ID、快照 hash 和放行决定查询历史。
- 系统服务：消费上游 receipt，按正式 ID、版本和 hash 建立幂等来源图。

## First Version Scope

1. 支持 `ACTIVE_ORDER_COMPLETION`、`PQC_INDEPENDENT`、`MANUAL`、`SCHEDULED` 四类入口。
2. 为批次执行保存 Origin、TraceLink、Manifest 和 append-only 历史。
3. 活跃订单来源必须包括流程 1 领料绑定、流程 4 completionBackfillReceipt、流程 6 batch provision 结果，并按需包括流程 2/3/5 正式事实。
4. 有实际损耗时保存正式损耗单/损耗 receipt；`NO_LOSS` 只保存正式无损耗事实，不创建损耗单。
5. 提供批次列表、详情、来源图/历史和来源捕获接口；放行决定接口必须带明确 `originId`，并通过 `RELEASE_DECISION` TraceLink 追加 `releaseApplicationId`。
6. 缺来源、版本/hash 不一致、重复来源身份或映射不完整时进入 `TRACE_MAPPING_BLOCKED`，禁止将查询结果当作完整追溯。

## Non-Goals

- 不拥有活跃订单完成、三类回填、损耗判定、批次创建/复用、四份材料上传或最终 `RELEASED` 状态。
- 不把尚未发生的 `releaseApplicationId` 作为活跃订单建批前置。
- 不按工单号、批号、名称、最新投影或 applicationId 猜测关系。
- 不在本任务内补造缺失的流程 1/4/5/6 正式 receipt，不默认接纳历史不完整数据。
- 不绕过流程 8 四材料 gate 或流程 10 最终放行。

## Functional Requirements

### FR-1 入口与正式凭证

- `ACTIVE_ORDER_COMPLETION` 必须消费流程 4 的 `completionBackfillReceiptId/hash`、流程 1 的 `pickListBindingId`/`sourceSnapshotHash` 和流程 6 的 `batchExecutionId`/provision receipt/status。
- `PQC_INDEPENDENT`、`MANUAL`、`SCHEDULED` 必须分别携带流程 9 签发的 `IndependentBatchPrerequisiteReceipt`，并以 `entryType + credentialId + payloadHash` 幂等。
- 每个入口的来源凭证、状态 owner、幂等键和 `NOT_APPLICABLE` 关系必须可审计。

### FR-2 来源关系与快照

Origin 至少表达 `batchExecutionId`、`entryType`、`activeOrderId`（适用时）、`workOrderId/code`、批号、完成 receipt、领料绑定、生产事实、PQC 事实、损耗事实、`hasActualLoss`、来源快照/hash、创建/复用结果和 source bundle hash。

TraceLink 使用正式 `sourceObjectId` 和 `linkType` 建立关系。规范来源身份为 `(linkType, sourceObjectId)`；同一身份的版本/hash 与已冻结值不一致必须阻断，不能通过增加 `sourceVersion` 生成第二条来源。

### FR-3 来源捕获与幂等

批次创建成功后，流程 7 才能在后继事务或可验证幂等事件中捕获来源。相同入口幂等键和相同 source bundle 重试返回同一结果；批次不存在、流程 6 provision 未成功、receipt 缺失或来源 hash 不一致时不得写入完整映射。

### FR-4 查询与后置放行关系

- 列表支持批次、入口、活跃订单、工单、批号、映射状态和来源完整性筛选；`releaseApplicationId` 只能通过 `RELEASE_DECISION` TraceLink 查询。
- 详情返回当前 Origin、全部 TraceLink、manifest/history、来源版本/hash、阻断码和权限可见性。
- 追溯接口返回批次 -> 活跃订单 -> 工单 -> 领料头/分录 -> 生产/PQC -> 损耗 -> 回填 -> 放行决定的有向链；独立入口对不适用节点返回 `NOT_APPLICABLE`。
- 放行决定追加必须指定 `originId`，且只能由流程 10 在流程 8 gate 成功后写入；流程 7 不写 `RELEASED`。

### FR-5 不可篡改与权限

Origin、TraceLink、Manifest 和来源快照一经捕获只能追加新历史，不允许更新或删除。数据库 append-only 触发器、服务层权限和审计日志必须同时生效。生产/PQC 角色只读其职责范围内事实；审计和质量角色可读全链；只有授权的流程 10 服务可追加放行决定。

## Business Rules

1. 流程 4 是 `ACTIVE_ORDER_COMPLETED` 唯一 owner，并先提交不可变 completionBackfillReceipt；流程 6 之后才返回 batchExecutionId；流程 7 再建图。
2. 流程 5 仅决定 `REQUIRED`、`NO_LOSS` 或 `BLOCKED`；只有 `REQUIRED` 才有损耗单，`NO_LOSS` 必须有正式无损耗事实。
3. 流程 7 的 `TRACE_CAPTURED` 必须同时满足 batchExecutionId、流程 6 成功结果、正式来源 ID、快照/hash 和完整性校验。
4. 映射缺失、来源身份冲突、快照 hash 不符、receipt 版本不一致或历史无法归属时为 `TRACE_MAPPING_BLOCKED`。
5. 四份材料齐套和最终放行分别由流程 8、流程 10 拥有；流程 7 只能消费其 manifest/gate 或 RELEASE_DECISION 关系。
6. 历史数据不得静默补领、补损耗、补 activeOrder 或补 releaseApplicationId；只能 dry-run 分类后经审批迁移或保持阻断。

## States and Transitions

`TRACE_PENDING` -> `TRACE_CAPTURED`：流程 6 provision 成功后，流程 7 以正式 receipt 和来源快照捕获完整关系。

`TRACE_PENDING` -> `TRACE_MAPPING_BLOCKED`：批次、receipt、正式来源、版本/hash、身份唯一性或权限校验失败。

`TRACE_CAPTURED` -> `TRACE_MAPPING_BLOCKED`：后续发现来源冲突、manifest/hash 不一致或历史完整性校验失败；原历史保留。

`TRACE_CAPTURED` -> `TRACE_CAPTURED`：相同幂等请求重放，返回既有 origin/trace 结果，不生成重复来源。

放行决定是后置 TraceLink，不改变 origin key；`RELEASED` 只由流程 10 的独立状态机写入。

## Edge Cases

- 流程 4 receipt 成功但流程 6 创建失败：receipt 保留，批次映射不写，允许流程 6 幂等重试。
- 批次已存在但流程 7 映射缺失：追溯返回阻断，不能凭工单/批号补链。
- `NO_LOSS`：显示无损耗正式事实和 `lossRelation=NOT_APPLICABLE`，不得创建损耗单。
- 独立入口：activeOrder、领料、完成、损耗关系全部显式 `NOT_APPLICABLE`，但其独立凭证必须可追溯。
- 同一 `sourceObjectId/linkType` 出现不同版本或 hash：拒绝捕获并返回 canonical identity mismatch。
- 同一批次存在多个 Origin：放行决定必须带 originId；缺少明确 originId 时拒绝，禁止取第一条或最新一条。
- 已放行历史来源不全：保留历史，标记人工复核，不自动改写为完整追溯。

## Acceptance Criteria

- AC-01 活跃订单建批不要求 releaseApplicationId，且详情可追溯 activeOrder、completion receipt、领料绑定和 batch provision receipt。
- AC-02 四类入口使用各自正式凭证和幂等键；独立入口返回 `NOT_APPLICABLE` 而非伪造活跃订单关系。
- AC-03 来源捕获严格按正式 ID、版本和 hash；任何猜测、默认、最新投影反查均被拒绝。
- AC-04 同一 canonical source identity 的 hash/version 冲突进入 `TRACE_MAPPING_BLOCKED`，不产生第二条关系。
- AC-05 有损耗才有损耗单；`NO_LOSS` 无损耗单但有正式无损耗事实。
- AC-06 详情/列表/追溯返回 Origin、TraceLink、Manifest 历史、阻断码和后置放行关系；列表的 releaseApplicationId 来自 `RELEASE_DECISION` TraceLink。
- AC-07 只有流程 10 在流程 8 gate 后追加 RELEASE_DECISION 并写 `RELEASED`；流程 7 不改变最终放行状态。
- AC-08 append-only 数据、权限、迁移 dry-run、回滚和跨批次隔离均有可复核证据；缺证据则保持 blocker。

## Open Questions

- 流程 1 `pickListBindingId`、领料头/分录快照和唯一 sourceEntryId 的最终 schema owner。
- 流程 4 completionBackfillReceipt 的签发接口、版本策略和 sourceSnapshotHash 计算规范。
- 流程 5 NO_LOSS 正式事实的 canonical receipt 类型和历史零损耗迁移策略。
- 流程 6 batch provision receipt/status 的跨服务事件或同步接口。
- 流程 8 manifest/gate 和流程 10 RELEASE_DECISION 的最终权限对象与接口版本。

## Latest Verification Boundary (2026-08-23)

## Tx-C Formal Producer and Event Contract (2026-08-23)

流程 6 成功创建或复用批次后，流程 7 通过 `POST /mes/pro/edhr-batch-execution/traceability/tx-c` 启动 Tx-C。请求只允许携带 `batchExecutionId`、`eventId`、`idempotencyKey` 和可选的预期 `sourceSnapshotHash`、`sourceBundleHash`、`completionBackfillReceiptHash`、`sourceVersion`；客户端不得携带 Origin、TraceLink、Manifest 或正式来源 payload。

生产者在持久化边界读取流程 6 的成功 provision 审计事件（`MesProEdhrOperationAuditEventDO`，`batchExecutionId + operationType=OPEN + resultStatus=SUCCESS`）、流程 1 的 `pickListBindingId` 及头/分录快照，并要求流程 4 completionBackfillReceipt、流程 2/3 生产/PQC 事实和流程 5 损耗事实以不可变 `sourceEvidence` 进入流程 6 审计 metadata。任一正式 receipt、sourceEvidence、绑定快照、租户或 hash/version 缺失/不一致时，生产者必须持久化 `TRACE_MAPPING_BLOCKED` 失败事件，不创建成功映射；禁止 mock、placeholder、默认成功、客户端补造或按工单号猜测。

Tx-C 在一个事务中写入不可变 Origin、TraceLink、Manifest 和 outbox 事件；成功事件为 `FLOW7_TRACE_MAPPING_SUCCEEDED`，携带 `batchExecutionId`、`originId`、`originLinkId`、`traceLinkHash`、`sourceSnapshotHash`、`manifestVersion` 和事件 payload hash。失败事件为 `FLOW7_TRACE_MAPPING_FAILED_RETRYABLE` 或 `FLOW7_TRACE_MAPPING_FAILED_FINAL`，稳定业务错误码为 `TRACE_MAPPING_BLOCKED`，并记录失败原因、重试白名单和 canonical source identity。流程 6 只消费成功 outbox 事件并自行推进 `BATCH_READY`；流程 7 不写流程 6 状态。

生产者在第一次正式读取后形成预检指纹，Tx-C 写入前再次读取相同持久化来源；`sourceSnapshotHash`、`sourceBundleHash`、receipt hash、source version、绑定版本或 canonical source identity 发生变化时必须 fail-fast，持久化 `SOURCE_CHANGED_AFTER_PRECHECK` 失败事件。成功/失败事件按 `eventId + idempotencyKey` 幂等，重复消费返回同一不可变事件，不更新或删除历史。

流程 8 只消费流程 7 的持久化读取合同：`batchExecutionId`、`originLinkId`、`traceLinkHash`、`sourceSnapshotHash`，并在其材料预检时传回期望 hash/version；关系不一致返回 `FLOW8_TRACE_LINK_ORIGIN_MISMATCH`，预检后来源变化返回 `FLOW8_SOURCE_PRECHECK_STALE`。权限按当前租户和 `mes:pro-edhr-batch-execution:trace-capture`/查询权限隔离，跨租户或不属于该批次的关系拒绝读取。

当前流程 6 审计 metadata 尚未持久化完整 `sourceEvidence`/receipt payload，故真实生产路径在该上游合同补齐前必须保持结构化 blocker；本节不把独立测试或静态 fixture 视为全链路完成。

- Current `int_main` Maven 3.9.16 clean compile passed at `2026-08-23T14:55:52+08:00` across 24 reactor modules (MES 2857 main sources); the focused 29 tests (17 validator + 12 service contract) passed at `2026-08-23T14:57:33+08:00` with 0 failures/errors/skips and `BUILD SUCCESS`; testResources copied normally.
- The linked-worktree flatten/compiler ACL failures are historical context only and are superseded by the fresh main-workspace result.
- The verified slice covers only Flow7 validator/service-contract behavior (including required `originId`, `RELEASE_DECISION` TraceLink lookup, canonical source identity, and loss-fact mapping); it does not prove the complete workflow, real database migration/Mapper/runtime permissions, service startup, the Flow8 four-material gate, the Flow10 `RELEASED` transition, or write-enabled E2E.
- Full regression, real database migration/append-only trigger/permission/runtime verification, upstream formal-receipt adapters/owners/fixtures, Flow8/Flow10 cross-thread integration, and write-enabled E2E remain `NOT RUN` and are blockers.
- The current task status is `partial / blocked`; this slice evidence is not release approval.

## Product Blockers

- 当前正式上游 receipt/快照适配器和 owner 尚未全部冻结，无法宣称全链路生产可用。
- 真实数据库 migration、append-only trigger、Mapper、权限对象和运行时验证尚未在本任务环境执行。
- 流程 8 四材料硬门禁、流程 10 唯一 RELEASED 终态和真实后置关系尚未完成跨线程集成。
- 完整 REGRESSION、服务启动和写入型 E2E 仍为 NOT RUN；29 项流程 7 定向测试只能证明独立切片。
