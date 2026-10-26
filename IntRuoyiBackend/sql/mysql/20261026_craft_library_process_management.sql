-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261026_rename_process_management_directory,20261021_mes_craft_library; type=menu; riskLevel=low
-- Move the existing craft page; grant its new ancestors only to existing page holders.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS move_craft_to_process_management;
DELIMITER $$
CREATE PROCEDURE move_craft_to_process_management()
BEGIN
  DECLARE craft_menu BIGINT;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;
  START TRANSACTION;
  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE id=991514 AND parent_id=900220
      AND path='/mes/edhr-process-settings' AND type=1 AND status=0 AND deleted=b'0')
    OR NOT EXISTS (SELECT 1 FROM system_menu WHERE id=900220 AND parent_id=0
      AND path='/mes/edhr-batch-processing' AND type=1 AND status=0 AND deleted=b'0')
    OR (SELECT COUNT(*) FROM system_menu WHERE component='mes/pro/craft/index'
      AND permission='mes:pro-craft:query' AND type=2 AND status=0 AND deleted=b'0')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing or changed process directory or craft menu';
  END IF;
  SELECT id INTO craft_menu FROM system_menu WHERE component='mes/pro/craft/index'
    AND permission='mes:pro-craft:query' AND type=2 AND status=0 AND deleted=b'0';
  IF EXISTS (SELECT 1 FROM system_menu WHERE path='/mes/pro/craft' AND id<>craft_menu AND deleted=b'0') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Craft menu route is occupied';
  END IF;
  IF EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted=b'0'
      AND (NOT JSON_VALID(menu_ids) OR JSON_TYPE(CAST(menu_ids AS JSON))<>'ARRAY')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Invalid tenant package menu scope';
  END IF;
  UPDATE system_menu SET name='工序管理',updater='craft-process-menu',update_time=NOW()
    WHERE id=991514;
  UPDATE system_menu SET parent_id=991514,path='/mes/pro/craft',sort=50,
    updater='craft-process-menu',update_time=NOW() WHERE id=craft_menu;
  INSERT INTO system_role_menu (role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
  SELECT DISTINCT source.role_id,900220,'craft-process-menu',NOW(),'craft-process-menu',NOW(),b'0',source.tenant_id
  FROM system_role_menu source WHERE source.deleted=b'0' AND source.menu_id=craft_menu
    AND NOT EXISTS (SELECT 1 FROM system_role_menu target WHERE target.deleted=b'0'
      AND target.role_id=source.role_id AND target.tenant_id=source.tenant_id AND target.menu_id=900220);
  UPDATE system_tenant_package package
  SET menu_ids=JSON_ARRAY_APPEND(CAST(package.menu_ids AS JSON),'$',900220),
    updater='craft-process-menu',update_time=NOW()
  WHERE package.deleted=b'0'
    AND JSON_CONTAINS(CAST(package.menu_ids AS JSON),CAST(craft_menu AS JSON),'$')
    AND NOT JSON_CONTAINS(CAST(package.menu_ids AS JSON),CAST('900220' AS JSON),'$');
  INSERT INTO system_role_menu (role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
  SELECT DISTINCT source.role_id,991514,'craft-process-menu',NOW(),'craft-process-menu',NOW(),b'0',source.tenant_id
  FROM system_role_menu source WHERE source.deleted=b'0' AND source.menu_id=craft_menu
    AND NOT EXISTS (SELECT 1 FROM system_role_menu target WHERE target.deleted=b'0'
      AND target.role_id=source.role_id AND target.tenant_id=source.tenant_id AND target.menu_id=991514);
  UPDATE system_tenant_package package
  SET menu_ids=JSON_ARRAY_APPEND(CAST(package.menu_ids AS JSON),'$',991514),
    updater='craft-process-menu',update_time=NOW()
  WHERE package.deleted=b'0'
    AND JSON_CONTAINS(CAST(package.menu_ids AS JSON),CAST(craft_menu AS JSON),'$')
    AND NOT JSON_CONTAINS(CAST(package.menu_ids AS JSON),CAST('991514' AS JSON),'$');
  COMMIT;
END$$
DELIMITER ;
CALL move_craft_to_process_management();
DROP PROCEDURE move_craft_to_process_management;
