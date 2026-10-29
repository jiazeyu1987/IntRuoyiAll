-- release-target-preflight: migrationId=20260903_dcc_registration_certificate_upload_role_category; allowedEnvironments=test,backup,prod
-- Read-only data prerequisites; evaluated immediately before this migration.
WITH target AS(SELECT DISTINCT tenant_id FROM system_role WHERE code='dcc_registration_certificate_approver' AND status=0 AND deleted=0)
SELECT CASE WHEN
  (EXISTS(SELECT 1 FROM target))
  AND (NOT EXISTS(SELECT 1 FROM system_role r JOIN target t ON t.tenant_id=r.tenant_id WHERE r.code IN('dcc_registration_certificate_upload','dcc_registration_certificate_approver') GROUP BY r.tenant_id,r.code HAVING COUNT(*)>1))
  AND (NOT EXISTS(SELECT 1 FROM system_role_category c JOIN target t ON t.tenant_id=c.tenant_id WHERE c.code='registration' GROUP BY c.tenant_id HAVING COUNT(*)>1))
  AND ((SELECT COUNT(*) FROM system_menu WHERE permission='dcc:registration-certificate:upload:create' AND status=0 AND deleted=0)=1)
  AND (NOT EXISTS(SELECT 1 FROM system_role_menu rm JOIN system_role r ON r.id=rm.role_id JOIN target t ON t.tenant_id=r.tenant_id JOIN system_menu m ON m.id=rm.menu_id AND m.permission='dcc:registration-certificate:upload:create' AND m.status=0 AND m.deleted=0 WHERE r.code='dcc_registration_certificate_upload' GROUP BY rm.role_id,rm.menu_id,rm.tenant_id HAVING COUNT(*)>1))
  AND (NOT EXISTS(SELECT 1 FROM system_role_menu rm JOIN system_role r ON r.id=rm.role_id AND r.code='dcc_registration_certificate_upload' WHERE rm.tenant_id<>r.tenant_id))
THEN 'TARGET_PREFLIGHT_PASS:20260903_dcc_registration_certificate_upload_role_category' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260903_dcc_registration_certificate_upload_role_category' END;
