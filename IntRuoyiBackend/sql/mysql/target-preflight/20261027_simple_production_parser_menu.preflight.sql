-- release-target-preflight: migrationId=20261027_simple_production_parser_menu; allowedEnvironments=test,backup,prod
-- Inspect the current baseline before pending dependencies run. Migration-time guards remain authoritative.
SELECT CASE WHEN
  (EXISTS (SELECT 1 FROM system_menu WHERE id=900220 AND type=1 AND status=0 AND deleted=b'0' AND ((parent_id=5100 AND path='edhr-batch-processing') OR (parent_id=0 AND path='/mes/edhr-batch-processing'))))
  AND (EXISTS (SELECT 1 FROM system_menu WHERE id=605071222 AND deleted=b'0' AND status=0 AND type=2 AND BINARY component IN ('form-center/parser/index','form-center/parser/ProductionParserPage') AND BINARY path IN ('form-center/parser','/mdm/form-center/parser')))
  AND (EXISTS (SELECT 1 FROM system_menu WHERE id=900365 AND deleted=b'0' AND status=0 AND type=2 AND BINARY component IN ('mes/pro/batchrecordformlist/index') AND BINARY path IN ('/mes/pro/batch-record-form-list')))
  AND (NOT EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted=b'0' AND CASE WHEN menu_ids IS NULL OR NOT JSON_VALID(menu_ids) THEN 1 ELSE JSON_TYPE(CAST(menu_ids AS JSON))<>'ARRAY' END))
  AND (NOT EXISTS (SELECT 1 FROM system_menu WHERE id=605071336 AND NOT (deleted=b'0' AND type=2 AND BINARY path='/mdm/form-center/simple-parser' AND BINARY component='form-center/simple-parser/index')))
  AND (NOT EXISTS (SELECT 1 FROM system_menu WHERE id<>605071336 AND deleted=b'0' AND BINARY path='/mdm/form-center/simple-parser'))
THEN 'TARGET_PREFLIGHT_PASS:20261027_simple_production_parser_menu' ELSE 'TARGET_PREFLIGHT_BLOCKED:20261027_simple_production_parser_menu:BASELINE_CONFLICT' END;
