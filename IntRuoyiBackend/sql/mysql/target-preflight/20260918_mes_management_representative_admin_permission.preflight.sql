-- release-target-preflight: migrationId=20260918_mes_management_representative_admin_permission; allowedEnvironments=test,backup,prod
SELECT CASE
  WHEN (SELECT COUNT(*) FROM `system_users` AS `u` JOIN `system_tenant` AS `t` ON `t`.`id` = `u`.`tenant_id` AND `t`.`deleted` = b'0' WHERE `u`.`username` = 'admin' AND `u`.`status` = 0 AND `u`.`deleted` = b'0') = 1
   AND (SELECT COUNT(*) FROM `system_role` AS `r` JOIN `system_users` AS `u` ON `u`.`tenant_id` = `r`.`tenant_id` AND `u`.`username` = 'admin' AND `u`.`status` = 0 AND `u`.`deleted` = b'0' WHERE `r`.`code` = 'MES_MANAGEMENT_REPRESENTATIVE' AND `r`.`status` = 0 AND `r`.`deleted` = b'0') = 1
   AND EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-release:query' AND `status` = 0 AND `deleted` = b'0')
   AND EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-release:approve' AND `status` = 0 AND `deleted` = b'0')
   AND EXISTS (
     SELECT 1 FROM `system_users` AS `u`
     WHERE `u`.`username` = 'admin' AND `u`.`status` = 0 AND `u`.`deleted` = b'0'
   )
  THEN 'TARGET_PREFLIGHT_PASS:20260918_mes_management_representative_admin_permission'
  ELSE 'TARGET_PREFLIGHT_BLOCKED:20260918_mes_management_representative_admin_permission'
END;
