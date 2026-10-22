-- release-target-preflight: migrationId=20260918_mes_pqc_release_owner_admin_assignment; allowedEnvironments=test,backup,prod
SELECT CASE
  WHEN (SELECT COUNT(*) FROM `system_users` AS `u` JOIN `system_tenant` AS `t` ON `t`.`id` = `u`.`tenant_id` AND `t`.`deleted` = b'0' WHERE `u`.`tenant_id` = 1 AND `u`.`username` = 'admin' AND `u`.`status` = 0 AND `u`.`deleted` = b'0') = 1
   AND (SELECT COUNT(*) FROM `system_role` WHERE `tenant_id` = 1 AND `code` = 'MES_PQC_RELEASE_OWNER' AND `status` = 0 AND `deleted` = b'0') = 1
   AND EXISTS (SELECT 1 FROM `system_users` AS `u` WHERE `u`.`tenant_id` = 1 AND `u`.`username` = 'admin' AND `u`.`status` = 0 AND `u`.`deleted` = b'0')
  THEN 'TARGET_PREFLIGHT_PASS:20260918_mes_pqc_release_owner_admin_assignment'
  ELSE 'TARGET_PREFLIGHT_BLOCKED:20260918_mes_pqc_release_owner_admin_assignment'
END;
