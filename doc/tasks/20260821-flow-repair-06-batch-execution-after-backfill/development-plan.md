# Development Plan

## Design Principle

先完成并提交三类回填原子阶段，再以不可变 receipt 驱动统一建批；receipt 的 payload 永不改变，流程6另建可变 provisioning record 管理批次状态。所有入口最终调用同一建批服务、流程7映射、同一四材料门禁和流程修复10最终放行状态。

## Canonical Order

`active-order join + formal pick-list binding` -> `frontline submit/review` -> `double progress 100%` -> `Tx-A completion + conditional backfill` -> `immutable CompletionBackfillReceipt` -> `Tx-B flow6 batch create/reuse + BatchProvisioningRecord` -> `Tx-C flow7 Origin/TraceLink/Manifest` -> `flow6 BATCH_READY projection` -> `four materials upload` -> `flow 8 hard gate` -> `flow 10 final release` -> `flow 7 post-release trace query/RELEASE_DECISION append`.

## M1: CompletionBackfillReceipt

### Immutable payload fields

- `receiptId`, `tenantId`, `activeOrderId`, `workOrderId`, `batchCode`, `routeId`, `routeVersionId`.
- `pickListBindingId`, `pickListId`, `sourceSnapshotHash`, `bindingVersion`, `batchPickListRelationId`.
- `completionVersion`, `completionTransactionId`, `completionEventId`, `batchRecordId`, `processInspectionId`.
- `hasActualLoss` is order-level; preserve per-process loss decision, `lossReportStatus`, and, when applicable, `lossRecordId`/`lossQuantity`. For no loss persist `lossQuantity=0`, `lossDecision=NO_LOSS/NOT_REQUIRED`, and the zero-loss confirmation snapshot. Missing `lossRecordId` never implies no loss.
- `sourceEventIds`, formal source snapshot/hash, and immutable `receiptHash`.
- No `status`, `batchExecutionId`, retry metadata, or provisioning error is mutable on this receipt. There is no persisted `BACKFILL_PENDING` or `BACKFILL_FAILED` receipt state.

### Mutable BatchProvisioningRecord (flow6 owner)

- `provisioningRecordId`, `receiptId` (active flow uses `completionBackfillReceiptId`; independent flow uses its signed prerequisite receipt), `sourceSnapshotHash`, `completionVersion` when applicable.
- `batchExecutionId` (nullable until Tx-B creates/reuses it), `status`: `BATCH_PROVISIONING`, `BATCH_PROVISIONING_RETRYABLE`, `BATCH_PROVISIONING_BLOCKED`, `BATCH_READY`.
- `lastErrorCode`, attempt count, correlation/idempotency key, outcome-unknown query marker, timestamps and audit metadata.

`BATCH_READY` means the batch exists and flow7 Tx-C has successfully completed the required Origin/TraceLink/Manifest mapping. It is not a receipt status and cannot be set by UI, flow7, or release approval.

`CompletionBackfillFailureAttempt` is a separate post-rollback audit append, not either receipt or provisioning record: `activeOrderId`, `completionVersion`, correlation/idempotency key, `sourceSnapshotHash`, stable error code and `occurredAt`.

### Invariants

1. `CompletionBackfillReceipt` is immutable after Tx-A commit and contains only successful backfill/source facts; it never contains a `BATCH_*` status or `batchExecutionId`.
2. `BatchProvisioningRecord` is the sole mutable owner of `BATCH_*`, `batchExecutionId`, errors and retry metadata.
3. `BATCH_READY` requires persisted batch ID plus successful flow7 Tx-C mapping; it cannot be set by UI, flow7, or release approval.
4. A receipt exists only after Tx-A commits successfully as immutable `BACKFILL_SUCCEEDED`; Tx-A failure persists no receipt and records only a separate auditable failure attempt.

## M2: Transaction and Failure Semantics

### Tx-A (one transaction)

Lock active order and completion version. Validate double 100%, formal pick-list binding and submit/review events. Write completion event, batch record, process inspection and conditional loss document in one database transaction. For no loss, persist `hasActualLoss=false`/`NO_LOSS` fact only. Any validation or write failure rolls back all Tx-A writes, persists no receipt and no `BACKFILL_FAILED` state, then appends `CompletionBackfillFailureAttempt` outside Tx-A with the stable error code and source snapshot, and returns `BACKFILL_ATOMIC_ROLLBACK`; the user may reinitiate completion.

### Tx-B (separate transaction)

Call `UnifiedBatchProvisioner.provision(receiptId, sourceSnapshotHash, idempotencyKey)`. Lock the `BatchProvisioningRecord`, create/reuse batch, persist `batchExecutionId`, and keep status `BATCH_PROVISIONING` pending flow7 mapping. Flow6 does not write Origin/TraceLink/Manifest. On provisioning failure update only the record to `BATCH_PROVISIONING_RETRYABLE` or `BATCH_PROVISIONING_BLOCKED` with an error code. Repeating the command with the same receipt and idempotency key is the only retry path; Tx-A is not repeated.

### Tx-C (flow7 mapping transaction)

After Tx-B creates/reuses `batchExecutionId`, emit one idempotent event keyed by `batchExecutionId+sourceSnapshotHash`. Flow7 owns an independent Tx-C that creates Origin/TraceLink/Manifest and emits mapping-completed or a stable failure event. Flow7 never creates a batch and never writes the flow6 provisioning state. Flow6 consumes the mapping-completed event and, in its own local transaction, changes its `BatchProvisioningRecord` from `BATCH_PROVISIONING` to `BATCH_READY`; mapping failure keeps provisioning pending or blocks it according to the persisted error code.

The completion command may orchestrate Tx-A, Tx-B and mapping-event dispatch synchronously, but these remain separate transactions. After Tx-A failure the UI is `COMPLETION_NOT_SUBMITTED`: no receipt or batch retry is shown and complete remains available for a corrected/reissued request. After Tx-B success but before Tx-C mapping completion the UI is `BATCH_MAPPING_PENDING`; materials and release are disabled. Only after flow6 consumes Tx-C success is the UI `BATCH_READY_FOR_MATERIALS`. Tx-B/Tx-C retry or block actions update only the provisioning record and never the immutable receipt.

## M3: Unified Service and Entry Adapters

### Active-order adapter

Requires `completionBackfillReceiptId`, flow1 binding fields and matching snapshot/version. It rejects missing or stale receipts with `BACKFILL_RECEIPT_REQUIRED` or `SOURCE_SNAPSHOT_MISMATCH`.

### Independent manual adapter

Accepts a formal `IndependentBatchPrerequisiteReceipt` containing work order, batch code, route/version, source snapshot, owner and source relation. It does not require an active order, but cannot omit the formal receipt. Idempotency key is `entryType+sourceReference+sourceVersion`.

### Independent schedule adapter

Consumes the scheduler's signed completion/source receipt and validates work order, batch code, route/version and source snapshot. It may create a legal independent batch without an active order relation.

All adapters emit the same `UnifiedBatchProvisionRequest` and call the same provisioner; no adapter uploads materials or changes final release state.

## M4: Four-Material Gate

After batch creation and successful Tx-C mapping, material nodes are exactly: `INCOMING_INSPECTION_REPORT` (来料检报告), `STERILIZATION_REPORT` (灭菌报告), `FINISHED_PRODUCT_INSPECTION_REPORT` (成品检报告), `FINISHED_PRODUCT_INSPECTION_RECORD` (成品检记录). Flow8 owns node upload/approval and the hard gate. Gate state is exactly `MATERIALS_PENDING`, `MATERIALS_READY`, or `MATERIALS_RECHECK_REQUIRED`; missing/incomplete material returns `MATERIAL_INCOMPLETE`, and stale/hash/source changes return `MATERIAL_SNAPSHOT_MISMATCH` with `MATERIALS_RECHECK_REQUIRED`. Final release requires `MATERIALS_READY`.

## M5: Idempotency, Version and Concurrency

- Tx-A: `activeOrderId+completionVersion`; duplicate completion returns the existing receipt.
- Tx-B: `receiptId+sourceSnapshotHash+entryIdempotencyKey`; concurrent calls serialize on `BatchProvisioningRecord`.
- Active natural key: `completionBackfillReceiptId+sourceSnapshotHash+completionVersion` (or equivalent frozen completion transaction tuple). `releaseApplicationId` is optional source association only and cannot drive active-order batch uniqueness. Independent natural key uses its formal source reference/version.
- Snapshot or binding version changes invalidate the old request; never silently adopt newer source.

## M6: Migration and Rollback

Legacy batches without valid completion receipt, flow1 binding, or flow7 mapping remain blocked by `LEGACY_BATCH_EXECUTION_MIGRATION_REQUIRED`. Migration must prove source snapshot and relationship before setting `BATCH_READY`; otherwise leave as blocked. Tx-A rollback removes all three backfills atomically; Tx-B rollback preserves receipt and audit. Releases are append-only for audit; rollback stops new release rather than deleting history.

## Cross-thread Contracts

| 线程 | Input | Output | Owner | Idempotency | Status | Failure codes |
|---|---|---|---|---|---|---|
| 1 正式领料绑定 | activeOrderId, pickListId, sourceSnapshotHash | pickListBindingId, pickListId, sourceSnapshotHash, bindingVersion, batchPickListRelationId | flow1 | activeOrderId+bindingVersion | BOUND | PICK_LIST_BINDING_REQUIRED, PICK_LIST_SNAPSHOT_CHANGED |
| 4 完成/回填 | double100, submit/review event IDs, flow1 contract, form inputs | immutable receipt: successful backfills, order-level hasActualLoss, per-process loss decisions/lossReportStatus, source snapshot/hash, receiptHash | flow4 | activeOrderId+completionVersion | BACKFILL_SUCCEEDED only after Tx-A commit | BACKFILL_ATOMIC_ROLLBACK (no receipt), BACKFILL_CONTRACT_NOT_FROZEN |
| 5 条件损耗 | actual loss fact/quantity | true: hasActualLoss=true, lossRecordId/lossQuantity/lossReportStatus; false: hasActualLoss=false, lossQuantity=0, lossDecision=NO_LOSS/NOT_REQUIRED, zero-loss confirmation snapshot | flow5 | activeOrderId+completionVersion | CREATED/NOT_REQUIRED | LOSS_SOURCE_INVALID |
| 6 批次 provision | receipt/independent prerequisite, source hash | BatchProvisioningRecord, batchExecutionId, BATCH_* status and error/retry metadata | flow6 | active: completionBackfillReceiptId+sourceSnapshotHash+completionVersion | BATCH_PROVISIONING/BATCH_READY | retry whitelist; permanent errors BLOCKED |
| 7 映射/追溯 | receipt, binding, batchExecutionId, source snapshot | Tx-C Origin/TraceLink/Manifest; post-release trace/RELEASE_DECISION | flow7 | batchExecutionId+sourceSnapshotHash | MAPPED | TRACE_MAPPING_BLOCKED |
| 8 材料门禁 | batchExecutionId, four current material versions | gate result | flow8 | batchExecutionId+materialVersionSetHash | MATERIALS_PENDING/MATERIALS_READY/MATERIALS_RECHECK_REQUIRED | RELEASE_MATERIAL_GATE_REQUIRED, MATERIAL_NODE_MISSING, MATERIAL_UPLOAD_INCOMPLETE, MATERIAL_FILE_NOT_VERIFIED, MATERIAL_VERSION_STALE, MATERIAL_HASH_MISMATCH, MATERIAL_VERSION_CONFLICT, MATERIAL_MANIFEST_CHANGED, MATERIAL_SOURCE_SNAPSHOT_CHANGED, RELEASE_ENTRY_GATE_BYPASS, IDEMPOTENCY_CONFLICT |
| 9 多入口合同 | entryType, source receipt/reference, sourceVersion | normalized provision request | flow9 | entryType+sourceReference+sourceVersion | ACCEPTED | ENTRY_PREREQUISITE_MISSING, ENTRY_SOURCE_INVALID |
| 10 最终放行 | gate receipt, material gate, approver snapshot | final release state/audit | flow10 | batchExecutionId+releaseVersion | RELEASED | RELEASE_GATE_BLOCKED, RELEASE_SNAPSHOT_MISMATCH |
| 11 总门禁 | contracts, BDD/TDD, migration evidence | go/no-go conclusion | flow11 | taskId+contractVersion | PASS | TDD_EVIDENCE_MISSING, MIGRATION_BLOCKED |

## Verification Gates

1. Design review checks state and owner uniqueness.
2. Future RED adds failing tests before implementation; future GREEN proves minimum behavior; current task records both as `NOT RUN`.
3. Flow11 accepts only complete BDD/TDD/regression/migration evidence; design review is not GREEN evidence.

## Current Status

in_progress（设计合同已冻结；流程6局部实现、主线融合和定向测试已通过，跨流程闭环与迁移仍待真实集成）。

## Coding Verification Update (2026-08-24)

`e539e8a2c` implements the Flow 6 task-gate overload and legacy fixture correction. The targeted 37-test suite and 24-module MES compile passed. This does not replace formal Flow 4/7/9 integration or migration gates.

## 主流程统一冻结合同（2026-08-22）

- Tx-A 只允许本地数据库读写：外部服务调用必须在事务前完成并形成版本/hash 可验证快照；不一致返回 `SOURCE_SNAPSHOT_MISMATCH`。流程4独占 receipt owner：Tx-A 成功才创建不可变 `BACKFILL_SUCCEEDED`；失败回滚后在 Tx-A 外追加 `CompletionBackfillFailureAttempt` 并返回 `BACKFILL_ATOMIC_ROLLBACK`，不提交 receipt、不产生 `BACKFILL_FAILED`。流程6独占 `BATCH_*` provision 状态。
- `BATCH_PROVISIONING_RETRYABLE` 仅接受 `BATCH_DB_DEADLOCK`、`BATCH_LOCK_TIMEOUT`、`BATCH_TRANSIENT_DB_UNAVAILABLE`、`BATCH_IDEMPOTENT_QUERY_TIMEOUT`、`BATCH_PROVISIONING_OUTCOME_UNKNOWN`；outcome unknown 先按同 receipt/幂等键查询结果，不得盲目新建。其它错误写入 `BATCH_PROVISIONING_BLOCKED`。
- 独立凭证字段、签发和有效期以 task.md 冻结合同为准；前端不得自造。PQC 活跃订单和独立申请必须按 entryType 分流，均调用统一 provisioner。
- 四材料有效定义补充 `COMPLETED`、文件持久化、元数据/SHA-256、当前版本和 source hash；有批准字段必须 `APPROVED`，无字段不得默认批准；版本/hash/来源变化使 gate 进入 `MATERIALS_RECHECK_REQUIRED`，放行前置只能是 `MATERIALS_READY`。
- 迁移先 dry-run 分类 `INCOMPLETE_OR_AMBIGUOUS`/`ALREADY_RELEASED_REVIEW_REQUIRED`，需 owner 批准后才可写入；独立追溯用 `NOT_APPLICABLE` 原因码表达不适用关系。
