-- release-target-preflight: migrationId=20260903_dcc_registration_certificate_download_role_permission; allowedEnvironments=test,prod,backup
-- Read-only identity/scope checks; this check never grants permissions or modifies accounts.
SELECT CASE WHEN
 EXISTS(SELECT 1 FROM system_menu WHERE permission='dcc:registration-certificate:access-request:create' AND status=0 AND deleted=b'0')
 AND EXISTS(SELECT 1 FROM system_role WHERE code='dcc_registration_certificate_approver' AND status=0 AND deleted=b'0')
 AND NOT EXISTS(SELECT 1 FROM system_role WHERE code='dcc_registration_certificate_approver' AND status=0 AND deleted=b'0' GROUP BY tenant_id HAVING COUNT(*)>1)
 THEN 'TARGET_PREFLIGHT_PASS:20260903_dcc_registration_certificate_download_role_permission' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260903_dcc_registration_certificate_download_role_permission:ROLE_PERMISSION_POLICY_OR_TENANT_IDENTITY_CONFLICT' END;
