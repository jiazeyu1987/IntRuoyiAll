-- release-migration: allowedEnvironments=test; dependsOn=20260829_erp_finance_invoice_voucher_print_role_permission; type=permission; riskLevel=low
SET NAMES utf8mb4;

START TRANSACTION;

DROP PROCEDURE IF EXISTS ensure_erp_invoice_print_test_tenant_user_permission_20260830;
DELIMITER //
CREATE PROCEDURE ensure_erp_invoice_print_test_tenant_user_permission_20260830()
BEGIN
  IF NOT EXISTS (
    SELECT 1
      FROM `system_tenant` AS `tenant`
     WHERE `tenant`.`id` = 122
       AND `tenant`.`name` = '测试租户'
       AND `tenant`.`deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing test tenant 122 name=测试租户 for invoice voucher print user permission';
  END IF;

  IF NOT EXISTS (
    SELECT 1
      FROM `system_users` AS `user`
     WHERE `user`.`username` = 'aoteman'
       AND `user`.`tenant_id` = 122
       AND `user`.`status` = 0
       AND `user`.`deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing test tenant aoteman user for invoice voucher print user permission';
  END IF;

  IF NOT EXISTS (
    SELECT 1
      FROM `system_role` AS `role`
     WHERE `role`.`tenant_id` = 122
       AND `role`.`code` = 'finance_invoice_voucher_print'
       AND `role`.`status` = 0
       AND `role`.`deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing finance invoice voucher print role in test tenant';
  END IF;

  UPDATE `system_user_role` AS `user_role`
    JOIN `system_users` AS `user`
      ON `user`.`id` = `user_role`.`user_id`
     AND `user`.`username` = 'aoteman'
     AND `user`.`tenant_id` = 122
     AND `user`.`status` = 0
     AND `user`.`deleted` = b'0'
    JOIN `system_role` AS `role`
      ON `role`.`id` = `user_role`.`role_id`
     AND `role`.`tenant_id` = `user`.`tenant_id`
     AND `role`.`code` = 'finance_invoice_voucher_print'
     AND `role`.`status` = 0
     AND `role`.`deleted` = b'0'
     SET `user_role`.`deleted` = b'0',
         `user_role`.`tenant_id` = `user`.`tenant_id`,
         `user_role`.`updater` = 'erp-invoice-voucher-print-test-user-permission',
         `user_role`.`update_time` = NOW()
   WHERE `user_role`.`deleted` = b'0';

  INSERT INTO `system_user_role` (
    `user_id`, `role_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    `user`.`id`,
    `role`.`id`,
    'erp-invoice-voucher-print-test-user-permission',
    NOW(),
    'erp-invoice-voucher-print-test-user-permission',
    NOW(),
    b'0',
    `user`.`tenant_id`
    FROM `system_users` AS `user`
    JOIN `system_role` AS `role`
      ON `role`.`tenant_id` = `user`.`tenant_id`
     AND `role`.`code` = 'finance_invoice_voucher_print'
     AND `role`.`status` = 0
     AND `role`.`deleted` = b'0'
   WHERE `user`.`username` = 'aoteman'
     AND `user`.`tenant_id` = 122
     AND `user`.`status` = 0
     AND `user`.`deleted` = b'0'
     AND NOT EXISTS (
       SELECT 1
         FROM `system_user_role` AS `existing`
        WHERE `existing`.`user_id` = `user`.`id`
          AND `existing`.`role_id` = `role`.`id`
          AND `existing`.`deleted` = b'0'
     );

  IF NOT EXISTS (
    SELECT 1
      FROM `system_user_role` AS `user_role`
      JOIN `system_users` AS `user`
        ON `user`.`id` = `user_role`.`user_id`
       AND `user`.`username` = 'aoteman'
       AND `user`.`tenant_id` = 122
       AND `user`.`status` = 0
       AND `user`.`deleted` = b'0'
      JOIN `system_role` AS `role`
        ON `role`.`id` = `user_role`.`role_id`
       AND `role`.`tenant_id` = `user`.`tenant_id`
       AND `role`.`code` = 'finance_invoice_voucher_print'
       AND `role`.`deleted` = b'0'
     WHERE `user_role`.`deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Test tenant aoteman is not assigned invoice voucher print role';
  END IF;
END//
DELIMITER ;

CALL ensure_erp_invoice_print_test_tenant_user_permission_20260830();
DROP PROCEDURE IF EXISTS ensure_erp_invoice_print_test_tenant_user_permission_20260830;

COMMIT;
