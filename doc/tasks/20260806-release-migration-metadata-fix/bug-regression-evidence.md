# Bug Regression Evidence

## Bug Summary And Expected Behavior

- Bug: `20260805_erp_nas_table_auto_sync.sql` declares `type=schema,job`, which violates the release migration metadata contract and blocks test-server release before build.
- Expected: Release metadata uses one allowed `type` enum while SQL still creates NAS sync tables and seeds the disabled scheduler job.

## Reproduction

- `python -X utf8 -m pytest script/tests/test_erp_nas_table_auto_sync_sql.py -q` failed before the fix because the NAS SQL first line still declared `dependsOn=20260612_erp_kingdee_sync_runtime.sql; type=schema,job`.
- `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql --sql-file E:\IntRuoyi\IntRuoyiBackend\sql\mysql\20260805_erp_nas_table_auto_sync.sql --sql-file E:\IntRuoyi\IntRuoyiBackend\sql\mysql\20260612_erp_kingdee_sync_runtime.sql --output ..\doc\tasks\20260806-release-migration-metadata-fix\migration-policy-gate-red.json` failed with `invalid type ... schema,job`.

## Root Cause

- The migration metadata used a descriptive composite value, `type=schema,job`, and a `.sql` suffix in `dependsOn`. The release manifest contract accepts exactly one `type` enum and dependency migration IDs without `.sql` suffixes.

## Regression Test

- `script/tests/test_erp_nas_table_auto_sync_sql.py` now asserts the exact first line metadata and separately verifies the scheduler job seed remains present.

## RED:

- `python -X utf8 -m pytest script/tests/test_erp_nas_table_auto_sync_sql.py -q` -> FAIL, expected reason: `nc_runtime.sql; type=schema,job` did not match the required first-line metadata.
- `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql --sql-file E:\IntRuoyi\IntRuoyiBackend\sql\mysql\20260805_erp_nas_table_auto_sync.sql --sql-file E:\IntRuoyi\IntRuoyiBackend\sql\mysql\20260612_erp_kingdee_sync_runtime.sql --output ..\doc\tasks\20260806-release-migration-metadata-fix\migration-policy-gate-red.json` -> FAIL, expected reason: `invalid type ... schema,job`.

## GREEN:

- `python -X utf8 -m pytest script/tests/test_erp_nas_table_auto_sync_sql.py -q` -> PASS, `4 passed`.
- `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql --output ..\doc\tasks\20260806-release-migration-metadata-fix\migration-policy-gate.json` -> PASS, `status=passed`, `migrationCount=443`.

## Risk And Regression Scope

- Scope is limited to one SQL metadata line and its focused static contract test; no runtime fallback or remote database operation is planned.

## Verification

- Target pytest and full migration policy gate both pass after the fix.
- Static metadata scans found no remaining `type=*job` or `dependsOn=*.sql` release-migration lines under `IntRuoyiBackend\sql\mysql`.

## Blockers And Follow-Up Actions

- Current workspace has unrelated dirty state that blocks safe final commit/push closeout without a separate user-authorized baseline or cleanup decision.
