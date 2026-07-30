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
