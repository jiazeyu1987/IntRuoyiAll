-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260813_z_form_center_menu_hide; type=menu; riskLevel=low
SET NAMES utf8mb4;

DELIMITER //

DROP PROCEDURE IF EXISTS `bpm_form_parser_menu_20260908`//
CREATE PROCEDURE `bpm_form_parser_menu_20260908`()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 990200
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing global basic data menu 990200 for form parser menu';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `parent_id` = 990200
      AND `path` = 'form-center/parser'
      AND `id` <> 605071222
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Conflicting basic data form-center/parser menu path';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `permission` IN ('form:parser:query', 'form:parser:production-batch-record')
      AND `id` NOT IN (605071222, 605071223)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Conflicting form parser permission menu exists';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `bpm_form_parser_menu_defs`;
  CREATE TEMPORARY TABLE `bpm_form_parser_menu_defs` (
    `id` bigint NOT NULL,
    `name` varchar(50) NOT NULL,
    `permission` varchar(100) NOT NULL,
    `type` tinyint NOT NULL,
    `sort` int NOT NULL,
    `parent_id` bigint NOT NULL,
    `path` varchar(200) NOT NULL,
    `icon` varchar(100) NOT NULL,
    `component` varchar(255) NOT NULL,
    `component_name` varchar(255) NOT NULL,
    `status` tinyint NOT NULL,
    `visible` bit(1) NOT NULL,
    `keep_alive` bit(1) NOT NULL,
    `always_show` bit(1) NOT NULL,
    PRIMARY KEY (`id`)
  ) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

  INSERT INTO `bpm_form_parser_menu_defs`
    (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
     `component_name`, `status`, `visible`, `keep_alive`, `always_show`)
  VALUES
    (605071222, '表单解析', 'form:parser:query', 2, 28, 990200, 'form-center/parser',
     'ep:document-copy', 'form-center/parser/index', 'FormCenterParser', 0, b'1', b'1', b'1'),
    (605071223, '生产批记录解析', 'form:parser:production-batch-record', 3, 1, 605071222,
     '', '', '', '', 0, b'1', b'1', b'1');

  IF EXISTS (
    SELECT 1
    FROM `system_menu` AS `existing`
    JOIN `bpm_form_parser_menu_defs` AS `def`
      ON `def`.`id` = `existing`.`id`
    WHERE `existing`.`deleted` <> b'0'
       OR COALESCE(`existing`.`permission`, '') <> `def`.`permission`
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Conflicting form parser system_menu id or permission exists';
  END IF;

  INSERT INTO `system_menu` (
    `id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`,
    `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
  )
  SELECT
    `def`.`id`, `def`.`name`, `def`.`permission`, `def`.`type`, `def`.`sort`, `def`.`parent_id`,
    `def`.`path`, `def`.`icon`, `def`.`component`, `def`.`component_name`, `def`.`status`,
    `def`.`visible`, `def`.`keep_alive`, `def`.`always_show`, '1', NOW(), '1', NOW(), b'0'
  FROM `bpm_form_parser_menu_defs` AS `def`
  LEFT JOIN `system_menu` AS `existing`
    ON `existing`.`id` = `def`.`id`
  WHERE `existing`.`id` IS NULL;

  UPDATE `system_menu` AS `existing`
  JOIN `bpm_form_parser_menu_defs` AS `def`
    ON `def`.`id` = `existing`.`id`
  SET `existing`.`name` = `def`.`name`,
      `existing`.`permission` = `def`.`permission`,
      `existing`.`type` = `def`.`type`,
      `existing`.`sort` = `def`.`sort`,
      `existing`.`parent_id` = `def`.`parent_id`,
      `existing`.`path` = `def`.`path`,
      `existing`.`icon` = `def`.`icon`,
      `existing`.`component` = `def`.`component`,
      `existing`.`component_name` = `def`.`component_name`,
      `existing`.`status` = `def`.`status`,
      `existing`.`visible` = `def`.`visible`,
      `existing`.`keep_alive` = `def`.`keep_alive`,
      `existing`.`always_show` = `def`.`always_show`,
      `existing`.`updater` = '1',
      `existing`.`update_time` = NOW()
  WHERE `existing`.`deleted` = b'0';

  IF (
    SELECT COUNT(*)
    FROM `system_menu`
    WHERE `id` IN (605071222, 605071223)
      AND `deleted` = b'0'
      AND `visible` = b'1'
  ) <> 2 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Form parser menus must remain visible';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_tenant_package` AS `package`
    WHERE `package`.`deleted` = b'0'
      AND NOT JSON_VALID(`package`.`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot merge form parser menus';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `form_parser_reference_packages`;
  CREATE TEMPORARY TABLE `form_parser_reference_packages` AS
  SELECT `package`.`id` AS `package_id`
  FROM `system_tenant_package` AS `package`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`)
    AND JSON_CONTAINS(`package`.`menu_ids`, CAST('605071201' AS JSON), '$');

  DROP TEMPORARY TABLE IF EXISTS `form_parser_package_menu_ids`;
  CREATE TEMPORARY TABLE `form_parser_package_menu_ids` (
    `package_id` bigint NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`package_id`, `menu_id`)
  );

  INSERT IGNORE INTO `form_parser_package_menu_ids` (`package_id`, `menu_id`)
  SELECT
    `package`.`id`,
    CAST(`existing_menu`.`menu_id` AS UNSIGNED)
  FROM `system_tenant_package` AS `package`
  JOIN JSON_TABLE(
    `package`.`menu_ids`,
    '$[*]' COLUMNS (`menu_id` bigint PATH '$')
  ) AS `existing_menu`
  JOIN `form_parser_reference_packages` AS `reference_package`
    ON `reference_package`.`package_id` = `package`.`id`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`);

  INSERT IGNORE INTO `form_parser_package_menu_ids` (`package_id`, `menu_id`)
  SELECT
    `reference_package`.`package_id`,
    `def`.`id`
  FROM `form_parser_reference_packages` AS `reference_package`
  CROSS JOIN `bpm_form_parser_menu_defs` AS `def`;

  UPDATE `system_tenant_package` AS `package`
  JOIN (
    SELECT DISTINCT
      `package_id`,
      JSON_ARRAYAGG(`menu_id`) OVER (
        PARTITION BY `package_id`
        ORDER BY `menu_id`
        ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
      ) AS `menu_ids`
    FROM `form_parser_package_menu_ids`
  ) AS `merged`
    ON `merged`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = `merged`.`menu_ids`,
      `package`.`updater` = '1',
      `package`.`update_time` = NOW();

  DROP TEMPORARY TABLE IF EXISTS `form_parser_reference_roles`;
  CREATE TEMPORARY TABLE `form_parser_reference_roles` AS
  SELECT DISTINCT
    `role_menu`.`role_id`,
    `role_menu`.`tenant_id`
  FROM `system_role_menu` AS `role_menu`
  WHERE `role_menu`.`deleted` = b'0'
    AND `role_menu`.`menu_id` = 605071201;

  INSERT INTO `system_role_menu` (
    `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    `reference_role`.`role_id`,
    `def`.`id`,
    '1',
    NOW(),
    '1',
    NOW(),
    b'0',
    `reference_role`.`tenant_id`
  FROM `form_parser_reference_roles` AS `reference_role`
  CROSS JOIN `bpm_form_parser_menu_defs` AS `def`
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` AS `existing`
    WHERE `existing`.`role_id` = `reference_role`.`role_id`
      AND `existing`.`menu_id` = `def`.`id`
      AND `existing`.`deleted` = b'0'
  );

  DROP TEMPORARY TABLE IF EXISTS `form_parser_reference_roles`;
  DROP TEMPORARY TABLE IF EXISTS `form_parser_package_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `form_parser_reference_packages`;
  DROP TEMPORARY TABLE IF EXISTS `bpm_form_parser_menu_defs`;
END//

CALL `bpm_form_parser_menu_20260908`()//
DROP PROCEDURE IF EXISTS `bpm_form_parser_menu_20260908`//

DELIMITER ;
