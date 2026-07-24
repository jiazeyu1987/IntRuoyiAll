# Execution Log: 工单详情 ERP 同步 BOM 后端

BDD: confirmed_workorder_can_sync_unique_approved_erp_bom -> Given a PREPARE or CONFIRMED work order has a product code and Kingdee returns exactly one approved BOM version for that parent material, When the sync endpoint runs, Then the current work-order BOM is replaced by ERP BOM rows computed from work-order quantity × ERP standard usage.

BDD: sync_replaces_existing_workorder_bom -> Given a work order already has local BOM rows and no downstream material transactions, When ERP BOM sync succeeds, Then old work-order BOM rows are removed and only the synced ERP BOM rows remain.

BDD: sync_blocks_when_multiple_approved_bom_versions_exist -> Given Kingdee returns more than one approved BOM version for the same parent material code, When ERP BOM sync runs, Then the request fails fast and does not modify the work-order BOM.

BDD: sync_blocks_when_downstream_material_transactions_exist -> Given the work order already has product issue, outsource issue, or material consume records, When ERP BOM sync runs, Then the request fails fast and does not modify the work-order BOM.

BDD: sync_blocks_when_local_mes_item_mapping_is_missing -> Given an ERP BOM child material code cannot be mapped to a local MES item, When ERP BOM sync runs, Then the request fails fast with the missing codes and does not modify the work-order BOM.

RED: `mvn --% -pl yudao-module-erp -Dtest=ErpKingdeeBomClientImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, compile broke because `ErpKingdeeBomClientImpl`, `ErpKingdeeBomLine`, and `ErpKingdeeProperties#getBom()` did not exist yet.

RED: `mvn --% -pl yudao-module-mes -Dtest=MesKingdeeWorkOrderBomSyncServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, compile broke because the work-order ERP BOM sync service, response types, and downstream count helpers did not exist yet.

GREEN: `mvn --% -pl yudao-module-erp -Dtest=ErpKingdeeBomClientImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS

GREEN: `mvn --% -pl yudao-module-mes -Dtest=MesKingdeeWorkOrderBomSyncServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS

GREEN: real-page invocation -> PASS for request-path verification, the frontend reached `/admin-api/mes/pro/work-order/903245/sync-erp-bom` and the backend initially returned the expected fail-fast message `ERP BOM 子项物料未映射到本地 MES 物料：A002.09.001.000021, A002.11.001.000012`.

GREEN: live mapped candidate direct sync -> PASS, direct authenticated POST to `/admin-api/mes/pro/work-order/903544/sync-erp-bom` returned `erpBomVersion=A003.017.15.001.2001_V1.0`, `syncedBomCount=3`, and database verification confirmed work-order `903544` BOM rows changed from `0` to `3`.

GREEN: live missing-master-data repair -> PASS, after manually补齐 ERP 产品与 MES 物料主数据 for `A002.09.001.000021` and `A002.11.001.000012`, direct authenticated POST to `/admin-api/mes/pro/work-order/903245/sync-erp-bom` returned `erpBomVersion=YXN.037.011.1002_V1.1`, `syncedBomCount=27`, and database verification confirmed work-order `903245` BOM rows changed from `0` to `27`.
