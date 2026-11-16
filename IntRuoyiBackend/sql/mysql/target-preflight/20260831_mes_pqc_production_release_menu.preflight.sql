-- release-target-preflight: migrationId=20260831_mes_pqc_production_release_menu; allowedEnvironments=test,prod,backup
-- Reviewed identity, parent/permission and package-scope prerequisites from this migration.
SELECT CASE WHEN
 EXISTS(SELECT 1 FROM system_menu WHERE id=900220 AND deleted=b'0' AND type=1)
 AND (SELECT COUNT(*) FROM system_menu WHERE permission='mes:pro-production-release:query' AND deleted=b'0')=1
 AND NOT EXISTS(SELECT 1 FROM system_tenant_package WHERE deleted=b'0' AND NOT JSON_VALID(menu_ids))
 THEN 'TARGET_PREFLIGHT_PASS:20260831_mes_pqc_production_release_menu' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260831_mes_pqc_production_release_menu:MENU_IDENTITY_PARENT_OR_PACKAGE_CONFLICT' END;
