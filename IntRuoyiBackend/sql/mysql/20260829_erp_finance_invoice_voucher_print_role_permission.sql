-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260828_erp_finance_invoice_voucher_print_menu,20260707_system_role_category_management; type=permission; riskLevel=medium
-- Ensure only finance_invoice_voucher_print owns the visible invoice voucher print entry.

SET NAMES utf8mb4;
START TRANSACTION;

DROP PROCEDURE IF EXISTS ensure_erp_invoice_print_role_20260829;
DELIMITER //
CREATE PROCEDURE ensure_erp_invoice_print_role_20260829()
BEGIN
  IF EXISTS (
    SELECT 1
      FROM `system_tenant_package`
     WHERE `deleted` = b'0'
       AND NOT JSON_VALID(`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot assign finance invoice voucher print role';
  END IF;

  IF NOT EXISTS (
    SELECT 1
      FROM `system_menu`
     WHERE `id` = 2563
       AND `status` = 0
       AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing enabled ERP system menu 2563';
  END IF;

  IF NOT EXISTS (
    SELECT 1
      FROM `system_menu`
     WHERE `id` = 2645
       AND `status` = 0
       AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing enabled ERP finance menu 2645';
  END IF;

  IF NOT EXISTS (
    SELECT 1
      FROM `system_menu`
     WHERE `id` = 6034
       AND `status` = 0
       AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing enabled invoice voucher print menu 6034';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM `system_menu`
     WHERE `id` = 6034
       AND `deleted` = b'0'
       AND NOT (
         `name` = '发票凭证打印'
         AND `permission` = 'erp:invoice-voucher-print:query'
         AND `type` = 2
         AND `parent_id` = 2645
         AND `path` = 'invoice-voucher-print'
         AND `component` = 'erp/finance/invoice-voucher-print/index'
         AND `component_name` = 'ErpInvoiceVoucherPrint'
       )
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invoice voucher print menu 6034 route/component mismatch';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_erp_finance_invoice_voucher_print_package_scope`;
  CREATE TEMPORARY TABLE `tmp_erp_finance_invoice_voucher_print_package_scope` (
    `package_id` bigint NOT NULL PRIMARY KEY
  ) ENGINE=Memory;

  INSERT IGNORE INTO `tmp_erp_finance_invoice_voucher_print_package_scope` (`package_id`)
  SELECT `package`.`id`
    FROM `system_tenant_package` AS `package`
   WHERE `package`.`deleted` = b'0'
     AND JSON_VALID(`package`.`menu_ids`)
     AND (
       JSON_CONTAINS(`package`.`menu_ids`, CAST('2563' AS JSON), '$')
       OR JSON_CONTAINS(`package`.`menu_ids`, CAST('2645' AS JSON), '$')
       OR JSON_CONTAINS(`package`.`menu_ids`, CAST('6034' AS JSON), '$')
     );

  IF (SELECT COUNT(*) FROM `tmp_erp_finance_invoice_voucher_print_package_scope`) = 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing invoice voucher print tenant package menu merge rows';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_erp_finance_invoice_voucher_print_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_erp_finance_invoice_voucher_print_package_menu_ids` (
    `package_id` bigint NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`package_id`, `menu_id`)
  ) ENGINE=Memory;

  INSERT IGNORE INTO `tmp_erp_finance_invoice_voucher_print_package_menu_ids` (`package_id`, `menu_id`)
  SELECT
    `package`.`id`,
    CAST(`existing_menu`.`menu_id` AS UNSIGNED)
    FROM `system_tenant_package` AS `package`
    JOIN `tmp_erp_finance_invoice_voucher_print_package_scope` AS `scope`
      ON `scope`.`package_id` = `package`.`id`
    JOIN JSON_TABLE(
      `package`.`menu_ids`,
      '$[*]' COLUMNS (`menu_id` bigint PATH '$')
    ) AS `existing_menu`
   WHERE `package`.`deleted` = b'0'
     AND JSON_VALID(`package`.`menu_ids`);

  INSERT IGNORE INTO `tmp_erp_finance_invoice_voucher_print_package_menu_ids` (`package_id`, `menu_id`)
  SELECT
    `scope`.`package_id`,
    `required_menu`.`menu_id`
    FROM `tmp_erp_finance_invoice_voucher_print_package_scope` AS `scope`
    JOIN (
      SELECT 2563 AS `menu_id`
      UNION ALL SELECT 2645 AS `menu_id`
      UNION ALL SELECT 6034 AS `menu_id`
    ) AS `required_menu`;

  UPDATE `system_tenant_package` AS `package`
    JOIN (
      SELECT DISTINCT
        `package_id`,
        JSON_ARRAYAGG(`menu_id`) OVER (
          PARTITION BY `package_id`
          ORDER BY `menu_id`
          ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
        ) AS `menu_ids`
        FROM `tmp_erp_finance_invoice_voucher_print_package_menu_ids`
    ) AS `merged`
      ON `merged`.`package_id` = `package`.`id`
     SET `package`.`menu_ids` = `merged`.`menu_ids`,
         `package`.`updater` = 'erp-invoice-voucher-print-role-permission',
         `package`.`update_time` = NOW();

  IF EXISTS (
    SELECT 1
      FROM `system_tenant_package` AS `package`
      JOIN `tmp_erp_finance_invoice_voucher_print_package_scope` AS `scope`
        ON `scope`.`package_id` = `package`.`id`
     WHERE `package`.`deleted` = b'0'
       AND JSON_VALID(`package`.`menu_ids`)
       AND (
         NOT JSON_CONTAINS(`package`.`menu_ids`, CAST('2563' AS JSON), '$')
         OR NOT JSON_CONTAINS(`package`.`menu_ids`, CAST('2645' AS JSON), '$')
         OR NOT JSON_CONTAINS(`package`.`menu_ids`, CAST('6034' AS JSON), '$')
       )
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invoice voucher print tenant package menu merge incomplete';
  END IF;

  IF NOT EXISTS (
    SELECT 1
      FROM `system_users` AS `user`
     WHERE `user`.`username` = 'admin'
       AND `user`.`tenant_id` = 1
       AND `user`.`status` = 0
       AND `user`.`deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing tenant 1 admin user for invoice voucher print role binding';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_erp_finance_invoice_voucher_print_target_tenant`;
  CREATE TEMPORARY TABLE `tmp_erp_finance_invoice_voucher_print_target_tenant` (
    `tenant_id` bigint NOT NULL PRIMARY KEY
  ) ENGINE=Memory;

  INSERT IGNORE INTO `tmp_erp_finance_invoice_voucher_print_target_tenant` (`tenant_id`)
  SELECT DISTINCT `tenant`.`id`
    FROM `system_tenant` AS `tenant`
    JOIN `system_tenant_package` AS `package`
      ON `package`.`id` = `tenant`.`package_id`
     AND `package`.`deleted` = b'0'
     AND JSON_VALID(`package`.`menu_ids`)
   WHERE `tenant`.`deleted` = b'0'
     AND JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('6034' AS JSON), '$');

  INSERT IGNORE INTO `tmp_erp_finance_invoice_voucher_print_target_tenant` (`tenant_id`)
  SELECT 1 AS `tenant_id`
    FROM `system_tenant` AS `tenant`
   WHERE `tenant`.`id` = 1
     AND `tenant`.`deleted` = b'0';

  INSERT IGNORE INTO `tmp_erp_finance_invoice_voucher_print_target_tenant` (`tenant_id`)
  SELECT DISTINCT `role`.`tenant_id`
    FROM `system_role_menu` AS `existing_role_menu`
    JOIN `system_role` AS `role`
      ON `role`.`id` = `existing_role_menu`.`role_id`
     AND `role`.`tenant_id` = `existing_role_menu`.`tenant_id`
     AND `role`.`deleted` = b'0'
    JOIN `system_tenant` AS `tenant`
      ON `tenant`.`id` = `role`.`tenant_id`
     AND `tenant`.`deleted` = b'0'
   WHERE `existing_role_menu`.`menu_id` = 6034
     AND `existing_role_menu`.`deleted` = b'0';

  IF (SELECT COUNT(*) FROM `tmp_erp_finance_invoice_voucher_print_target_tenant`) = 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing target tenant for finance invoice voucher print role';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM `system_role_category` AS `category`
      JOIN `tmp_erp_finance_invoice_voucher_print_target_tenant` AS `target_tenant`
        ON `target_tenant`.`tenant_id` = `category`.`tenant_id`
     WHERE `category`.`code` = 'finance'
     GROUP BY `category`.`tenant_id`
    HAVING COUNT(*) > 1
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Duplicate finance role category code in target tenant';
  END IF;

  INSERT INTO `system_role_category` (
    `name`, `code`, `sort`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    '财务',
    'finance',
    55,
    0,
    '财务权限角色',
    'erp-invoice-voucher-print-role-permission',
    NOW(),
    'erp-invoice-voucher-print-role-permission',
    NOW(),
    b'0',
    `target_tenant`.`tenant_id`
    FROM `tmp_erp_finance_invoice_voucher_print_target_tenant` AS `target_tenant`
   WHERE NOT EXISTS (
     SELECT 1
      FROM `system_role_category` AS `existing`
     WHERE `existing`.`tenant_id` = `target_tenant`.`tenant_id`
        AND `existing`.`code` = 'finance'
   );

  UPDATE `system_role_category` AS `category`
    JOIN `tmp_erp_finance_invoice_voucher_print_target_tenant` AS `target_tenant`
      ON `target_tenant`.`tenant_id` = `category`.`tenant_id`
     SET `category`.`name` = '财务',
         `category`.`sort` = 55,
         `category`.`status` = 0,
         `category`.`remark` = '财务权限角色',
         `category`.`updater` = 'erp-invoice-voucher-print-role-permission',
         `category`.`update_time` = NOW(),
         `category`.`deleted` = b'0'
   WHERE `category`.`code` = 'finance';

  IF EXISTS (
    SELECT 1
      FROM `tmp_erp_finance_invoice_voucher_print_target_tenant` AS `target_tenant`
      LEFT JOIN `system_role_category` AS `category`
        ON `category`.`tenant_id` = `target_tenant`.`tenant_id`
       AND `category`.`code` = 'finance'
       AND `category`.`status` = 0
       AND `category`.`deleted` = b'0'
     WHERE `category`.`id` IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing finance role category for invoice voucher print role tenant';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM `system_role` AS `role`
      JOIN `tmp_erp_finance_invoice_voucher_print_target_tenant` AS `target_tenant`
        ON `target_tenant`.`tenant_id` = `role`.`tenant_id`
     WHERE `role`.`code` = 'finance_invoice_voucher_print'
     GROUP BY `role`.`tenant_id`
    HAVING COUNT(*) > 1
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Duplicate finance invoice voucher print role code in target tenant';
  END IF;

  INSERT INTO `system_role` (
    `name`, `code`, `sort`, `category_id`, `data_scope`, `data_scope_dept_ids`,
    `status`, `type`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    '财务发票打印',
    'finance_invoice_voucher_print',
    6034,
    `category`.`id`,
    1,
    '',
    0,
    2,
    '发票凭证打印助手入口权限角色',
    'erp-invoice-voucher-print-role-permission',
    NOW(),
    'erp-invoice-voucher-print-role-permission',
    NOW(),
    b'0',
    `target_tenant`.`tenant_id`
    FROM `tmp_erp_finance_invoice_voucher_print_target_tenant` AS `target_tenant`
    JOIN `system_role_category` AS `category`
      ON `category`.`tenant_id` = `target_tenant`.`tenant_id`
     AND `category`.`code` = 'finance'
     AND `category`.`status` = 0
     AND `category`.`deleted` = b'0'
   WHERE NOT EXISTS (
     SELECT 1
       FROM `system_role` AS `existing_role`
      WHERE `existing_role`.`tenant_id` = `target_tenant`.`tenant_id`
        AND `existing_role`.`code` = 'finance_invoice_voucher_print'
   );

  UPDATE `system_role` AS `role`
    JOIN `tmp_erp_finance_invoice_voucher_print_target_tenant` AS `target_tenant`
      ON `target_tenant`.`tenant_id` = `role`.`tenant_id`
    JOIN `system_role_category` AS `category`
      ON `category`.`tenant_id` = `role`.`tenant_id`
     AND `category`.`code` = 'finance'
     AND `category`.`status` = 0
     AND `category`.`deleted` = b'0'
     SET `role`.`name` = '财务发票打印',
         `role`.`sort` = 6034,
         `role`.`category_id` = `category`.`id`,
         `role`.`data_scope` = 1,
         `role`.`data_scope_dept_ids` = '',
         `role`.`status` = 0,
         `role`.`type` = 2,
         `role`.`remark` = '发票凭证打印助手入口权限角色',
         `role`.`deleted` = b'0',
         `role`.`updater` = 'erp-invoice-voucher-print-role-permission',
         `role`.`update_time` = NOW()
   WHERE `role`.`code` = 'finance_invoice_voucher_print';

  DROP TEMPORARY TABLE IF EXISTS `tmp_erp_finance_invoice_voucher_print_role`;
  CREATE TEMPORARY TABLE `tmp_erp_finance_invoice_voucher_print_role` (
    `role_id` bigint NOT NULL,
    `tenant_id` bigint NOT NULL,
    PRIMARY KEY (`role_id`, `tenant_id`)
  ) ENGINE=Memory;

  INSERT INTO `tmp_erp_finance_invoice_voucher_print_role` (`role_id`, `tenant_id`)
  SELECT `role`.`id`, `role`.`tenant_id`
    FROM `system_role` AS `role`
    JOIN `tmp_erp_finance_invoice_voucher_print_target_tenant` AS `target_tenant`
      ON `target_tenant`.`tenant_id` = `role`.`tenant_id`
   WHERE `role`.`code` = 'finance_invoice_voucher_print'
     AND `role`.`status` = 0
     AND `role`.`deleted` = b'0';

  IF (SELECT COUNT(*) FROM `tmp_erp_finance_invoice_voucher_print_role`) <> (SELECT COUNT(*) FROM `tmp_erp_finance_invoice_voucher_print_target_tenant`) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Finance invoice voucher print role creation or recovery failed';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_erp_finance_invoice_voucher_print_role_permission_menu`;
  CREATE TEMPORARY TABLE `tmp_erp_finance_invoice_voucher_print_role_permission_menu` (
    `menu_id` bigint NOT NULL PRIMARY KEY
  ) ENGINE=Memory;

  INSERT INTO `tmp_erp_finance_invoice_voucher_print_role_permission_menu` (`menu_id`)
  SELECT 2563 AS `menu_id`
  UNION ALL
  SELECT 2645 AS `menu_id`
  UNION ALL
  SELECT 6034 AS `menu_id`;

  IF EXISTS (
    SELECT 1
      FROM `tmp_erp_finance_invoice_voucher_print_role_permission_menu` AS `expected_menu`
      LEFT JOIN `system_menu` AS `menu`
        ON `menu`.`id` = `expected_menu`.`menu_id`
       AND `menu`.`status` = 0
       AND `menu`.`deleted` = b'0'
     WHERE `menu`.`id` IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing invoice voucher print role permission menu';
  END IF;

  UPDATE `system_role_menu` AS `role_menu`
    JOIN `system_role` AS `role`
      ON `role`.`id` = `role_menu`.`role_id`
     AND `role`.`tenant_id` = `role_menu`.`tenant_id`
     AND `role`.`deleted` = b'0'
    JOIN `tmp_erp_finance_invoice_voucher_print_target_tenant` AS `target_tenant`
      ON `target_tenant`.`tenant_id` = `role_menu`.`tenant_id`
     SET `role_menu`.`deleted` = b'1',
         `role_menu`.`updater` = 'erp-invoice-voucher-print-role-permission',
         `role_menu`.`update_time` = NOW()
   WHERE `role_menu`.`menu_id` = 6034
     AND `role_menu`.`deleted` = b'0'
     AND `role`.`code` <> 'finance_invoice_voucher_print';

  UPDATE `system_role_menu` AS `role_menu`
    JOIN `tmp_erp_finance_invoice_voucher_print_role` AS `print_role`
      ON `print_role`.`role_id` = `role_menu`.`role_id`
     AND `print_role`.`tenant_id` = `role_menu`.`tenant_id`
    JOIN `tmp_erp_finance_invoice_voucher_print_role_permission_menu` AS `expected_menu`
      ON `expected_menu`.`menu_id` = `role_menu`.`menu_id`
     SET `role_menu`.`deleted` = b'0',
         `role_menu`.`updater` = 'erp-invoice-voucher-print-role-permission',
         `role_menu`.`update_time` = NOW();

  INSERT INTO `system_role_menu` (
    `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    `print_role`.`role_id`,
    `expected_menu`.`menu_id`,
    'erp-invoice-voucher-print-role-permission',
    NOW(),
    'erp-invoice-voucher-print-role-permission',
    NOW(),
    b'0',
    `print_role`.`tenant_id`
    FROM `tmp_erp_finance_invoice_voucher_print_role` AS `print_role`
    CROSS JOIN `tmp_erp_finance_invoice_voucher_print_role_permission_menu` AS `expected_menu`
   WHERE NOT EXISTS (
     SELECT 1
       FROM `system_role_menu` AS `existing`
      WHERE `existing`.`role_id` = `print_role`.`role_id`
        AND `existing`.`tenant_id` = `print_role`.`tenant_id`
        AND `existing`.`menu_id` = `expected_menu`.`menu_id`
   );

  DROP TEMPORARY TABLE IF EXISTS `tmp_erp_finance_invoice_voucher_print_role_menu_keep`;
  CREATE TEMPORARY TABLE `tmp_erp_finance_invoice_voucher_print_role_menu_keep` AS
  SELECT
    MIN(`role_menu`.`id`) AS `keep_id`,
    `role_menu`.`role_id`,
    `role_menu`.`tenant_id`,
    `role_menu`.`menu_id`
    FROM `system_role_menu` AS `role_menu`
    JOIN `tmp_erp_finance_invoice_voucher_print_role` AS `print_role`
      ON `print_role`.`role_id` = `role_menu`.`role_id`
     AND `print_role`.`tenant_id` = `role_menu`.`tenant_id`
    JOIN `tmp_erp_finance_invoice_voucher_print_role_permission_menu` AS `expected_menu`
      ON `expected_menu`.`menu_id` = `role_menu`.`menu_id`
   WHERE `role_menu`.`deleted` = b'0'
   GROUP BY `role_menu`.`role_id`, `role_menu`.`tenant_id`, `role_menu`.`menu_id`;

  UPDATE `system_role_menu` AS `role_menu`
    JOIN `tmp_erp_finance_invoice_voucher_print_role_menu_keep` AS `keep`
      ON `keep`.`role_id` = `role_menu`.`role_id`
     AND `keep`.`tenant_id` = `role_menu`.`tenant_id`
     AND `keep`.`menu_id` = `role_menu`.`menu_id`
     SET `role_menu`.`deleted` = b'1',
         `role_menu`.`updater` = 'erp-invoice-voucher-print-role-permission',
         `role_menu`.`update_time` = NOW()
   WHERE `role_menu`.`deleted` = b'0'
     AND `role_menu`.`id` <> `keep`.`keep_id`;

  UPDATE `system_user_role` AS `user_role`
    JOIN `system_users` AS `user`
      ON `user`.`id` = `user_role`.`user_id`
     AND `user`.`username` = 'admin'
     AND `user`.`tenant_id` = 1
     AND `user`.`status` = 0
     AND `user`.`deleted` = b'0'
    JOIN `system_role` AS `role`
      ON `role`.`id` = `user_role`.`role_id`
     AND `role`.`tenant_id` = `user`.`tenant_id`
     AND `role`.`code` = 'finance_invoice_voucher_print'
     AND `role`.`deleted` = b'0'
     SET `user_role`.`deleted` = b'0',
         `user_role`.`tenant_id` = `user`.`tenant_id`,
         `user_role`.`updater` = 'erp-invoice-voucher-print-role-permission',
         `user_role`.`update_time` = NOW();

  INSERT INTO `system_user_role` (
    `user_id`, `role_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    `user`.`id`,
    `role`.`id`,
    'erp-invoice-voucher-print-role-permission',
    NOW(),
    'erp-invoice-voucher-print-role-permission',
    NOW(),
    b'0',
    `user`.`tenant_id`
    FROM `system_users` AS `user`
    JOIN `system_role` AS `role`
      ON `role`.`tenant_id` = `user`.`tenant_id`
     AND `role`.`code` = 'finance_invoice_voucher_print'
     AND `role`.`status` = 0
     AND `role`.`deleted` = b'0'
   WHERE `user`.`username` = 'admin'
     AND `user`.`tenant_id` = 1
     AND `user`.`status` = 0
     AND `user`.`deleted` = b'0'
     AND NOT EXISTS (
       SELECT 1
         FROM `system_user_role` AS `existing`
        WHERE `existing`.`user_id` = `user`.`id`
          AND `existing`.`role_id` = `role`.`id`
     );

  IF EXISTS (
    SELECT 1
      FROM `tmp_erp_finance_invoice_voucher_print_role` AS `print_role`
      CROSS JOIN `tmp_erp_finance_invoice_voucher_print_role_permission_menu` AS `expected_menu`
     WHERE NOT EXISTS (
       SELECT 1
         FROM `system_role_menu` AS `role_menu`
        WHERE `role_menu`.`role_id` = `print_role`.`role_id`
          AND `role_menu`.`tenant_id` = `print_role`.`tenant_id`
          AND `role_menu`.`menu_id` = `expected_menu`.`menu_id`
          AND `role_menu`.`deleted` = b'0'
     )
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invoice voucher print role menu permission grant incomplete';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM `system_role_menu` AS `role_menu`
      JOIN `tmp_erp_finance_invoice_voucher_print_role` AS `print_role`
        ON `print_role`.`role_id` = `role_menu`.`role_id`
       AND `print_role`.`tenant_id` = `role_menu`.`tenant_id`
      JOIN `tmp_erp_finance_invoice_voucher_print_role_permission_menu` AS `expected_menu`
        ON `expected_menu`.`menu_id` = `role_menu`.`menu_id`
     WHERE `role_menu`.`deleted` = b'0'
     GROUP BY `role_menu`.`role_id`, `role_menu`.`tenant_id`, `role_menu`.`menu_id`
    HAVING COUNT(*) > 1
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invoice voucher print role menu permission grant has duplicate active bindings';
  END IF;

  IF NOT EXISTS (
    SELECT 1
      FROM `system_user_role` AS `user_role`
      JOIN `system_users` AS `user`
        ON `user`.`id` = `user_role`.`user_id`
       AND `user`.`username` = 'admin'
       AND `user`.`tenant_id` = 1
       AND `user`.`status` = 0
       AND `user`.`deleted` = b'0'
      JOIN `system_role` AS `role`
        ON `role`.`id` = `user_role`.`role_id`
       AND `role`.`tenant_id` = `user`.`tenant_id`
       AND `role`.`code` = 'finance_invoice_voucher_print'
       AND `role`.`deleted` = b'0'
     WHERE `user_role`.`deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Admin user is not assigned invoice voucher print role';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM `system_role_menu` AS `role_menu`
      JOIN `system_role` AS `role`
        ON `role`.`id` = `role_menu`.`role_id`
       AND `role`.`tenant_id` = `role_menu`.`tenant_id`
       AND `role`.`deleted` = b'0'
     WHERE `role_menu`.`menu_id` = 6034
       AND `role_menu`.`deleted` = b'0'
       AND `role`.`code` <> 'finance_invoice_voucher_print'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Active invoice voucher print menu is still granted to a non-finance print role';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_erp_finance_invoice_voucher_print_role_menu_keep`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_erp_finance_invoice_voucher_print_role_permission_menu`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_erp_finance_invoice_voucher_print_role`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_erp_finance_invoice_voucher_print_target_tenant`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_erp_finance_invoice_voucher_print_package_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_erp_finance_invoice_voucher_print_package_scope`;
END//
DELIMITER ;

CALL ensure_erp_invoice_print_role_20260829();

DROP PROCEDURE IF EXISTS ensure_erp_invoice_print_role_20260829;

COMMIT;
