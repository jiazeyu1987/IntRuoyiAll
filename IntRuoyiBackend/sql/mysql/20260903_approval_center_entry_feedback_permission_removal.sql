-- release-migration: allowedEnvironments=test; dependsOn=20260803_mes_frontline_pressure_pump_all_process_permission,20260807_test_tenant1_all_role_permission_sync; type=permission; riskLevel=low
-- Remove the feedback all-processes button from the tenant 1 approval-center entry role.

SET NAMES utf8mb4;
START TRANSACTION;

DROP PROCEDURE IF EXISTS remove_approval_center_feedback_permission_20260903;
DELIMITER //
CREATE PROCEDURE remove_approval_center_feedback_permission_20260903()
BEGIN
  DROP TEMPORARY TABLE IF EXISTS `tmp_approval_center_feedback_target_role`;
  CREATE TEMPORARY TABLE `tmp_approval_center_feedback_target_role` (
    `role_id` bigint NOT NULL,
    `tenant_id` bigint NOT NULL,
    PRIMARY KEY (`role_id`, `tenant_id`)
  ) ENGINE=Memory;

  INSERT INTO `tmp_approval_center_feedback_target_role` (`role_id`, `tenant_id`)
  SELECT `target_role`.`id`, `target_role`.`tenant_id`
    FROM `system_role` AS `target_role`
   WHERE `target_role`.`code` = 'approval_center_entry'
     AND `target_role`.`tenant_id` = 1
     AND `target_role`.`status` = 0
     AND `target_role`.`deleted` = b'0';

  IF (SELECT COUNT(*) FROM `tmp_approval_center_feedback_target_role`) <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing or ambiguous approval center entry role for feedback permission removal';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_approval_center_feedback_target_menu`;
  CREATE TEMPORARY TABLE `tmp_approval_center_feedback_target_menu` (
    `menu_id` bigint NOT NULL PRIMARY KEY
  ) ENGINE=Memory;

  INSERT INTO `tmp_approval_center_feedback_target_menu` (`menu_id`)
  SELECT `menu`.`id`
    FROM `system_menu` AS `menu`
   WHERE `menu`.`id` = 900450
     AND `menu`.`permission` = 'mes:pro-feedback:frontline-pressure-pump:all-processes'
     AND `menu`.`type` = 3
     AND `menu`.`parent_id` = 5550
     AND `menu`.`status` = 0
     AND `menu`.`deleted` = b'0';

  IF (SELECT COUNT(*) FROM `tmp_approval_center_feedback_target_menu`) <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing or ambiguous feedback button menu for approval center entry role';
  END IF;

  UPDATE `system_role_menu` AS `role_menu`
    JOIN `tmp_approval_center_feedback_target_role` AS `target_role`
      ON `target_role`.`role_id` = `role_menu`.`role_id`
     AND `target_role`.`tenant_id` = `role_menu`.`tenant_id`
    JOIN `tmp_approval_center_feedback_target_menu` AS `target_menu`
      ON `target_menu`.`menu_id` = `role_menu`.`menu_id`
     SET `role_menu`.`deleted` = b'1',
         `role_menu`.`updater` = 'approval-center-feedback-permission-removal',
         `role_menu`.`update_time` = NOW()
   WHERE `role_menu`.`deleted` = b'0';

  IF EXISTS (
    SELECT 1
      FROM `system_role_menu` AS `role_menu`
      JOIN `tmp_approval_center_feedback_target_role` AS `target_role`
        ON `target_role`.`role_id` = `role_menu`.`role_id`
       AND `target_role`.`tenant_id` = `role_menu`.`tenant_id`
      JOIN `tmp_approval_center_feedback_target_menu` AS `target_menu`
        ON `target_menu`.`menu_id` = `role_menu`.`menu_id`
     WHERE `role_menu`.`deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Approval center entry feedback button permission removal incomplete';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_approval_center_feedback_target_menu`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_approval_center_feedback_target_role`;
END//
DELIMITER ;

CALL remove_approval_center_feedback_permission_20260903();
DROP PROCEDURE IF EXISTS remove_approval_center_feedback_permission_20260903;

COMMIT;
