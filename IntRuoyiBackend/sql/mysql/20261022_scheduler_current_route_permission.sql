-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260709_mes_route_flow_config_unification,20260728_mes_scheduler_route_flow_list_permission; supersedes=20260708_mes_scheduler_process_route_save_permission; type=data; riskLevel=medium; requiresTargetPreflight=true
-- 900122 was retired by 07-09; its explicit successor is 5727, not a resurrected legacy permission.
-- Existing 07-28 handles 5723/5730. Preserve package scope; never enlarge packages here.
DELIMITER $$
CREATE PROCEDURE release_20261022_scheduler_permission()
BEGIN
 DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
 IF EXISTS(SELECT 1 FROM system_menu WHERE id=900122 AND deleted=b'0')
 OR EXISTS(SELECT 1 FROM system_role_menu WHERE menu_id=900122 AND deleted=b'0')
 OR NOT EXISTS(SELECT 1 FROM system_menu WHERE id=5727 AND permission='mes:pro-route:schedule-config:update' AND status=0 AND deleted=b'0')
 OR NOT EXISTS(SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='mes_pro_route_flow_config') THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='CURRENT_ROUTE_UNIFICATION_NOT_ESTABLISHED';
 END IF;
 IF NOT EXISTS(SELECT 1 FROM system_role WHERE tenant_id=1 AND code='mes_scheduler' AND status=0 AND deleted=b'0') THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='CURRENT_ROUTE_SCHEDULER_ROLE_MISSING';
 END IF;
 IF EXISTS(SELECT 1 FROM system_role r JOIN system_tenant t ON t.id=r.tenant_id AND t.deleted=b'0' JOIN system_tenant_package p ON p.id=t.package_id AND p.deleted=b'0' WHERE r.code='mes_scheduler' AND r.status=0 AND r.deleted=b'0' AND NOT JSON_VALID(p.menu_ids)) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='CURRENT_ROUTE_PACKAGE_MENU_JSON_INVALID';
 END IF;
 START TRANSACTION;
 UPDATE system_role_menu rm JOIN system_role r ON r.id=rm.role_id AND r.tenant_id=rm.tenant_id
 LEFT JOIN system_tenant t ON t.id=r.tenant_id AND t.deleted=b'0'
 LEFT JOIN system_tenant_package p ON p.id=t.package_id AND p.deleted=b'0'
 SET rm.deleted=b'0',rm.updater='release-20261022-route',rm.update_time=NOW()
 WHERE rm.menu_id=5727 AND rm.deleted=b'1' AND r.code='mes_scheduler' AND r.status=0 AND r.deleted=b'0'
 AND (r.tenant_id=1 OR JSON_CONTAINS(CAST(p.menu_ids AS JSON),'5727','$'));
 INSERT INTO system_role_menu(role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
 SELECT r.id,5727,'release-20261022-route',NOW(),'release-20261022-route',NOW(),b'0',r.tenant_id
 FROM system_role r LEFT JOIN system_tenant t ON t.id=r.tenant_id AND t.deleted=b'0'
 LEFT JOIN system_tenant_package p ON p.id=t.package_id AND p.deleted=b'0'
 WHERE r.code='mes_scheduler' AND r.status=0 AND r.deleted=b'0'
 AND (r.tenant_id=1 OR JSON_CONTAINS(CAST(p.menu_ids AS JSON),'5727','$'))
 AND NOT EXISTS(SELECT 1 FROM system_role_menu rm WHERE rm.role_id=r.id AND rm.tenant_id=r.tenant_id AND rm.menu_id=5727 AND rm.deleted=b'0');
 COMMIT;
END$$
DELIMITER ;
CALL release_20261022_scheduler_permission();
DROP PROCEDURE release_20261022_scheduler_permission;
