-- release-target-preflight: migrationId=20260807_test_tenant1_all_role_permission_sync; allowedEnvironments=test
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND LOWER(table_name) IN ('system_menu','system_role','system_role_category','system_role_menu')) = 4
  AND (SELECT COUNT(*) FROM system_role WHERE tenant_id = 1 AND deleted = b'0') >= 1
THEN 'TARGET_PREFLIGHT_PASS:20260807_test_tenant1_all_role_permission_sync' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260807_test_tenant1_all_role_permission_sync' END;
