# Database Schema Evidence - AC-M21 Process Inspection Aggregation

## Data Change Goal

Create a first-class structured aggregate table for AC-M21 so leader-approved PQC submissions become auditable process-inspection rows, instead of only a status marker on `mes_pro_process_pool_pqc_record`.

## Affected Entities

- New table: `mes_pqc_process_inspection_aggregate_detail`.
- Existing table: `mes_pqc_inspection_task` task status comment now includes `CONFIRMED`.
- Existing test fixture: H2 `create_tables.sql` now includes the aggregate detail table.
- Existing cleanup fixture: `clean.sql` deletes aggregate rows before PQC records.

## Migration

- Migration file: `IntRuoyiBackend/sql/mysql/20260805_mes_pqc_process_inspection_aggregate_detail.sql`.
- Depends on:
  - `20260803_mes_process_pool_pqc_process_inspection_aggregation`
  - `20260803_mes_pqc_item_equipment_standard_snapshot`
- Key constraints:
  - Unique key: `tenant_id + event_id + source_piece_detail_id + deleted`.
  - Indexes by review, PQC task/sample, and production submit event.

## Data Safety

- Non-destructive schema addition.
- No existing data is silently backfilled or coerced.
- Existing `mes_pqc_inspection_task.task_status` values are not rewritten; the migration only documents the formal `CONFIRMED` state.
- Duplicate aggregation is prevented by both service CAS and database unique key.

## Rollback

- Remove `mes_pqc_process_inspection_aggregate_detail` if rollback is required before production use.
- Existing PQC records and task rows remain intact.
- Reverting the task status comment is metadata-only and does not change stored task states.

## BDD

- BDD: Approved PQC review creates structured process inspection aggregation -> aggregate detail rows are persisted with tenant/event/review/task/item/piece traceability.
- BDD: Duplicate or cross-tenant aggregation is excluded -> unique key and tenant-matched CAS prevent duplicate or cross-tenant detail rows.

## RED

- RED: target Maven test command failed because aggregate detail DO/mapper/schema did not exist before implementation.

## GREEN

- GREEN: selected production `javac` over new DO/mapper/service/mapper changes -> PASS.
- GREEN: `MesProcessPoolSchemaTest` + `MesQaPqcSchemaTest` via JUnit Console -> PASS, 5 tests successful.

## Verification

- Verified new DO maps to `mes_pqc_process_inspection_aggregate_detail`.
- Verified migration contains aggregate table, unique key, review/task/submit-event indexes, and `CONFIRMED` task status metadata.
- Verified H2 fixture contains the aggregate table and cleanup deletes aggregate rows before parent PQC records.

## Blockers

- Full Maven verification remains blocked by unrelated module test compile failures and Windows native memory exhaustion during full MES javac.
