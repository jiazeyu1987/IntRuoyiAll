# Execution Log: Kingdee purchase order synchronization frontend

BDD: Frontend manual trigger -> Given an administrator has permission on the ERP purchase order list page, When the administrator clicks the Kingdee sync button, Then the frontend calls the sync API, shows loading, refreshes the list on success, and surfaces backend errors on failure.

## Evidence

- M1/M2: Completed. Frontend task document and BDD scenario were created in the frontend repository before production code changes.
- RED: `node doc\tasks\20260512-kingdee-purchase-order-sync\verify-frontend-contract.cjs` -> FAIL, expected missing API response type, sync API wrapper, permission-guarded button, loading state, and sync handler.
- GREEN: `node doc\tasks\20260512-kingdee-purchase-order-sync\verify-frontend-contract.cjs` -> PASS.
- GREEN: `pnpm exec eslint src/api/erp/purchase/order/index.ts src/views/erp/purchase/order/index.vue` -> PASS.
- BLOCKED: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> FAIL because the repository has unrelated existing TypeScript errors outside the owned files, including `src/api/mall/statistics/trade.ts`, BPMN designer components, and pay/system views.
