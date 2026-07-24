-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260721_form_center_menu_under_basic_data; type=menu; riskLevel=low
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS form_center_business_action_page_retire;

DELIMITER //
CREATE PROCEDURE form_center_business_action_page_retire()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'system_menu'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Form center page retirement requires system_menu';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 605071200
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Form center parent menu 605071200 is required';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_tenant_package` AS `package`
    WHERE `package`.`deleted` = b'0'
      AND NOT JSON_VALID(`package`.`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot retire form center page';
  END IF;

  UPDATE `system_menu`
  SET `parent_id` = 605071200,
      `sort` = CASE `id`
        WHEN 605071210 THEN 20
        WHEN 605071211 THEN 21
        WHEN 605071212 THEN 22
        WHEN 605071213 THEN 23
        WHEN 605071218 THEN 24
        WHEN 605071219 THEN 25
        ELSE `sort`
      END,
      `updater` = 'codex',
      `update_time` = NOW()
  WHERE `deleted` = b'0'
    AND `parent_id` = 605071209
    AND `id` IN (605071210, 605071211, 605071212, 605071213, 605071218, 605071219);

  UPDATE `system_role_menu`
  SET `deleted` = b'1',
      `updater` = 'codex',
      `update_time` = NOW()
  WHERE `menu_id` = 605071209
    AND `deleted` = b'0';

  DROP TEMPORARY TABLE IF EXISTS `tmp_form_center_business_action_page_packages`;
  CREATE TEMPORARY TABLE `tmp_form_center_business_action_page_packages` (
    `package_id` bigint NOT NULL,
    PRIMARY KEY (`package_id`)
  );

  INSERT IGNORE INTO `tmp_form_center_business_action_page_packages` (`package_id`)
  SELECT DISTINCT `package`.`id`
  FROM `system_tenant_package` AS `package`
  JOIN JSON_TABLE(
    `package`.`menu_ids`,
    '$[*]' COLUMNS (`menu_id` bigint PATH '$')
  ) AS `existing_menu`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`)
    AND CAST(`existing_menu`.`menu_id` AS UNSIGNED) = 605071209;

  DROP TEMPORARY TABLE IF EXISTS `tmp_form_center_business_action_page_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_form_center_business_action_page_package_menu_ids` (
    `package_id` bigint NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`package_id`, `menu_id`)
  );

  INSERT IGNORE INTO `tmp_form_center_business_action_page_package_menu_ids` (`package_id`, `menu_id`)
  SELECT
    `package_scope`.`package_id`,
    CAST(`existing_menu`.`menu_id` AS UNSIGNED)
  FROM `tmp_form_center_business_action_page_packages` AS `package_scope`
  JOIN `system_tenant_package` AS `package`
    ON `package`.`id` = `package_scope`.`package_id`
   AND `package`.`deleted` = b'0'
   AND JSON_VALID(`package`.`menu_ids`)
  JOIN JSON_TABLE(
    `package`.`menu_ids`,
    '$[*]' COLUMNS (`menu_id` bigint PATH '$')
  ) AS `existing_menu`
  WHERE CAST(`existing_menu`.`menu_id` AS UNSIGNED) <> 605071209;

  UPDATE `system_tenant_package` AS `package`
  JOIN `tmp_form_center_business_action_page_packages` AS `package_scope`
    ON `package_scope`.`package_id` = `package`.`id`
  LEFT JOIN (
    SELECT DISTINCT
      `package_id`,
      JSON_ARRAYAGG(`menu_id`) OVER (
        PARTITION BY `package_id`
        ORDER BY `menu_id`
        ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
      ) AS `menu_ids`
    FROM `tmp_form_center_business_action_page_package_menu_ids`
  ) AS `merged`
    ON `merged`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = COALESCE(`merged`.`menu_ids`, JSON_ARRAY()),
      `package`.`updater` = 'codex',
      `package`.`update_time` = NOW();

  UPDATE `system_menu`
  SET `visible` = b'0',
      `status` = 1,
      `deleted` = b'1',
      `updater` = 'codex',
      `update_time` = NOW()
  WHERE `id` = 605071209
    AND `deleted` = b'0';

  IF EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 605071209
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Form center page menu 605071209 is still active';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `parent_id` = 605071209
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Form center page menu still owns active child menus';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_role_menu`
    WHERE `menu_id` = 605071209
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Form center page role binding is still active';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_tenant_package` AS `package`
    WHERE `package`.`deleted` = b'0'
      AND JSON_VALID(`package`.`menu_ids`)
      AND JSON_CONTAINS(`package`.`menu_ids`, CAST('605071209' AS JSON), '$')
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Form center page package binding is still active';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_form_center_business_action_page_package_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_form_center_business_action_page_packages`;
END//
DELIMITER ;

CALL form_center_business_action_page_retire();

DROP PROCEDURE IF EXISTS form_center_business_action_page_retire;
