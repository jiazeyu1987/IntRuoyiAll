-- release-target-preflight: migrationId=20261126_system_tenant_default_login; allowedEnvironments=test,backup,prod
SELECT CASE
  WHEN (SELECT COUNT(*) FROM system_tenant WHERE name='芋道源码' AND deleted=0 AND status=0)<>1
    THEN 'TARGET_PREFLIGHT_BLOCKED:20261126_system_tenant_default_login:INITIAL_TENANT'
  WHEN EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
    AND TABLE_NAME='system_tenant' AND COLUMN_NAME='default_login')
    THEN 'TARGET_PREFLIGHT_BLOCKED:20261126_system_tenant_default_login:COLUMN_ALREADY_EXISTS'
  ELSE 'TARGET_PREFLIGHT_PASS:20261126_system_tenant_default_login'
END;
