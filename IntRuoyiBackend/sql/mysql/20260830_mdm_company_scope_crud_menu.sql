-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260829_registration_certificate_management_menu_hierarchy; type=data; riskLevel=medium
-- Purpose: Add a formal authorized-company maintenance page and grant query/create/update/delete permissions.

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_mdm_company_scope_crud_menu;
DELIMITER $$
CREATE PROCEDURE ensure_mdm_company_scope_crud_menu()
BEGIN
  IF NOT EXISTS (
      SELECT 1
        FROM `system_menu`
       WHERE `id` = 990200
         AND `deleted` = b'0'
         AND `path` = '/mdm'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing MDM basic data menu 990200 for authorized-company menu';
  END IF;

  IF EXISTS (
      SELECT 1
        FROM `system_tenant_package` AS `package`
       WHERE `package`.`deleted` = b'0'
         AND NOT JSON_VALID(`package`.`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot merge authorized-company menu';
  END IF;

  IF EXISTS (
      SELECT 1
        FROM `system_menu`
       WHERE `deleted` = b'0'
         AND `permission` = 'mdm:company-scope:query'
         AND `id` <> 990255
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Active mdm:company-scope:query menu conflicts with authorized-company menu; run the menu hierarchy migration first';
  END IF;

  IF EXISTS (
      SELECT 1
        FROM `system_menu`
       WHERE `deleted` = b'0'
         AND `permission` IN (
           'mdm:company-scope:create',
           'mdm:company-scope:update',
           'mdm:company-scope:delete'
         )
         AND `id` NOT IN (990256, 990257, 990258)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Active mdm:company-scope button menu conflicts with authorized-company menu';
  END IF;

  INSERT INTO `system_menu`
    (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
     `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 990255, '授权公司', 'mdm:company-scope:query', 2, 16, 990200, 'company-scope', 'ep:connection',
         'mdm/company-scope/index', 'MdmCompanyScope',
         0, b'1', b'1', b'1', 'mdm-company-scope-crud-menu', NOW(), 'mdm-company-scope-crud-menu', NOW(), b'0'
  WHERE NOT EXISTS (
      SELECT 1
        FROM `system_menu`
       WHERE `deleted` = b'0'
         AND (`id` = 990255 OR (`parent_id` = 990200 AND `path` = 'company-scope')
              OR `permission` = 'mdm:company-scope:query')
  );

  UPDATE `system_menu`
     SET `name` = '授权公司',
         `permission` = 'mdm:company-scope:query',
         `type` = 2,
         `sort` = 16,
         `parent_id` = 990200,
         `path` = 'company-scope',
         `icon` = 'ep:connection',
         `component` = 'mdm/company-scope/index',
         `component_name` = 'MdmCompanyScope',
         `status` = 0,
         `visible` = b'1',
         `keep_alive` = b'1',
         `always_show` = b'1',
         `deleted` = b'0',
         `updater` = 'mdm-company-scope-crud-menu',
         `update_time` = NOW()
   WHERE `id` = 990255;

  INSERT INTO `system_menu`
    (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
     `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 990256, '授权公司新增', 'mdm:company-scope:create', 3, 10, 990255, '', '', '', '',
         0, b'1', b'1', b'1', 'mdm-company-scope-crud-menu', NOW(), 'mdm-company-scope-crud-menu', NOW(), b'0'
  WHERE NOT EXISTS (
      SELECT 1 FROM `system_menu` WHERE `id` = 990256
  );

  INSERT INTO `system_menu`
    (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
     `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 990257, '授权公司修改', 'mdm:company-scope:update', 3, 20, 990255, '', '', '', '',
         0, b'1', b'1', b'1', 'mdm-company-scope-crud-menu', NOW(), 'mdm-company-scope-crud-menu', NOW(), b'0'
  WHERE NOT EXISTS (
      SELECT 1 FROM `system_menu` WHERE `id` = 990257
  );

  INSERT INTO `system_menu`
    (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
     `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 990258, '授权公司删除', 'mdm:company-scope:delete', 3, 30, 990255, '', '', '', '',
         0, b'1', b'1', b'1', 'mdm-company-scope-crud-menu', NOW(), 'mdm-company-scope-crud-menu', NOW(), b'0'
  WHERE NOT EXISTS (
      SELECT 1 FROM `system_menu` WHERE `id` = 990258
  );

  UPDATE `system_menu`
     SET `parent_id` = 990255,
         `status` = 0,
         `visible` = b'1',
         `deleted` = b'0',
         `updater` = 'mdm-company-scope-crud-menu',
         `update_time` = NOW()
   WHERE `id` IN (990256, 990257, 990258);

  IF NOT EXISTS (
      SELECT 1
        FROM `system_menu`
       WHERE `id` = 990255
         AND `deleted` = b'0'
         AND `name` = '授权公司'
         AND `permission` = 'mdm:company-scope:query'
         AND `type` = 2
         AND `parent_id` = 990200
         AND `path` = 'company-scope'
         AND `component` = 'mdm/company-scope/index'
         AND `component_name` = 'MdmCompanyScope'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Authorized-company menu final contract mismatch';
  END IF;

  IF EXISTS (
      SELECT 1
        FROM (
          SELECT 990256 AS `id`, '授权公司新增' AS `name`, 'mdm:company-scope:create' AS `permission`, 10 AS `sort`
          UNION ALL
          SELECT 990257 AS `id`, '授权公司修改' AS `name`, 'mdm:company-scope:update' AS `permission`, 20 AS `sort`
          UNION ALL
          SELECT 990258 AS `id`, '授权公司删除' AS `name`, 'mdm:company-scope:delete' AS `permission`, 30 AS `sort`
        ) AS `expected`
        LEFT JOIN `system_menu` AS `menu`
          ON `menu`.`id` = `expected`.`id`
       WHERE `menu`.`id` IS NULL
          OR `menu`.`deleted` <> b'0'
          OR `menu`.`name` <> `expected`.`name`
          OR `menu`.`permission` <> `expected`.`permission`
          OR `menu`.`type` <> 3
          OR `menu`.`sort` <> `expected`.`sort`
          OR `menu`.`parent_id` <> 990255
          OR `menu`.`status` <> 0
          OR `menu`.`visible` <> b'1'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Authorized-company button menu final contract mismatch';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mdm_company_scope_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mdm_company_scope_menu_ids` (
    `menu_id` BIGINT NOT NULL PRIMARY KEY
  );

  INSERT INTO `tmp_mdm_company_scope_menu_ids` (`menu_id`) VALUES
    (990255), (990256), (990257), (990258);

  DROP TEMPORARY TABLE IF EXISTS `tmp_mdm_company_scope_target_packages`;
  CREATE TEMPORARY TABLE `tmp_mdm_company_scope_target_packages` AS
  SELECT DISTINCT `package`.`id` AS `package_id`
    FROM `system_tenant_package` AS `package`
    INNER JOIN JSON_TABLE(
      `package`.`menu_ids`,
      '$[*]' COLUMNS (`menu_id` BIGINT PATH '$')
    ) AS `existing_menu`
      ON 1 = 1
   WHERE `package`.`deleted` = b'0'
     AND JSON_VALID(`package`.`menu_ids`)
     AND `existing_menu`.`menu_id` IN (990200, 990249, 990253);

  DROP TEMPORARY TABLE IF EXISTS `tmp_mdm_company_scope_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mdm_company_scope_package_menu_ids` (
    `package_id` BIGINT NOT NULL,
    `menu_id` BIGINT NOT NULL,
    PRIMARY KEY (`package_id`, `menu_id`)
  );

  INSERT IGNORE INTO `tmp_mdm_company_scope_package_menu_ids` (`package_id`, `menu_id`)
  SELECT `target_package`.`package_id`, `existing_menu`.`menu_id`
    FROM `tmp_mdm_company_scope_target_packages` AS `target_package`
    INNER JOIN `system_tenant_package` AS `package`
      ON `package`.`id` = `target_package`.`package_id`
    INNER JOIN JSON_TABLE(
      `package`.`menu_ids`,
      '$[*]' COLUMNS (`menu_id` BIGINT PATH '$')
    ) AS `existing_menu`
      ON 1 = 1;

  INSERT IGNORE INTO `tmp_mdm_company_scope_package_menu_ids` (`package_id`, `menu_id`)
  SELECT `target_package`.`package_id`, `menu`.`menu_id`
    FROM `tmp_mdm_company_scope_target_packages` AS `target_package`
    CROSS JOIN `tmp_mdm_company_scope_menu_ids` AS `menu`;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mdm_company_scope_package_menu_json`;
  CREATE TEMPORARY TABLE `tmp_mdm_company_scope_package_menu_json` AS
  SELECT `package_id`, JSON_ARRAYAGG(`menu_id`) AS `menu_ids`
    FROM (
      SELECT `package_id`, `menu_id`
        FROM `tmp_mdm_company_scope_package_menu_ids`
       ORDER BY `package_id`, `menu_id`
    ) AS `ordered_menu`
   GROUP BY `package_id`;

  UPDATE `system_tenant_package` AS `package`
    INNER JOIN `tmp_mdm_company_scope_package_menu_json` AS `merged`
      ON `merged`.`package_id` = `package`.`id`
     SET `package`.`menu_ids` = `merged`.`menu_ids`,
         `package`.`updater` = 'mdm-company-scope-crud-menu',
         `package`.`update_time` = NOW()
   WHERE `package`.`deleted` = b'0';

  DROP TEMPORARY TABLE IF EXISTS `tmp_mdm_company_scope_target_roles`;
  CREATE TEMPORARY TABLE `tmp_mdm_company_scope_target_roles` AS
  SELECT DISTINCT
      `tenant`.`id` AS `tenant_id`,
      `role`.`id` AS `role_id`
    FROM `system_tenant` AS `tenant`
    INNER JOIN `system_role` AS `role`
      ON `role`.`tenant_id` = `tenant`.`id`
     AND (
       `role`.`code` = 'tenant_admin'
       OR (`tenant`.`id` = 1 AND `role`.`code` = 'super_admin')
     )
     AND `role`.`deleted` = b'0'
    LEFT JOIN `tmp_mdm_company_scope_target_packages` AS `target_package`
      ON `target_package`.`package_id` = `tenant`.`package_id`
   WHERE `tenant`.`deleted` = b'0'
     AND (`target_package`.`package_id` IS NOT NULL OR `tenant`.`id` = 1);

  UPDATE `system_role_menu` AS `role_menu`
    INNER JOIN `tmp_mdm_company_scope_target_roles` AS `target_role`
      ON `target_role`.`tenant_id` = `role_menu`.`tenant_id`
     AND `target_role`.`role_id` = `role_menu`.`role_id`
    INNER JOIN `tmp_mdm_company_scope_menu_ids` AS `menu`
      ON `menu`.`menu_id` = `role_menu`.`menu_id`
     SET `role_menu`.`deleted` = b'0',
         `role_menu`.`updater` = 'mdm-company-scope-crud-menu',
         `role_menu`.`update_time` = NOW()
   WHERE `role_menu`.`deleted` = b'1';

  INSERT INTO `system_role_menu`
    (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT `target_role`.`role_id`, `menu`.`menu_id`,
         'mdm-company-scope-crud-menu', NOW(), 'mdm-company-scope-crud-menu', NOW(), b'0',
         `target_role`.`tenant_id`
    FROM `tmp_mdm_company_scope_target_roles` AS `target_role`
    CROSS JOIN `tmp_mdm_company_scope_menu_ids` AS `menu`
   WHERE NOT EXISTS (
      SELECT 1
        FROM `system_role_menu` AS `existing`
       WHERE `existing`.`tenant_id` = `target_role`.`tenant_id`
         AND `existing`.`role_id` = `target_role`.`role_id`
         AND `existing`.`menu_id` = `menu`.`menu_id`
         AND `existing`.`deleted` = b'0'
   );

  DROP TEMPORARY TABLE IF EXISTS `tmp_mdm_company_scope_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mdm_company_scope_target_packages`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mdm_company_scope_package_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mdm_company_scope_package_menu_json`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mdm_company_scope_target_roles`;
END$$
DELIMITER ;

CALL ensure_mdm_company_scope_crud_menu();
DROP PROCEDURE IF EXISTS ensure_mdm_company_scope_crud_menu;
