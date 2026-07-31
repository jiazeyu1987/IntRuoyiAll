# Verification Report

## Result

PASS for the authorized test-server MES three-tab sync. The 工序设置列表、工艺流程、排产工单 whitelist data and user-authorized dependencies are now synchronized from local `tenant_id=1 / 芋道源码` to test server `172.30.30.58 / tenant_id=1`.

## Data Sync Evidence

- Authorized dependency sync: `mes_md_item.id=924005` inserted with source ID; `system_users.910269 -> 910293`; 18 conflicting production work orders remapped to `925781..925798`, 20 inserted with source IDs, and 2 already matched.
- Remaining blocker sync: form template versions `27/32`, 14 permission scopes, calendar rule `1`, and workstation `900131 -> 922057` resolved with backup evidence.
- External reference cleanup: 19 active non-whitelist reference groups were backed up and soft-deleted; final active references = 0.
- Main whitelist sync: `sync_three_tab_whitelist.py` replaced 20 whitelist tables and inserted `2,989` source rows; each table postcheck has matching row count and hash.
- Batch record report dependency sync: page validation exposed 14 missing `mes_pro_batch_record_report` rows plus definition `47` and version `130`; `sync_missing_batch_record_reports.py` inserted the minimal required metadata and postcheck missing count = 0.

## Final Verification

- Command: `python -X utf8 doc/tasks/20260731-mes-three-tab-test-sync/tools/three_tab_sync_preflight.py`
- Result: PASS, `blocker_count=0`, source whitelist rows `2,989`, target whitelist rows `2,989`.
- Command: `node doc/tasks/20260731-mes-three-tab-test-sync/tools/verify_test_server_three_tabs.mjs --timeout 90000`
- Result: PASS against `http://172.30.30.58:8081/`, tenant/user label `芋道源码/admin`.
- Real page totals: 工序设置 `65`, 工艺流程 `3`, 排产工单 `10`.
- Evidence files: `artifacts/preflight-summary.md`, `artifacts/three-tab-whitelist-sync-summary.md`, `artifacts/batch-record-report-dependency-sync-summary.md`, `artifacts/test-server-e2e/result.json`.

## Backup Evidence

- Authorized dependency backups: `mes_three_tab_dep_remap_backup_20260731012048_*`.
- Remaining blocker backups: `m3rembk_20260731021918_*`.
- External reference backups: `m3extbk_20260731023513_*`.
- Main whitelist backups: `m3syncbk_20260731025926_*`.
- Batch record report metadata backups: `m3brepbk_20260731115458_def`, `m3brepbk_20260731115458_ver`, `m3brepbk_20260731115458_report`.

## Closeout Status

Implementation, required verification, experience consolidation, and cleanup preview/apply are complete. Task status is `completed`; Git commit/push status is handled by repository policy and current branch state.
