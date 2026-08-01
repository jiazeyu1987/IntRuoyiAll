# Verification Report

## Summary

- Fixed the test-server schedule-calendar blocker for tenant `芋道源码` / user `zhaojie`.
- Pre-write backup passed: `/opt/intruoyi/runtime/task-backups/20260801-fix-test-schedule-material-item-mapping/pml_related_20260801020129.sql.gz`, SHA256 `d0003f77ee2aa048068c1e571cd20f6d3654927fdaf5d11c2856a937e36d0cee`.
- Data repair changed only tenant `1` white-listed tables: `mes_kingdee_production_material_list` and two missing `mes_md_item` rows.

## Data Verification

- `881MO093613` and follow-on `881MO093617` PML child mappings are resolved by same-tenant item code.
- Tenant-1 unique-resolvable null child mappings are now `0`; unresolved rows without same-tenant item remain untouched.
- `CODEX-FACTOR-20260708093210` PML now links to scheduled work order `925877`, row count `29`, null child count `0`.
- Tenant-1 PML orphan child reference count is `0`.

## API Verification

- GREEN: `GET /admin-api/mes/pro/schedule-calendar/month?month=2026-08` returned HTTP `200`, business code `0`, data present, `dayCount=31`.
- GREEN: `/erp/production-material-list/detail-list?sourceBillNo=SIM-881MO093613` returned business code `0`, count `27`.
- GREEN: `/erp/production-material-list/detail-list?sourceBillNo=PPBOM00309004` returned business code `0`, count `29`.

## Page Verification

- Real login page reached the slider captcha and was blocked at `请完成安全验证 / 向右滑动完成验证`.
- Token-bootstrap post-login rendering passed for `http://172.30.30.58:8081/mes/pro/schedule-calendar`: month API code `0`, title visible, no mapping/PML/system-error text, console/page error counts `0`.

## Validator Results

- PASS: bug regression evidence validator -> `Bug regression evidence is valid.`
- PASS: database schema evidence validator -> `Database schema evidence is valid.`
- PASS: backup disaster recovery evidence validator -> `Backup disaster recovery evidence is valid.`

## Cleanup

- PASS: cleanup preview kept `task.md`, `execution-log.md`, and `verification-report.md`, with no blocked paths or warnings.
- PASS: cleanup apply deleted only task-local temporary evidence files after validator PASS summaries were copied into retained reports.

## Blockers

- Real-login Playwright E2E remains blocked by slider captcha; token-bootstrap is recorded separately and does not claim real-login success.
- Git closeout remains blocked because `int_main` is ahead of `origin/int_main` by `10` commits and the worktree contains unrelated dirty changes; no task commit or push was made to avoid mixing unrelated work.
