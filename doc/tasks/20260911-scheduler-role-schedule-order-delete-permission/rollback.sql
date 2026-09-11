-- rollback-migration: allowedEnvironments=test,backup,prod; requiresBackup=true; type=permission; riskLevel=medium
-- Roll back only bindings created or reactivated by the scheduler delete permission migration.

SET NAMES utf8mb4;
START TRANSACTION;

UPDATE `system_role_menu` AS `role_menu`
JOIN `system_role` AS `role`
  ON `role`.`id` = `role_menu`.`role_id`
 AND `role`.`tenant_id` = `role_menu`.`tenant_id`
JOIN `system_menu` AS `menu`
  ON `menu`.`id` = `role_menu`.`menu_id`
SET `role_menu`.`deleted` = b'1',
    `role_menu`.`updater` = 'rollback-20260911-mes-scheduler-schedule-order-delete',
    `role_menu`.`update_time` = NOW()
WHERE `role`.`code` = 'mes_scheduler'
  AND `role`.`status` = 0
  AND `role`.`deleted` = b'0'
  AND `menu`.`permission` = 'mes:pro-schedule-order:delete'
  AND `menu`.`status` = 0
  AND `menu`.`deleted` = b'0'
  AND `role_menu`.`deleted` = b'0'
  AND (
    `role_menu`.`creator` = 'mes-scheduler-schedule-order-delete-20260911'
    OR `role_menu`.`updater` = 'mes-scheduler-schedule-order-delete-20260911'
  );

COMMIT;
