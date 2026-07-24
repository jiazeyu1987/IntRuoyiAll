# Execution Log: Kingdee production sale stock sync actions

BDD: Sync Kingdee production work orders -> Given an operator is on the MES production work-order page and K3Cloud access is configured, When the operator clicks `同步金蝶`, Then IntRuoyi imports the latest configured production work orders and returns created/skipped counts.

BDD: Sync Kingdee sale orders -> Given an operator is on the ERP sale-order page and K3Cloud access is configured, When the operator clicks `同步金蝶`, Then IntRuoyi imports the latest configured sale orders and returns created/skipped counts.

BDD: Sync Kingdee instant stock -> Given an operator is on the ERP product-stock page and K3Cloud access is configured, When the operator clicks `同步金蝶`, Then IntRuoyi refreshes ERP instant stock from Kingdee and returns the covered stock-row count.

RED: live `POST /admin-api/mes/pro/work-order/sync-kingdee` -> FAIL, `FBillNo is blank` from real Kingdee `PRD_MO` rows proved the query needed invalid-row filtering.

RED: live `POST /admin-api/erp/sale-order/sync-kingdee` -> FAIL, `FMaterialId.FNumber is blank` from real Kingdee sale-order rows proved the query needed invalid-row filtering.

RED: live production and sale sync after malformed-row fix -> FAIL by timeout, both flows were scanning at least `30000` upstream rows and were too broad for a user-triggered sync.

RED: live `POST /admin-api/erp/stock/sync-kingdee` -> FAIL, MySQL rejected oversized `erp_product.standard` values from real Kingdee material specifications.

RED: live stock sync after schema expansion -> FAIL by timeout, real instant stock sample still contained `20661` rows and `13433` unique materials, proving per-material local product creation was too slow.

GREEN: `mvn -pl yudao-module-erp,yudao-module-mes -am "-Dtest=ErpKingdeeProductionOrderClientImplTest,ErpKingdeeSaleOrderClientImplTest,MesKingdeeProductionOrderSyncServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS

GREEN: `mvn -pl yudao-module-erp -am "-Dtest=ErpKingdeeStockSyncServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS

GREEN: `mvn -pl yudao-server -am "-Dmaven.test.skip=true" package` -> PASS

GREEN: live `POST /admin-api/mes/pro/work-order/sync-kingdee` with `tenant-id: 1` and local `admin/admin123` token -> PASS, `createdCount=999`, `skippedCount=1`, about `56.11s`

GREEN: live `POST /admin-api/erp/sale-order/sync-kingdee` with `tenant-id: 1` and local `admin/admin123` token -> PASS, `createdCount=312`, `skippedCount=0`, about `122.52s`

GREEN: local schema migration `sql/mysql/20260513_erp_product_standard_expand.sql` -> PASS, `erp_product.standard` confirmed at `varchar(1024)`

GREEN: live `POST /admin-api/erp/stock/sync-kingdee` rerun with `tenant-id: 1` and local `admin/admin123` token -> PASS, `syncedCount=15953`, about `26.28s`

GREEN: Playwright production work-order page verification -> PASS, browser request `POST /admin-api/mes/pro/work-order/sync-kingdee` returned `200` and response body reported `createdCount=0`, `skippedCount=1000`

Evidence:

- Real K3Cloud malformed production row existed for `PRD_MO` with `FID=266374`, blank `FBillNo`, `FDocumentStatus='Z'`, and null material number.
- Real K3Cloud production statuses included `5`, `6`, and `7`; production status mapping was aligned to local work-order states as:
  - `1 -> PREPARE`
  - `2/3/4 -> CONFIRMED`
  - `5/6/7 -> FINISHED`
- Real K3Cloud scale checks showed:
  - production sample reached at least `30000` valid rows before scope cap
  - sale sample reached at least `30000` valid rows before scope cap
  - stock sample contained `20661` rows, `13433` unique materials, and `164` unique warehouse keys
- Final stock optimization avoided extra Kingdee material-detail lookups and batch-created missing ERP products before refreshing stock rows.
