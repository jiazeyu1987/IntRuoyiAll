# Database Schema Evidence

## Data Change Goal And Affected Entities

- Goal: Correct release migration metadata for NAS table auto sync SQL without changing the intended schema or seeded scheduler job behavior.
- Affected migration: `IntRuoyiBackend/sql/mysql/20260805_erp_nas_table_auto_sync.sql`.
- Affected entities: `erp_nas_table_sync_plan`, `erp_nas_table_sync_plan_item`, `erp_nas_table_sync_run`, `erp_nas_table_sync_run_item`, and seeded `infra_job` handler `erpNasTableAutoSyncJob`.

## Database Engine And Migration Tool

- Engine: MySQL SQL migration under `IntRuoyiBackend/sql/mysql`.
- Migration gate: `IntRuoyiBackend/script/release/run-release-migration-policy-gate.py`.

## Schema Or Migration Changes

- Only the `release-migration` metadata line changed:
- Before: `dependsOn=20260612_erp_kingdee_sync_runtime.sql; type=schema,job`
- After: `dependsOn=20260612_erp_kingdee_sync_runtime; type=schema`
- The DDL for `erp_nas_table_sync_*` tables and DML for `infra_job` were not changed.

## Data Safety Analysis

- This fix targets metadata only; table DDL and `infra_job` DML are expected to remain unchanged.

## Rollback Or Recovery Plan

- Revert the single SQL metadata line and focused test change if verification reveals a contract mismatch.

## BDD Scenarios

- BDD: NAS table auto sync release metadata -> Given the NAS sync SQL includes schema DDL and a scheduler job seed, When release metadata is validated, Then `type` is a single allowed enum and the migration policy gate passes.

## RED:

- `python -X utf8 -m pytest script/tests/test_erp_nas_table_auto_sync_sql.py -q` -> FAIL, expected reason: the first-line metadata did not match the allowed single-type contract.
- `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql --sql-file E:\IntRuoyi\IntRuoyiBackend\sql\mysql\20260805_erp_nas_table_auto_sync.sql --sql-file E:\IntRuoyi\IntRuoyiBackend\sql\mysql\20260612_erp_kingdee_sync_runtime.sql --output ..\doc\tasks\20260806-release-migration-metadata-fix\migration-policy-gate-red.json` -> FAIL, expected reason: `invalid type ... schema,job`.

## GREEN:

- `python -X utf8 -m pytest script/tests/test_erp_nas_table_auto_sync_sql.py -q` -> PASS, `4 passed`.
- `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql --output ..\doc\tasks\20260806-release-migration-metadata-fix\migration-policy-gate.json` -> PASS, `status=passed`, `migrationCount=443`.

## Migration Verification

- Full migration policy gate passed. NAS entry resolved as `migrationId=20260805_erp_nas_table_auto_sync`, `type=schema`, `dependsOn=["20260612_erp_kingdee_sync_runtime"]`, `riskLevel=medium`.
- Static scans found no remaining `release-migration` metadata lines with `type=*job` or `dependsOn=*.sql`.

## Blockers

- Current workspace has unrelated dirty state that blocks safe final commit/push closeout without a separate user-authorized baseline or cleanup decision.
