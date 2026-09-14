# Backend API Design

## Purpose and Scope

Implement the backend invariant that once an eDHR batch execution void becomes effective, the batch is terminal and all active workbench-facing work tasks for that batch are canceled in the same controlled business flow.

Scope includes all effective void paths:

- Normal BPM-approved batch void.
- Direct platform void paths, including golden-finger bulk void when it reuses direct void effect.
- Any legacy or duplicate effective void path still capable of setting batch status to `VOIDED`.

Scope excludes:

- Adding a new fallback path.
- Physically deleting work tasks, signatures, forms, archive rows, or change events.
- Allowing users to continue processing the old workbench tasks after void.

## Evidence Reviewed

- `IntRuoyiBackend/yudao-module-mes/src/main/java/.../MesProEdhrBatchExecutionServiceImpl.java`: batch status constants include `BATCH_STATUS_VOIDED = 60`; action lock reason is `批次已作废，只能追溯审计`.
- `IntRuoyiBackend/yudao-module-mes/src/main/java/.../MesProEdhrBatchVoidEffectServiceImpl.java`: effective void currently sets batch status to `60`, clears active context, and invalidates archive.
- `IntRuoyiBackend/yudao-module-mes/src/main/java/.../MesProEdhrWorkTaskServiceImpl.java`: `cancelActiveTasksByBatch(batchExecutionId, reason)` exists and cancels active tasks while revoking runtime task entitlement.
- `IntRuoyiBackend/yudao-module-mes/src/main/java/.../MesProEdhrWorkTaskMapper.java`: actionable workbench queries already exclude terminal batch statuses `30, 40, 50, 60`.
- `doc/tasks/20260726-edhr-personal-console-open-task-status/`: previous verified behavior says terminal batches must not surface as actionable personal-console tasks and `openTask` must remain fail-fast.

## Modules

- `MesProEdhrBatchVoidEffectServiceImpl`: should become the primary effective void enforcement point for task cancellation.
- `MesProEdhrRecordChangeServiceImpl`: must either delegate to the same effective void service or apply the exact same invariant if a legacy path remains active.
- `MesProEdhrWorkTaskServiceImpl`: existing work-task cancellation primitive remains the single service boundary for canceling active tasks and revoking runtime entitlement.
- `MesProEdhrWorkTaskMapper`: existing terminal-batch filtering remains a defensive query boundary, not the only source of correctness.
- `MesProEdhrBatchExecutionServiceImpl#openTask`: must continue blocking terminal voided batches; this is a safety gate, not a UI problem.

## API Contracts

No new public API is required for the first implementation.

Existing void endpoints and callbacks must preserve their current request/response contracts while adding this side effect:

- Input: approved/effective batch void request with batch execution ID and reason/comment.
- Processing: mark batch `VOIDED`, clear active context, invalidate latest archive if present, cancel active work tasks, record change event status/effective time.
- Output: same existing void response object.

The work task cancellation reason should be deterministic and audit-readable:

- Recommended format: `批次已作废：<reasonText>` when `reasonText` exists.
- If no detailed reason exists but the flow has a comment, use `批次已作废：<comment>`.
- If neither exists, fail fast if the existing void policy requires a reason; do not silently use an empty reason.

## Error Model

- If batch is already `VOIDED`, keep existing invalid-status behavior unless the caller already has an explicit idempotency contract.
- If work task cancellation fails, the void transaction must fail and roll back rather than leaving `VOIDED` batch with active actionable tasks.
- If required void reason/signature/BPM definition is missing, keep existing fail-fast errors.
- If a user opens an old task after void, `openTask` must continue returning a terminal-status error; do not convert that to success or a read-only fill page.

## Transactions and Idempotency

The effective void path must be transactional.

Recommended order:

1. Load and validate batch.
2. Validate void request and approval context.
3. Persist batch status `VOIDED`.
4. Clear active context key.
5. Invalidate latest archive when present.
6. Cancel active work tasks through `MesProEdhrWorkTaskService.cancelActiveTasksByBatch`.
7. Mark change event effective and return the persisted response.

`cancelActiveTasksByBatch` only targets active待处理、处理中、逾期 statuses; already completed and already canceled tasks remain untouched. Re-running a guarded effective void must not mutate completed history.

## Open Questions

- Whether `MesProEdhrRecordChangeServiceImpl` is still an active production path or only legacy coverage. Implementation must verify before editing to avoid duplicate side effects.
- Whether cancellation should create an additional operation-audit event per work task or whether work-task status, reason, completedAt, change event, and existing audit stream are sufficient.

## Design Blockers

- Implementation must not start without RED tests proving at least one effective void path leaves active tasks today.
- If current schema lacks `mes_pro_edhr_work_task.reason`, `remark`, `completed_at`, or status support, implementation must stop and run schema verification before any SQL or code change.
