# Execution Log: 生产工单列表用产品编号替换工单来源列

BDD: workorder_list_replaces_source_with_product_number -> Given a real admin user opens the MES production work-order list, When the page renders the table headers, Then the `工单来源` column should be absent and a `产品编号` column should appear in its place.

BDD: workorder_list_keeps_product_identity_visible -> Given production work-order rows already carry `productCode`, When the list renders after the change, Then users can read the product identifier directly from the main work-order list without opening the detail dialog.

RED: `npx.cmd --yes --package @playwright/cli playwright-cli --session workorder-product-number-column run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-pro-workorder-product-number-column\scripts\verify-workorder-product-number-column.mjs` -> FAIL, the real page still showed `工单来源` and the headers were `工单编码 | 工单名称 | 工单类型 | 工单来源 | 产品编码 | 产品名称 | ...`.

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session workorder-product-number-column run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-pro-workorder-product-number-column\scripts\verify-workorder-product-number-column.mjs` -> PASS, the real page returned `headers=["工单编码","工单名称","工单类型","产品编号","产品名称","规格型号","单位","工单数量","已生产数量","客户编码","客户名称","需求日期","工单状态","创建时间","操作"]` and no longer showed `工单来源`.

GREEN: `pnpm exec eslint src/views/mes/pro/workorder/index.vue` -> PASS, the scoped production work-order list page passed targeted frontend static validation.
