-- release-target-preflight: migrationId=20260903_dcc_registration_certificate_upload_action_permissions; allowedEnvironments=test,prod,backup
-- Exact existing menu/role/user identities; never creates substitute accounts or grants.
SELECT CASE WHEN
 EXISTS(SELECT 1 FROM system_role WHERE code='dcc_registration_certificate_upload' AND status=0 AND deleted=b'0')
 AND NOT EXISTS(SELECT 1 FROM system_role WHERE code='dcc_registration_certificate_upload' AND status=0 AND deleted=b'0' GROUP BY tenant_id HAVING COUNT(*)<>1)
 AND (SELECT COUNT(DISTINCT permission) FROM system_menu WHERE permission IN('dcc:registration-certificate:upload:create','dcc:registration-certificate:renewal:upload','dcc:registration-certificate:change:submit','dcc:project-code:query') AND status=0 AND deleted=b'0')=4
 AND (SELECT COUNT(*) FROM system_menu WHERE permission IN('dcc:registration-certificate:upload:create','dcc:registration-certificate:renewal:upload','dcc:registration-certificate:change:submit','dcc:project-code:query') AND status=0 AND deleted=b'0')=4
 THEN 'TARGET_PREFLIGHT_PASS:20260903_dcc_registration_certificate_upload_action_permissions' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260903_dcc_registration_certificate_upload_action_permissions:MISSING_AMBIGUOUS_OR_CROSS_TENANT_IDENTITY' END;
