-- release-target-preflight: migrationId=20260813_z_form_center_menu_hide; allowedEnvironments=test,prod,backup
-- Reviewed identity, parent/permission and package-scope prerequisites from this migration.
SELECT CASE WHEN
 EXISTS(SELECT 1 FROM system_menu WHERE id=605071200 AND deleted=b'0' AND parent_id=990200 AND path='form-center' AND type=1)
 AND EXISTS(SELECT 1 FROM system_menu WHERE id=605071220 AND deleted=b'0' AND permission='form:effect:query' AND type=2 AND parent_id=605071200 AND path='effect' AND component='form-center/effect/index' AND component_name='FormCenterEffect')
 AND EXISTS(SELECT 1 FROM system_menu WHERE id=605071221 AND deleted=b'0' AND permission='form:effect:retry' AND type=3 AND parent_id=605071220)
 AND EXISTS(SELECT 1 FROM system_menu WHERE id=605071201 AND deleted=b'0' AND parent_id=990200 AND sort=29 AND path='form-center/template' AND permission='form:template:query' AND component='form-center/template/index' AND component_name='FormCenterTemplate' AND visible=b'1' AND status=0)
 THEN 'TARGET_PREFLIGHT_PASS:20260813_z_form_center_menu_hide' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260813_z_form_center_menu_hide:MENU_IDENTITY_PARENT_OR_PACKAGE_CONFLICT' END;
