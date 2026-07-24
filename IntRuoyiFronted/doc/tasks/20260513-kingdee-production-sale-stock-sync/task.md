# Task: Kingdee production sale stock sync frontend

## Goal

Expose usable `同步金蝶` actions on the MES production work-order page, the ERP sale-order page, and the ERP product-stock page.

## Scope

- Add toolbar buttons, loading states, success feedback, and data refresh wiring.
- Keep the current page layout and permissions model intact.
- Verify the user path with Playwright against the real local frontend.

## Milestones

- [x] M1: Previous frontend task checked and completed before this new task.
- [x] M2: Frontend task document created before production code changes.
- [x] M3: Candidate production/sale/stock target pages inspected.
- [x] M4: Confirm requested stock scope as ERP product-stock page.
- [x] M5: Add RED evidence for missing routes/contracts and broken local login path.
- [x] M6: Implement frontend sync actions and page refresh behavior.
- [x] M7: Run lint/E2E verification, update evidence, and commit frontend changes.

## Expected Verification

- The production work-order page shows a `同步金蝶` button and calls the production sync API.
- The sale-order page shows a `同步金蝶` button and calls the sale sync API.
- The product-stock page shows a `同步金蝶` button and calls the stock sync API.
- Success feedback is shown and the visible list refreshes after the sync returns.

## Current Status

Completed.

## Final Verification

- Frontend lint passed:
  - `pnpm exec eslint src/api/mes/pro/workorder/index.ts src/api/erp/sale/order/index.ts src/api/erp/stock/stock/index.ts src/views/mes/pro/workorder/index.vue src/views/erp/sale/order/index.vue src/views/erp/stock/stock/index.vue`
- Playwright browser verification passed for visible page wiring:
  - Production work-order page: button visible, click sent `POST /admin-api/mes/pro/work-order/sync-kingdee`, browser received `200`, response body showed `createdCount=0`, `skippedCount=1000`
  - Sale-order page: button visible, click sent `POST /admin-api/erp/sale-order/sync-kingdee`, browser received `200`, response body showed `createdCount=0`, `skippedCount=312`
  - Product-stock page: button visible, click sent `POST /admin-api/erp/stock/sync-kingdee`, browser received `200`, response body showed `syncedCount=15953`

## Residual Notes

- The local login page was later reverified on the normal click path after restarting the stale `8081` frontend dev process, so authenticated browser verification no longer depends on localStorage session injection.
