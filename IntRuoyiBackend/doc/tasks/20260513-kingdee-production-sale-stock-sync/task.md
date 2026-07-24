# Task: Kingdee production sale stock sync actions

## Goal

Add usable Kingdee sync actions for MES production work orders, ERP sale orders, and ERP instant stock data in IntRuoyi, reusing the shared IntPP K3Cloud runtime configuration.

## Scope

- Backend: add sync endpoints and services for production work orders, sale orders, and instant stock.
- Backend: keep fail-fast behavior for missing K3Cloud credentials and malformed upstream rows.
- Backend: avoid duplicate imports through local sync-record tables.
- Backend: keep local master-data creation aligned with what each sync flow actually needs.
- Frontend: expose matching `同步金蝶` actions on the confirmed production, sale, and stock pages.

## Milestones

- [x] M1: Previous backend task checked and completed before this new task.
- [x] M2: Backend task document created before production code changes.
- [x] M3: Current K3Cloud coverage and candidate target pages inspected.
- [x] M4: Confirm requested stock scope as ERP instant stock page.
- [x] M5: Add RED evidence for malformed Kingdee rows, unsupported production statuses, full-history timeout scope, and stock schema overflow.
- [x] M6: Implement backend sync endpoints, schema, runtime config, and performance fixes.
- [x] M7: Run targeted tests, package verification, live API verification, update evidence, and commit backend changes.

## Expected Verification

- `POST /admin-api/mes/pro/work-order/sync-kingdee` imports production work orders from Kingdee and returns created/skipped counts.
- `POST /admin-api/erp/sale-order/sync-kingdee` imports sale orders from Kingdee and returns created/skipped counts.
- `POST /admin-api/erp/stock/sync-kingdee` refreshes ERP instant stock and returns synced count.
- Targeted backend tests cover malformed-row filtering, production status mapping, and stock aggregation behavior.
- No silent fallback or mock success path is introduced.

## Current Status

Completed.

## Final Verification

- Targeted tests passed:
  - `mvn -pl yudao-module-erp,yudao-module-mes -am "-Dtest=ErpKingdeeProductionOrderClientImplTest,ErpKingdeeSaleOrderClientImplTest,MesKingdeeProductionOrderSyncServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - `mvn -pl yudao-module-erp -am "-Dtest=ErpKingdeeStockSyncServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- Server packaging passed:
  - `mvn -pl yudao-server -am "-Dmaven.test.skip=true" package`
- Live API verification passed on local backend:
  - Production sync: `createdCount=999`, `skippedCount=1`, elapsed about `56.11s`
  - Sale sync: `createdCount=312`, `skippedCount=0`, elapsed about `122.52s`
  - Stock sync: `syncedCount=15953`, elapsed about `26.28s` on the verified rerun after local master data was established
- Local schema verification passed:
  - `erp_product.standard` expanded to `varchar(1024)` in local MySQL before rerunning stock sync

## Residual Notes

- During late frontend verification, the local login page temporarily suffered from a stale dev-process overlay and was later reverified after the `8081` frontend was restarted on current code.
