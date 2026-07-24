# Task: Enable live Kingdee purchase-order sync

## Goal

Make the existing `同步金蝶` purchase-order action usable in the local IntRuoyi environment by loading real K3Cloud runtime configuration and auto-provisioning the local ERP master data required to create purchase orders.

## Scope

- Bridge local IntPP K3Cloud runtime configuration into the IntRuoyi backend local profile without storing duplicate secrets in the repository.
- Extend Kingdee purchase-order sync so missing local supplier/product/unit/category master data can be created from real Kingdee source data.
- Keep fail-fast behavior for missing Kingdee prerequisites or invalid source payloads; do not add silent fallback or mock success.
- Add or update backend regression tests first, then implement the minimal production change.

## Milestones

- [x] M1: Previous backend task checked and confirmed completed before starting this new bug-fix task.
- [x] M2: Task document created before backend production code changes.
- [x] M3: Root cause evidence captured for the current sync failure and local-data preconditions.
- [x] M4: RED tests added for local profile config import and master-data auto-provision behavior.
- [x] M5: Backend sync/config implementation updated to satisfy the new RED coverage.
- [x] M6: Targeted verification passes and live local sync path is exercised.
- [x] M7: Evidence updated and backend task changes committed separately if verification passes.

## Expected Verification

- The backend local profile reads Kingdee runtime values from the existing `D:\ProjectPackage\Int\IntPP\backend\.env` file instead of requiring duplicate secrets in this repository.
- The sync flow can create a missing supplier from a real Kingdee supplier number/name and record the mapping for subsequent runs.
- The sync flow can create missing product unit/category/product records from real Kingdee material metadata before creating the purchase order.
- Targeted backend tests cover host-style Kingdee base URLs, supplier auto-provision, and product auto-provision without adding fallback branches.
- Local live verification reaches Kingdee and no longer fails immediately with `Kingdee K3Cloud采购订单同步配置缺失：yudao.erp.kingdee.base-url`.

## Current Status

Completed. The local backend now reads live Kingdee settings from the existing IntPP `.env`, host-style base URLs are normalized to `/K3Cloud`, missing local supplier/product/unit/category master data is auto-provisioned from real Kingdee source data, and the purchase-order sync path has been verified end-to-end.

## Root Cause Evidence

- The live backend log shows `Kingdee K3Cloud采购订单同步配置缺失：yudao.erp.kingdee.base-url` on `POST /admin-api/erp/purchase-order/sync-kingdee`.
- The existing K3Cloud credentials and purchase-order org/query settings already exist in `D:\ProjectPackage\Int\IntPP\backend\.env`.
- The local `ruoyi-vue-pro` database currently has zero rows in `erp_supplier`, `erp_product`, `erp_product_unit`, and `erp_product_category`, so current static mapping-only sync cannot create purchase orders even after credentials are supplied.
- Direct K3Cloud verification on 2026-05-13 succeeded for `ValidateUser.common.kdsvc`, and real `PUR_PurchaseOrder` plus `BD_MATERIAL`/`BD_Supplier` payloads were inspected to confirm the source contains the fields needed for auto-provisioning.

## Final Verification

- Local config bridge verification passed with `python doc\tasks\20260513-kingdee-purchase-order-sync-live-enable\verify_local_kingdee_config.py`.
- Targeted backend regression tests passed with `mvn -pl yudao-module-erp -am "-Dtest=ErpKingdeePurchaseOrderSyncServiceImplTest,ErpKingdeePurchaseOrderClientImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`.
- Live API verification created 297 local purchase orders on the first real sync run and then returned `createdCount=0, skippedCount=297` in about 4.5 seconds on the second run.
- Playwright verified the real frontend user path on `http://localhost:8081/erp/purchase/order`, including the visible `同步金蝶` button and the refreshed purchase-order list with 297 records.
