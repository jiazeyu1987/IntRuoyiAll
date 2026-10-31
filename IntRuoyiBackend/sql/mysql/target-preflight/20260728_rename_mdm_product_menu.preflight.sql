-- release-target-preflight: migrationId=20260728_rename_mdm_product_menu; allowedEnvironments=test,prod,backup
-- Reviewed identity, parent/permission and package-scope prerequisites from this migration.
SELECT CASE WHEN
 EXISTS(SELECT 1 FROM system_menu WHERE id=990201 AND deleted=b'0' AND permission='mdm:product:query' AND type=2)
 THEN 'TARGET_PREFLIGHT_PASS:20260728_rename_mdm_product_menu' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260728_rename_mdm_product_menu:MENU_IDENTITY_PARENT_OR_PACKAGE_CONFLICT' END;
