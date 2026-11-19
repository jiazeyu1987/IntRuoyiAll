-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260626_role_management_split_rename_navigation; type=permission; riskLevel=medium
-- 设备三表导出依赖车间导出权限；补齐已有后端权限对应的菜单定义。
-- 仅扩展已包含设备导出和车间设置的套餐，并授权其中已启用的管理员。
-- 普通角色仍需显式授权；不撤销后端车间导出权限检查。
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS ensure_mes_workshop_export_permission;
DELIMITER $$
CREATE PROCEDURE ensure_mes_workshop_export_permission()
BEGIN
  DECLARE export_menu_id BIGINT DEFAULT NULL;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
  START TRANSACTION;
  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE id=5160
      AND component='mes/md/workstation/workshop/index' AND status=0 AND deleted=b'0')
      OR NOT EXISTS (SELECT 1 FROM system_menu WHERE id=5325
      AND permission='mes:dv-machinery:export' AND status=0 AND deleted=b'0') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing workshop page or machinery export permission';
  END IF;
  IF (SELECT COUNT(*) FROM system_menu WHERE permission='mes:md-workshop:export')>1
      OR EXISTS (SELECT 1 FROM system_menu WHERE permission='mes:md-workshop:export'
        AND (parent_id<>5160 OR type<>3 OR status<>0 OR deleted<>b'0')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Workshop export permission conflicts with reviewed contract';
  END IF;
  SELECT MAX(id) INTO export_menu_id FROM system_menu WHERE permission='mes:md-workshop:export';
  IF export_menu_id IS NULL THEN
    INSERT INTO system_menu
      (name,permission,type,sort,parent_id,path,icon,component,component_name,status,
       visible,keep_alive,always_show,creator,create_time,updater,update_time,deleted)
    VALUES ('车间导出','mes:md-workshop:export',3,5,5160,'','','','',0,
       b'1',b'1',b'1','workshop-export',NOW(),'workshop-export',NOW(),b'0');
    SET export_menu_id=LAST_INSERT_ID();
  END IF;
  IF EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted=b'0' AND NOT JSON_VALID(menu_ids)) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Invalid tenant package menu_ids JSON';
  END IF;
  UPDATE system_tenant_package
     SET menu_ids=JSON_ARRAY_APPEND(menu_ids,'$',export_menu_id),
         updater='workshop-export',update_time=NOW()
   WHERE deleted=b'0' AND JSON_CONTAINS(menu_ids,CAST('5160' AS JSON),'$')
     AND JSON_CONTAINS(menu_ids,CAST('5325' AS JSON),'$')
     AND NOT JSON_CONTAINS(menu_ids,CAST(export_menu_id AS JSON),'$');
  INSERT INTO system_role_menu
    (role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
  SELECT r.id,export_menu_id,'workshop-export',NOW(),'workshop-export',NOW(),b'0',r.tenant_id
    FROM system_role r
    JOIN system_tenant t ON t.id=r.tenant_id AND t.deleted=b'0' AND t.status=0
   WHERE r.deleted=b'0' AND r.status=0 AND r.code IN ('super_admin','tenant_admin')
     AND (t.package_id=0 OR EXISTS (
       SELECT 1 FROM system_tenant_package p WHERE p.id=t.package_id AND p.deleted=b'0'
         AND JSON_CONTAINS(p.menu_ids,CAST(export_menu_id AS JSON),'$')))
     AND NOT EXISTS (SELECT 1 FROM system_role_menu existing
       WHERE existing.tenant_id=r.tenant_id AND existing.role_id=r.id
         AND existing.menu_id=export_menu_id AND existing.deleted=b'0');
  COMMIT;
END$$
DELIMITER ;
CALL ensure_mes_workshop_export_permission();
DROP PROCEDURE ensure_mes_workshop_export_permission;
