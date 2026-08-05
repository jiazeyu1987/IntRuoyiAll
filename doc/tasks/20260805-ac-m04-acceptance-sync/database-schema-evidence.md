# Database Schema Evidence

## Goal

修复 AC-M04/RRM full real E2E 中 PQC 正式提交前置：生产填写页真实提交进入后端后，当前运行库缺 P0 process-pool event 幂等字段，导致后端查询 `mes_pro_process_pool_event.event_idempotency_key` 报缺列。

## Affected Entities

- `mes_pro_process_pool_event.event_idempotency_key`
- `mes_pro_process_pool_event.recordbook_entry_id`
- `mes_pro_process_pool_pqc_record.production_submit_event_id`
- `mes_pro_process_pool_quantity_fragment.production_submit_event_id`

## Database Engine And Tooling

- Engine: local Docker MySQL, database `ruoyi-vue-pro`.
- Migration under review: `IntRuoyiBackend\sql\mysql\20260803_mes_process_pool_event_idempotency.sql`.
- Repair and verification gates used:
  - `IntRuoyiBackend\script\p0\verify_p0_runtime_migration_apply_preflight.py`
  - `IntRuoyiBackend\script\p0\verify_p0_runtime_backfill_sources.py`
  - `IntRuoyiBackend\script\p0\verify_p0_runtime_backfill_repair_plan.py`
  - `IntRuoyiBackend\script\p0\verify_p0_runtime_backfill_repair_manifest.py`
  - `IntRuoyiBackend\script\p0\verify_p0_runtime_migration.py`

## BDD

BDD: P0 runtime idempotency migration gate -> Given production submit events and PQC records exist in the runtime database, When applying the P0 migration, Then every historical row must have a formal source for idempotency, recordbook entry and submit-root backfill before schema/index migration proceeds.

## RED Evidence

- RED: schema probe -> FAIL because `mes_pro_process_pool_event` lacks `event_idempotency_key` / `recordbook_entry_id`.
- RED: `python -X utf8 IntRuoyiBackend\script\p0\verify_p0_runtime_migration_apply_preflight.py` -> BLOCKED with 88 historical backfill blockers.
- RED: `python -X utf8 IntRuoyiBackend\script\p0\verify_p0_runtime_backfill_sources.py` -> BLOCKED because current structured sources cannot uniquely derive the 88 target backfill rows.
- RED: `python -X utf8 IntRuoyiBackend\script\p0\verify_p0_runtime_backfill_repair_plan.py` -> BLOCKED and read-only; DB writes require authorization, backup, rollback, row manifest and dry-run.

## GREEN Evidence

- GREEN: user authorization -> PASS; user explicitly authorized local-only P0 backfill repair for Docker MySQL `ruoyi-vue-pro`.
- GREEN: backup -> PASS; `db-backup/acm04-p0-backfill-extended-20260805-203724.sql` SHA256 `317BD20FD77F473327B5DAAAEAC5C4A51D474958A9B32A7D652732310C17C8B8`, plus review-signature backup `db-backup/acm04-review-signature-20260805-204459.sql` SHA256 `AEF0616C59C4DD85E9CD851B1855D7B72C68FE84469D984632D0E84DF9E5BBC6`.
- GREEN: `python -X utf8 IntRuoyiBackend\script\p0\verify_p0_runtime_backfill_repair_manifest.py --manifest doc\tasks\20260805-ac-m04-acceptance-sync\db-repair\p0-backfill-repair-manifest.json` -> PASS with `entryCount=88`.
- GREEN: `db-repair/p0-backfill-apply.sql` -> PASS; applied to the authorized local runtime DB, with rollback retained in `db-repair/p0-backfill-rollback.sql`.
- GREEN: `python -X utf8 IntRuoyiBackend\script\p0\verify_p0_runtime_migration_apply_preflight.py` -> PASS with `blockers=[]`.
- GREEN: `python -X utf8 IntRuoyiBackend\script\p0\verify_p0_runtime_backfill_sources.py` -> PASS with `blockers=[]` and all target row counts at 0.
- GREEN: `python -X utf8 IntRuoyiBackend\script\p0\verify_p0_runtime_migration.py` -> PASS; required P0 columns and indexes exist.
- GREEN: local DB post-count probe -> PASS; `repair_events=19`, `repair_entries=21`, `repair_recordbook_events=21`, and missing counts for PQC submit roots, quantity fragment submit roots, event idempotency keys, and recordbook entries are all 0.

## Safety Decision

Initial decision: no DB writes were executed before authorization. The migration/backfill could not be completed from current formal sources, so applying partial DDL, synthetic idempotency keys, default recordbook entries, historical event IDs, or deleting old test rows was blocked by the project no-fallback policy.

Authorization update: the user explicitly authorized "授权修复本机库 P0 backfill". The authorized scope is only the local Docker MySQL runtime database `ruoyi-vue-pro`; remote test, production and backup servers remain out of scope.

Final safety decision: after authorization, the repair used a row-level manifest and signed local business reconstruction metadata instead of silent defaults. The repair did not touch remote servers and did not record database passwords or application secrets.

## Rollback Plan

- Full restore option: restore `db-backup/acm04-p0-backfill-extended-20260805-203724.sql` into the local `ruoyi-vue-pro` database.
- Targeted rollback option: execute `db-repair/p0-backfill-rollback.sql`, then rerun `verify_p0_runtime_migration_apply_preflight.py` and schema probes.

## Verification

- Verification: schema and migration gates were executed against the authorized local Docker MySQL runtime.
- Verification: manifest, apply preflight, source audit, and runtime migration verifier all returned PASS after the repair.
- Verification: no password, token or secret value was written to this evidence file.
- Verification: post-repair missing counts are all 0 for the four P0 historical backfill targets.

## Blockers

No database-schema blocker remains for the local P0 runtime repair. The task remains in progress because RRM `real:check` and full real E2E must still be rerun; schema verifier PASS does not replace true Playwright user-path acceptance.
