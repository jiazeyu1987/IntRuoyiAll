# Verification Report

## Summary

Completed on local formal backend `48081`. The source tenant `1 / 芋道源码` route export was repaired at the source-data level, re-exported, imported into target tenant `122 / 测试租户`, and semantically compared. All compared business sections match.

## Root Cause

- Initial export/import failures were caused by source and target data integrity blockers, not by the final comparison logic.
- The final blocker was source routes `ROUTE-XLSX-00001` and `ROUTE-XLSX-00002` being enabled but missing a key process in current route-process data and ACTIVE/DRAFT relationship-graph snapshots.
- Existing product behavior requires enabled routes to have one key process. Frontend defaulting and Markdown import rules both support the terminal/final process as the default key-process choice when none exists.

## Repair

- Backed up source rows: `artifacts/source_tenant_1_key_process_before_repair.sql`.
- Repaired source key-process flags: `artifacts/repair_source_key_process_flags.sql`.
- Verified repair: `artifacts/repair_source_key_process_flags_result.tsv`.
- Re-exported source workbook: `artifacts/source_tenant_1_route_export_key_repaired.xlsx` (`26933` bytes).
- Imported into target tenant: `artifacts/import_result_tenant_122_after_key_repair_valid_export.json`, response `code=0`.

## Import Result

- Routes: `3`.
- Route processes: `63`.
- Route products: `16`.
- Route product BOM rows: `49`.
- Imported route codes: `RT000028`, `ROUTE-XLSX-00002`, `ROUTE-XLSX-00001`.

## Consistency Result

- `route_basic`: match, `3 / 3`.
- `route_processes`: match, `63 / 63`.
- `flow_edges`: match, `60 / 60`.
- `boundary_edges`: match, `8 / 8`.
- `layouts`: match, `63 / 63`.
- `products`: match, `16 / 16`.
- `product_boms`: match, `49 / 49`.
- `schedule_configs`: match, `63 / 63`.
- `flow_configs`: match, `4 / 4`.
- `flow_process_configs`: match, `77 / 77`.
- `batch_record_bindings`: match, `20 / 20`.

## Evidence Files

- Full semantic comparison: `artifacts/route_import_consistency_report.json`.
- Diff rows: `artifacts/route_import_consistency_diffs.tsv`; file contains only the header, so no semantic differences were found.
- Target post-import key-process check: each imported route has exactly one key process matching the source route.

## Remaining Closeout

The data recovery and verification are complete. Task status is `ready_for_closeout`; repository cleanup, commit, and push are not performed in this verification step.
