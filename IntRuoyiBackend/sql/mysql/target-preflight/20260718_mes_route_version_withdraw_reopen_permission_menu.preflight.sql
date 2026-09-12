-- release-target-preflight: migrationId=20260718_mes_route_version_withdraw_reopen_permission_menu; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND LOWER(table_name) IN ('system_menu','system_role_menu','system_tenant_package')) = 3
  AND NOT EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted = b'0' AND menu_ids IS NOT NULL AND NOT JSON_VALID(menu_ids))
THEN 'TARGET_PREFLIGHT_PASS:20260718_mes_route_version_withdraw_reopen_permission_menu' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260718_mes_route_version_withdraw_reopen_permission_menu' END;
