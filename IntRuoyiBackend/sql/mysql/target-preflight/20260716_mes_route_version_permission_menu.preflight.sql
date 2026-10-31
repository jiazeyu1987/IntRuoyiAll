-- release-target-preflight: migrationId=20260716_mes_route_version_permission_menu; allowedEnvironments=test,prod,backup
-- Reviewed identity, parent/permission and package-scope prerequisites from this migration.
SELECT CASE WHEN
 EXISTS(SELECT 1 FROM system_menu WHERE id=5720 AND deleted=b'0')
 AND NOT EXISTS(SELECT 1 FROM system_tenant_package WHERE deleted=b'0' AND NOT JSON_VALID(menu_ids))
 AND NOT EXISTS(SELECT 1 FROM system_menu WHERE id BETWEEN 5730 AND 5734 AND (permission<>CASE id WHEN 5730 THEN 'mes:pro-route:version-query' WHEN 5731 THEN 'mes:pro-route:version-create' WHEN 5732 THEN 'mes:pro-route:version-submit' WHEN 5733 THEN 'mes:pro-route:version-cancel' WHEN 5734 THEN 'mes:pro-route:version-publish' END OR parent_id<>5720))
 AND NOT EXISTS(SELECT 1 FROM system_menu WHERE deleted=b'0' AND permission IN('mes:pro-route:version-query','mes:pro-route:version-create','mes:pro-route:version-submit','mes:pro-route:version-cancel','mes:pro-route:version-publish') AND id NOT BETWEEN 5730 AND 5734)
 THEN 'TARGET_PREFLIGHT_PASS:20260716_mes_route_version_permission_menu' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260716_mes_route_version_permission_menu:MENU_IDENTITY_PARENT_OR_PACKAGE_CONFLICT' END;
