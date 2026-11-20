-- release-target-preflight: migrationId=20260903_dcc_registration_certificate_upload_download_request_permission; allowedEnvironments=test,prod,backup
-- Exact existing menu/role/user identities; never creates substitute accounts or grants.
SELECT CASE WHEN
 EXISTS(SELECT 1 FROM system_role WHERE code='dcc_registration_certificate_upload' AND status=0 AND deleted=b'0')
 AND NOT EXISTS(SELECT 1 FROM system_role WHERE code='dcc_registration_certificate_upload' AND status=0 AND deleted=b'0' GROUP BY tenant_id HAVING COUNT(*)<>1)
 AND EXISTS(SELECT 1 FROM system_menu WHERE id=990237 AND permission='dcc:registration-certificate:access-request:create' AND status=0 AND deleted=b'0')
 THEN 'TARGET_PREFLIGHT_PASS:20260903_dcc_registration_certificate_upload_download_request_permission' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260903_dcc_registration_certificate_upload_download_request_permission:MISSING_AMBIGUOUS_OR_CROSS_TENANT_IDENTITY' END;
