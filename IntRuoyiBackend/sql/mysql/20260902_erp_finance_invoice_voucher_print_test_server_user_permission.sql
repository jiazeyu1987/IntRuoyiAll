-- release-migration: allowedEnvironments=test; dependsOn=20260829_erp_finance_invoice_voucher_print_role_permission; type=permission; riskLevel=low
SET NAMES utf8mb4;

START TRANSACTION;

DROP PROCEDURE IF EXISTS ensure_erp_invoice_print_test_server_user_permission_20260902;
DELIMITER //
CREATE PROCEDURE ensure_erp_invoice_print_test_server_user_permission_20260902()
BEGIN
  IF NOT EXISTS (
    SELECT 1
      FROM `system_menu` AS `menu`
     WHERE `menu`.`id` = 6034
       AND `menu`.`permission` = 'erp:invoice-voucher-print:query'
       AND `menu`.`status` = 0
       AND `menu`.`deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing enabled invoice voucher print menu 6034 for test server user permission';
  END IF;

  IF (
    SELECT COUNT(*)
      FROM `system_users` AS `target_user`
      JOIN `system_tenant` AS `tenant`
        ON `tenant`.`id` = `target_user`.`tenant_id`
       AND `tenant`.`status` = 0
       AND `tenant`.`deleted` = b'0'
     WHERE `target_user`.`username` = 'admin'
       AND `target_user`.`status` = 0
       AND `target_user`.`deleted` = b'0'
  ) <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Test server invoice voucher print user admin is missing or ambiguous';
  END IF;

  IF (
    SELECT COUNT(*)
      FROM `system_users` AS `target_user`
      JOIN `system_tenant` AS `tenant`
        ON `tenant`.`id` = `target_user`.`tenant_id`
       AND `tenant`.`status` = 0
       AND `tenant`.`deleted` = b'0'
     WHERE `target_user`.`username` = 'jiyingying'
       AND `target_user`.`status` = 0
       AND `target_user`.`deleted` = b'0'
  ) <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Test server invoice voucher print user jiyingying is missing or ambiguous';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM `system_users` AS `target_user`
      JOIN `system_tenant` AS `tenant`
        ON `tenant`.`id` = `target_user`.`tenant_id`
       AND `tenant`.`status` = 0
       AND `tenant`.`deleted` = b'0'
      LEFT JOIN `system_role` AS `role`
        ON `role`.`tenant_id` = `target_user`.`tenant_id`
       AND `role`.`code` = 'finance_invoice_voucher_print'
       AND `role`.`status` = 0
       AND `role`.`deleted` = b'0'
     WHERE `target_user`.`username` IN ('admin', 'jiyingying')
       AND `target_user`.`status` = 0
       AND `target_user`.`deleted` = b'0'
       AND `role`.`id` IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing finance invoice voucher print role for test server target user tenant';
  END IF;

  UPDATE `system_user_role` AS `user_role`
    JOIN `system_users` AS `target_user`
      ON `target_user`.`id` = `user_role`.`user_id`
     AND `target_user`.`username` IN ('admin', 'jiyingying')
     AND `target_user`.`status` = 0
     AND `target_user`.`deleted` = b'0'
    JOIN `system_tenant` AS `tenant`
      ON `tenant`.`id` = `target_user`.`tenant_id`
     AND `tenant`.`status` = 0
     AND `tenant`.`deleted` = b'0'
    JOIN `system_role` AS `role`
      ON `role`.`id` = `user_role`.`role_id`
     AND `role`.`tenant_id` = `target_user`.`tenant_id`
     AND `role`.`code` = 'finance_invoice_voucher_print'
     AND `role`.`status` = 0
     AND `role`.`deleted` = b'0'
     SET `user_role`.`deleted` = b'0',
         `user_role`.`tenant_id` = `target_user`.`tenant_id`,
         `user_role`.`updater` = 'erp-invoice-voucher-print-test-server-user-permission',
         `user_role`.`update_time` = NOW();

  INSERT INTO `system_user_role` (
    `user_id`, `role_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    `target_user`.`id`,
    `role`.`id`,
    'erp-invoice-voucher-print-test-server-user-permission',
    NOW(),
    'erp-invoice-voucher-print-test-server-user-permission',
    NOW(),
    b'0',
    `target_user`.`tenant_id`
    FROM `system_users` AS `target_user`
    JOIN `system_tenant` AS `tenant`
      ON `tenant`.`id` = `target_user`.`tenant_id`
     AND `tenant`.`status` = 0
     AND `tenant`.`deleted` = b'0'
    JOIN `system_role` AS `role`
      ON `role`.`tenant_id` = `target_user`.`tenant_id`
     AND `role`.`code` = 'finance_invoice_voucher_print'
     AND `role`.`status` = 0
     AND `role`.`deleted` = b'0'
   WHERE `target_user`.`username` IN ('admin', 'jiyingying')
     AND `target_user`.`status` = 0
     AND `target_user`.`deleted` = b'0'
     AND NOT EXISTS (
       SELECT 1
         FROM `system_user_role` AS `existing`
        WHERE `existing`.`user_id` = `target_user`.`id`
          AND `existing`.`role_id` = `role`.`id`
          AND `existing`.`deleted` = b'0'
     );

  IF EXISTS (
    SELECT 1
      FROM `system_users` AS `target_user`
      JOIN `system_tenant` AS `tenant`
        ON `tenant`.`id` = `target_user`.`tenant_id`
       AND `tenant`.`status` = 0
       AND `tenant`.`deleted` = b'0'
      JOIN `system_role` AS `role`
        ON `role`.`tenant_id` = `target_user`.`tenant_id`
       AND `role`.`code` = 'finance_invoice_voucher_print'
       AND `role`.`status` = 0
       AND `role`.`deleted` = b'0'
      LEFT JOIN `system_user_role` AS `user_role`
        ON `user_role`.`user_id` = `target_user`.`id`
       AND `user_role`.`role_id` = `role`.`id`
       AND `user_role`.`tenant_id` = `target_user`.`tenant_id`
       AND `user_role`.`deleted` = b'0'
     WHERE `target_user`.`username` IN ('admin', 'jiyingying')
       AND `target_user`.`status` = 0
       AND `target_user`.`deleted` = b'0'
       AND `user_role`.`id` IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Test server invoice voucher print user permission grant incomplete';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM `system_users` AS `target_user`
      JOIN `system_tenant` AS `tenant`
        ON `tenant`.`id` = `target_user`.`tenant_id`
       AND `tenant`.`status` = 0
       AND `tenant`.`deleted` = b'0'
      JOIN `system_role` AS `role`
        ON `role`.`tenant_id` = `target_user`.`tenant_id`
       AND `role`.`code` = 'finance_invoice_voucher_print'
       AND `role`.`status` = 0
       AND `role`.`deleted` = b'0'
      JOIN `system_user_role` AS `user_role`
        ON `user_role`.`user_id` = `target_user`.`id`
       AND `user_role`.`role_id` = `role`.`id`
       AND `user_role`.`tenant_id` = `target_user`.`tenant_id`
       AND `user_role`.`deleted` = b'0'
     WHERE `target_user`.`username` IN ('admin', 'jiyingying')
       AND `target_user`.`status` = 0
       AND `target_user`.`deleted` = b'0'
     GROUP BY `target_user`.`id`, `role`.`id`
    HAVING COUNT(*) <> 1
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Test server invoice voucher print user permission grant has duplicate active bindings';
  END IF;
END//
DELIMITER ;

CALL ensure_erp_invoice_print_test_server_user_permission_20260902();
DROP PROCEDURE IF EXISTS ensure_erp_invoice_print_test_server_user_permission_20260902;

COMMIT;
