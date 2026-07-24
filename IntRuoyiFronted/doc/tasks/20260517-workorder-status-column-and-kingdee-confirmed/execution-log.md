# Execution Log: 生产工单列表客户编码改工单状态前端

BDD: workorder_list_replaces_customer_code_with_status -> Given a real user opens the MES production work-order list, When the table headers render, Then `客户编码` should be absent and `工单状态` should appear in that same slot without a second duplicate status column later in the table.

RED: pre-change behavior -> FAIL, the list still exposed `客户编码` while `工单状态` appeared in a separate later column.

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session workorder-status-column run-code --filename D:\\ProjectPackage\\Int\\IntRuoyi\\yudao-ui-admin-vue3\\doc\\tasks\\20260517-workorder-status-column-and-kingdee-confirmed\\scripts\\verify-workorder-status-column.mjs` -> PASS, `headerTexts` show `工单状态` in the former customer-code slot and no `客户编码` column.
