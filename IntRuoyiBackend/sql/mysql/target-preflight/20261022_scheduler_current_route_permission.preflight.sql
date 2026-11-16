-- release-target-preflight: migrationId=20261022_scheduler_current_route_permission; allowedEnvironments=test,prod,backup
SELECT CASE WHEN
 NOT EXISTS(SELECT 1 FROM system_menu WHERE id=900122 AND deleted=b'0')
 AND NOT EXISTS(SELECT 1 FROM system_role_menu WHERE menu_id=900122 AND deleted=b'0')
 AND EXISTS(SELECT 1 FROM system_menu WHERE id=5727 AND permission='mes:pro-route:schedule-config:update' AND status=0 AND deleted=b'0')
 AND EXISTS(SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='mes_pro_route_flow_config')
 AND EXISTS(SELECT 1 FROM system_role WHERE tenant_id=1 AND code='mes_scheduler' AND status=0 AND deleted=b'0')
 AND NOT EXISTS(SELECT 1 FROM system_role r JOIN system_tenant t ON t.id=r.tenant_id AND t.deleted=b'0' JOIN system_tenant_package p ON p.id=t.package_id AND p.deleted=b'0' WHERE r.code='mes_scheduler' AND r.status=0 AND r.deleted=b'0' AND NOT JSON_VALID(p.menu_ids))
 AND NOT EXISTS(SELECT 1 FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA=DATABASE() AND ROUTINE_NAME='release_20261022_scheduler_permission')
 THEN 'TARGET_PREFLIGHT_PASS:20261022_scheduler_current_route_permission' ELSE 'TARGET_PREFLIGHT_BLOCKED:20261022_scheduler_current_route_permission:UNIFICATION_OR_SCOPE_CONFLICT' END;
