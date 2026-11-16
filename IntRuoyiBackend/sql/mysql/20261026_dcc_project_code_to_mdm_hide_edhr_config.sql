-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261026_edhr_lifecycle_directories; type=menu; riskLevel=low
-- Existing project-code roles and tenant packages receive only the new ancestor.
-- Remove the eDHR configuration navigation entry without deleting business configuration.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS move_dcc_project_code_to_mdm;
DELIMITER $$
CREATE PROCEDURE move_dcc_project_code_to_mdm()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;
  START TRANSACTION;
  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE id=990200 AND parent_id=0
      AND path='/mdm' AND type=1 AND status=0 AND deleted=b'0')
    OR NOT EXISTS (SELECT 1 FROM system_menu WHERE id=990210
      AND parent_id IN (991515,990200) AND path='/mes/md/dcc-project-code'
      AND component='dcc/controlled-file/basic-data/project-code/index'
      AND type=2 AND status=0 AND deleted=b'0')
    OR NOT EXISTS (SELECT 1 FROM system_menu WHERE id=991501 AND parent_id=991515
      AND path='/mes/pro/edhr-config' AND component='mes/pro/edhr-config/index'
      AND type=2 AND deleted=b'0') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing or changed master data, project code or eDHR configuration menu';
  END IF;
  IF EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted=b'0'
      AND (NOT JSON_VALID(menu_ids) OR JSON_TYPE(CAST(menu_ids AS JSON))<>'ARRAY')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Invalid tenant package menu scope';
  END IF;
  UPDATE system_menu SET parent_id=990200,updater='dcc-project-code-mdm',update_time=NOW()
    WHERE id=990210;
  UPDATE system_menu SET visible=b'0',updater='dcc-project-code-mdm',update_time=NOW()
    WHERE id=991501;
  INSERT INTO system_role_menu (role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
  SELECT DISTINCT source.role_id,990200,'dcc-project-code-mdm',NOW(),'dcc-project-code-mdm',NOW(),b'0',source.tenant_id
  FROM system_role_menu source WHERE source.deleted=b'0' AND source.menu_id=990210
    AND NOT EXISTS (SELECT 1 FROM system_role_menu target WHERE target.deleted=b'0'
      AND target.role_id=source.role_id AND target.tenant_id=source.tenant_id AND target.menu_id=990200);
  UPDATE system_tenant_package package
  SET menu_ids=JSON_ARRAY_APPEND(CAST(package.menu_ids AS JSON),'$',990200),
    updater='dcc-project-code-mdm',update_time=NOW()
  WHERE package.deleted=b'0'
    AND JSON_CONTAINS(CAST(package.menu_ids AS JSON),CAST('990210' AS JSON),'$')
    AND NOT JSON_CONTAINS(CAST(package.menu_ids AS JSON),CAST('990200' AS JSON),'$');
  COMMIT;
END$$
DELIMITER ;
CALL move_dcc_project_code_to_mdm();
DROP PROCEDURE move_dcc_project_code_to_mdm;
