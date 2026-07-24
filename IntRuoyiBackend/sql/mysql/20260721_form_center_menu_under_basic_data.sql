-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260717_bpm_form_center; type=data; riskLevel=low
-- Purpose: move Form Center from workflow management to the global Basic Data menu.

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS move_form_center_menu_under_basic_data;
DELIMITER //
CREATE PROCEDURE move_form_center_menu_under_basic_data()
BEGIN
  SET @form_center_basic_data_menu_id := (
    SELECT `id`
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND (`id` = 990200 OR `path` = '/mdm')
    ORDER BY CASE WHEN `id` = 990200 THEN 0 ELSE 1 END
    LIMIT 1
  );

  IF @form_center_basic_data_menu_id IS NULL THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing global basic data menu 990200 for form center';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 605071200
      AND `path` = 'form-center'
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing form center parent menu 605071200';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_form_center_basic_data_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_form_center_basic_data_menu_ids` (
    `id` bigint NOT NULL,
    PRIMARY KEY (`id`)
  );

  INSERT INTO `tmp_form_center_basic_data_menu_ids` (`id`)
  SELECT `id`
  FROM `system_menu`
  WHERE `deleted` = b'0'
    AND `id` IN (
      605071200, 605071201, 605071202, 605071203, 605071204, 605071205, 605071206,
      605071207, 605071208, 605071210, 605071211, 605071212, 605071213,
      605071214, 605071215, 605071216, 605071217, 605071218, 605071219, 605071220, 605071221
    );

  IF (SELECT COUNT(*) FROM `tmp_form_center_basic_data_menu_ids`) <> 21 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing form center menu rows for basic data move';
  END IF;

  UPDATE `system_menu`
  SET `parent_id` = @form_center_basic_data_menu_id,
      `sort` = 30,
      `updater` = 'codex',
      `update_time` = NOW()
  WHERE `id` = 605071200
    AND `deleted` = b'0';

  IF EXISTS (
    SELECT 1
    FROM `system_tenant_package` AS `package`
    WHERE `package`.`deleted` = b'0'
      AND NOT JSON_VALID(`package`.`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot move form center under basic data';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_form_center_basic_data_package_ids`;
  CREATE TEMPORARY TABLE `tmp_form_center_basic_data_package_ids` (
    `package_id` bigint NOT NULL,
    PRIMARY KEY (`package_id`)
  );

  INSERT IGNORE INTO `tmp_form_center_basic_data_package_ids` (`package_id`)
  SELECT DISTINCT `package`.`id`
  FROM `system_tenant_package` AS `package`
  JOIN JSON_TABLE(
    `package`.`menu_ids`,
    '$[*]' COLUMNS (`menu_id` bigint PATH '$')
  ) AS `existing_menu`
  JOIN `tmp_form_center_basic_data_menu_ids` AS `form_menu`
    ON `form_menu`.`id` = `existing_menu`.`menu_id`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`);

  DROP TEMPORARY TABLE IF EXISTS `tmp_form_center_basic_data_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_form_center_basic_data_package_menu_ids` (
    `package_id` bigint NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`package_id`, `menu_id`)
  );

  INSERT IGNORE INTO `tmp_form_center_basic_data_package_menu_ids` (`package_id`, `menu_id`)
  SELECT
    `package_scope`.`package_id`,
    CAST(`existing_menu`.`menu_id` AS UNSIGNED)
  FROM `tmp_form_center_basic_data_package_ids` AS `package_scope`
  JOIN `system_tenant_package` AS `package`
    ON `package`.`id` = `package_scope`.`package_id`
   AND `package`.`deleted` = b'0'
   AND JSON_VALID(`package`.`menu_ids`)
  JOIN JSON_TABLE(
    `package`.`menu_ids`,
    '$[*]' COLUMNS (`menu_id` bigint PATH '$')
  ) AS `existing_menu`;

  INSERT IGNORE INTO `tmp_form_center_basic_data_package_menu_ids` (`package_id`, `menu_id`)
  SELECT `package_id`, @form_center_basic_data_menu_id
  FROM `tmp_form_center_basic_data_package_ids`;

  UPDATE `system_tenant_package` AS `package`
  JOIN (
    SELECT DISTINCT
      `package_id`,
      JSON_ARRAYAGG(`menu_id`) OVER (
        PARTITION BY `package_id`
        ORDER BY `menu_id`
        ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
      ) AS `menu_ids`
    FROM `tmp_form_center_basic_data_package_menu_ids`
  ) AS `merged`
    ON `merged`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = `merged`.`menu_ids`,
      `package`.`updater` = 'codex',
      `package`.`update_time` = NOW();

  INSERT INTO `system_role_menu` (
    `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT DISTINCT
    `role_scope`.`role_id`,
    @form_center_basic_data_menu_id,
    'codex',
    NOW(),
    'codex',
    NOW(),
    b'0',
    `role_scope`.`tenant_id`
  FROM (
    SELECT DISTINCT `role_menu`.`role_id`, `role_menu`.`tenant_id`
    FROM `system_role_menu` AS `role_menu`
    JOIN `tmp_form_center_basic_data_menu_ids` AS `form_menu`
      ON `form_menu`.`id` = `role_menu`.`menu_id`
    WHERE `role_menu`.`deleted` = b'0'
  ) AS `role_scope`
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` AS `existing`
    WHERE `existing`.`role_id` = `role_scope`.`role_id`
      AND `existing`.`menu_id` = @form_center_basic_data_menu_id
      AND `existing`.`tenant_id` = `role_scope`.`tenant_id`
      AND `existing`.`deleted` = b'0'
  );

  DROP TEMPORARY TABLE IF EXISTS `tmp_form_center_basic_data_package_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_form_center_basic_data_package_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_form_center_basic_data_menu_ids`;
END//
DELIMITER ;

CALL move_form_center_menu_under_basic_data();

DROP PROCEDURE IF EXISTS move_form_center_menu_under_basic_data;
