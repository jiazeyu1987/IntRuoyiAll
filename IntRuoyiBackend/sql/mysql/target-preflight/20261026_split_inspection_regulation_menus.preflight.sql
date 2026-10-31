-- release-target-preflight: migrationId=20261026_split_inspection_regulation_menus; allowedEnvironments=test,backup,prod
-- Inspect the current baseline before pending dependencies run. Migration-time guards remain authoritative.
SELECT CASE WHEN
  (EXISTS (SELECT 1 FROM system_menu WHERE id=900220 AND type=1 AND status=0 AND deleted=b'0' AND ((parent_id=5100 AND path='edhr-batch-processing') OR (parent_id=0 AND path='/mes/edhr-batch-processing'))))
  AND (EXISTS (SELECT 1 FROM system_menu WHERE id=900434 AND deleted=b'0' AND status=0 AND type=2 AND BINARY component IN ('mes/pro/processpool/QaRegulationPage') AND BINARY path IN ('/mes/pro/process-pool/qa-regulation')))
  AND (NOT EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted=b'0' AND CASE WHEN menu_ids IS NULL OR NOT JSON_VALID(menu_ids) THEN 1 ELSE JSON_TYPE(CAST(menu_ids AS JSON))<>'ARRAY' END))
  AND (NOT EXISTS (SELECT 1 FROM system_menu WHERE id=991520 AND NOT (deleted=b'0' AND type=2 AND BINARY path='/mes/pro/process-pool/common-regulation' AND BINARY component='mes/pro/processpool/CommonRegulationPage')))
  AND (NOT EXISTS (SELECT 1 FROM system_menu WHERE id<>991520 AND deleted=b'0' AND BINARY path='/mes/pro/process-pool/common-regulation'))
THEN 'TARGET_PREFLIGHT_PASS:20261026_split_inspection_regulation_menus' ELSE 'TARGET_PREFLIGHT_BLOCKED:20261026_split_inspection_regulation_menus:BASELINE_CONFLICT' END;
