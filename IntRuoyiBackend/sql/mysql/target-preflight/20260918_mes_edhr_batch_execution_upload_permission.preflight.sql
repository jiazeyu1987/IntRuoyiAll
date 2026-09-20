-- release-target-preflight: migrationId=20260918_mes_edhr_batch_execution_upload_permission; allowedEnvironments=test,backup,prod
SELECT CASE
  WHEN EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900033 AND `permission` = 'mes:pro-edhr-batch-execution:query' AND `type` = 2 AND `deleted` = b'0')
   AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-batch-execution:upload' AND `id` <> 900043 AND `deleted` = b'0')
   AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900043 AND `permission` <> 'mes:pro-edhr-batch-execution:upload' AND `deleted` = b'0')
   AND EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900043 AND `name` = 'eDHR批次执行上传' AND `permission` = 'mes:pro-edhr-batch-execution:upload' AND `type` = 3 AND `parent_id` = 900033 AND `status` = 0 AND `deleted` = b'0')
   AND NOT EXISTS (
     SELECT 1
     FROM `system_users` AS `u`
     JOIN `system_user_role` AS `ur` ON `ur`.`user_id` = `u`.`id` AND `ur`.`tenant_id` = `u`.`tenant_id` AND `ur`.`deleted` = b'0'
     JOIN `system_role` AS `r` ON `r`.`id` = `ur`.`role_id` AND `r`.`tenant_id` = `u`.`tenant_id` AND `r`.`code` = 'super_admin' AND `r`.`status` = 0 AND `r`.`deleted` = b'0'
     WHERE `u`.`username` = 'admin' AND `u`.`deleted` = b'0'
       AND NOT EXISTS (SELECT 1 FROM `system_role_menu` AS `rm` WHERE `rm`.`role_id` = `r`.`id` AND `rm`.`tenant_id` = `r`.`tenant_id` AND `rm`.`menu_id` = 900043 AND `rm`.`deleted` = b'0')
   )
  THEN 'TARGET_PREFLIGHT_PASS:20260918_mes_edhr_batch_execution_upload_permission'
  ELSE 'TARGET_PREFLIGHT_BLOCKED:20260918_mes_edhr_batch_execution_upload_permission'
END;
