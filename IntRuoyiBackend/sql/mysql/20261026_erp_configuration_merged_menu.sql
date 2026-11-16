-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261026_edhr_lifecycle_directories; type=menu; riskLevel=medium
-- One ERP configuration entry; retain all existing leaf grants and the former config URL.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS migrate_erp_configuration_merged_menu;
DELIMITER $$
CREATE PROCEDURE migrate_erp_configuration_merged_menu()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;
  START TRANSACTION;
  IF (SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND status=0 AND type=2
      AND parent_id=991511 AND ((id=6013 AND component='erp/sync/index' AND path='/erp/kingdee-sync')
        OR (id=991502 AND component='erp/sync/config/index' AND path='/erp/sync/config')))<>2 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing ERP synchronization source menus';
  END IF;
  IF EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted=b'0'
      AND (NOT JSON_VALID(menu_ids) OR JSON_TYPE(CAST(menu_ids AS JSON))<>'ARRAY')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Invalid tenant package menu_ids';
  END IF;
  UPDATE system_menu SET name='ERP配置',updater='erp-config-merge',update_time=NOW() WHERE id=6013;
  UPDATE system_menu SET visible=b'0',updater='erp-config-merge',update_time=NOW() WHERE id=991502;
  -- Existing configuration holders retain navigation to the merged page, not new action permissions.
  INSERT INTO system_role_menu (role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
  SELECT DISTINCT s.role_id,6013,'erp-config-merge',NOW(),'erp-config-merge',NOW(),b'0',s.tenant_id
  FROM system_role_menu s WHERE s.deleted=b'0' AND s.menu_id=991502
    AND NOT EXISTS (SELECT 1 FROM system_role_menu t WHERE t.deleted=b'0'
      AND t.role_id=s.role_id AND t.tenant_id=s.tenant_id AND t.menu_id=6013);
  UPDATE system_tenant_package p
  SET menu_ids=JSON_ARRAY_APPEND(CAST(p.menu_ids AS JSON),'$',6013),updater='erp-config-merge',update_time=NOW()
  WHERE p.deleted=b'0' AND JSON_CONTAINS(CAST(p.menu_ids AS JSON),CAST('991502' AS JSON),'$')
    AND NOT JSON_CONTAINS(CAST(p.menu_ids AS JSON),CAST('6013' AS JSON),'$');
  COMMIT;
END$$
DELIMITER ;
CALL migrate_erp_configuration_merged_menu();
DROP PROCEDURE migrate_erp_configuration_merged_menu;
