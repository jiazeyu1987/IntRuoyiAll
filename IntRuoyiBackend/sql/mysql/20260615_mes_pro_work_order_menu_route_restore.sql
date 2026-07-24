-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=menu; riskLevel=low
-- Restore MES production work order menu under MES > 生产管理.
-- The 20260612 ERP menu migration moved menu 5530 under 基础数据, which changes
-- the dynamic route from /mes/pro/work-order to /mes/md/work-order.

DROP PROCEDURE IF EXISTS restore_mes_pro_work_order_menu_route;

DELIMITER //
CREATE PROCEDURE restore_mes_pro_work_order_menu_route()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 5700
      AND `type` = 1
      AND `path` = 'pro'
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing MES production management parent menu 5700, cannot restore production work order route';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 5530
      AND `type` = 2
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing MES production work order menu 5530, cannot restore route';
  END IF;

  UPDATE `system_menu`
  SET `parent_id` = 5700,
      `path` = 'work-order',
      `icon` = 'ep:document-copy',
      `component` = 'mes/pro/workorder/index',
      `component_name` = 'MesProWorkOrder',
      `sort` = 1,
      `updater` = '1',
      `update_time` = NOW()
  WHERE `id` = 5530
    AND `type` = 2
    AND `deleted` = b'0';
END//
DELIMITER ;

CALL restore_mes_pro_work_order_menu_route();

DROP PROCEDURE IF EXISTS restore_mes_pro_work_order_menu_route;
