-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261026_edhr_lifecycle_directories,20260908_bpm_form_parser_menu; type=menu; riskLevel=low
-- Preserve the existing parser URL, component and leaf permissions.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS move_form_parser_under_production_forms;
DELIMITER $$
CREATE PROCEDURE move_form_parser_under_production_forms()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;
  START TRANSACTION;
  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE id=991512 AND parent_id=900220
      AND path='/mes/edhr-production-forms' AND type=1 AND status=0 AND deleted=b'0')
    OR NOT EXISTS (SELECT 1 FROM system_menu WHERE id=900220 AND parent_id=0
      AND path='/mes/edhr-batch-processing' AND type=1 AND status=0 AND deleted=b'0')
    OR NOT EXISTS (SELECT 1 FROM system_menu WHERE id=605071222
      AND parent_id IN (990200,991512) AND path IN ('form-center/parser','/mdm/form-center/parser')
      AND component='form-center/parser/index' AND permission='form:parser:query'
      AND type=2 AND status=0 AND deleted=b'0') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing or changed production forms/parser menu';
  END IF;
  IF EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted=b'0'
      AND (NOT JSON_VALID(menu_ids) OR JSON_TYPE(CAST(menu_ids AS JSON))<>'ARRAY')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Invalid tenant package menu scope';
  END IF;
  UPDATE system_menu SET parent_id=991512,path='/mdm/form-center/parser',sort=30,
    updater='form-parser-navigation',update_time=NOW() WHERE id=605071222;
  DROP TEMPORARY TABLE IF EXISTS tmp_parser_menu_holders;
  CREATE TEMPORARY TABLE tmp_parser_menu_holders (id BIGINT PRIMARY KEY);
  INSERT INTO tmp_parser_menu_holders
  WITH RECURSIVE descendants AS (
    SELECT id FROM system_menu WHERE id=605071222 AND deleted=b'0'
    UNION ALL
    SELECT m.id FROM system_menu m JOIN descendants d ON m.parent_id=d.id WHERE m.deleted=b'0'
  ) SELECT DISTINCT id FROM descendants;
  -- Grant navigation ancestors only to existing parser page/button holders.
  INSERT INTO system_role_menu (role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
  SELECT DISTINCT s.role_id,a.id,'form-parser-navigation',NOW(),'form-parser-navigation',NOW(),b'0',s.tenant_id
  FROM system_role_menu s JOIN tmp_parser_menu_holders h ON h.id=s.menu_id
  CROSS JOIN (SELECT 900220 AS id UNION ALL SELECT 991512) a
  WHERE s.deleted=b'0' AND NOT EXISTS (SELECT 1 FROM system_role_menu t WHERE t.deleted=b'0'
    AND t.role_id=s.role_id AND t.tenant_id=s.tenant_id AND t.menu_id=a.id);
  UPDATE system_tenant_package p
  SET menu_ids=JSON_ARRAY_APPEND(CAST(menu_ids AS JSON),'$',900220),updater='form-parser-navigation',update_time=NOW()
  WHERE p.deleted=b'0' AND NOT JSON_CONTAINS(CAST(p.menu_ids AS JSON),CAST('900220' AS JSON),'$')
    AND EXISTS (SELECT 1 FROM tmp_parser_menu_holders h WHERE JSON_CONTAINS(CAST(p.menu_ids AS JSON),CAST(h.id AS JSON),'$'));
  UPDATE system_tenant_package p
  SET menu_ids=JSON_ARRAY_APPEND(CAST(menu_ids AS JSON),'$',991512),updater='form-parser-navigation',update_time=NOW()
  WHERE p.deleted=b'0' AND NOT JSON_CONTAINS(CAST(p.menu_ids AS JSON),CAST('991512' AS JSON),'$')
    AND EXISTS (SELECT 1 FROM tmp_parser_menu_holders h WHERE JSON_CONTAINS(CAST(p.menu_ids AS JSON),CAST(h.id AS JSON),'$'));
  COMMIT;
  DROP TEMPORARY TABLE tmp_parser_menu_holders;
END$$
DELIMITER ;
CALL move_form_parser_under_production_forms();
DROP PROCEDURE move_form_parser_under_production_forms;
