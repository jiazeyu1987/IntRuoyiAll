-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260728_rename_mdm_product_menu; type=menu; riskLevel=low
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS move_showroom_master_data_menu;
DELIMITER $$
CREATE PROCEDURE move_showroom_master_data_menu()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;
  START TRANSACTION;
  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE id=980100 AND parent_id=0
      AND path='showroom' AND type=1 AND status=0 AND deleted=b'0')
    OR NOT EXISTS (SELECT 1 FROM system_menu WHERE id=990201 AND name='展厅主数据'
      AND component='mdm/product/index' AND component_name='MdmProduct'
      AND permission='mdm:product:query' AND type=2 AND status=0 AND deleted=b'0'
      AND ((parent_id=5101 AND path='showroom-product')
        OR (parent_id=980100 AND path='/mes/md/showroom-product'))) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing or changed showroom/master data menu';
  END IF;
  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted=b'0' AND id<>990201
      AND path='/mes/md/showroom-product') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Showroom master data route is occupied';
  END IF;
  IF EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted=b'0'
      AND (NOT JSON_VALID(menu_ids) OR JSON_TYPE(CAST(menu_ids AS JSON))<>'ARRAY')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Invalid tenant package menu scope';
  END IF;
  UPDATE system_menu SET parent_id=980100,path='/mes/md/showroom-product',
    updater='showroom-mdm-menu',update_time=NOW()
  WHERE id=990201 AND (parent_id<>980100 OR path<>'/mes/md/showroom-product');
  -- Only existing page holders receive its new navigation ancestor.
  INSERT INTO system_role_menu (role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
  SELECT DISTINCT source.role_id,980100,'showroom-mdm-menu',NOW(),'showroom-mdm-menu',NOW(),b'0',source.tenant_id
  FROM system_role_menu source WHERE source.deleted=b'0' AND source.menu_id=990201
    AND NOT EXISTS (SELECT 1 FROM system_role_menu target WHERE target.deleted=b'0'
      AND target.role_id=source.role_id AND target.tenant_id=source.tenant_id AND target.menu_id=980100);
  UPDATE system_tenant_package package
  SET menu_ids=JSON_ARRAY_APPEND(CAST(package.menu_ids AS JSON),'$',980100),
    updater='showroom-mdm-menu',update_time=NOW()
  WHERE package.deleted=b'0'
    AND JSON_CONTAINS(CAST(package.menu_ids AS JSON),CAST('990201' AS JSON),'$')
    AND NOT JSON_CONTAINS(CAST(package.menu_ids AS JSON),CAST('980100' AS JSON),'$');
  COMMIT;
END$$
DELIMITER ;
CALL move_showroom_master_data_menu();
DROP PROCEDURE move_showroom_master_data_menu;
