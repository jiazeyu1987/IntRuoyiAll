# Execution Log: 工单详情 ERP 同步 BOM 前端

BDD: detail_dialog_exposes_erp_bom_sync_button -> Given a user opens an existing work-order detail dialog, When the footer renders, Then an `ERP同步BOM` action appears to the left of `查看条码`.

BDD: detail_dialog_syncs_and_refreshes_bom_tabs -> Given the work-order detail dialog is open and the backend sync succeeds, When the user clicks `ERP同步BOM`, Then the frontend calls the sync API, shows loading, keeps the dialog open, and refreshes both the work-order BOM tab and material-demand tab.

BDD: detail_dialog_surfaces_backend_sync_errors -> Given the backend rejects the sync because of non-unique ERP BOM, downstream usage, or missing local item mapping, When the user clicks `ERP同步BOM`, Then the frontend stops loading and surfaces the backend failure instead of silently downgrading.

RED: source contract check -> FAIL, before implementation `WorkOrderForm.vue` footer exposed `查看条码` only and `src/api/mes/pro/workorder/index.ts` had no `syncErpBom(workOrderId)` wrapper.

GREEN: real-page fail-fast verification -> PASS, the same Playwright verification script first confirmed `hasSyncText=true`, `hasBarcodeText=true`, `syncResponseStatus=200`, and `surfacedMissingItemMessage=true` when the sampled work order still lacked local master-data mapping.

GREEN: real-page success verification -> PASS, after补齐主数据映射 the same Playwright verification script returned `hasSyncText=true`, `hasBarcodeText=true`, `syncResponseStatus=200`, and `syncResponseSnippet={"code":0,...,"workOrderId":903245,"erpBomVersion":"YXN.037.011.1002_V1.1","syncedBomCount":27}`.
