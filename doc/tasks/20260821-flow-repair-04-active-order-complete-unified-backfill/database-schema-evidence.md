# Flow4 Database Schema Evidence

## Data Change

The completion receipt table stores immutable Tx-A evidence and formal result IDs for batch-record, process-inspection, and loss/`NO_LOSS` outcomes. Unique tenant/order/idempotency constraints preserve one successful completion receipt and deterministic retry behavior.

## Engine and Migration

- Engine: MySQL.
- Migration: `IntRuoyiBackend/sql/mysql/20260822_mes_process_pool_active_order_completion_receipt.sql`.
- The schema stores `batch_record_id` and `process_inspection_id` as required formal result references; receipt status is fixed to `BACKFILL_SUCCEEDED` by service validation and the receipt has no mutable batch-execution state.

## Safety and Rollback

The migration was statically reviewed and covered by `MesProcessPoolTeamLeaderSchemaTest`. Database apply, backup, rollback, and historical-data reconciliation were not run because no authorized test tenant/database was available. Rollback requires the project migration owner to use the approved backup/recovery procedure; no destructive rollback was attempted.

## BDD

- `BDD: receipt persistence -> Given` all three Tx-A writes succeed; `When` the transaction commits; `Then` exactly one immutable receipt row references the formal result IDs.
- `BDD: receipt failure -> Given` a writer or receipt constraint fails; `When` Tx-A commits; `Then` the transaction rolls back and no success receipt remains.
- `BDD: tenant isolation -> Given` a receiptId from another tenant; `When` Flow6 reads it; `Then` the read is blocked.

## TDD Evidence

- `RED: schema contract assertions` -> FAIL before the new formal result ID columns/NO_LOSS contract existed.
- `GREEN: MesProcessPoolTeamLeaderSchemaTest` -> PASS as part of the 37/37 targeted suite.
- `GREEN: git diff --check` -> PASS.

## Verification

The schema contract test verifies required receipt columns, formal result IDs, and the `NO_LOSS` branch. The migration file is present and statically reviewed; no database apply was run.

## Blockers

Migration apply/rollback and a real database transaction rollback proof remain `NOT RUN`; do not treat unit tests as database execution evidence.
