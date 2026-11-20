-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261026_mes_smart_scheduling_root_menu; type=menu; riskLevel=low
-- Repair the known post-migration drift without replaying historical initialization.
-- Preserve IDs, the current root directory, role bindings and tenant-package grants.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS repair_schedule_order_menu_20261120;
DELIMITER $$
CREATE PROCEDURE repair_schedule_order_menu_20261120()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
  START TRANSACTION;
  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE id=900120 AND name='智能排产'
      AND type=1 AND parent_id=0 AND path='/mes/smart-scheduling'
      AND permission='mes:pro-smart-scheduling:query' AND status=0 AND deleted=b'0')
    OR NOT EXISTS (SELECT 1 FROM system_menu WHERE id=5580 AND type=2
      AND component='mes/pro/scheduleorder/index' AND component_name='MesProScheduleOrder'
      AND status=0 AND visible=b'1' AND deleted=b'0'
      AND ((name='排产工单池' AND parent_id=5700 AND path='schedule-order' AND permission='' AND sort=2)
        OR (name='排产工单' AND parent_id=900120 AND path='/mes/pro/schedule-order'
          AND permission='mes:pro-schedule-order:query' AND sort=1)))
    OR EXISTS (SELECT 1 FROM system_menu WHERE deleted=b'0' AND id<>5580
      AND (path='/mes/pro/schedule-order' OR (type=2 AND component='mes/pro/scheduleorder/index')))
  THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='MENU_FORWARD_BASELINE_CONFLICT';
  END IF;
  UPDATE system_menu SET name='排产工单',parent_id=900120,path='/mes/pro/schedule-order',
    permission='mes:pro-schedule-order:query',sort=1,updater='menu-forward-20261120',update_time=NOW()
  WHERE id=5580 AND name='排产工单池' AND parent_id=5700 AND path='schedule-order' AND permission='' AND sort=2;
  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE id=5580 AND name='排产工单'
      AND parent_id=900120 AND path='/mes/pro/schedule-order'
      AND permission='mes:pro-schedule-order:query' AND sort=1 AND deleted=b'0') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='MENU_FORWARD_POSTCONDITION_FAILED';
  END IF;
  COMMIT;
END$$
DELIMITER ;
CALL repair_schedule_order_menu_20261120();
DROP PROCEDURE repair_schedule_order_menu_20261120;
