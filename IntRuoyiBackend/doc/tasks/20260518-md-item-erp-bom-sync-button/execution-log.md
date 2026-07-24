# Execution Log

- 2026-05-18: Created backend task package `20260518-md-item-erp-bom-sync-button`.
- 2026-05-18: Blocked previous backend task `20260518-workorder-row-freeze-toggle-action` due user priority switch.
- BDD: Sync one product BOM from ERP -> Given an editable MES product with a local code and one approved ERP BOM version, When the sync endpoint is called, Then the existing `mes_md_product_bom` rows are deleted and replaced with ERP-derived BOM rows for that product only.
- BDD: Reject ambiguous ERP BOM versions -> Given ERP returns multiple approved BOM versions for the same parent material code, When the sync endpoint is called, Then the request fails fast and no BOM rows are changed.
- BDD: Reject missing ERP BOM -> Given ERP returns no approved BOM for the current parent material code, When the sync endpoint is called, Then the request fails fast and no BOM rows are changed.
- BDD: Reject missing local child item mapping -> Given an ERP BOM child material code does not exist in local `mes_md_item`, When the sync endpoint is called, Then the request fails fast and reports the missing codes.
- BDD: Reject recursive local BOM child -> Given an ERP BOM child item already owns a local product BOM, When the sync endpoint is called, Then the request fails fast and does not replace the current product BOM.
- BDD: Preserve ratio precision -> Given ERP returns numerator and denominator decimal values, When the sync endpoint is called, Then the saved `quantity` uses the backend ratio precision policy and stores the normalized ratio value.
- RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesKingdeeProductBomSyncServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, compiler cannot find `MesKingdeeProductBomSyncServiceImpl` because the product-level ERP BOM sync service does not exist yet.
- GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesKingdeeProductBomSyncServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, all 6 product-level ERP BOM sync tests passed after the new endpoint/service/result/error mapping were added.
