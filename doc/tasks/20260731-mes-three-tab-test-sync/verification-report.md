# Verification Report

## Result

BLOCKED. The requested three-tab sync was not executed because preflight found hard blockers. The user later authorized syncing missing 物料、用户、生产工单 dependencies, but that dependency sync is also blocked by target global primary-key conflicts and remaining unapproved blockers.

## Evidence

- Command: `python -X utf8 doc/tasks/20260731-mes-three-tab-test-sync/tools/three_tab_sync_preflight.py`
- Result: FAIL by design, because blocker count is `13`.
- Source whitelist scope: `2,989` rows.
- Target current whitelist scope: `1,096` rows.
- Evidence files: `artifacts/preflight-report.json`, `artifacts/preflight-summary.md`.
- Additional authorization: missing `mes_md_item.id=924005`, `system_users.id=910269`, and `33` missing `mes_pro_work_order` IDs may be synced if safe.
- Dependency sync attempt: failed before data insertion could be verified because target primary keys are globally occupied by other tenants.

## Blockers

- Schema mismatch: route version snapshot must be `MEDIUMTEXT`; schedule order `promise_date` must be nullable; target batch record report table lacks `form_definition_id/form_version_id`.
- Capacity mismatch: source route snapshots exceed the target `TEXT` limit.
- Missing dependencies still not authorized: form template versions `27/32`, 14 permission scopes.
- Authorized dependency blocker: `system_users.id=910269` already exists under `tenant_id=122`; `13` authorized missing production work order IDs already exist under `tenant_id=122/162`. Direct insert into `tenant_id=1` would violate primary keys.
- Inconsistent dependencies: 5 existing production work orders in `tenant_id=1`, calendar rule `1`, and workstation dependency do not match source identity.
- External references: 19 non-whitelist reference groups still point to target records that would be removed or changed by replacement.

## Zero Write Confirmation

The three-tab replacement was not performed. Read-only postcheck confirms the authorized dependency rows are still absent from test-server `tenant_id=1`: `mes_md_item=0`, `system_users=0`, `mes_pro_work_order=0`.

The failed authorized dependency attempt left only empty backup tables as evidence: `mes_three_tab_dep_backup_20260731010102_mes_md_item`, `mes_three_tab_dep_backup_20260731010102_mes_pro_work_order`, and `mes_three_tab_dep_backup_20260731010102_system_users` each contain `0` rows.

## Required User Decision

Proceeding requires an explicit decision because the safe direct sync path is blocked. Either authorize deterministic ID remapping for the conflicting user/work orders and all three-tab references, authorize cleanup/overwrite of conflicting rows in other tenants, or leave this task blocked until schema/dependency/reference blockers are resolved.
