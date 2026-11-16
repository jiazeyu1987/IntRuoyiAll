-- release-target-preflight: migrationId=20261031_simple_common_parser_menu; allowedEnvironments=test,backup,prod
-- Read-only baseline predicates from this migration; mutation-time guards remain authoritative.
SELECT CASE
 WHEN (NOT EXISTS (SELECT 1 FROM system_menu WHERE id=991521 AND parent_id=991513
      AND name='检验解析' AND permission='form:parser:query' AND type=2 AND status=0 AND deleted=b'0')) THEN 'TARGET_PREFLIGHT_BLOCKED:20261031_simple_common_parser_menu:CONDITION_1'
 WHEN (EXISTS (SELECT 1 FROM system_menu WHERE id=991525 AND NOT (
      parent_id=991513 AND path='/mdm/form-center/simple-common-parser'
      AND component='form-center/simple-common-parser/index' AND component_name='FormCenterSimpleCommonParser'
      AND permission='form:parser:query' AND type=2 AND deleted=b'0'))
    OR EXISTS (SELECT 1 FROM system_menu WHERE id<>991525 AND deleted=b'0'
      AND (path='/mdm/form-center/simple-common-parser' OR component_name='FormCenterSimpleCommonParser'))) THEN 'TARGET_PREFLIGHT_BLOCKED:20261031_simple_common_parser_menu:CONDITION_2'
 WHEN (EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted=b'0'
      AND (NOT JSON_VALID(menu_ids) OR JSON_TYPE(CAST(menu_ids AS JSON))<>'ARRAY'))) THEN 'TARGET_PREFLIGHT_BLOCKED:20261031_simple_common_parser_menu:CONDITION_3'
 ELSE 'TARGET_PREFLIGHT_PASS:20261031_simple_common_parser_menu' END;
