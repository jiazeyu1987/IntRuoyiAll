-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; supersedes=20260624_dcc_view_matrix_test_tenant_prereq,20260807_test_tenant1_all_role_permission_sync; type=data; riskLevel=low; requiresTargetPreflight=true
-- Retire one-off test account/dept installation and tenant-1 test role-template synchronizers from application upgrades.
-- Existing protected matrix fixtures, signature credentials, tenant-1 roles and permissions must remain untouched.
-- This marker verifies the authorized test identity; it does not claim the old seed ran.
DELIMITER $$
CREATE PROCEDURE release_20261022_retire_test_initializers()
BEGIN
 IF NOT EXISTS(SELECT 1 FROM system_tenant WHERE id=122 AND name=CONVERT(UNHEX('E6B58BE8AF95E7A79FE688B7') USING utf8mb4) COLLATE utf8mb4_unicode_ci AND deleted=b'0')
 OR (SELECT COUNT(*) FROM system_users WHERE tenant_id=122 AND username='aoteman' AND deleted=b'0')<>1 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='AUTHORIZED_TEST_TENANT_122_AOTEMAN_IDENTITY_MISSING';
 END IF;
END$$
DELIMITER ;
CALL release_20261022_retire_test_initializers();
DROP PROCEDURE release_20261022_retire_test_initializers;
