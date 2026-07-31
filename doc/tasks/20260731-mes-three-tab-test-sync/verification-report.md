# Verification Report

## Result

BLOCKED for the main three-tab replacement. The user-authorized 物料、用户、生产工单 dependency sync has been completed with deterministic ID remapping, but the 工序设置、工艺流程、排产工单 whitelist replacement itself still has unresolved blockers.

## Evidence

- Command: `python -X utf8 doc/tasks/20260731-mes-three-tab-test-sync/tools/sync_authorized_missing_dependencies.py`
- Result: PASS. Generated `artifacts/dependency-remap-plan.json`, `artifacts/dependency-remap-summary.md`, and `artifacts/authorized-dependency-sync-result.json`.
- Command: `python -X utf8 doc/tasks/20260731-mes-three-tab-test-sync/tools/three_tab_sync_preflight.py`
- Result: FAIL by design, because remaining blocker count is `10`.
- Source whitelist scope: `2,989` rows.
- Target current whitelist scope: `1,096` rows.
- Evidence files: `artifacts/preflight-report.json`, `artifacts/preflight-summary.md`.
- Additional authorization: missing `mes_md_item.id=924005`, `system_users.id=910269`, and `33` missing `mes_pro_work_order` IDs may be synced if safe.
- Dependency sync result: `mes_md_item.id=924005` inserted with source ID; `system_users.910269` remapped to `910293`; `18` conflicting production work orders remapped to `925781..925798`; `20` production work orders inserted with source IDs; `2` production work orders already matched in target.

## Blockers

- Schema mismatch: route version snapshot must be `MEDIUMTEXT`; schedule order `promise_date` must be nullable; target batch record report table lacks `form_definition_id/form_version_id`.
- Capacity mismatch: source route snapshots exceed the target `TEXT` limit.
- Missing dependencies still not authorized: form template versions `27/32`, 14 permission scopes.
- Inconsistent dependencies still not authorized: calendar rule `1` and workstation dependency do not match source identity.
- External references: 19 non-whitelist reference groups still point to target records that would be removed or changed by replacement.

## Dependency Sync Confirmation

The authorized dependency rows are now present and identity-checked in test-server `tenant_id=1`: `mes_md_item` missing/mismatch `0`, `system_users` missing/mismatch `0`, and `mes_pro_work_order` missing/mismatch `0` after applying `dependency-remap-plan.json`.

Backup tables created for this dependency write: `mes_three_tab_dep_remap_backup_20260731012048_mes_md_item`, `mes_three_tab_dep_remap_backup_20260731012048_system_users`, and `mes_three_tab_dep_remap_backup_20260731012048_mes_pro_work_order`.

Idempotency check passed: re-running the dependency script loads the existing remap plan, reports `pending_insert_total=0`, and does not allocate new IDs or insert duplicate dependency rows.

The main three-tab replacement was not performed. No whitelist table replacement, schema migration, external reference cleanup, publish, restart, or Playwright validation has been executed yet.

## Required User Decision

Proceeding requires explicit authorization for the remaining blockers: schema migration, form template versions, permission scopes, calendar/workstation mismatch handling, and the 19 non-whitelist active reference groups. Until those are resolved, the three requested pages cannot be safely replaced.
