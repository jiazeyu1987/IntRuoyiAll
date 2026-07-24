-- edhr-commercial-menu-template
-- Purpose: source template for future eDHR commercial page menu migrations.
-- This file is not a runtime migration. Copy it into a dated sql/mysql migration only
-- after the target page component exists and the real database has been checked.
-- Required replacements:
--   {{TASK_ID}}: owning task id or migration marker
--   {{MENU_ROWS}}: SELECT/UNION rows from commercial-page-menu-contract.json planned pages
--   {{BUTTON_ROWS}}: SELECT/UNION rows for page action permissions

DROP PROCEDURE IF EXISTS ensure_mes_edhr_commercial_menus_{{TASK_ID}};
DELIMITER $$
CREATE PROCEDURE ensure_mes_edhr_commercial_menus_{{TASK_ID}}()
BEGIN
  IF NOT EXISTS (
      SELECT 1 FROM `system_menu`
      WHERE `id` = 900220
        AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR parent menu 900220; cannot create commercial eDHR menus';
  END IF;

  IF EXISTS (
      SELECT 1 FROM `system_tenant_package` AS `package`
      WHERE `package`.`deleted` = b'0'
        AND NOT JSON_VALID(`package`.`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot merge commercial eDHR menus';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_commercial_menu_contract`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_commercial_menu_contract` (
    `id` bigint NOT NULL,
    `name` varchar(64) NOT NULL,
    `permission` varchar(128) NOT NULL,
    `type` tinyint NOT NULL,
    `sort` int NOT NULL,
    `parent_id` bigint NOT NULL,
    `path` varchar(200) NOT NULL,
    `icon` varchar(64) NOT NULL,
    `component` varchar(255) NOT NULL,
    `component_name` varchar(255) NOT NULL,
    PRIMARY KEY (`id`)
  );

  INSERT INTO `tmp_mes_edhr_commercial_menu_contract`
    (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`)
  {{MENU_ROWS}};

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_commercial_button_contract`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_commercial_button_contract` (
    `id` bigint NOT NULL,
    `name` varchar(64) NOT NULL,
    `permission` varchar(128) NOT NULL,
    `type` tinyint NOT NULL,
    `sort` int NOT NULL,
    `parent_id` bigint NOT NULL,
    PRIMARY KEY (`id`)
  );

  INSERT INTO `tmp_mes_edhr_commercial_button_contract`
    (`id`, `name`, `permission`, `type`, `sort`, `parent_id`)
  {{BUTTON_ROWS}};

  IF EXISTS (
      SELECT `permission`
      FROM (
        SELECT `permission` FROM `tmp_mes_edhr_commercial_menu_contract`
        UNION ALL
        SELECT `permission` FROM `tmp_mes_edhr_commercial_button_contract`
      ) AS `permissions`
      GROUP BY `permission`
      HAVING COUNT(*) > 1
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Duplicate eDHR commercial menu permission; cannot continue';
  END IF;

  INSERT INTO `system_menu`
    (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
     `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT
    `contract`.`id`, `contract`.`name`, `contract`.`permission`, `contract`.`type`, `contract`.`sort`,
    `contract`.`parent_id`, `contract`.`path`, `contract`.`icon`, `contract`.`component`, `contract`.`component_name`,
    0, b'1', b'1', b'1', '{{TASK_ID}}', NOW(), '{{TASK_ID}}', NOW(), b'0'
  FROM `tmp_mes_edhr_commercial_menu_contract` AS `contract`
  WHERE NOT EXISTS (
    SELECT 1 FROM `system_menu` AS `menu`
    WHERE `menu`.`id` = `contract`.`id`
       OR (`menu`.`permission` = `contract`.`permission` AND `menu`.`type` = `contract`.`type`)
  );

  INSERT INTO `system_menu`
    (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
     `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT
    `contract`.`id`, `contract`.`name`, `contract`.`permission`, `contract`.`type`, `contract`.`sort`,
    `contract`.`parent_id`, '', '', '', '', 0, b'1', b'1', b'1', '{{TASK_ID}}', NOW(), '{{TASK_ID}}', NOW(), b'0'
  FROM `tmp_mes_edhr_commercial_button_contract` AS `contract`
  WHERE NOT EXISTS (
    SELECT 1 FROM `system_menu` AS `menu`
    WHERE `menu`.`id` = `contract`.`id`
       OR (`menu`.`permission` = `contract`.`permission` AND `menu`.`type` = `contract`.`type`)
  );

  IF (
      SELECT COUNT(*)
      FROM `system_menu` AS `menu`
      JOIN `tmp_mes_edhr_commercial_menu_contract` AS `contract` ON `contract`.`id` = `menu`.`id`
      WHERE `menu`.`deleted` = b'0'
  ) <> (SELECT COUNT(*) FROM `tmp_mes_edhr_commercial_menu_contract`) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing commercial eDHR page menu rows after insert; cannot merge tenant bindings';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_commercial_target_packages`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_commercial_target_packages` AS
  SELECT `package`.`id` AS `package_id`
  FROM `system_tenant_package` AS `package`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`)
    AND JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('900220' AS JSON), '$');

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_commercial_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_commercial_menu_ids` AS
  SELECT `id`
  FROM `tmp_mes_edhr_commercial_menu_contract`
  UNION
  SELECT `id`
  FROM `tmp_mes_edhr_commercial_button_contract`;

  UPDATE `system_tenant_package` AS `package`
  JOIN (
    SELECT
      `source`.`package_id`,
      JSON_ARRAYAGG(CAST(`source`.`menu_id` AS UNSIGNED)) AS `menu_ids`
    FROM (
      SELECT
        `package`.`id` AS `package_id`,
        CAST(`existing`.`menu_id` AS UNSIGNED) AS `menu_id`
      FROM `system_tenant_package` AS `package`
      JOIN JSON_TABLE(
        CAST(`package`.`menu_ids` AS JSON),
        '$[*]' COLUMNS (`menu_id` bigint PATH '$')
      ) AS `existing`
      WHERE `package`.`id` IN (SELECT `package_id` FROM `tmp_mes_edhr_commercial_target_packages`)
      UNION
      SELECT
        `package_id`,
        `id` AS `menu_id`
      FROM `tmp_mes_edhr_commercial_target_packages`
      CROSS JOIN `tmp_mes_edhr_commercial_menu_ids`
    ) AS `source`
    GROUP BY `source`.`package_id`
  ) AS `merged` ON `merged`.`package_id` = `package`.`id`
  SET
    `package`.`menu_ids` = CAST(`merged`.`menu_ids` AS CHAR),
    `package`.`updater` = '{{TASK_ID}}',
    `package`.`update_time` = NOW();

  UPDATE `system_role_menu` AS `role_menu`
  JOIN `system_role` AS `role` ON `role`.`id` = `role_menu`.`role_id`
  SET
    `role_menu`.`deleted` = b'0',
    `role_menu`.`updater` = '{{TASK_ID}}',
    `role_menu`.`update_time` = NOW()
  WHERE `role`.`code` = 'tenant_admin'
    AND `role`.`deleted` = b'0'
    AND `role_menu`.`menu_id` IN (SELECT `id` FROM `tmp_mes_edhr_commercial_menu_ids`);

  INSERT INTO `system_role_menu`
    (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT
    `role`.`id`,
    `menu_ids`.`id`,
    '{{TASK_ID}}',
    NOW(),
    '{{TASK_ID}}',
    NOW(),
    b'0',
    `role`.`tenant_id`
  FROM `system_role` AS `role`
  JOIN `system_tenant` AS `tenant` ON `tenant`.`id` = `role`.`tenant_id`
  JOIN `tmp_mes_edhr_commercial_menu_ids` AS `menu_ids`
  WHERE `role`.`code` = 'tenant_admin'
    AND `role`.`deleted` = b'0'
    AND `tenant`.`deleted` = b'0'
    AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` AS `existing`
      WHERE `existing`.`role_id` = `role`.`id`
        AND `existing`.`menu_id` = `menu_ids`.`id`
    );
END$$
DELIMITER ;

CALL ensure_mes_edhr_commercial_menus_{{TASK_ID}}();
DROP PROCEDURE IF EXISTS ensure_mes_edhr_commercial_menus_{{TASK_ID}};
