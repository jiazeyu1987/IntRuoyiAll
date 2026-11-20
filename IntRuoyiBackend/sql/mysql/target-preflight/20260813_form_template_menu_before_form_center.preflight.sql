-- release-target-preflight: migrationId=20260813_form_template_menu_before_form_center; allowedEnvironments=test,prod,backup
-- Reviewed identity, parent/permission and package-scope prerequisites from this migration.
SELECT CASE WHEN
 EXISTS(SELECT 1 FROM system_menu WHERE id=990200 AND deleted=b'0' AND path='/mdm')
 AND EXISTS(SELECT 1 FROM system_menu WHERE id=605071200 AND deleted=b'0' AND type=1 AND parent_id=990200 AND path='form-center')
 AND EXISTS(SELECT 1 FROM system_menu WHERE id=605071201 AND deleted=b'0' AND permission='form:template:query' AND type=2 AND component='form-center/template/index' AND component_name='FormCenterTemplate' AND ((parent_id=605071200 AND path='template') OR(parent_id=990200 AND path='form-center/template')))
 AND (SELECT COUNT(*) FROM system_menu WHERE id IN(605071202,605071203,605071204,605071205,605071206,605071207,605071208) AND deleted=b'0' AND type=3 AND parent_id=605071201)=7
 AND NOT EXISTS(SELECT 1 FROM system_menu WHERE deleted=b'0' AND parent_id=990200 AND path='form-center/template' AND id<>605071201)
 THEN 'TARGET_PREFLIGHT_PASS:20260813_form_template_menu_before_form_center' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260813_form_template_menu_before_form_center:MENU_IDENTITY_PARENT_OR_PACKAGE_CONFLICT' END;
