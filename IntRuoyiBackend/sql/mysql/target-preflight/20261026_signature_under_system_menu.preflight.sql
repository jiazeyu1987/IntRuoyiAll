-- release-target-preflight: migrationId=20261026_signature_under_system_menu; allowedEnvironments=test,backup,prod
-- Inspect the current baseline before pending dependencies run. Migration-time guards remain authoritative.
SELECT CASE WHEN
  (NOT (NOT EXISTS (SELECT 1 FROM system_menu WHERE id=1 AND name='系统管理'
      AND parent_id=0 AND path='/system' AND type=1 AND status=0 AND deleted=b'0')
    OR NOT EXISTS (SELECT 1 FROM system_menu WHERE id=900218 AND name='电子签名'
      AND parent_id IN (0,1) AND path='/signature-governance'
      AND component='signature-governance/index' AND type=2 AND status=0 AND deleted=b'0')))
  AND (NOT (EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted=b'0'
      AND (NOT JSON_VALID(menu_ids) OR JSON_TYPE(CAST(menu_ids AS JSON))<>'ARRAY'))))
THEN 'TARGET_PREFLIGHT_PASS:20261026_signature_under_system_menu' ELSE 'TARGET_PREFLIGHT_BLOCKED:20261026_signature_under_system_menu:BASELINE_CONFLICT' END;
