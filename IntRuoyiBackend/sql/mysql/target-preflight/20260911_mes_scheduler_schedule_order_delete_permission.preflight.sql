-- release-target-preflight: migrationId=20260911_mes_scheduler_schedule_order_delete_permission; allowedEnvironments=test,prod,backup
SELECT CASE WHEN
  EXISTS (
    SELECT 1
      FROM `system_role` AS `role`
     WHERE `role`.`code` = 'mes_scheduler'
       AND `role`.`status` = 0
       AND `role`.`deleted` = b'0'
  )
  AND NOT EXISTS (
    SELECT `role`.`tenant_id`
      FROM `system_role` AS `role`
     WHERE `role`.`code` = 'mes_scheduler'
       AND `role`.`status` = 0
       AND `role`.`deleted` = b'0'
     GROUP BY `role`.`tenant_id`
    HAVING COUNT(*) <> 1
  )
  AND (
    SELECT COUNT(*)
      FROM `system_menu` AS `menu`
     WHERE `menu`.`permission` = 'mes:pro-schedule-order:delete'
       AND `menu`.`type` = 3
       AND `menu`.`status` = 0
       AND `menu`.`deleted` = b'0'
  ) = 1
  AND NOT EXISTS (
    SELECT `role_menu`.`role_id`, `role_menu`.`tenant_id`, `role_menu`.`menu_id`
      FROM `system_role` AS `role`
      JOIN `system_role_menu` AS `role_menu`
        ON `role_menu`.`role_id` = `role`.`id`
       AND `role_menu`.`tenant_id` = `role`.`tenant_id`
       AND `role_menu`.`deleted` = b'0'
      JOIN `system_menu` AS `menu`
        ON `menu`.`id` = `role_menu`.`menu_id`
       AND `menu`.`permission` = 'mes:pro-schedule-order:delete'
       AND `menu`.`type` = 3
       AND `menu`.`status` = 0
       AND `menu`.`deleted` = b'0'
     WHERE `role`.`code` = 'mes_scheduler'
       AND `role`.`status` = 0
       AND `role`.`deleted` = b'0'
     GROUP BY `role_menu`.`role_id`, `role_menu`.`tenant_id`, `role_menu`.`menu_id`
    HAVING COUNT(*) > 1
  )
THEN 'TARGET_PREFLIGHT_PASS:20260911_mes_scheduler_schedule_order_delete_permission'
ELSE 'TARGET_PREFLIGHT_BLOCKED:20260911_mes_scheduler_schedule_order_delete_permission'
END;
