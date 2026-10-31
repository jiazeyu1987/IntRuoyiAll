-- release-target-preflight: migrationId=20260730_signature_regular_users_my_signature_only; allowedEnvironments=test,prod,backup
-- Read-only identity/scope checks; this check never grants permissions or modifies accounts.
SELECT CASE WHEN
 EXISTS(SELECT 1 FROM system_menu WHERE id=900218 AND deleted=b'0' AND path='/signature-governance' AND status=0)
 AND EXISTS(SELECT 1 FROM system_menu WHERE id=900418 AND deleted=b'0' AND path='my-signature' AND status=0)
 AND EXISTS(SELECT 1 FROM system_menu WHERE id=900411 AND deleted=b'0' AND path='signature-records' AND status=0)
 AND NOT EXISTS(SELECT 1 FROM system_role_menu WHERE menu_id IN(900218,900418) GROUP BY tenant_id,role_id,menu_id HAVING COUNT(*)>1)
 THEN 'TARGET_PREFLIGHT_PASS:20260730_signature_regular_users_my_signature_only' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260730_signature_regular_users_my_signature_only:ROLE_PERMISSION_POLICY_OR_TENANT_IDENTITY_CONFLICT' END;
