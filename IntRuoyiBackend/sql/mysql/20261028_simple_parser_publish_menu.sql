-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261027_simple_production_parser_menu,20261028_simple_parser_publish_receipt; type=menu; riskLevel=low
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS simple_parser_publish_menu;
DELIMITER $$
CREATE PROCEDURE simple_parser_publish_menu()
BEGIN
  DECLARE publish_id BIGINT DEFAULT 991523;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
  START TRANSACTION;
  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE id=605071336 AND component='form-center/simple-parser/index' AND deleted=b'0')
    OR NOT EXISTS (SELECT 1 FROM system_menu WHERE id=605071223 AND permission='form:parser:production-batch-record' AND deleted=b'0') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing simple parser or existing import permission menu';
  END IF;
  IF (SELECT COUNT(*) FROM system_menu WHERE permission='form:parser:production-batch-record:publish' AND deleted=b'0')>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Duplicate parser publish permission';
  END IF;
  IF EXISTS (SELECT 1 FROM system_menu WHERE permission='form:parser:production-batch-record:publish' AND deleted=b'0') THEN
    SELECT id INTO publish_id FROM system_menu WHERE permission='form:parser:production-batch-record:publish' AND deleted=b'0';
  ELSE
    IF EXISTS (SELECT 1 FROM system_menu WHERE id=publish_id) THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Parser publish menu identity occupied';
    END IF;
    INSERT INTO system_menu (id,name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,keep_alive,always_show,creator,create_time,updater,update_time,deleted)
    VALUES (publish_id,'简易解析发布','form:parser:production-batch-record:publish',3,1,605071336,'','','','',0,b'1',b'0',b'0','simple-parser',NOW(),'simple-parser',NOW(),b'0');
  END IF;
  IF EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted=b'0' AND (NOT JSON_VALID(menu_ids) OR JSON_TYPE(CAST(menu_ids AS JSON))<>'ARRAY')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Invalid tenant package menu scope';
  END IF;
  -- 只授予同时拥有既有生产解析操作及简易解析菜单的角色。
  INSERT INTO system_role_menu (role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
  SELECT DISTINCT i.role_id,publish_id,'simple-parser',NOW(),'simple-parser',NOW(),b'0',i.tenant_id
  FROM system_role_menu i JOIN system_role_menu page ON page.role_id=i.role_id AND page.tenant_id=i.tenant_id AND page.menu_id=605071336 AND page.deleted=b'0'
  WHERE i.menu_id=605071223 AND i.deleted=b'0'
    AND NOT EXISTS (SELECT 1 FROM system_role_menu p WHERE p.role_id=i.role_id AND p.tenant_id=i.tenant_id AND p.menu_id=publish_id AND p.deleted=b'0');
  UPDATE system_tenant_package SET menu_ids=JSON_ARRAY_APPEND(CAST(menu_ids AS JSON),'$',publish_id),updater='simple-parser',update_time=NOW()
  WHERE deleted=b'0' AND JSON_CONTAINS(CAST(menu_ids AS JSON),CAST('605071223' AS JSON),'$')
    AND JSON_CONTAINS(CAST(menu_ids AS JSON),CAST('605071336' AS JSON),'$')
    AND NOT JSON_CONTAINS(CAST(menu_ids AS JSON),CAST(publish_id AS JSON),'$');
  COMMIT;
END$$
DELIMITER ;
CALL simple_parser_publish_menu();
DROP PROCEDURE simple_parser_publish_menu;
