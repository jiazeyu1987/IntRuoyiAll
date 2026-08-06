# Verification Report

## Result

- Status: implementation verified; closeout commit/push blocked by pre-existing unrelated dirty workspace state.
- Fix: `20260805_erp_nas_table_auto_sync.sql` metadata now uses `dependsOn=20260612_erp_kingdee_sync_runtime; type=schema`.
- Scope: no DDL/DML changes were made beyond the SQL metadata line.

## Evidence

- RED: `python -X utf8 -m pytest script/tests/test_erp_nas_table_auto_sync_sql.py -q` failed before the SQL fix because the first line still included `.sql` dependency suffix and `type=schema,job`.
- RED: `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql --sql-file E:\IntRuoyi\IntRuoyiBackend\sql\mysql\20260805_erp_nas_table_auto_sync.sql --sql-file E:\IntRuoyi\IntRuoyiBackend\sql\mysql\20260612_erp_kingdee_sync_runtime.sql --output ..\doc\tasks\20260806-release-migration-metadata-fix\migration-policy-gate-red.json` failed with `invalid type ... schema,job`.
- GREEN: `python -X utf8 -m pytest script/tests/test_erp_nas_table_auto_sync_sql.py -q` passed with `4 passed`.
- GREEN: `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql --output ..\doc\tasks\20260806-release-migration-metadata-fix\migration-policy-gate.json` passed with `status=passed`, `migrationCount=443`, NAS `type=schema`, NAS `dependsOn=["20260612_erp_kingdee_sync_runtime"]`.
- GREEN: static metadata scans found no remaining `type=*job` or `dependsOn=*.sql` release-migration lines under `IntRuoyiBackend\sql\mysql`.
- GREEN: bug-regression and database-schema evidence validators both passed.

## Remaining Blocker

- The repository already contains many unrelated dirty and untracked changes outside this task. Under the project Git policy, final commit/push closeout requires either a separate baseline decision for those existing changes or a clean task-owned commit path.
