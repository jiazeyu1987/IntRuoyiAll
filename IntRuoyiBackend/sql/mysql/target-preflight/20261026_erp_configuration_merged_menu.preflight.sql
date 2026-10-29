-- release-target-preflight: migrationId=20261026_erp_configuration_merged_menu; allowedEnvironments=test,backup,prod
-- Inspect the current baseline before pending dependencies run. Migration-time guards remain authoritative.
SELECT CASE WHEN
  (EXISTS (SELECT 1 FROM system_menu WHERE id=900220 AND type=1 AND status=0 AND deleted=b'0' AND ((parent_id=5100 AND path='edhr-batch-processing') OR (parent_id=0 AND path='/mes/edhr-batch-processing'))))
  AND (EXISTS (SELECT 1 FROM system_menu WHERE id=6013 AND deleted=b'0' AND status=0 AND type=2 AND BINARY component IN ('erp/sync/index') AND BINARY path IN ('kingdee-sync','/erp/kingdee-sync')))
  AND (EXISTS (SELECT 1 FROM system_menu WHERE id=991502 AND deleted=b'0' AND status=0 AND type=2 AND BINARY component IN ('erp/sync/config/index') AND BINARY path IN ('sync/config','/erp/sync/config')))
  AND (NOT EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted=b'0' AND CASE WHEN menu_ids IS NULL OR NOT JSON_VALID(menu_ids) THEN 1 ELSE JSON_TYPE(CAST(menu_ids AS JSON))<>'ARRAY' END))
THEN 'TARGET_PREFLIGHT_PASS:20261026_erp_configuration_merged_menu' ELSE 'TARGET_PREFLIGHT_BLOCKED:20261026_erp_configuration_merged_menu:BASELINE_CONFLICT' END;
