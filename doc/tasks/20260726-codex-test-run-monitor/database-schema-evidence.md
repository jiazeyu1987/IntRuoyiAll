# Database Schema Evidence

## Goal

- Persist live Codex test run monitor progress on execution-case snapshots.

## Engine / Migration

- MySQL migration: `IntRuoyiBackend/sql/mysql/20260726_system_codex_test_run_monitor_progress.sql`.
- Test schema: `IntRuoyiBackend/yudao-module-system/src/test/resources/sql/create_tables.sql`.

## Schema Changes

- `system_codex_test_execution_case.progress_phase varchar(32) null`
- `system_codex_test_execution_case.current_method_sort int null`
- `system_codex_test_execution_case.current_checkpoint_sort int null`
- `system_codex_test_execution_case.progress_message varchar(512) null`

## Data Safety

- Additive nullable fields only.
- No data deletion, coercion, or backfill.
- Migration is idempotent via `information_schema.COLUMNS` checks.

## Rollback / Recovery

- Safe rollback is dropping the four nullable monitor columns if required; no existing business result data depends on them for final pass/fail evidence.

## BDD Scenarios

- BDD: Progress schema -> Given the runtime schema has Codex execution cases / When migration runs / Then nullable progress columns exist without rewriting historical data.
- BDD: Test schema parity -> Given backend DB tests create execution cases / When service tests run / Then H2 schema includes the same progress columns.

## RED / GREEN

- RED: `python -m pytest IntRuoyiBackend/script/tests/test_codex_test_run_monitor_progress_migration.py -q` failed while SQL script and test schema fields were absent.
- GREEN: same command passed, 2 tests.

## Verification

- Verification: migration metadata, idempotent procedure, four new columns, and H2 test schema parity validated by pytest.

## Blockers

- Blockers: none for additive schema migration.