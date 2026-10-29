-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; supersedes=20260618_post_release_role_e2e_gate,20260618_post_release_role_e2e_gate_password_fix,20260618_post_release_role_e2e_gate_smoke_contract,20260619_post_release_role_e2e_gate_smoke_admin_resource_capacity_fix,20260619_post_release_role_e2e_gate_smoke_route_900026_line_fix,20260619_post_release_role_e2e_gate_smoke_username_fix,20260619_post_release_role_e2e_gate_smoke_username_password_freshness_fix,20260619_post_release_role_e2e_gate_smoke_z_erp_job_permission_fix,20260902_erp_finance_invoice_voucher_print_test_server_user_permission,20260903_dcc_registration_certificate_pengyongli_upload_role,20260903_dcc_registration_certificate_wanglixuan_upload_role; type=data; riskLevel=low; requiresTargetPreflight=true
-- Explicit retirement of one-off historical E2E fixtures, across all environments.
-- Do not create fixture users, reset credentials, broaden role data scopes or revive menus.
-- The replacement verifies only the authorized tenant/account identity, never passwords.
DELIMITER $$
CREATE PROCEDURE release_20261022_retire_e2e_fixtures()
BEGIN
 IF NOT EXISTS(SELECT 1 FROM system_tenant WHERE id=122 AND name=CONVERT(UNHEX('E6B58BE8AF95E7A79FE688B7') USING utf8mb4) COLLATE utf8mb4_unicode_ci AND deleted=b'0')
 OR (SELECT COUNT(*) FROM system_users WHERE tenant_id=122 AND username='aoteman' AND deleted=b'0')<>1 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='AUTHORIZED_TEST_TENANT_122_AOTEMAN_IDENTITY_MISSING';
 END IF;
END$$
DELIMITER ;
CALL release_20261022_retire_e2e_fixtures();
DROP PROCEDURE release_20261022_retire_e2e_fixtures;
