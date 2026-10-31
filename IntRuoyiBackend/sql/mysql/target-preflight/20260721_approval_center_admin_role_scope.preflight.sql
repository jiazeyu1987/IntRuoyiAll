-- release-target-preflight: migrationId=20260721_approval_center_admin_role_scope; allowedEnvironments=test,prod,backup
-- Read-only identity/scope checks; this check never grants permissions or modifies accounts.
SELECT CASE WHEN
 (SELECT COUNT(*) FROM system_users WHERE tenant_id=1 AND username='admin' AND status=0 AND deleted=b'0')=1
 AND (SELECT COUNT(*) FROM system_role WHERE tenant_id=1 AND code='approval_admin' AND status=0 AND deleted=b'0')=1
 AND NOT EXISTS(SELECT 1 FROM system_user_role ur JOIN system_users u ON u.id=ur.user_id AND u.tenant_id=1 AND u.username='admin' JOIN system_role r ON r.id=ur.role_id AND r.tenant_id=1 AND r.code='approval_admin' GROUP BY ur.user_id,ur.role_id HAVING COUNT(*)>1)
 AND NOT EXISTS(SELECT 1 FROM system_user_role ur JOIN system_users u ON u.id=ur.user_id AND u.tenant_id=1 AND u.username='admin' JOIN system_role r ON r.id=ur.role_id AND r.tenant_id=1 AND r.code='approval_admin' WHERE ur.tenant_id<>1)
 THEN 'TARGET_PREFLIGHT_PASS:20260721_approval_center_admin_role_scope' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260721_approval_center_admin_role_scope:ROLE_PERMISSION_POLICY_OR_TENANT_IDENTITY_CONFLICT' END;
