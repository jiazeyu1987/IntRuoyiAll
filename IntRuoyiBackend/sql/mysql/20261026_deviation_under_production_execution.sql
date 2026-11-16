-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261026_edhr_lifecycle_directories,20260924_mes_edhr_deviation_management; type=menu; riskLevel=low
-- Place deviation management after nonconformance review. Grant navigation only
-- to existing deviation-query holders. Preserve existing business permissions.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS move_deviation_under_execution;
DELIMITER $$
CREATE PROCEDURE move_deviation_under_execution()
BEGIN
  DECLARE deviation_menu_id BIGINT;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;
  START TRANSACTION;
  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE id=991516 AND type=1 AND deleted=b'0'
      AND path='/mes/edhr-production-execution')
    OR NOT EXISTS (SELECT 1 FROM system_menu WHERE id=9008440 AND deleted=b'0'
      AND permission='mes:pro-edhr-deviation:query') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing production execution directory or deviation query permission';
  END IF;
  IF (SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND type=2
      AND (component='mes/pro/edhr-deviation/index'
        OR path='/mes/pro/feedback/edhr-deviation'))>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Duplicate deviation menus';
  END IF;
  SELECT MAX(id) INTO deviation_menu_id FROM system_menu WHERE deleted=b'0' AND type=2
    AND component='mes/pro/edhr-deviation/index';
  IF deviation_menu_id IS NULL THEN
    IF EXISTS (SELECT 1 FROM system_menu WHERE id=991530
        OR (deleted=b'0' AND path='/mes/pro/feedback/edhr-deviation')) THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Deviation menu identity conflict';
    END IF;
    SET deviation_menu_id=991530;
    INSERT INTO system_menu
      (id,name,permission,type,sort,parent_id,path,icon,component,component_name,status,
       visible,keep_alive,always_show,creator,create_time,updater,update_time,deleted)
    VALUES (991530,'偏差管理','mes:pro-edhr-deviation:query',2,90,991516,
      '/mes/pro/feedback/edhr-deviation','ep:warning','mes/pro/edhr-deviation/index',
      'MesProEdhrDeviation',0,b'1',b'1',b'1','deviation-menu',NOW(),'deviation-menu',NOW(),b'0');
  ELSE
    UPDATE system_menu SET parent_id=991516,sort=90,name='偏差管理',
      path='/mes/pro/feedback/edhr-deviation',visible=b'1',updater='deviation-menu',update_time=NOW()
      WHERE id=deviation_menu_id;
  END IF;
  INSERT INTO system_role_menu (role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
  SELECT DISTINCT s.role_id,t.id,'deviation-menu',NOW(),'deviation-menu',NOW(),b'0',s.tenant_id
  FROM system_role_menu s CROSS JOIN system_menu t
  WHERE s.deleted=b'0' AND s.menu_id IN (9008440,deviation_menu_id)
    AND t.id IN (deviation_menu_id,991516,900220) AND t.deleted=b'0'
    AND NOT EXISTS (SELECT 1 FROM system_role_menu r WHERE r.deleted=b'0'
      AND r.role_id=s.role_id AND r.tenant_id=s.tenant_id AND r.menu_id=t.id);
  IF EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted=b'0'
      AND (NOT JSON_VALID(menu_ids) OR JSON_TYPE(CAST(menu_ids AS JSON))<>'ARRAY')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Invalid tenant package menu_ids';
  END IF;
  UPDATE system_tenant_package SET menu_ids=JSON_ARRAY_APPEND(CAST(menu_ids AS JSON),'$',deviation_menu_id),
      updater='deviation-menu',update_time=NOW()
    WHERE deleted=b'0' AND JSON_CONTAINS(CAST(menu_ids AS JSON),'9008440','$')
      AND NOT JSON_CONTAINS(CAST(menu_ids AS JSON),CAST(deviation_menu_id AS JSON),'$');
  UPDATE system_tenant_package SET menu_ids=JSON_ARRAY_APPEND(CAST(menu_ids AS JSON),'$',991516),
      updater='deviation-menu',update_time=NOW()
    WHERE deleted=b'0' AND JSON_CONTAINS(CAST(menu_ids AS JSON),CAST(deviation_menu_id AS JSON),'$')
      AND NOT JSON_CONTAINS(CAST(menu_ids AS JSON),'991516','$');
  UPDATE system_tenant_package SET menu_ids=JSON_ARRAY_APPEND(CAST(menu_ids AS JSON),'$',900220),
      updater='deviation-menu',update_time=NOW()
    WHERE deleted=b'0' AND JSON_CONTAINS(CAST(menu_ids AS JSON),CAST(deviation_menu_id AS JSON),'$')
      AND NOT JSON_CONTAINS(CAST(menu_ids AS JSON),'900220','$');
  COMMIT;
END$$
DELIMITER ;
CALL move_deviation_under_execution();
DROP PROCEDURE move_deviation_under_execution;
