# Verification Report

## Summary

Blocked. The test tenant route delete had already completed and was re-verified as empty. A fresh source workbook was exported from tenant `1 / 芋道源码`, but the formal import into tenant `122 / 测试租户` failed before any rows were written because the exported `工序BOM` sheet contains blank `工序编码` values.

## Commands

- `Invoke-RestMethod http://127.0.0.1:48081/actuator/health` -> `UP`.
- DB tenant check -> `1=芋道源码`, `122=测试租户`.
- Source export -> `GET /admin-api/mes/pro/route/export-import-xlsx`, `tenant-id=1`, output `artifacts/source_tenant_1_route_export_latest.xlsx`.
- Target import attempt via `tenant-id=1 + visit-tenant-id=122` -> `code=403`, `msg=没有该操作权限`.
- Target import attempt via `tenant-id=122` as test E2E account -> `code=1040501417`, `msg=工艺路线导入导出 Excel 的 工序BOM 第 2 行 工序编码 不能为空`.
- Workbook inspection -> `artifacts/workbook_sheet_summary.json`.
- Source BOM blocker query -> `artifacts/source_bom_missing_process.tsv`.
- Post-failed-import count query -> `artifacts/route_related_counts_after_failed_import.tsv`.

## Result

Import/export consistency cannot be proven because the import did not succeed. The source workbook is internally inconsistent with the import contract: 46 active source BOM rows have `process_id=0`, so export writes blank process codes while import requires `工序编码`. The target test tenant remains empty across the route-related tables after the failed import; there was no partial import.

## Next Decision Required

- Option 1: Repair source tenant BOM rows by assigning formal route process bindings, re-export, and re-import.
- Option 2: Restore the test tenant from `artifacts/route_tenant_122_before_delete.sql` before any further retry.
