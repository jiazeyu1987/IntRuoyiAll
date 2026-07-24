# Execution Log: Kingdee production sale stock sync frontend

BDD: Frontend production sync entry -> Given an authenticated operator is on the MES production work-order page, When the operator clicks `同步金蝶`, Then the frontend sends the production sync request, shows loading, and refreshes the table on success.

BDD: Frontend sale sync entry -> Given an authenticated operator is on the ERP sale-order page, When the operator clicks `同步金蝶`, Then the frontend sends the sale sync request, shows loading, and refreshes the table on success.

BDD: Frontend stock sync entry -> Given an authenticated operator is on the ERP product-stock page, When the operator clicks `同步金蝶`, Then the frontend sends the stock sync request, shows loading, and refreshes the table on success.

RED: direct local login-page click in Playwright -> FAIL, the existing login component threw `TypeError: Cannot read properties of undefined (reading 'close')` with captcha disabled.

GREEN: `pnpm exec eslint src/api/mes/pro/workorder/index.ts src/api/erp/sale/order/index.ts src/api/erp/stock/stock/index.ts src/views/mes/pro/workorder/index.vue src/views/erp/sale/order/index.vue src/views/erp/stock/stock/index.vue` -> PASS

GREEN: Playwright production work-order page verification -> PASS, browser request `POST /admin-api/mes/pro/work-order/sync-kingdee` returned `200` and response body reported `createdCount=0`, `skippedCount=1000`

GREEN: Playwright sale-order page verification -> PASS, browser request `POST /admin-api/erp/sale-order/sync-kingdee` returned `200` and response body reported `createdCount=0`, `skippedCount=312`

GREEN: Playwright product-stock page verification -> PASS, browser request `POST /admin-api/erp/stock/sync-kingdee` returned `200` and response body reported `syncedCount=15953`

Evidence:

- Verified sale-order page URL: `http://localhost:8081/erp/sale/order`
- Verified product-stock page URL: `http://localhost:8081/erp/stock/stock`
- Verified production work-order page URL: `http://localhost:8081/mes/pro/work-order`
- Later verification used the normal visible login-button path after restarting the stale local `8081` frontend dev process.
