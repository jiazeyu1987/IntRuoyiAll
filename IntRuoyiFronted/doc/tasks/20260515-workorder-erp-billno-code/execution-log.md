BDD: workorder_list_hides_source_doc_column -> Given a real admin user opens the MES production work-order list, When the page renders the table headers, Then the `来源单据编号` column is not visible and the `工单编码` column remains visible.

BDD: synced_workorder_code_uses_erp_bill_no -> Given production work orders are synced from ERP, When users view MES work-order identifiers in downstream pages, Then the work-order code should be the ERP production order bill number rather than a generated `KDMO-...` surrogate.

RED: `npx.cmd --yes --package @playwright/cli playwright-cli --session workorder-erp-billno run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260515-workorder-erp-billno-code\scripts\verify-workorder-list-columns.mjs` -> FAIL, the real page at `http://127.0.0.1:8081/mes/pro/work-order` still showed the `来源单据编号` column and visible legacy `KDMO-...` work-order codes.

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session workorder-erp-billno run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260515-workorder-erp-billno-code\scripts\verify-workorder-list-columns.mjs` -> PASS, real login reached `http://127.0.0.1:8081/mes/pro/work-order` and returned `hasCodeHeader=true`, `hasSourceHeader=false`, `hasLegacyKingdeeCode=false`.

CHECK: `pnpm ts:check` -> FAIL, repository-wide pre-existing type errors remain in unrelated files such as `src/components/bpmnProcessDesigner/package/designer/ProcessViewer.vue`, `src/config/axios/service.ts`, `src/layout/components/TagsView/src/TagsView.vue`, and multiple non-MES pages.

GREEN: `pnpm exec eslint src/views/mes/pro/workorder/index.vue` -> PASS, the changed production work-order list page passed targeted frontend static validation.
