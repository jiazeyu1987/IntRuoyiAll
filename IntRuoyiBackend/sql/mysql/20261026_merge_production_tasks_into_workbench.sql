-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261026_rename_production_execution_menus; type=menu; riskLevel=low
-- Retire the page, retaining the existing permission identity and its role/package
-- grants for workbench, notification and approval-center consumers. No new grants.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS merge_production_tasks_into_workbench;
DELIMITER $$
CREATE PROCEDURE merge_production_tasks_into_workbench()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;
  START TRANSACTION;
  IF (SELECT COUNT(*) FROM system_menu WHERE id=900230 AND deleted=b'0'
      AND permission='mes:pro-edhr-work-task:query'
      AND ((type=2 AND path='/mes/pro/feedback/edhr-work-task'
            AND component='mes/pro/edhr-work-task/WorkTaskBoardPage')
           OR (type=3 AND path='' AND component=''))) <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Production task menu identity is missing or changed';
  END IF;
  -- Do not delete the ID: role_menu and tenant_package.menu_ids refer to it.
  UPDATE system_menu SET type=3, name='生产任务查询', path='', component='',
      component_name='', visible=b'0', icon='', updater='merge-production-tasks', update_time=NOW()
    WHERE id=900230;
  -- Button permissions remain under the production directory, not a button parent.
  UPDATE system_menu child JOIN system_menu retired ON retired.id=900230
    SET child.parent_id=retired.parent_id, child.updater='merge-production-tasks', child.update_time=NOW()
    WHERE child.parent_id=900230 AND child.type=3 AND child.deleted=b'0';
  COMMIT;
END$$
DELIMITER ;
CALL merge_production_tasks_into_workbench();
DROP PROCEDURE merge_production_tasks_into_workbench;
