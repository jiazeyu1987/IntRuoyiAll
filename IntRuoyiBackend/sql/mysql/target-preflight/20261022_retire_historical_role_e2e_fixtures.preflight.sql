-- release-target-preflight: migrationId=20261022_retire_historical_role_e2e_fixtures; allowedEnvironments=test,prod,backup
SELECT CASE WHEN
 EXISTS(SELECT 1 FROM system_tenant WHERE id=122 AND name=CONVERT(UNHEX('E6B58BE8AF95E7A79FE688B7') USING utf8mb4) COLLATE utf8mb4_unicode_ci AND deleted=b'0')
 AND (SELECT COUNT(*) FROM system_users WHERE tenant_id=122 AND username='aoteman' AND deleted=b'0')=1
 AND NOT EXISTS(SELECT 1 FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA=DATABASE() AND ROUTINE_NAME='release_20261022_retire_e2e_fixtures')
 THEN 'TARGET_PREFLIGHT_PASS:20261022_retire_historical_role_e2e_fixtures' ELSE 'TARGET_PREFLIGHT_BLOCKED:20261022_retire_historical_role_e2e_fixtures:AUTHORIZED_TEST_IDENTITY_OR_INTERRUPTED_OPERATION' END;
