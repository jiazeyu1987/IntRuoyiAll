# AC-M19 Database Schema Evidence

## Data Change Goal

Persist deterministic aggregation configuration and traceability for AC-M19 formal batch-record writes.

## Engine And Migration Tool

- Database engine: MySQL-compatible project migration SQL under `IntRuoyiBackend/sql/mysql`.
- Test fixture: H2-style DDL in `IntRuoyiBackend/yudao-module-mes/src/test/resources/sql/create_tables.sql`.

## Schema Changes

- Added `mes_pro_batch_record_cell_link_rule.aggregation_strategy`.
- Added `source_event_ids_json`, `source_allocation_ids_json`, `aggregate_hash`, and `backfill_idempotency_key` to `mes_pro_process_pool_order_process_completion`.
- Added `idx_mes_pp_order_process_completion_aggregate`.

## Data Safety

The completion migration fails fast if an existing completion table has rows but lacks the new non-null aggregate trace columns. The cell-link migration adds `aggregation_strategy` idempotently. No destructive MES data operation was added.

## Rollback Or Recovery Plan

Rollback is schema-level: remove the added aggregate trace columns and aggregate index only before AC-M19 code paths are deployed. If existing completion rows already exist without aggregate trace values, perform a formal data backfill first rather than applying placeholder defaults.

## BDD Scenarios

- `BDD: Schema stores aggregation strategy -> Given a process-pool report field maps to formal batch record; When multiple source reports feed one field; Then the mapping row can store the configured aggregation strategy.`
- `BDD: Completion stores aggregate trace -> Given order process completion triggers formal batch record write; When the completion row is saved; Then source event ids, source allocation ids, aggregate hash, and idempotency key are persisted.`

## RED

`RED: mvn -pl yudao-module-mes "-Dtest=MesProcessPoolTeamLeaderSchemaTest,MesProBatchRecordCellLinkSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, expected reason before fix: migration SQL did not contain aggregate trace columns/index or aggregation_strategy.`

## GREEN

`GREEN: mvn -pl yudao-module-mes "-Dtest=MesProcessPoolTeamLeaderSchemaTest,MesProBatchRecordCellLinkSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> BLOCKED, local JVM memory/page file pressure prevents reliable Maven execution in the shared workspace.`

## Migration Verification

`git diff --check` passed for migration SQL and test fixture DDL. Static SQL token checks confirm required column and index names are present.

## Blockers

Runtime migration execution against a database was not performed in this task because the requested scope is code/schema repair and Maven verification is currently resource-blocked.
