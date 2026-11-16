-- release-target-preflight: migrationId=20260903_dcc_registration_certificate_upload_hidden_routes; allowedEnvironments=test,backup,prod
-- Read-only data prerequisites; evaluated immediately before this migration.
WITH defs(id,path,component,route) AS(
 SELECT 990231,'/mdm/registration-certificate/detail/:id' COLLATE utf8mb4_unicode_ci,'dcc/registration-certificate/detail/index' COLLATE utf8mb4_unicode_ci,'DccRegistrationCertificateDetail' COLLATE utf8mb4_unicode_ci
 UNION ALL SELECT 990232,'/mdm/registration-certificate/history/:id','dcc/registration-certificate/history/index','DccRegistrationCertificateHistory'),
 roles AS(SELECT id,tenant_id FROM system_role WHERE code='dcc_registration_certificate_upload' AND status=0 AND deleted=0)
SELECT CASE WHEN
  (EXISTS(SELECT 1 FROM roles))
  AND (NOT EXISTS(SELECT 1 FROM roles GROUP BY tenant_id HAVING COUNT(*)<>1))
  AND (NOT EXISTS(SELECT 1 FROM defs d LEFT JOIN system_menu m ON m.id=d.id AND BINARY m.path=BINARY d.path AND BINARY m.component=BINARY d.component AND BINARY m.component_name=BINARY d.route AND m.permission='dcc:registration-certificate:query-current' AND m.type=2 AND m.visible=0 AND m.status=0 AND m.deleted=0 WHERE m.id IS NULL))
  AND (NOT EXISTS(SELECT 1 FROM system_role_menu rm JOIN roles r ON r.id=rm.role_id WHERE rm.menu_id IN(990231,990232) AND rm.tenant_id<>r.tenant_id))
THEN 'TARGET_PREFLIGHT_PASS:20260903_dcc_registration_certificate_upload_hidden_routes' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260903_dcc_registration_certificate_upload_hidden_routes' END;
