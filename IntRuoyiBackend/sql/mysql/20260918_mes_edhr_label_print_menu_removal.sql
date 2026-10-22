-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260618_mes_edhr_label_print_queue,20260618_mes_edhr_print_policy_reissue_void; type=menu; riskLevel=low
-- Retire the eDHR label-print UI menus while preserving label, instance, print-task, policy, and audit capabilities.
-- Fail-fast migration: no hard delete, no business-table change, no fallback route.
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_mes_edhr_label_print_menus_removed;
DELIMITER $$
CREATE PROCEDURE ensure_mes_edhr_label_print_menus_removed()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_label_print_package_menu_ids`;
    DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_label_print_affected_packages`;
    DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_label_print_retired_menu_ids`;
    RESIGNAL;
  END;

  IF NOT EXISTS (
      SELECT 1
      FROM `system_menu`
      WHERE `id` = 900220
        AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR root system_menu row; cannot retire label-print menus safely';
  END IF;

  IF EXISTS (
      SELECT 1
      FROM `system_tenant_package` AS `package`
      WHERE `package`.`deleted` = b'0'
        AND NOT JSON_VALID(`package`.`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot retire eDHR label-print menus';
  END IF;

  IF (
    SELECT COUNT(*)
    FROM `system_menu`
    WHERE `id` IN (900320, 900321, 900322, 900323, 900324, 900325, 900326, 900327, 900328, 900329, 900330, 900331, 900338, 900339, 900340, 900341, 900342, 900343, 900344, 900345, 900346)
  ) <> 21 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR label-print system_menu rows; cannot retire menu assignments safely';
  END IF;

  IF (
    SELECT COUNT(*)
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `id` IN (900320, 900321, 900322, 900323, 900324, 900325, 900326, 900327, 900328, 900329, 900330, 900331, 900338, 900339, 900340, 900341, 900342, 900343, 900344, 900345, 900346)
  ) NOT IN (0, 21) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Partial eDHR label-print menu retirement state; refusing to continue';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_label_print_retired_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_label_print_retired_menu_ids` (
    `id` bigint NOT NULL,
    PRIMARY KEY (`id`)
  );

  INSERT INTO `tmp_mes_edhr_label_print_retired_menu_ids` (`id`)
  VALUES
    (900320),
    (900321),
    (900322),
    (900323),
    (900324),
    (900325),
    (900326),
    (900327),
    (900328),
    (900329),
    (900330),
    (900331),
    (900338),
    (900339),
    (900340),
    (900341),
    (900342),
    (900343),
    (900344),
    (900345),
    (900346);

  START TRANSACTION;

  UPDATE `system_role_menu` AS `role_menu`
  JOIN `tmp_mes_edhr_label_print_retired_menu_ids` AS `retired`
    ON `retired`.`id` = `role_menu`.`menu_id`
  SET `role_menu`.`deleted` = b'1',
      `role_menu`.`updater` = 'edhr-label-print-menu-removal',
      `role_menu`.`update_time` = NOW()
  WHERE `role_menu`.`deleted` = b'0';

  UPDATE `system_menu` AS `menu`
  JOIN `tmp_mes_edhr_label_print_retired_menu_ids` AS `retired`
    ON `retired`.`id` = `menu`.`id`
  SET `menu`.`status` = 1,
      `menu`.`path` = '',
      `menu`.`component` = '',
      `menu`.`component_name` = '',
      `menu`.`visible` = b'0',
      `menu`.`keep_alive` = b'0',
      `menu`.`always_show` = b'0',
      `menu`.`deleted` = b'1',
      `menu`.`updater` = 'edhr-label-print-menu-removal',
      `menu`.`update_time` = NOW()
  WHERE `menu`.`deleted` = b'0';

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_label_print_affected_packages`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_label_print_affected_packages` AS
  SELECT DISTINCT
    `package`.`id` AS `package_id`
  FROM `system_tenant_package` AS `package`
  JOIN JSON_TABLE(
    `package`.`menu_ids`,
    '$[*]' COLUMNS (`menu_id` bigint PATH '$')
  ) AS `existing_menu`
    ON TRUE
  JOIN `tmp_mes_edhr_label_print_retired_menu_ids` AS `retired`
    ON `retired`.`id` = `existing_menu`.`menu_id`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`);

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_label_print_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_label_print_package_menu_ids` (
    `package_id` bigint NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`package_id`, `menu_id`)
  );

  INSERT INTO `tmp_mes_edhr_label_print_package_menu_ids` (`package_id`, `menu_id`)
  SELECT DISTINCT
    `affected`.`package_id`,
    CAST(`existing_menu`.`menu_id` AS UNSIGNED)
  FROM `tmp_mes_edhr_label_print_affected_packages` AS `affected`
  JOIN `system_tenant_package` AS `package`
    ON `package`.`id` = `affected`.`package_id`
   AND `package`.`deleted` = b'0'
   AND JSON_VALID(`package`.`menu_ids`)
  JOIN JSON_TABLE(
    `package`.`menu_ids`,
    '$[*]' COLUMNS (`menu_id` bigint PATH '$')
  ) AS `existing_menu`
  WHERE NOT EXISTS (
    SELECT 1
    FROM `tmp_mes_edhr_label_print_retired_menu_ids` AS `retired`
    WHERE `retired`.`id` = `existing_menu`.`menu_id`
  );

  UPDATE `system_tenant_package` AS `package`
  JOIN `tmp_mes_edhr_label_print_affected_packages` AS `affected`
    ON `affected`.`package_id` = `package`.`id`
  LEFT JOIN (
    SELECT
      `ordered`.`package_id`,
      JSON_ARRAYAGG(`ordered`.`menu_id`) AS `menu_ids`
    FROM (
      SELECT `package_id`, `menu_id`
      FROM `tmp_mes_edhr_label_print_package_menu_ids`
      ORDER BY `package_id`, `menu_id`
    ) AS `ordered`
    GROUP BY `ordered`.`package_id`
  ) AS `filtered`
    ON `filtered`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = COALESCE(`filtered`.`menu_ids`, JSON_ARRAY()),
      `package`.`updater` = 'edhr-label-print-menu-removal',
      `package`.`update_time` = NOW();

  IF EXISTS (
      SELECT 1
      FROM `system_menu` AS `menu`
      JOIN `tmp_mes_edhr_label_print_retired_menu_ids` AS `retired`
        ON `retired`.`id` = `menu`.`id`
      WHERE `menu`.`deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'eDHR label-print system_menu rows remain active after retirement';
  END IF;

  IF EXISTS (
      SELECT 1
      FROM `system_role_menu` AS `role_menu`
      JOIN `tmp_mes_edhr_label_print_retired_menu_ids` AS `retired`
        ON `retired`.`id` = `role_menu`.`menu_id`
      WHERE `role_menu`.`deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'eDHR label-print role menu assignments remain active after retirement';
  END IF;

  IF EXISTS (
      SELECT 1
      FROM `system_tenant_package` AS `package`
      JOIN JSON_TABLE(
        `package`.`menu_ids`,
        '$[*]' COLUMNS (`menu_id` bigint PATH '$')
      ) AS `existing_menu`
        ON TRUE
      JOIN `tmp_mes_edhr_label_print_retired_menu_ids` AS `retired`
        ON `retired`.`id` = `existing_menu`.`menu_id`
      WHERE `package`.`deleted` = b'0'
        AND JSON_VALID(`package`.`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'eDHR label-print tenant package menu assignments remain after retirement';
  END IF;

  COMMIT;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_label_print_package_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_label_print_affected_packages`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_label_print_retired_menu_ids`;
END$$
DELIMITER ;

CALL ensure_mes_edhr_label_print_menus_removed();

DROP PROCEDURE IF EXISTS ensure_mes_edhr_label_print_menus_removed;
