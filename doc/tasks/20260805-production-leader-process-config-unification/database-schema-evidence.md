# P1 Database Schema Evidence

- Task ID: `20260805-production-leader-process-config-unification`
- Scope: PRD P1 only
- Date: `2026-08-05`
- Overall verdict: `PASS`.

## Data Change Goal

The affected entity is `mes_pro_process_pool_device_parameter_rule`.

P1 makes every parameter rule formally belong to a route process and requires a stored target value. The migration must reject incomplete history rather than guessing a route process or target value.

- Database engine: MySQL 8.0.
- Migration mechanism: versioned SQL under `IntRuoyiBackend/sql/mysql/`.
- Policy tool: `IntRuoyiBackend/script/release/run-release-migration-policy-gate.py`.
- Test strategy: Python migration contract plus MES JUnit schema contract.

## Migration

Added `IntRuoyiBackend/sql/mysql/20260805_mes_process_pool_device_parameter_route_process_constraints.sql`.

- Complete release metadata:
  - `allowedEnvironments=test,backup,prod`
  - `dependsOn=20260731_mes_process_pool_team_leader_p1_runtime_config`
  - `type=schema`
  - `riskLevel=medium`
- The dependency is the real migration that introduced `default_value` on the target table.
- The preflight checks the table, required columns, every historical row, the legacy unique-index definition, duplicate rows under the new identity, and unexpected pre-existing new-index state.
- The final single `ALTER TABLE` changes:
  - `route_process_id` to `bigint NOT NULL`
  - `default_value` to `decimal(24,6) NOT NULL COMMENT '目标值'`
  - drops `uk_mes_pp_device_parameter_rule`
  - adds `uk_mes_pp_device_parameter_route_process (tenant_id, route_process_id, device_id, parameter_code, deleted)`

## Safety

- The NULL preflight is table-wide: `route_process_id IS NULL OR default_value IS NULL`.
- The NULL preflight has no `deleted=0` or equivalent active-row filter, so deleted history also blocks the migration.
- Any NULL history raises `SIGNAL SQLSTATE '45000'` with an instruction to complete formal data governance first.
- Duplicate target identities raise `SIGNAL SQLSTATE '45000'` before the unique index is changed.
- The migration contains no `UPDATE` or `INSERT INTO` for the target table.
- There is no default route, first-route selection, `process_id`-based route inference, `COALESCE`, `IFNULL`, or default target-value backfill.
- The executor did not apply this migration to a real database or alter any environment data.

## Rollback

- If a preflight `SIGNAL` fires, the target table schema and data remain unchanged. The helper procedure may remain; run `DROP PROCEDURE IF EXISTS preflight_mes_pp_device_parameter_route_process_constraints` before retry. The migration also performs that cleanup at the start of every retry.
- The two `NOT NULL` changes and index replacement are performed in one MySQL `ALTER TABLE` statement.
- Before rollback, verify there are no duplicates under the legacy identity `tenant_id + process_id + device_id + parameter_code + deleted`.
- While only P1 exists and no P2-P4 route-scoped writes have occurred, rollback can drop `uk_mes_pp_device_parameter_route_process`, restore `uk_mes_pp_device_parameter_rule`, and make `route_process_id/default_value` nullable together with the prior application contract.
- If legacy-key duplicates exist after later route-scoped writes, rollback is blocked until formal data governance or backup restoration is approved.

## BDD Scenarios

- BDD: Historical NULL parameter rule blocks migration -> Given any historical row has a NULL route process or target value regardless of deleted state / When the P1 migration runs / Then it raises SQLSTATE 45000 and requires formal data governance.
- BDD: Valid history receives strict route-process constraints -> Given all historical route process and target values are present / When the P1 migration runs / Then both columns become NOT NULL and the route-process unique identity replaces the legacy process-only identity.
- BDD: Migration never guesses missing business facts -> Given incomplete history has no confirmed formal source / When the migration is reviewed or executed / Then it performs no data backfill and passes the target migration metadata and schema contracts.

## RED Evidence

- RED: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_process_pool_device_parameter_route_process_migration.py -q` -> FAIL, `4 failed in 0.22s`; expected reason: `20260805_mes_process_pool_device_parameter_route_process_constraints.sql` did not yet exist.
- Diagnostic only: the first Maven attempt timed out after 240 seconds without a surefire report. The task-owned Maven Java PID was verified and stopped; this timeout was not treated as RED success.

## GREEN Evidence

- GREEN: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_process_pool_device_parameter_route_process_migration.py -q` -> PASS, `4 passed in 0.14s`.
- GREEN: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`.
- GREEN: `python -X utf8 IntRuoyiBackend\script\release\run-release-migration-policy-gate.py --sql-root IntRuoyiBackend\sql\mysql --sql-file IntRuoyiBackend\sql\mysql\20260630_mes_pro_work_order_erp_snapshot_fields.sql --sql-file IntRuoyiBackend\sql\mysql\20260730_mes_process_pool_fifo_allocation.sql --sql-file IntRuoyiBackend\sql\mysql\20260730_mes_process_pool_review_copy.sql --sql-file IntRuoyiBackend\sql\mysql\20260730_mes_process_pool_team_leader.sql --sql-file IntRuoyiBackend\sql\mysql\20260731_mes_process_pool_team_leader_p1_runtime_config.sql --sql-file IntRuoyiBackend\sql\mysql\20260805_mes_process_pool_device_parameter_route_process_constraints.sql --output C:\Users\BJB110\AppData\Local\Temp\20260805-production-leader-process-config-unification-target-migration-policy-gate.json` -> PASS, `status=passed`, `migrationCount=6`.
- The target migration policy output records SHA-256 `7993f089e0377002c829bfcf56d9b8c6f3c0bed8caa634ebaa3576744394c9ce`.
- RED: `python -X utf8 -m pytest script\tests\test_release_migration_metadata.py -q` from `IntRuoyiBackend` -> FAIL, `2 failed, 1 passed`; expected reason: existing `20260805_erp_nas_table_auto_sync.sql` used `dependsOn=...sql` and `type=schema,job`.
- GREEN: `python -X utf8 -m pytest script\tests\test_release_migration_metadata.py -q` from `IntRuoyiBackend` -> PASS, `3 passed in 0.31s`.
- GREEN: `python -X utf8 IntRuoyiBackend\script\release\run-release-migration-policy-gate.py --sql-root IntRuoyiBackend\sql\mysql --output doc\tasks\20260805-production-leader-process-config-unification\migration-policy-gate.json` -> PASS, `status=passed`, `migrationCount=440`.

## Migration Verification

| Acceptance ID | Result | Evidence |
| --- | --- | --- |
| P1-AC1 | PASS | Python and JUnit contracts require the table-wide NULL query, forbid a deleted-only filter, and require `SIGNAL SQLSTATE '45000'` plus formal data-governance messaging. |
| P1-AC2 | PASS | Migration and schema contracts require both `NOT NULL` changes, removal of `uk_mes_pp_device_parameter_rule`, and the exact route-process unique index. |
| P1-AC3 | PASS | No-backfill, real dependency metadata, target dependency-chain policy gate, schema contracts, and the full repository migration policy gate all pass after the authorized metadata repair. |

Validator self-test:

- `python -X utf8 C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --self-test` -> PASS, `Database schema validator self-test passed.`
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc\tasks\20260805-production-leader-process-config-unification\database-schema-evidence.md` -> PASS, `Database schema evidence is valid.`

## Blockers

- None for P1.

## Authorized Non-MES Metadata Repair

- User authorization: after the P1 full-gate blocker was reported, the user replied `继续`.
- Scope: `IntRuoyiBackend/sql/mysql/20260805_erp_nas_table_auto_sync.sql` release metadata only.
- Repair: changed `dependsOn=20260612_erp_kingdee_sync_runtime.sql; type=schema,job` to `dependsOn=20260612_erp_kingdee_sync_runtime; type=schema`.
- Contract: added `test_erp_nas_table_auto_sync_has_single_schema_migration_type` and retained the existing all-SQL dependency-suffix scan.
