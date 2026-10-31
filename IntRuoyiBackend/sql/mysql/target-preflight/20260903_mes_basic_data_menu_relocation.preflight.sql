-- release-target-preflight: migrationId=20260903_mes_basic_data_menu_relocation; allowedEnvironments=test,prod,backup
-- Reviewed identity, parent/permission and package-scope prerequisites from this migration.
SELECT CASE WHEN
 EXISTS(SELECT 1 FROM system_menu WHERE id=5101 AND deleted=b'0')
 AND EXISTS(SELECT 1 FROM system_menu WHERE id=990210 AND deleted=b'0' AND type=2 AND permission='dcc:project-code:query')
 AND EXISTS(SELECT 1 FROM system_menu WHERE id=990201 AND deleted=b'0' AND type=2 AND permission='mdm:product:query')
 AND NOT EXISTS(SELECT 1 FROM system_menu WHERE parent_id=5101 AND deleted=b'0' AND ((path='dcc-project-code' AND id<>990210) OR(path='showroom-product' AND id<>990201)))
 THEN 'TARGET_PREFLIGHT_PASS:20260903_mes_basic_data_menu_relocation' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260903_mes_basic_data_menu_relocation:MENU_IDENTITY_PARENT_OR_PACKAGE_CONFLICT' END;
