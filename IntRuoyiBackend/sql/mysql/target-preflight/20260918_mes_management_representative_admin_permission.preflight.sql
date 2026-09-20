-- release-target-preflight: migrationId=20260918_mes_management_representative_admin_permission; allowedEnvironments=test,backup,prod
SELECT CASE
  WHEN (SELECT COUNT(*) FROM `system_users` AS `u` JOIN `system_tenant` AS `t` ON `t`.`id` = `u`.`tenant_id` AND `t`.`deleted` = b'0' WHERE `u`.`username` = 'admin' AND `u`.`status` = 0 AND `u`.`deleted` = b'0') = 1
   AND (SELECT COUNT(*) FROM `system_role` AS `r` JOIN `system_users` AS `u` ON `u`.`tenant_id` = `r`.`tenant_id` AND `u`.`username` = 'admin' AND `u`.`status` = 0 AND `u`.`deleted` = b'0' WHERE `r`.`code` = 'MES_MANAGEMENT_REPRESENTATIVE' AND `r`.`status` = 0 AND `r`.`deleted` = b'0') = 1
   AND (SELECT COUNT(*) FROM `system_menu` WHERE `permission` IN ('mes:pro-edhr-release:query','mes:pro-edhr-release:approve') AND `status` = 0 AND `deleted` = b'0') = 2
   AND NOT EXISTS (
     SELECT 1
     FROM `system_users` AS `u`
     JOIN `system_role` AS `r` ON `r`.`tenant_id` = `u`.`tenant_id` AND `r`.`code` = 'MES_MANAGEMENT_REPRESENTATIVE' AND `r`.`status` = 0 AND `r`.`deleted` = b'0'
     JOIN `system_menu` AS `m` ON `m`.`permission` IN ('mes:pro-edhr-release:query','mes:pro-edhr-release:approve') AND `m`.`status` = 0 AND `m`.`deleted` = b'0'
     WHERE `u`.`username` = 'admin' AND `u`.`status` = 0 AND `u`.`deleted` = b'0'
       AND NOT EXISTS (SELECT 1 FROM `system_user_role` AS `ur` WHERE `ur`.`user_id` = `u`.`id` AND `ur`.`role_id` = `r`.`id` AND `ur`.`tenant_id` = `u`.`tenant_id` AND `ur`.`deleted` = b'0')
   )
  THEN 'TARGET_PREFLIGHT_PASS:20260918_mes_management_representative_admin_permission'
  ELSE 'TARGET_PREFLIGHT_BLOCKED:20260918_mes_management_representative_admin_permission'
END;
