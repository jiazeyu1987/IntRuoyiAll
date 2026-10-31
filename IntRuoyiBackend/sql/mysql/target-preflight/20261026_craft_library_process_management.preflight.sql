-- release-target-preflight: migrationId=20261026_craft_library_process_management; allowedEnvironments=test,backup,prod
-- Inspect the current baseline before pending dependencies run. Migration-time guards remain authoritative.
SELECT CASE WHEN
  (EXISTS (SELECT 1 FROM system_menu WHERE id=900220 AND type=1 AND status=0 AND deleted=b'0' AND ((parent_id=5100 AND path='edhr-batch-processing') OR (parent_id=0 AND path='/mes/edhr-batch-processing'))))
  AND (NOT EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted=b'0' AND CASE WHEN menu_ids IS NULL OR NOT JSON_VALID(menu_ids) THEN 1 ELSE JSON_TYPE(CAST(menu_ids AS JSON))<>'ARRAY' END))
  AND ((SELECT COUNT(*) FROM system_menu WHERE component='mes/pro/craft/index' AND permission='mes:pro-craft:query' AND type=2 AND status=0 AND deleted=b'0')=1)
  AND (NOT EXISTS (SELECT 1 FROM system_menu WHERE path='/mes/pro/craft' AND deleted=b'0' AND NOT (component='mes/pro/craft/index' AND permission='mes:pro-craft:query')))
THEN 'TARGET_PREFLIGHT_PASS:20261026_craft_library_process_management' ELSE 'TARGET_PREFLIGHT_BLOCKED:20261026_craft_library_process_management:BASELINE_CONFLICT' END;
