-- release-target-preflight: migrationId=20260708_mes_scheduler_edhr_route_900025_route_edit_permission; allowedEnvironments=test,prod,backup
-- Read-only identity/scope checks; this check never grants permissions or modifies accounts.
SELECT CASE WHEN
 EXISTS(SELECT 1 FROM mes_pro_route t JOIN system_role r ON r.tenant_id=t.tenant_id AND r.status=0 AND r.deleted=b'0' AND (r.code='mes_scheduler' OR r.name=CONVERT(UNHEX('E68E92E4BAA7E59198') USING utf8mb4) COLLATE utf8mb4_unicode_ci) WHERE t.id=900025 AND t.deleted=b'0')
 AND NOT EXISTS(SELECT 1 FROM mes_pro_edhr_permission_scope s LEFT JOIN mes_pro_route r ON r.id=900025 AND r.tenant_id=s.tenant_id AND r.deleted=b'0' WHERE s.object_type='ROUTE' AND s.object_id='900025' AND s.deleted=b'0' AND r.id IS NULL)
 AND NOT EXISTS(SELECT 1 FROM mes_pro_edhr_permission_scope WHERE object_type='ROUTE' AND object_id='900025' AND deleted=b'0' GROUP BY tenant_id HAVING COUNT(*)>1)
 AND NOT EXISTS(SELECT 1 FROM mes_pro_edhr_permission_rule WHERE subject_type='ROLE' AND ability='ROUTE_EDIT' AND deleted=b'0' GROUP BY tenant_id,scope_id,subject_id,ability HAVING COUNT(*)>1)
 THEN 'TARGET_PREFLIGHT_PASS:20260708_mes_scheduler_edhr_route_900025_route_edit_permission' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260708_mes_scheduler_edhr_route_900025_route_edit_permission:ROLE_PERMISSION_POLICY_OR_TENANT_IDENTITY_CONFLICT' END;
