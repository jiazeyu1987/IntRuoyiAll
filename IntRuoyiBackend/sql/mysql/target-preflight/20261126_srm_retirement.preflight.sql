-- release-target-preflight: migrationId=20261126_srm_retirement; allowedEnvironments=test,backup,prod
-- SRM authorization is selected by its existing paths/permissions, without fixed site IDs.
SELECT CASE
  WHEN (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
    AND TABLE_NAME IN ('system_menu','system_role_menu','system_tenant_package','system_role','system_role_category'))<>5
    THEN 'TARGET_PREFLIGHT_BLOCKED:20261126_srm_retirement:AUTHORIZATION_TABLES'
  WHEN EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted=0
    AND CASE WHEN JSON_VALID(menu_ids) THEN JSON_TYPE(menu_ids)<>'ARRAY' ELSE TRUE END)
    THEN 'TARGET_PREFLIGHT_BLOCKED:20261126_srm_retirement:PACKAGE_MENU_JSON'
  ELSE 'TARGET_PREFLIGHT_PASS:20261126_srm_retirement'
END;
