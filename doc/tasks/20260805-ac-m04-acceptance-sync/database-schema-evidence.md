# Database Schema Evidence

## Goal

修复 AC-M04/RRM full real E2E 中 PQC 正式提交前置：生产填写页真实提交进入后端后，当前运行库缺 P0 process-pool event 幂等字段，导致后端查询 `mes_pro_process_pool_event.event_idempotency_key` 报缺列。

## Affected Entities

- `mes_pro_process_pool_event.event_idempotency_key`
- `mes_pro_process_pool_event.recordbook_entry_id`
- `mes_pro_process_pool_pqc_record.production_submit_event_id`
- `mes_pro_process_pool_quantity_fragment.production_submit_event_id`
- `mes_pro_process_pool_team_leader_scope` local RRM fixture rows for process `922985` and workstation `980010`
- `mes_pro_process_pool_team_leader_scope` local PQC review scope from leader user `512` to actual employee user `914524`
- `mes_pro_process_pool_team_process_device` local RRM fixture row for process `922985` and device `41`
- `mes_pro_process_pool_team_employee_binding` local RRM fixture row for process `922985` and employee profile `980022`
- `mes_pro_task` local RRM fixture row for work order `980008`, route `922119`, process `922985`, and workstation `980010`
- `system_users` local RRM test-account rows `512, 659, 964, 1301, 1520, 1618, 910272`
- `mes_pqc_process_inspection_aggregate_detail.active_order_id`
- `mes_pqc_process_inspection_aggregate_detail.route_version_id`
- `mes_pqc_process_inspection_aggregate_detail.actual_inspection_quantity`
- `mes_pqc_process_inspection_aggregate_detail` unique key `uk_mes_pqc_process_inspection_aggregate`
- `mes_pqc_process_inspection_aggregate_detail` indexes `idx_mes_pqc_process_inspection_review`, `idx_mes_pqc_process_inspection_task`, `idx_mes_pqc_process_inspection_submit_event`

## Database Engine And Tooling

- Engine: local Docker MySQL, database `ruoyi-vue-pro`.
- Migration under review: `IntRuoyiBackend\sql\mysql\20260803_mes_process_pool_event_idempotency.sql`.
- Runtime closure migration under review: `IntRuoyiBackend\sql\mysql\20260805_mes_pqc_process_inspection_aggregate_runtime_closure.sql`.
- Repair and verification gates used:
  - `IntRuoyiBackend\script\p0\verify_p0_runtime_migration_apply_preflight.py`
  - `IntRuoyiBackend\script\p0\verify_p0_runtime_backfill_sources.py`
  - `IntRuoyiBackend\script\p0\verify_p0_runtime_backfill_repair_plan.py`
  - `IntRuoyiBackend\script\p0\verify_p0_runtime_backfill_repair_manifest.py`
  - `IntRuoyiBackend\script\p0\verify_p0_runtime_migration.py`

## BDD

BDD: P0 runtime idempotency migration gate -> Given production submit events and PQC records exist in the runtime database, When applying the P0 migration, Then every historical row must have a formal source for idempotency, recordbook entry and submit-root backfill before schema/index migration proceeds.

BDD: RRM primary process runtime prerequisites -> Given the primary route process resolves to process `922985`, workstation `980010` and device `41`, When the production employee loads runtime configuration, Then the same production leader must own explicit process/workstation scopes plus process-device and process-employee bindings; adjacent process fixtures must not be used as fallback.

BDD: RRM primary process production task -> Given the PQC source event must be created through a real production fill submission, When the production task page is queried by work order `980008`, route `922119`, and process `922985`, Then exactly one formal tenant-owned task must provide the matching workstation, item, quantity, and active status; an adjacent-process task must not be reused.

BDD: RRM temporary credential restoration -> Given seven local RRM accounts were temporarily enabled and the wrapper did not establish its restore flag after the successful update, When recovering the local database, Then all seven rows must be restored from the exact WHERE-side binlog image for password, updater and update_time in one guarded transaction; guessed hashes and partial restore are forbidden.

BDD: RRM PQC review scope -> Given PQC event `133` is a formal submitted event for actual employee `914524`, When PQC leader `512` loads the scope-filtered submission board, Then the event is visible only through an enabled `PQC + EMPLOYEE` scope from `512` to `914524`; the timeline authority filter must not be removed or bypassed.

BDD: AC-M21 process inspection aggregate runtime closure -> Given AC-M20 may have created `mes_pqc_process_inspection_aggregate_detail` before the full AC-M21 table shape, When the local runtime applies the closure migration, Then the existing table must be repaired with `active_order_id`, non-null `route_version_id`, non-null `actual_inspection_quantity`, standard unique key and query indexes, with missing formal PQC task sources failing fast.

## RED Evidence

- RED: schema probe -> FAIL because `mes_pro_process_pool_event` lacks `event_idempotency_key` / `recordbook_entry_id`.
- RED: `python -X utf8 IntRuoyiBackend\script\p0\verify_p0_runtime_migration_apply_preflight.py` -> BLOCKED with 88 historical backfill blockers.
- RED: `python -X utf8 IntRuoyiBackend\script\p0\verify_p0_runtime_backfill_sources.py` -> BLOCKED because current structured sources cannot uniquely derive the 88 target backfill rows.
- RED: `python -X utf8 IntRuoyiBackend\script\p0\verify_p0_runtime_backfill_repair_plan.py` -> BLOCKED and read-only; DB writes require authorization, backup, rollback, row manifest and dry-run.
- RED: full RRM real E2E -> BLOCKED at production runtime configuration with `frontline runtime deviceId=41`.
- RED: local read-only fixture probe -> expected four primary-process semantic bindings, observed all four counts at 0 while candidate IDs were collision-free.
- RED: first fixture apply -> MySQL `ERROR 1267 Illegal mix of collations`; four task-owned rows persisted because procedure DDL implicitly committed the original transaction. The first result was rejected and must be exactly rolled back before retrying with corrected transaction placement and collation.
- RED: full RRM real E2E after runtime fixture repair -> BLOCKED because the exact production-task page query for work order `980008`, route `922119`, and process `922985` returned zero rows.
- RED: local exact task probe -> expected one primary-process task, observed semantic count `0`; candidate ID `981940` and code `RRM-20260805-PRIMARY-922985` were collision-free.
- RED: local account restore preflight -> expected seven accounts in their pre-wrapper state, observed one shared temporary password value and one shared database-clock update time. Binlog position `8815139` contains the exact seven-row before image required for recovery.
- RED: local PQC review-scope probe -> event `133`, PQC record `90`, and task `93` exist, but leader `512` has responsible employee IDs `{512,659}` while the event actual employee is `914524`; the current board query returns `0`, and the same query with `914524` included returns `1`.
- RED: AC-M21 runtime closure schema test -> `MesQaPqcSchemaTest` failed because `20260805_mes_pqc_process_inspection_aggregate_runtime_closure.sql` did not exist, leaving no migration contract to repair existing aggregate tables.
- RED: full real E2E after PQC approval -> backend aggregation insert failed with `Unknown column 'actual_inspection_quantity' in 'field list'` in `MesPqcProcessInspectionAggregateDetailMapper.insert`, proving the local runtime table was still the older AC-M20 shape.

## GREEN Evidence

- GREEN: user authorization -> PASS; user explicitly authorized local-only P0 backfill repair for Docker MySQL `ruoyi-vue-pro`.
- GREEN: backup -> PASS; `db-backup/acm04-p0-backfill-extended-20260805-203724.sql` SHA256 `317BD20FD77F473327B5DAAAEAC5C4A51D474958A9B32A7D652732310C17C8B8`, plus review-signature backup `db-backup/acm04-review-signature-20260805-204459.sql` SHA256 `AEF0616C59C4DD85E9CD851B1855D7B72C68FE84469D984632D0E84DF9E5BBC6`.
- GREEN: `python -X utf8 IntRuoyiBackend\script\p0\verify_p0_runtime_backfill_repair_manifest.py --manifest doc\tasks\20260805-ac-m04-acceptance-sync\db-repair\p0-backfill-repair-manifest.json` -> PASS with `entryCount=88`.
- GREEN: `db-repair/p0-backfill-apply.sql` -> PASS; applied to the authorized local runtime DB, with rollback retained in `db-repair/p0-backfill-rollback.sql`.
- GREEN: `python -X utf8 IntRuoyiBackend\script\p0\verify_p0_runtime_migration_apply_preflight.py` -> PASS with `blockers=[]`.
- GREEN: `python -X utf8 IntRuoyiBackend\script\p0\verify_p0_runtime_backfill_sources.py` -> PASS with `blockers=[]` and all target row counts at 0.
- GREEN: `python -X utf8 IntRuoyiBackend\script\p0\verify_p0_runtime_migration.py` -> PASS; required P0 columns and indexes exist.
- GREEN: local DB post-count probe -> PASS; `repair_events=19`, `repair_entries=21`, `repair_recordbook_events=21`, and missing counts for PQC submit roots, quantity fragment submit roots, event idempotency keys, and recordbook entries are all 0.
- GREEN: local RRM account restoration -> PASS; seven rows were restored from the exact before image at `binlog.000128` position `8815139`, the guarded transaction reported 7 restored rows, and the temporary credential value remained on 0 target rows. The database-clock timestamp `2026-08-06 03:36:18` is future-skewed relative to the current date `2026-08-05`.
- GREEN: credential artifact cleanup -> PASS; the temporary decoded-binlog carrier was deleted after verification and no password hash was written to project evidence.
- GREEN: RRM primary-process fixture repair -> PASS; backup、manifest、transactional apply、exact post-count verification 均已完成，full real E2E 继续到后续 PQC 阶段。
- GREEN: RRM primary-process production task -> PASS; task `981940` 已按精确备份、manifest、transactional apply 和 post-count 验证写入，full real E2E 已使用该正式任务创建生产 source event。
- GREEN: RRM PQC review scope -> PASS; backup `db-backup/acm04-rrm-pqc-review-scope-20260805.sql` SHA256 `21A4C7D7E4D16ADEC838A2202BBE7A4C4CE8F4C0105EF5BF7948C44AFEC74BFA`，apply/rollback/manifest 齐备，后置验证 `SCOPE_ROW=1`、`VISIBLE_EVENT=1`、残留存储过程 `0`。
- GREEN: AC-M21 schema regression -> PASS; `mvn -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -am "-Dtest=MesQaPqcSchemaTest,MesProcessPoolSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` passed 7 schema tests after adding the closure migration.
- GREEN: AC-M21 release migration policy gate -> PASS; `ac-m21-runtime-closure-policy-gate.json` reports `status=passed` and `migrationCount=17`.
- GREEN: AC-M21 local backup -> PASS; `db-backup/acm04-ac-m21-aggregate-runtime-closure-20260806.sql` SHA256 `8D9DD18114ED4BD603EA94CB504D76B3954660E6645C12FF8BE86F37342BF674`.
- GREEN: AC-M21 local apply and post-verify -> PASS; the local runtime table now has `active_order_id`, `route_version_id NOT NULL`, `actual_inspection_quantity NOT NULL`, missing required source count `0`, residual procedure count `0`, and all required aggregate indexes.

## Safety Decision

Initial decision: no DB writes were executed before authorization. The migration/backfill could not be completed from current formal sources, so applying partial DDL, synthetic idempotency keys, default recordbook entries, historical event IDs, or deleting old test rows was blocked by the project no-fallback policy.

Authorization update: the user explicitly authorized "授权修复本机库 P0 backfill". The authorized scope is only the local Docker MySQL runtime database `ruoyi-vue-pro`; remote test, production and backup servers remain out of scope.

Final safety decision: after authorization, the repair used a row-level manifest and signed local business reconstruction metadata instead of silent defaults. The repair did not touch remote servers and did not record database passwords or application secrets.

## Rollback Plan

- Full restore option: restore `db-backup/acm04-p0-backfill-extended-20260805-203724.sql` into the local `ruoyi-vue-pro` database.
- Targeted rollback option: execute `db-repair/p0-backfill-rollback.sql`, then rerun `verify_p0_runtime_migration_apply_preflight.py` and schema probes.
- RRM primary-process fixture rollback: execute `db-repair/rrm-primary-process-runtime-prereq-rollback.sql`; it may delete only the four exact task-owned rows whose creator/updater/remark and business keys still match the manifest.
- RRM primary-process task rollback: execute `db-repair/rrm-primary-process-task-rollback.sql`; it may delete only task `981940` when every task-owned field still matches and no downstream feedback, issue, or schedule-extension dependency exists.
- RRM account recovery source: local `binlog.000128` position `8815139`; restore only the seven authorized test-account rows and verify no temporary credential value remains.
- RRM PQC review scope rollback: execute `db-repair/rrm-pqc-review-scope-rollback.sql`; it may delete only scope `980041` when leader, employee, creator, updater, remark, enabled, tenant and deleted fields still match the manifest.
- AC-M21 aggregate runtime closure rollback: restore `db-backup/acm04-ac-m21-aggregate-runtime-closure-20260806.sql` for a full local runtime restore, or run the guarded targeted rollback in `db-repair/acm21-aggregate-runtime-closure-rollback.sql` and rerun the aggregate schema post-verify.

## Verification

- Verification: schema and migration gates were executed against the authorized local Docker MySQL runtime.
- Verification: manifest, apply preflight, source audit, and runtime migration verifier all returned PASS after the repair.
- Verification: no password, token or secret value was written to this evidence file.
- Verification: post-repair missing counts are all 0 for the four P0 historical backfill targets.

## Blockers

No database-schema blocker remains for the local P0 runtime repair. The task remains in progress because RRM `real:check` and full real E2E must still be rerun; schema verifier PASS does not replace true Playwright user-path acceptance.

No database-schema blocker remains for the AC-M21 local aggregate runtime closure. The remaining requirement is real Playwright acceptance proving PQC leader approval creates aggregate detail and exposes the read-only aggregation evidence.
