-- release-target-preflight: migrationId=20261118_mes_route_batch_change_approval_read_permissions; allowedEnvironments=test,backup,prod
SELECT CASE
 WHEN NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='mes:pro-craft:query' AND deleted=b'0') OR NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='bpm:task:update' AND deleted=b'0') THEN 'TARGET_PREFLIGHT_BLOCKED:20261118_mes_route_batch_change_approval_read_permissions:READ_MENU_MISSING'
 WHEN EXISTS(SELECT 1 FROM system_tenant_package WHERE deleted=b'0' AND (menu_ids IS NULL OR NOT JSON_VALID(menu_ids))) THEN 'TARGET_PREFLIGHT_BLOCKED:20261118_mes_route_batch_change_approval_read_permissions:PACKAGE_JSON_INVALID'
 ELSE 'TARGET_PREFLIGHT_PASS:20261118_mes_route_batch_change_approval_read_permissions' END;
