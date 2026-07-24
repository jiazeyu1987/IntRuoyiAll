# Execution Log - 20260715 release metadata personal-file fix

## BDD Scenarios

BDD: personal-file migration metadata -> Given the personal-file decommission SQL is included in a code-only release, When the release migration policy gate scans SQL files, Then the migration must expose explicit allowed environments, dependency, type, and risk metadata.

## TDD Evidence

- RED: `python -X utf8 -m pytest script/tests/test_dcc_personal_file_decommission.py::test_dcc_personal_file_decommission_migration_has_release_metadata -q` -> FAIL, expected reason: SQL first line was `-- Decommission...` instead of required `-- release-migration: ...`.
- RED: `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql --output doc/tasks/20260715-release-metadata-personal-file-fix/migration-policy-gate.json` -> FAIL, expected reason after first fix: existing 20260714 migrations used `.sql` suffixes inside `dependsOn`.
- RED: `python -X utf8 -m pytest script/tests/test_release_migration_metadata.py::test_release_migration_depends_on_uses_migration_ids_without_sql_suffix -q` -> FAIL, expected reason: five `dependsOn` entries ended with `.sql`.
- GREEN: `python -X utf8 -m pytest script/tests/test_dcc_personal_file_decommission.py script/tests/test_release_migration_metadata.py -q` -> PASS, `6 passed`.
- GREEN: `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql --output doc/tasks/20260715-release-metadata-personal-file-fix/migration-policy-gate.json` -> PASS, `status=passed`, `migrationCount=297`.

## Commands And Evidence

- BLOCKER source: maintenance release `build-release` r1 failed with `Release migration metadata missing: ...\sql\mysql\20260714_dcc_personal_file_decommission.sql`.
- Changed SQL metadata:
  - `sql/mysql/20260714_dcc_personal_file_decommission.sql`: added `allowedEnvironments=test,backup,prod; dependsOn=20260513_dcc_base_schema; type=menu; riskLevel=low`.
  - `sql/mysql/20260714_bpm_category_seed_fix.sql`: removed `.sql` suffix from `dependsOn=20260512_bpm_base_schema`.
  - `sql/mysql/20260714_dcc_controlled_file_logs_consolidation.sql`: removed `.sql` suffixes from two dependencies.
  - `sql/mysql/20260714_mes_batch_record_version_approval_bpm_seed.sql` and `sql/mysql/20260714_mes_edhr_batch_execution_void_bpm_seed.sql`: removed `.sql` suffix from `dependsOn=20260714_bpm_category_seed_fix`.
