-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261026_edhr_lifecycle_directories; type=menu; riskLevel=low
-- Cross-leader read access is granted only to existing history-trace menu holders.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS install_in_process_order_menu;
DELIMITER $$
CREATE PROCEDURE install_in_process_order_menu()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;
  START TRANSACTION;
  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE id=991516 AND type=1 AND deleted=b'0'
      AND path='/mes/edhr-production-execution')
    OR NOT EXISTS (SELECT 1 FROM system_menu WHERE id=991510 AND type=2 AND deleted=b'0'
      AND component='mes/pro/edhr-batch/BatchRecordHistoryPage') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing production execution directory or history trace menu';
  END IF;
  IF EXISTS (SELECT 1 FROM system_menu WHERE id=991540 AND NOT (deleted=b'0' AND type=2
      AND component='mes/pro/in-process-order/index' AND permission='mes:pro-in-process-order:query'))
    OR EXISTS (SELECT 1 FROM system_menu WHERE id<>991540 AND deleted=b'0'
      AND (path='/mes/pro/in-process-order' OR component='mes/pro/in-process-order/index'
        OR permission='mes:pro-in-process-order:query')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='In-process order menu identity conflict';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE id=991540) THEN
    INSERT INTO system_menu
      (id,name,permission,type,sort,parent_id,path,icon,component,component_name,status,
       visible,keep_alive,always_show,creator,create_time,updater,update_time,deleted)
    VALUES (991540,'在制工单','mes:pro-in-process-order:query',2,5,991516,
      '/mes/pro/in-process-order','ep:list','mes/pro/in-process-order/index',
      'MesInProcessOrder',0,b'1',b'1',b'1','in-process-order',NOW(),'in-process-order',NOW(),b'0');
  END IF;
  INSERT INTO system_role_menu (role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
  SELECT DISTINCT s.role_id,t.id,'in-process-order',NOW(),'in-process-order',NOW(),b'0',s.tenant_id
  FROM system_role_menu s CROSS JOIN system_menu t
  WHERE s.deleted=b'0' AND s.menu_id=991510
    AND t.id IN (991540,991516,900220) AND t.deleted=b'0'
    AND NOT EXISTS (SELECT 1 FROM system_role_menu r WHERE r.deleted=b'0'
      AND r.role_id=s.role_id AND r.tenant_id=s.tenant_id AND r.menu_id=t.id);
  IF EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted=b'0'
      AND (NOT JSON_VALID(menu_ids) OR JSON_TYPE(CAST(menu_ids AS JSON))<>'ARRAY')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Invalid tenant package menu_ids';
  END IF;
  UPDATE system_tenant_package SET menu_ids=JSON_ARRAY_APPEND(CAST(menu_ids AS JSON),'$',991540),
      updater='in-process-order',update_time=NOW()
    WHERE deleted=b'0' AND JSON_CONTAINS(CAST(menu_ids AS JSON),'991510','$')
      AND NOT JSON_CONTAINS(CAST(menu_ids AS JSON),'991540','$');
  UPDATE system_tenant_package SET menu_ids=JSON_ARRAY_APPEND(CAST(menu_ids AS JSON),'$',991516),
      updater='in-process-order',update_time=NOW()
    WHERE deleted=b'0' AND JSON_CONTAINS(CAST(menu_ids AS JSON),'991540','$')
      AND NOT JSON_CONTAINS(CAST(menu_ids AS JSON),'991516','$');
  UPDATE system_tenant_package SET menu_ids=JSON_ARRAY_APPEND(CAST(menu_ids AS JSON),'$',900220),
      updater='in-process-order',update_time=NOW()
    WHERE deleted=b'0' AND JSON_CONTAINS(CAST(menu_ids AS JSON),'991540','$')
      AND NOT JSON_CONTAINS(CAST(menu_ids AS JSON),'900220','$');
  COMMIT;
END$$
DELIMITER ;
CALL install_in_process_order_menu();
DROP PROCEDURE install_in_process_order_menu;
