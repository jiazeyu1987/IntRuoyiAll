-- release-target-preflight: migrationId=20260917_mes_edhr_batch_execution_admin_visible; allowedEnvironments=test,backup,prod
SELECT CASE
  WHEN EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900220 AND `deleted` = b'0')
   AND EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900033 AND `permission` = 'mes:pro-edhr-batch-execution:query' AND `type` = 2 AND `parent_id` = 900220 AND `status` = 0 AND `deleted` = b'0')
   AND EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900034 AND `permission` = 'mes:pro-edhr-batch-execution:query' AND `type` = 3 AND `status` = 0 AND `deleted` = b'0')
   AND EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900036 AND `permission` = 'mes:pro-edhr-batch-execution:update' AND `type` = 3 AND `status` = 0 AND `deleted` = b'0')
   AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `path` = '/mes/pro/feedback/edhr-batch-execution' AND `id` <> 900033 AND `deleted` = b'0')
   AND NOT EXISTS (
     SELECT 1 FROM `system_tenant_package`
     WHERE `deleted` = b'0' AND `menu_ids` IS NOT NULL AND `menu_ids` <> '' AND NOT JSON_VALID(`menu_ids`)
   )
   AND NOT EXISTS (
     SELECT 1
     FROM `system_users` AS `u`
     JOIN `system_user_role` AS `ur` ON `ur`.`user_id` = `u`.`id` AND `ur`.`tenant_id` = `u`.`tenant_id` AND `ur`.`deleted` = b'0'
     JOIN `system_role` AS `r` ON `r`.`id` = `ur`.`role_id` AND `r`.`tenant_id` = `u`.`tenant_id` AND `r`.`code` = 'super_admin' AND `r`.`status` = 0 AND `r`.`deleted` = b'0'
     WHERE `u`.`username` = 'admin' AND `u`.`deleted` = b'0'
       AND NOT EXISTS (
         SELECT 1 FROM `system_role_menu` AS `rm`
         WHERE `rm`.`role_id` = `r`.`id` AND `rm`.`tenant_id` = `r`.`tenant_id`
           AND `rm`.`menu_id` IN (900220,900033,900034,900036) AND `rm`.`deleted` = b'0'
       )
   )
  THEN 'TARGET_PREFLIGHT_PASS:20260917_mes_edhr_batch_execution_admin_visible'
  ELSE 'TARGET_PREFLIGHT_BLOCKED:20260917_mes_edhr_batch_execution_admin_visible'
END;
