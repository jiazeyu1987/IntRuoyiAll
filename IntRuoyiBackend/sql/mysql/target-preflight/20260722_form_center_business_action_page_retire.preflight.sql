-- release-target-preflight: migrationId=20260722_form_center_business_action_page_retire; allowedEnvironments=test,prod,backup
-- Reviewed identity, parent/permission and package-scope prerequisites from this migration.
SELECT CASE WHEN
 EXISTS(SELECT 1 FROM system_menu WHERE id=605071200 AND deleted=b'0' AND path='form-center')
 AND EXISTS(SELECT 1 FROM system_menu WHERE id=605071209 AND deleted=b'0' AND permission='form:instance:create')
 AND (SELECT COUNT(*) FROM system_menu WHERE id IN(605071210,605071211,605071212,605071213,605071218,605071219) AND deleted=b'0' AND type=3 AND parent_id IN(605071200,605071209))=6
 AND NOT EXISTS(SELECT 1 FROM system_menu WHERE parent_id=605071209 AND deleted=b'0' AND id NOT IN(605071210,605071211,605071212,605071213,605071218,605071219))
 AND NOT EXISTS(SELECT 1 FROM system_tenant_package WHERE deleted=b'0' AND NOT JSON_VALID(menu_ids))
 THEN 'TARGET_PREFLIGHT_PASS:20260722_form_center_business_action_page_retire' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260722_form_center_business_action_page_retire:MENU_IDENTITY_PARENT_OR_PACKAGE_CONFLICT' END;
