-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261026_form_parser_under_production_forms; type=menu; riskLevel=medium
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS split_form_parser_menus;
DELIMITER $$
CREATE PROCEDURE split_form_parser_menus()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;
  START TRANSACTION;
  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE id=605071222 AND parent_id=991512
      AND path='/mdm/form-center/parser' AND permission='form:parser:query'
      AND component IN ('form-center/parser/index','form-center/parser/ProductionParserPage')
      AND type=2 AND status=0 AND deleted=b'0')
    OR (SELECT COUNT(*) FROM system_menu WHERE type=1 AND status=0 AND deleted=b'0'
      AND parent_id=900220 AND ((id=991512 AND path='/mes/edhr-production-forms')
        OR (id=991513 AND path='/mes/edhr-inspection-forms')))<>2 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing or changed parser/form directories';
  END IF;
  IF EXISTS (SELECT 1 FROM system_menu WHERE id=991521 AND NOT (
      parent_id=991513 AND path='/mdm/form-center/inspection-parser'
      AND component='form-center/parser/InspectionParserPage'
      AND component_name='FormCenterInspectionParser' AND permission='form:parser:query'
      AND type=2 AND deleted=b'0'))
    OR EXISTS (SELECT 1 FROM system_menu WHERE id<>991521 AND deleted=b'0'
      AND (path='/mdm/form-center/inspection-parser' OR component_name='FormCenterInspectionParser')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Inspection parser menu identity occupied';
  END IF;
  IF EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted=b'0'
      AND (NOT JSON_VALID(menu_ids) OR JSON_TYPE(CAST(menu_ids AS JSON))<>'ARRAY')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Invalid tenant package menu scope';
  END IF;
  UPDATE system_menu SET name='生产解析',sort=0,component='form-center/parser/ProductionParserPage',
    component_name='FormCenterProductionParser',updater='parser-split',update_time=NOW() WHERE id=605071222;
  INSERT INTO system_menu
    (id,name,permission,type,sort,parent_id,path,icon,component,component_name,status,
     visible,keep_alive,always_show,creator,create_time,updater,update_time,deleted)
  SELECT 991521,'检验解析',permission,2,0,991513,'/mdm/form-center/inspection-parser',icon,
    'form-center/parser/InspectionParserPage','FormCenterInspectionParser',status,
    visible,keep_alive,always_show,'parser-split',NOW(),'parser-split',NOW(),b'0'
  FROM system_menu WHERE id=605071222 AND NOT EXISTS (SELECT 1 FROM system_menu WHERE id=991521);
  -- Existing parser page holders retain both capabilities and receive the inspection ancestor.
  INSERT INTO system_role_menu (role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
  SELECT DISTINCT s.role_id,a.id,'parser-split',NOW(),'parser-split',NOW(),b'0',s.tenant_id
  FROM system_role_menu s CROSS JOIN (SELECT 991521 AS id UNION ALL SELECT 991513) a
  WHERE s.deleted=b'0' AND s.menu_id=605071222
    AND NOT EXISTS (SELECT 1 FROM system_role_menu t WHERE t.deleted=b'0'
      AND t.role_id=s.role_id AND t.tenant_id=s.tenant_id AND t.menu_id=a.id);
  UPDATE system_tenant_package p
  SET menu_ids=JSON_ARRAY_APPEND(CAST(p.menu_ids AS JSON),'$',991521),updater='parser-split',update_time=NOW()
  WHERE p.deleted=b'0' AND JSON_CONTAINS(CAST(p.menu_ids AS JSON),CAST('605071222' AS JSON),'$')
    AND NOT JSON_CONTAINS(CAST(p.menu_ids AS JSON),CAST('991521' AS JSON),'$');
  UPDATE system_tenant_package p
  SET menu_ids=JSON_ARRAY_APPEND(CAST(p.menu_ids AS JSON),'$',991513),updater='parser-split',update_time=NOW()
  WHERE p.deleted=b'0' AND JSON_CONTAINS(CAST(p.menu_ids AS JSON),CAST('605071222' AS JSON),'$')
    AND NOT JSON_CONTAINS(CAST(p.menu_ids AS JSON),CAST('991513' AS JSON),'$');
  COMMIT;
END$$
DELIMITER ;
CALL split_form_parser_menus();
DROP PROCEDURE split_form_parser_menus;
