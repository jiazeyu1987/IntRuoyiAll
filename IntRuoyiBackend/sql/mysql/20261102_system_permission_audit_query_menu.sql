-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260626_role_management_split_rename_navigation; type=permission; riskLevel=medium
-- 将已有统一审计查询权限注册为权限角色页的内部查询按钮。
-- 仅授权既有启用超级管理员/租户管理员，普通角色须由管理员另行显式授权。
-- 仅向已包含权限角色页的套餐增加此按钮；系统套餐 0 按既有全量菜单规则处理。
-- 不启用审计、不改写审计事件或业务数据。回滚时仅撤销本按钮的角色/套餐授权。
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS ensure_system_permission_audit_query_menu;
DELIMITER $$
CREATE PROCEDURE ensure_system_permission_audit_query_menu()
BEGIN
  DECLARE audit_menu_id BIGINT DEFAULT NULL;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
  START TRANSACTION;
  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE id=900183 AND parent_id=101
      AND component='system/role/index' AND status=0 AND deleted=b'0') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing exact permission-role parent menu 900183';
  END IF;
  IF (SELECT COUNT(*) FROM system_menu WHERE permission='system:gxp-audit:query')>1
      OR EXISTS (SELECT 1 FROM system_menu WHERE permission='system:gxp-audit:query'
        AND (parent_id<>900183 OR type<>3 OR status<>0 OR deleted<>b'0')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Existing GxP audit query menu conflicts with reviewed contract';
  END IF;
  SELECT MAX(id) INTO audit_menu_id FROM system_menu WHERE permission='system:gxp-audit:query';
  IF audit_menu_id IS NULL THEN
    INSERT INTO system_menu
      (name,permission,type,sort,parent_id,path,icon,component,component_name,status,
       visible,keep_alive,always_show,creator,create_time,updater,update_time,deleted)
    VALUES ('权限审计查询','system:gxp-audit:query',3,90,900183,'','','','',0,
       b'1',b'1',b'1','permission-audit',NOW(),'permission-audit',NOW(),b'0');
    SET audit_menu_id=LAST_INSERT_ID();
  END IF;

  UPDATE system_tenant_package
     SET menu_ids=JSON_ARRAY_APPEND(menu_ids,'$',audit_menu_id),
         updater='permission-audit',update_time=NOW()
   WHERE deleted=b'0' AND JSON_VALID(menu_ids)
     AND JSON_CONTAINS(menu_ids,CAST('900183' AS JSON),'$')
     AND NOT JSON_CONTAINS(menu_ids,CAST(audit_menu_id AS JSON),'$');

  INSERT INTO system_role_menu
    (role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
  SELECT r.id,audit_menu_id,'permission-audit',NOW(),'permission-audit',NOW(),b'0',r.tenant_id
    FROM system_role r
    JOIN system_tenant t ON t.id=r.tenant_id AND t.deleted=b'0' AND t.status=0
   WHERE r.deleted=b'0' AND r.status=0 AND r.code IN ('super_admin','tenant_admin')
     AND (t.package_id=0 OR EXISTS (
       SELECT 1 FROM system_tenant_package p WHERE p.id=t.package_id
         AND p.deleted=b'0' AND JSON_VALID(p.menu_ids)
         AND JSON_CONTAINS(p.menu_ids,CAST(audit_menu_id AS JSON),'$')))
     AND NOT EXISTS (SELECT 1 FROM system_role_menu existing
       WHERE existing.tenant_id=r.tenant_id AND existing.role_id=r.id
         AND existing.menu_id=audit_menu_id AND existing.deleted=b'0');
  COMMIT;
END$$
DELIMITER ;
CALL ensure_system_permission_audit_query_menu();
DROP PROCEDURE ensure_system_permission_audit_query_menu;
