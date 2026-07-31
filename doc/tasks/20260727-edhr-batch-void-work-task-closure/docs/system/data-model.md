# Data Model

## Purpose and Scope

Define the data state transitions needed for eDHR batch void and work task closure without introducing new schema unless implementation proves current columns are missing.

## Evidence Reviewed

- Batch status constants include `CLOSED=30`, `ARCHIVED=40`, `REJECTED=50`, `VOIDED=60`.
- Work task statuses include待处理、处理中、已完成、已取消、已逾期.
- Existing `cancelActiveTasksByBatch` writes `status=CANCELED`, `reason`, `remark`, and `completedAt`.
- Existing void effect invalidates latest archive with `archiveValidFlag=false`, `archiveValidStatus=VOIDED`, and `invalidatedByChangeEventId`.

## Entities

- `mes_pro_edhr_batch_execution`: authoritative batch execution lifecycle state.
- `mes_pro_edhr_work_task`: workbench-facing user task state and ownership.
- `mes_pro_edhr_record_change_event`: controlled change event for void/reopen/supplement/reexecute.
- `mes_pro_edhr_batch_execution_archive`: sealed/archive state invalidated by void.
- `mes_pro_edhr_batch_execution_signature`: signature evidence for void request/approval where applicable.
- Runtime task entitlement records managed by existing permission scope service.

## Relationships

- A work task belongs to a batch through `batch_execution_id`.
- A void change event targets one batch execution through `batch_execution_id`.
- Archive invalidation references the effective change event through `invalidated_by_change_event_id`.
- Runtime task entitlement source key references the work task and must be revoked when active work task is canceled.

## State Models

Batch execution:

- Before void: may be in a voidable non-terminal state according to existing policy.
- Effective void: `status=60/VOIDED`.
- After void: audit-only, not processable by `openTask`.

Work task:

- Before void: active tasks may be待处理、处理中、or已逾期.
- Effective void: active tasks become `CANCELED`, with `completedAt` and reason.
- Completed history: `DONE` remains `DONE`; do not rewrite valid completed history.
- Already canceled: remains `CANCELED`.

Archive:

- Existing archive, if present, becomes invalid with `archiveValidStatus=VOIDED`.
- Missing archive is allowed only if the existing void policy already permits that state.

## Migration Notes

No migration is planned in the design phase. Implementation must stop and verify schema if any required work-task or archive columns are missing from current database/migrations.

## Data Integrity Rules

- It is invalid for a `VOIDED` batch to have active work tasks visible as actionable workbench tasks.
- It is invalid to relax terminal `openTask` checks to compensate for stale tasks.
- It is invalid to physically delete work tasks to remove them from workbench.
- Task cancellation and batch void must be in one transactional unit where possible.
- Direct SQL repair is not part of normal flow and requires separate authorization if needed for historical dirty data.

## Open Questions

- Whether historical rows from before this fix should be repaired with a one-time migration or left hidden by query filtering. Current implementation design only covers new effective voids.

## Design Blockers

- Any required schema mismatch blocks implementation.
- Any request to repair existing historical production data requires separate data-safety approval and database rules.
