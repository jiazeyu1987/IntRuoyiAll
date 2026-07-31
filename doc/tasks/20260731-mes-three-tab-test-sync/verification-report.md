# Verification Report

## Result

BLOCKED. The requested three-tab sync was not executed because read-only preflight found hard blockers that would violate the user requirement to sync only 工序设置、工艺流程、排产工单 and leave all other data untouched.

## Evidence

- Command: `python -X utf8 doc/tasks/20260731-mes-three-tab-test-sync/tools/three_tab_sync_preflight.py`
- Result: FAIL by design, because blocker count is `13`.
- Source whitelist scope: `2,989` rows.
- Target current whitelist scope: `1,096` rows.
- Evidence files: `artifacts/preflight-report.json`, `artifacts/preflight-summary.md`.

## Blockers

- Schema mismatch: route version snapshot must be `MEDIUMTEXT`; schedule order `promise_date` must be nullable; target batch record report table lacks `form_definition_id/form_version_id`.
- Capacity mismatch: source route snapshots exceed the target `TEXT` limit.
- Missing dependencies: form template versions `27/32`, 14 permission scopes, item `924005`, user `910269`.
- Inconsistent dependencies: production work orders, calendar rule `1`, and workstation dependency do not match source identity.
- External references: 19 non-whitelist reference groups still point to target records that would be removed or changed by replacement.

## Zero Write Confirmation

No test-server write was performed. The run only queried local Docker MySQL and the test-server MySQL through SSH; it did not run data replacement, backup restore, publish, restart, or E2E write paths.
