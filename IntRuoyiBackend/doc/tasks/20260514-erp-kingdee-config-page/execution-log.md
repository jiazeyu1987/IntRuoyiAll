# Execution Log: ERP Kingdee config management backend

BDD: ERP config page shows current effective settings -> Given the local runtime already has Kingdee settings from `.env` and/or saved config rows, When the ERP config page backend is queried, Then it returns the effective settings with current runtime values as defaults.

BDD: ERP config save updates future sync reads -> Given an operator saves new Kingdee settings in the ERP config page, When ERP or MES sync actions run afterward, Then they use the saved effective settings instead of only the boot-time defaults.

BDD: ERP config save persists a dedicated config payload -> Given no prior ERP config row exists, When the operator saves the ERP config page, Then the backend creates a dedicated persisted config entry that can be read back later.

RED: repo search before implementation -> FAIL, there was no dedicated ERP Kingdee config endpoint, no ERP config page backend, and sync services read only the boot-time `ErpKingdeeProperties` bean.

GREEN: `mvn -pl yudao-module-erp,yudao-module-mes -am "-Dtest=ErpKingdeeConfigServiceImplTest,ErpKingdeeProductSyncServiceImplTest,ErpKingdeePurchaseOrderSyncServiceImplTest,ErpKingdeeSaleOrderSyncServiceImplTest,ErpKingdeeStockSyncServiceImplTest,MesKingdeeProductionOrderSyncServiceImplTest,MesKingdeeItemSyncServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, the ERP config provider and all affected ERP/MES Kingdee sync services compile and pass targeted tests.

GREEN: `mvn -pl yudao-server -am "-Dmaven.test.skip=true" package` -> PASS, the local backend jar includes the new ERP config controller and effective-config provider.

GREEN: local menu seed SQL `sql/mysql/20260514_erp_kingdee_config_page.sql` applied -> PASS, the local ERP menu now includes `配置管理`, and the admin role has page/query/save permissions.

GREEN: real browser-backed config flow -> PASS, `GET /admin-api/erp/kingdee-config/get` returned the current effective config, `PUT /admin-api/erp/kingdee-config/save` returned `200`, and `infra_config.config_key = yudao.erp.kingdee.config` was persisted.
