# Database Schema Evidence

## Scope

- Source tenant: `1 / 芋道源码`.
- Target tenant: `122 / 测试租户`.
- Route-related tables: `mes_pro_route`, `mes_pro_route_version`, `mes_pro_route_process`, `mes_pro_route_process_flow_edge`, `mes_pro_route_process_flow_boundary_edge`, `mes_pro_route_process_flow_layout`, `mes_pro_route_product`, `mes_pro_route_product_bom`, `mes_pro_route_schedule_config`, `mes_pro_route_flow_config`, `mes_pro_route_flow_process_config`, `mes_pro_route_flow_process_batch_record`, `mes_pro_schedule_resource_adjustment`.

## Evidence

- Tenant uniqueness checked via `system_tenant`: `1=芋道源码`, `122=测试租户`.
- Source export workbook row counts are recorded in `artifacts/workbook_sheet_summary.json`: 3 routes, 63 route-process rows, 60 flow edges, 8 boundary rows, 63 layouts, 16 product bindings, 50 BOM rows, 63 schedule config rows, 4 flow config rows, 77 flow-process config rows, 20 process-form binding rows.
- Target delete evidence before this continuation is preserved in `artifacts/route_tenant_122_before_delete.sql`; delete result evidence is preserved in `artifacts/delete_route_tenant_122.tsv`.
- Current post-failed-import counts are recorded in `artifacts/route_related_counts_after_failed_import.tsv`; all listed route-related tables for `tenant_id=122` are `0`.
- Source data blocker is recorded in `artifacts/source_bom_missing_process.tsv`: 46 active source BOM rows have `process_id=0`, causing blank exported process code.

## 2026-07-30 48081 Repair Evidence

- Source key-process backup: `artifacts/source_tenant_1_key_process_before_repair.sql` backs up tenant `1` current route-process rows for routes `900025/900026` and ACTIVE/DRAFT version rows `271/272/160/372`.
- Source key-process repair: `artifacts/repair_source_key_process_flags.sql` updates only tenant `1` routes `ROUTE-XLSX-00001` and `ROUTE-XLSX-00002`; current route-process terminal rows become key, and ACTIVE/DRAFT snapshot terminal nodes are updated with JSON boolean `keyFlag=true`.
- Repair verification: `artifacts/repair_source_key_process_flags_result.tsv` shows exactly one key process in current table and each ACTIVE/DRAFT snapshot for both routes.
- Import verification: `artifacts/import_result_tenant_122_after_key_repair_valid_export.json` shows formal import success into tenant `122` with `routeCount=3`, `routeProcessCount=63`, `routeProductCount=16`, `routeProductBomCount=49`.
- Consistency verification: `artifacts/route_import_consistency_report.json` compares source tenant `1` and target tenant `122` by route/process/item/form business keys and reports all compared sections as matching.
