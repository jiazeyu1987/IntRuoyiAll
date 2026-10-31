-- release-target-preflight: migrationId=20260721_form_center_menu_under_basic_data; allowedEnvironments=test,prod,backup
-- Reviewed identity, parent/permission and package-scope prerequisites from this migration.
SELECT CASE WHEN
 EXISTS(SELECT 1 FROM system_menu WHERE id=990200 AND deleted=b'0' AND path='/mdm')
 AND EXISTS(SELECT 1 FROM system_menu WHERE id=605071200 AND deleted=b'0' AND path='form-center')
 AND (SELECT COUNT(*) FROM system_menu WHERE id IN(605071200,605071201,605071202,605071203,605071204,605071205,605071206,605071207,605071208,605071210,605071211,605071212,605071213,605071214,605071215,605071216,605071217,605071218,605071219,605071220,605071221) AND deleted=b'0')=21
 AND NOT EXISTS(SELECT 1 FROM system_tenant_package WHERE deleted=b'0' AND NOT JSON_VALID(menu_ids))
 THEN 'TARGET_PREFLIGHT_PASS:20260721_form_center_menu_under_basic_data' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260721_form_center_menu_under_basic_data:MENU_IDENTITY_PARENT_OR_PACKAGE_CONFLICT' END;
