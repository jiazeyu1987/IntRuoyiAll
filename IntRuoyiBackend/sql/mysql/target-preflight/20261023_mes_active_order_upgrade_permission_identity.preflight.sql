-- release-target-preflight: migrationId=20261023_mes_active_order_upgrade_permission_identity; allowedEnvironments=test,backup,prod
SELECT CASE WHEN ((SELECT COUNT(*) FROM system_menu WHERE id=900436 AND deleted=0 AND status=0 AND type=2
 AND permission='mes:pro-process-pool-team-leader:query'
 AND path='/mes/pro/process-pool/production-leader'
 AND component='mes/pro/processpool/ProductionLeaderWorkbenchPage')=1 AND NOT EXISTS(SELECT 1 FROM system_menu WHERE (id=900452 OR permission='mes:pro-process-pool-team-leader:version-upgrade')
 AND NOT(id=900452 AND permission='mes:pro-process-pool-team-leader:version-upgrade' AND type=3 AND deleted=0 AND status=0 AND parent_id IN(900310,900436)))
 AND NOT EXISTS(SELECT 1 FROM system_role_menu rm WHERE rm.menu_id=900452 AND rm.deleted=0 AND NOT EXISTS(SELECT 1 FROM system_role r WHERE r.id=rm.role_id AND r.tenant_id=rm.tenant_id))
 AND NOT EXISTS(SELECT 1 FROM system_role_menu WHERE menu_id=900452 AND NOT EXISTS(SELECT 1 FROM system_menu WHERE id=900452)) AND EXISTS(SELECT 1 FROM act_re_procdef WHERE KEY_='mes-active-order-version-upgrade-v1')
 AND NOT EXISTS(SELECT 1 FROM bpm_business_approval_policy WHERE deleted=0 AND data_domain='MES' AND system_code='MES' AND object_type='MES_ACTIVE_ORDER' AND action_code='VERSION_UPGRADE_RESTART' AND object_state='VERSION_UPGRADE_PENDING' AND status='PUBLISHED' AND COALESCE(effect_executor_code,'')<>'MES_ACTIVE_ORDER_VERSION_UPGRADE_RESTART')
 AND NOT EXISTS(SELECT tenant_id FROM bpm_business_approval_policy WHERE deleted=0 AND data_domain='MES' AND system_code='MES' AND object_type='MES_ACTIVE_ORDER' AND action_code='VERSION_UPGRADE_RESTART' AND object_state='VERSION_UPGRADE_PENDING' AND status='PUBLISHED' GROUP BY tenant_id HAVING COUNT(*)>1)) AND NOT EXISTS (SELECT 1 FROM information_schema.routines WHERE routine_schema=DATABASE() AND routine_name='ensure_mes_upgrade_permission_identity_20261023')
THEN 'TARGET_PREFLIGHT_PASS:20261023_mes_active_order_upgrade_permission_identity' ELSE 'TARGET_PREFLIGHT_BLOCKED:20261023_mes_active_order_upgrade_permission_identity:PREREQUISITE_OR_IDENTITY_CONFLICT' END;
