-- release-target-preflight: migrationId=20261026_edhr_data_sync_directory; allowedEnvironments=test,backup,prod
-- Inspect the current baseline before pending dependencies run. Migration-time guards remain authoritative.
SELECT CASE WHEN
  (EXISTS (SELECT 1 FROM system_menu WHERE id=900220 AND type=1 AND status=0 AND deleted=b'0' AND ((parent_id=5100 AND path='edhr-batch-processing') OR (parent_id=0 AND path='/mes/edhr-batch-processing'))))
  AND (EXISTS (SELECT 1 FROM system_menu WHERE id=6013 AND deleted=b'0' AND status=0 AND type=2 AND BINARY component IN ('erp/sync/index') AND BINARY path IN ('kingdee-sync','/erp/kingdee-sync')))
  AND (EXISTS (SELECT 1 FROM system_menu WHERE id=991502 AND deleted=b'0' AND status=0 AND type=2 AND BINARY component IN ('erp/sync/config/index') AND BINARY path IN ('sync/config','/erp/sync/config')))
  AND (EXISTS (SELECT 1 FROM system_menu WHERE id=2565 AND deleted=b'0' AND status=0 AND type=2 AND BINARY component IN ('erp/product/product/index') AND BINARY path IN ('erp-product','/mes/md/erp-product')))
  AND (EXISTS (SELECT 1 FROM system_menu WHERE id=2590 AND deleted=b'0' AND status=0 AND type=2 AND BINARY component IN ('erp/stock/stock/index') AND BINARY path IN ('erp-stock','/mes/md/erp-stock')))
  AND (EXISTS (SELECT 1 FROM system_menu WHERE id=6023 AND deleted=b'0' AND status=0 AND type=2 AND BINARY component IN ('erp/production/bom-list/index') AND BINARY path IN ('bom-list','/erp/production/bom-list')))
  AND (EXISTS (SELECT 1 FROM system_menu WHERE id=6021 AND deleted=b'0' AND status=0 AND type=2 AND BINARY component IN ('erp/production/material-list/index') AND BINARY path IN ('material-list','/erp/production/material-list')))
  AND (EXISTS (SELECT 1 FROM system_menu WHERE id=6032 AND deleted=b'0' AND status=0 AND type=2 AND BINARY component IN ('erp/production/pick-list/index') AND BINARY path IN ('pick-list','/erp/production/pick-list')))
  AND (EXISTS (SELECT 1 FROM system_menu WHERE id=605071318 AND deleted=b'0' AND status=0 AND type=2 AND BINARY component IN ('erp/production/replenishment-list/index') AND BINARY path IN ('replenishment-list','/erp/production/replenishment-list')))
  AND (EXISTS (SELECT 1 FROM system_menu WHERE id=6025 AND deleted=b'0' AND status=0 AND type=2 AND BINARY component IN ('erp/production/inventory-list/index') AND BINARY path IN ('inventory-list','/erp/production/inventory-list')))
  AND (EXISTS (SELECT 1 FROM system_menu WHERE id=5530 AND deleted=b'0' AND status=0 AND type=2 AND BINARY component IN ('mes/pro/workorder/index') AND BINARY path IN ('work-order','/mes/pro/work-order')))
  AND (NOT EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted=b'0' AND CASE WHEN menu_ids IS NULL OR NOT JSON_VALID(menu_ids) THEN 1 ELSE JSON_TYPE(CAST(menu_ids AS JSON))<>'ARRAY' END))
  AND (NOT EXISTS (SELECT 1 FROM system_menu WHERE id=991511 AND NOT (deleted=b'0' AND type=1 AND BINARY path='/mes/edhr-data-sync' AND BINARY component='')))
  AND (NOT EXISTS (SELECT 1 FROM system_menu WHERE id<>991511 AND deleted=b'0' AND BINARY path='/mes/edhr-data-sync'))
THEN 'TARGET_PREFLIGHT_PASS:20261026_edhr_data_sync_directory' ELSE 'TARGET_PREFLIGHT_BLOCKED:20261026_edhr_data_sync_directory:BASELINE_CONFLICT' END;
