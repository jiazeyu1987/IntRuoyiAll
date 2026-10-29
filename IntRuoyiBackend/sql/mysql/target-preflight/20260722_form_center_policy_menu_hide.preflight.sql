-- release-target-preflight: migrationId=20260722_form_center_policy_menu_hide; allowedEnvironments=test,prod,backup
-- Reviewed identity, parent/permission and package-scope prerequisites from this migration.
SELECT CASE WHEN
 EXISTS(SELECT 1 FROM system_menu WHERE id=605071200 AND deleted=b'0' AND path='form-center')
 AND EXISTS(SELECT 1 FROM system_menu WHERE id=605071214 AND deleted=b'0' AND permission='form:policy:query' AND type=2 AND parent_id=605071200 AND path='policy' AND component='form-center/policy/index' AND component_name='FormCenterPolicy')
 AND (SELECT COUNT(*) FROM system_menu WHERE id IN(605071215,605071216,605071217) AND deleted=b'0' AND type=3 AND parent_id=605071214)=3
 THEN 'TARGET_PREFLIGHT_PASS:20260722_form_center_policy_menu_hide' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260722_form_center_policy_menu_hide:MENU_IDENTITY_PARENT_OR_PACKAGE_CONFLICT' END;
