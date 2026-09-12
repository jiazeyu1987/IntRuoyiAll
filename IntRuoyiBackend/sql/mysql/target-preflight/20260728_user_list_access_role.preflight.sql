-- release-target-preflight: migrationId=20260728_user_list_access_role; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND LOWER(table_name) IN ('system_menu','system_role','system_role_category','system_role_menu')) = 4
THEN 'TARGET_PREFLIGHT_PASS:20260728_user_list_access_role' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260728_user_list_access_role' END;
