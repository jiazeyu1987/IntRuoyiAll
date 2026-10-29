-- release-target-preflight: migrationId=20261026_edhr_erp_config_menu_order; allowedEnvironments=test,backup,prod
-- Inspect the current baseline before pending dependencies run. Migration-time guards remain authoritative.
SELECT CASE WHEN
  (EXISTS (SELECT 1 FROM system_menu WHERE id=900220 AND type=1 AND status=0 AND deleted=b'0' AND ((parent_id=5100 AND path='edhr-batch-processing') OR (parent_id=0 AND path='/mes/edhr-batch-processing'))))
  AND (EXISTS (SELECT 1 FROM system_menu WHERE id=6013 AND deleted=b'0' AND status=0 AND type=2 AND BINARY component IN ('erp/sync/index') AND BINARY path IN ('kingdee-sync','/erp/kingdee-sync')))
  AND (EXISTS (SELECT 1 FROM system_menu WHERE id=991502 AND deleted=b'0' AND status=0 AND type=2 AND BINARY component IN ('erp/sync/config/index') AND BINARY path IN ('sync/config','/erp/sync/config')))
  AND (EXISTS (SELECT 1 FROM system_menu WHERE id=5530 AND deleted=b'0' AND status=0 AND type=2 AND BINARY component IN ('mes/pro/workorder/index') AND BINARY path IN ('work-order','/mes/pro/work-order')))
THEN 'TARGET_PREFLIGHT_PASS:20261026_edhr_erp_config_menu_order' ELSE 'TARGET_PREFLIGHT_BLOCKED:20261026_edhr_erp_config_menu_order:BASELINE_CONFLICT' END;
