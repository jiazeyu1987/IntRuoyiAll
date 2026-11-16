-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261026_edhr_lifecycle_directories; type=menu; riskLevel=medium
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS split_inspection_regulation_menus;
DELIMITER $$
CREATE PROCEDURE split_inspection_regulation_menus()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;
  START TRANSACTION;
  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE id=991513 AND type=1
      AND path='/mes/edhr-inspection-forms' AND status=0 AND deleted=b'0')
    OR NOT EXISTS (SELECT 1 FROM system_menu WHERE id=900434 AND parent_id=991513
      AND path='/mes/pro/process-pool/qa-regulation' AND component='mes/pro/processpool/QaRegulationPage'
      AND type=2 AND status=0 AND deleted=b'0') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing inspection directory or QA regulation menu';
  END IF;
  IF EXISTS (SELECT 1 FROM system_menu WHERE id=991520 AND NOT (
      parent_id=991513 AND path='/mes/pro/process-pool/common-regulation'
      AND component='mes/pro/processpool/CommonRegulationPage'
      AND component_name='MesProProcessPoolCommonRegulation' AND type=2 AND deleted=b'0'))
    OR EXISTS (SELECT 1 FROM system_menu WHERE id<>991520 AND deleted=b'0'
      AND (path='/mes/pro/process-pool/common-regulation'
        OR component_name='MesProProcessPoolCommonRegulation')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Common regulation menu identity occupied';
  END IF;
  IF EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted=b'0'
      AND (NOT JSON_VALID(menu_ids) OR JSON_TYPE(CAST(menu_ids AS JSON))<>'ARRAY')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Invalid tenant package menu scope';
  END IF;
  UPDATE system_menu SET name='QA检验规程',updater='inspection-menu-split',update_time=NOW()
    WHERE id=900434;
  INSERT INTO system_menu
    (id,name,permission,type,sort,parent_id,path,icon,component,component_name,status,
     visible,keep_alive,always_show,creator,create_time,updater,update_time,deleted)
  SELECT 991520,'通用检验规程',permission,2,20,991513,
    '/mes/pro/process-pool/common-regulation','ep:document-checked',
    'mes/pro/processpool/CommonRegulationPage','MesProProcessPoolCommonRegulation',status,
    visible,keep_alive,always_show,'inspection-menu-split',NOW(),'inspection-menu-split',NOW(),b'0'
  FROM system_menu WHERE id=900434 AND NOT EXISTS (SELECT 1 FROM system_menu WHERE id=991520);
  -- Preserve access for existing QA workspace holders only; reuse its business permissions.
  INSERT INTO system_role_menu (role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
  SELECT DISTINCT s.role_id,991520,'inspection-menu-split',NOW(),'inspection-menu-split',NOW(),b'0',s.tenant_id
  FROM system_role_menu s WHERE s.deleted=b'0' AND s.menu_id=900434
    AND NOT EXISTS (SELECT 1 FROM system_role_menu t WHERE t.deleted=b'0'
      AND t.role_id=s.role_id AND t.tenant_id=s.tenant_id AND t.menu_id=991520);
  UPDATE system_tenant_package p
  SET menu_ids=JSON_ARRAY_APPEND(CAST(p.menu_ids AS JSON),'$',991520),
    updater='inspection-menu-split',update_time=NOW()
  WHERE p.deleted=b'0' AND JSON_CONTAINS(CAST(p.menu_ids AS JSON),CAST('900434' AS JSON),'$')
    AND NOT JSON_CONTAINS(CAST(p.menu_ids AS JSON),CAST('991520' AS JSON),'$');
  COMMIT;
END$$
DELIMITER ;
CALL split_inspection_regulation_menus();
DROP PROCEDURE split_inspection_regulation_menus;
