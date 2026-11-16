-- release-target-preflight: migrationId=20261028_simple_parser_publish_menu; allowedEnvironments=test,backup,prod
-- Inspect the current baseline before pending dependencies run. Migration-time guards remain authoritative.
SELECT CASE WHEN
  (NOT EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted=b'0' AND CASE WHEN menu_ids IS NULL OR NOT JSON_VALID(menu_ids) THEN 1 ELSE JSON_TYPE(CAST(menu_ids AS JSON))<>'ARRAY' END))
  AND (EXISTS (SELECT 1 FROM system_menu WHERE id=605071223 AND permission='form:parser:production-batch-record' AND deleted=b'0'))
  AND ((SELECT COUNT(*) FROM system_menu WHERE permission='form:parser:production-batch-record:publish' AND deleted=b'0')<=1)
  AND (EXISTS (SELECT 1 FROM system_menu WHERE permission='form:parser:production-batch-record:publish' AND deleted=b'0') OR NOT EXISTS (SELECT 1 FROM system_menu WHERE id=991523))
THEN 'TARGET_PREFLIGHT_PASS:20261028_simple_parser_publish_menu' ELSE 'TARGET_PREFLIGHT_BLOCKED:20261028_simple_parser_publish_menu:BASELINE_CONFLICT' END;
