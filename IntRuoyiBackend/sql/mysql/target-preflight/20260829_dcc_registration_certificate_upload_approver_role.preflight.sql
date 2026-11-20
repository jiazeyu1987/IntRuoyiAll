-- release-target-preflight: migrationId=20260829_dcc_registration_certificate_upload_approver_role; allowedEnvironments=test,backup,prod
-- Read-only data prerequisites; evaluated immediately before this migration.
WITH required_permission(permission) AS (
 SELECT 'dcc:registration-certificate:upload:approve' COLLATE utf8mb4_unicode_ci UNION ALL SELECT 'bpm:task:query' UNION ALL SELECT 'bpm:process-instance:query'),
 initial_users AS (SELECT DISTINCT u.id,u.tenant_id FROM system_users u
 JOIN system_dept d ON d.id=u.dept_id AND d.tenant_id=u.tenant_id AND d.status=0 AND d.deleted=0
 JOIN system_user_role ur ON ur.user_id=u.id AND ur.tenant_id=u.tenant_id AND ur.deleted=0
 JOIN system_role r ON r.id=ur.role_id AND r.tenant_id=ur.tenant_id AND r.code='approval_center_entry' AND r.status=0 AND r.deleted=0
 WHERE u.status=0 AND u.deleted=0 AND u.username<>'admin' AND d.name COLLATE utf8mb4_unicode_ci='注册部' COLLATE utf8mb4_unicode_ci),
 target AS(SELECT DISTINCT tenant_id FROM initial_users)
SELECT CASE WHEN
  (NOT EXISTS(SELECT 1 FROM required_permission p LEFT JOIN system_menu m ON m.permission=p.permission AND m.status=0 AND m.deleted=0 GROUP BY p.permission HAVING COUNT(m.id)<>1))
  AND (EXISTS(SELECT 1 FROM initial_users))
  AND (NOT EXISTS(SELECT 1 FROM target t LEFT JOIN system_role_category c ON c.tenant_id=t.tenant_id AND c.code='dcc' AND c.status=0 AND c.deleted=0 GROUP BY t.tenant_id HAVING COUNT(c.id)<>1))
  AND (NOT EXISTS(SELECT 1 FROM system_role r JOIN target t ON t.tenant_id=r.tenant_id WHERE r.code='dcc_registration_certificate_approver' GROUP BY r.tenant_id HAVING COUNT(*)>1))
  AND (NOT EXISTS(SELECT 1 FROM system_user_role ur JOIN initial_users u ON u.id=ur.user_id JOIN system_role r ON r.id=ur.role_id AND r.code='dcc_registration_certificate_approver' WHERE ur.tenant_id<>u.tenant_id OR r.tenant_id<>u.tenant_id))
THEN 'TARGET_PREFLIGHT_PASS:20260829_dcc_registration_certificate_upload_approver_role' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260829_dcc_registration_certificate_upload_approver_role' END;
