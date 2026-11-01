-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261028_simple_pqc_parser_menu; type=menu; riskLevel=low
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS add_simple_common_parser_menu;
DELIMITER $$
CREATE PROCEDURE add_simple_common_parser_menu()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;
  START TRANSACTION;
  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE id=991521 AND parent_id=991513
      AND name='检验解析' AND permission='form:parser:query' AND type=2 AND status=0 AND deleted=b'0') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing production parser or form center menu';
  END IF;
  IF EXISTS (SELECT 1 FROM system_menu WHERE id=991525 AND NOT (
      parent_id=991513 AND path='/mdm/form-center/simple-common-parser'
      AND component='form-center/simple-common-parser/index' AND component_name='FormCenterSimpleCommonParser'
      AND permission='form:parser:query' AND type=2 AND deleted=b'0'))
    OR EXISTS (SELECT 1 FROM system_menu WHERE id<>991525 AND deleted=b'0'
      AND (path='/mdm/form-center/simple-common-parser' OR component_name='FormCenterSimpleCommonParser')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Simple parser menu identity occupied';
  END IF;
  IF EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted=b'0'
      AND (NOT JSON_VALID(menu_ids) OR JSON_TYPE(CAST(menu_ids AS JSON))<>'ARRAY')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Invalid tenant package menu scope';
  END IF;
  INSERT INTO system_menu
    (id,name,permission,type,sort,parent_id,path,icon,component,component_name,status,
     visible,keep_alive,always_show,creator,create_time,updater,update_time,deleted)
  SELECT 991525,'简易通用解析',permission,2,6,991513,'/mdm/form-center/simple-common-parser','ep:edit-pen',
    'form-center/simple-common-parser/index','FormCenterSimpleCommonParser',status,
    visible,b'0',always_show,'simple-common-parser',NOW(),'simple-common-parser',NOW(),b'0'
  FROM system_menu WHERE id=991521 AND NOT EXISTS (SELECT 1 FROM system_menu WHERE id=991525);
  UPDATE system_menu SET sort=6,keep_alive=b'0',updater='simple-common-parser',update_time=NOW()
  WHERE id=991525;
  -- Only existing production parser role holders and tenant packages gain this sibling entry.
  INSERT INTO system_role_menu (role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
  SELECT DISTINCT s.role_id,991525,'simple-common-parser',NOW(),'simple-common-parser',NOW(),b'0',s.tenant_id
  FROM system_role_menu s WHERE s.deleted=b'0' AND s.menu_id=991521
    AND NOT EXISTS (SELECT 1 FROM system_role_menu t WHERE t.deleted=b'0'
      AND t.role_id=s.role_id AND t.tenant_id=s.tenant_id AND t.menu_id=991525);
  UPDATE system_tenant_package p
  SET menu_ids=JSON_ARRAY_APPEND(CAST(p.menu_ids AS JSON),'$',991525),updater='simple-common-parser',update_time=NOW()
  WHERE p.deleted=b'0' AND JSON_CONTAINS(CAST(p.menu_ids AS JSON),CAST('991521' AS JSON),'$')
    AND NOT JSON_CONTAINS(CAST(p.menu_ids AS JSON),CAST('991525' AS JSON),'$');
  COMMIT;
END$$
DELIMITER ;
CALL add_simple_common_parser_menu();
DROP PROCEDURE add_simple_common_parser_menu;
