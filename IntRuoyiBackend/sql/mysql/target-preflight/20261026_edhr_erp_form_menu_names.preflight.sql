-- release-target-preflight: migrationId=20261026_edhr_erp_form_menu_names; allowedEnvironments=test,backup,prod
-- Inspect the current baseline before pending dependencies run. Migration-time guards remain authoritative.
SELECT CASE WHEN
  (EXISTS (SELECT 1 FROM system_menu WHERE id=900220 AND type=1 AND status=0 AND deleted=b'0' AND ((parent_id=5100 AND path='edhr-batch-processing') OR (parent_id=0 AND path='/mes/edhr-batch-processing'))))
  AND (EXISTS (SELECT 1 FROM system_menu WHERE id=2565 AND deleted=b'0' AND status=0 AND type=2 AND BINARY component IN ('erp/product/product/index') AND BINARY path IN ('erp-product','/mes/md/erp-product')))
  AND (EXISTS (SELECT 1 FROM system_menu WHERE id=2590 AND deleted=b'0' AND status=0 AND type=2 AND BINARY component IN ('erp/stock/stock/index') AND BINARY path IN ('erp-stock','/mes/md/erp-stock')))
  AND (EXISTS (SELECT 1 FROM system_menu WHERE id=6032 AND deleted=b'0' AND status=0 AND type=2 AND BINARY component IN ('erp/production/pick-list/index') AND BINARY path IN ('pick-list','/erp/production/pick-list')))
  AND (EXISTS (SELECT 1 FROM system_menu WHERE id=605071318 AND deleted=b'0' AND status=0 AND type=2 AND BINARY component IN ('erp/production/replenishment-list/index') AND BINARY path IN ('replenishment-list','/erp/production/replenishment-list')))
  AND (EXISTS (SELECT 1 FROM system_menu WHERE id=5530 AND deleted=b'0' AND status=0 AND type=2 AND BINARY component IN ('mes/pro/workorder/index') AND BINARY path IN ('work-order','/mes/pro/work-order')))
THEN 'TARGET_PREFLIGHT_PASS:20261026_edhr_erp_form_menu_names' ELSE 'TARGET_PREFLIGHT_BLOCKED:20261026_edhr_erp_form_menu_names:BASELINE_CONFLICT' END;
