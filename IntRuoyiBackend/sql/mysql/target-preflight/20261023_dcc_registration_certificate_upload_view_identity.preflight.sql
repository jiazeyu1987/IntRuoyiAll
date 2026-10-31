-- release-target-preflight: migrationId=20261023_dcc_registration_certificate_upload_view_identity; allowedEnvironments=test,prod,backup
-- Exact existing menu/role/user identities; never creates substitute accounts or grants.
SELECT CASE WHEN
 EXISTS(SELECT 1 FROM system_role WHERE code='dcc_registration_certificate_upload' AND status=0 AND deleted=b'0')
 AND NOT EXISTS(SELECT 1 FROM system_role WHERE code='dcc_registration_certificate_upload' AND status=0 AND deleted=b'0' GROUP BY tenant_id HAVING COUNT(*)<>1)
 AND EXISTS(SELECT 1 FROM system_menu WHERE id=991601 AND permission='dcc:registration-certificate:query-current' AND type=2 AND path='/mdm/registration-certificate' AND status=0 AND deleted=b'0')
 AND EXISTS(SELECT 1 FROM system_menu WHERE id=990253 AND type=1 AND parent_id=990200 AND path='registration-certificate-management' AND permission='' AND component IS NULL AND component_name IS NULL AND status=0 AND deleted=b'0')
 AND NOT EXISTS(SELECT 1 FROM system_role_menu rm JOIN system_role r ON r.id=rm.role_id AND r.tenant_id=rm.tenant_id WHERE r.code='dcc_registration_certificate_upload' AND r.status=0 AND r.deleted=b'0' AND rm.menu_id IN(991601,990253) GROUP BY rm.tenant_id,rm.role_id,rm.menu_id HAVING COUNT(*)>1)
 THEN 'TARGET_PREFLIGHT_PASS:20261023_dcc_registration_certificate_upload_view_identity' ELSE 'TARGET_PREFLIGHT_BLOCKED:20261023_dcc_registration_certificate_upload_view_identity:MISSING_AMBIGUOUS_OR_CROSS_TENANT_IDENTITY' END;
