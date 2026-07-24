-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=menu; riskLevel=low
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_system_config_package_menu;

DELIMITER //
CREATE PROCEDURE ensure_system_config_package_menu()
BEGIN
  IF EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE (
        `id` = 910300
        AND (
          `deleted` <> b'0'
          OR `permission` <> 'system:config-package:query'
          OR `component` <> 'system/config-package/index'
        )
      )
      OR (
        `id` = 910301
        AND (`deleted` <> b'0' OR `permission` <> 'system:config-package:export')
      )
      OR (
        `id` = 910302
        AND (`deleted` <> b'0' OR `permission` <> 'system:config-package:import')
      )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Conflicting system_menu id exists for system config package menus';
  END IF;

  INSERT INTO `system_menu` (
    `id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`,
    `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
  )
  SELECT
    910300, '配置包中心', 'system:config-package:query', 2, 99, 1, 'config-package',
    'ep:document-copy', 'system/config-package/index', 'SystemConfigPackage',
    0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND (
        `id` = 910300
        OR `permission` = 'system:config-package:query'
        OR `path` = 'config-package'
        OR `component` = 'system/config-package/index'
      )
  );

  INSERT INTO `system_menu` (
    `id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`,
    `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
  )
  SELECT
    910301, '配置包导出', 'system:config-package:export', 3, 1, 910300, '',
    '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND (`id` = 910301 OR `permission` = 'system:config-package:export')
  );

  INSERT INTO `system_menu` (
    `id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`,
    `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
  )
  SELECT
    910302, '配置包导入', 'system:config-package:import', 3, 2, 910300, '',
    '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND (`id` = 910302 OR `permission` = 'system:config-package:import')
  );

  DROP TEMPORARY TABLE IF EXISTS `tmp_system_config_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_system_config_package_menu_ids` AS
  SELECT `id`
  FROM `system_menu`
  WHERE `deleted` = b'0'
    AND (
      (`permission` = 'system:config-package:query' AND `component` = 'system/config-package/index')
      OR `permission` IN ('system:config-package:export', 'system:config-package:import')
    );

  IF (SELECT COUNT(*) FROM `tmp_system_config_package_menu_ids`) <> 3 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing or duplicated system config package menu rows';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_tenant_package` AS `package`
    WHERE `package`.`deleted` = b'0'
      AND NOT JSON_VALID(`package`.`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot merge system config package menus';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_system_config_package_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_system_config_package_package_menu_ids` (
    `package_id` bigint NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`package_id`, `menu_id`)
  );

  INSERT IGNORE INTO `tmp_system_config_package_package_menu_ids` (`package_id`, `menu_id`)
  SELECT
    `package`.`id`,
    CAST(`existing_menu`.`menu_id` AS UNSIGNED)
  FROM `system_tenant_package` AS `package`
  JOIN JSON_TABLE(
    `package`.`menu_ids`,
    '$[*]' COLUMNS (`menu_id` bigint PATH '$')
  ) AS `existing_menu`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`);

  INSERT IGNORE INTO `tmp_system_config_package_package_menu_ids` (`package_id`, `menu_id`)
  SELECT
    `package`.`id`,
    `config_menu`.`id`
  FROM `system_tenant_package` AS `package`
  CROSS JOIN `tmp_system_config_package_menu_ids` AS `config_menu`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`)
    AND JSON_CONTAINS(`package`.`menu_ids`, CAST('1' AS JSON), '$');

  UPDATE `system_tenant_package` AS `package`
  JOIN (
    SELECT DISTINCT
      `package_id`,
      JSON_ARRAYAGG(`menu_id`) OVER (
        PARTITION BY `package_id`
        ORDER BY `menu_id`
        ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
      ) AS `menu_ids`
    FROM `tmp_system_config_package_package_menu_ids`
  ) AS `merged`
    ON `merged`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = `merged`.`menu_ids`,
      `package`.`updater` = '1',
      `package`.`update_time` = NOW();

  INSERT INTO `system_role_menu` (
    `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    `role`.`id`,
    `config_menu`.`id`,
    '1',
    NOW(),
    '1',
    NOW(),
    b'0',
    `role`.`tenant_id`
  FROM `system_role` AS `role`
  CROSS JOIN `tmp_system_config_package_menu_ids` AS `config_menu`
  LEFT JOIN `system_tenant` AS `tenant`
    ON `tenant`.`id` = `role`.`tenant_id`
   AND `tenant`.`deleted` = b'0'
  LEFT JOIN `system_tenant_package` AS `package`
    ON `package`.`id` = `tenant`.`package_id`
   AND `package`.`deleted` = b'0'
   AND JSON_VALID(`package`.`menu_ids`)
  WHERE `role`.`deleted` = b'0'
    AND (
      `role`.`code` = 'super_admin'
      OR (
        `role`.`code` = 'tenant_admin'
        AND `package`.`id` IS NOT NULL
        AND JSON_CONTAINS(`package`.`menu_ids`, CAST(`config_menu`.`id` AS JSON), '$')
      )
    )
    AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` AS `existing`
      WHERE `existing`.`role_id` = `role`.`id`
        AND `existing`.`menu_id` = `config_menu`.`id`
        AND `existing`.`deleted` = b'0'
    );

  DROP TEMPORARY TABLE IF EXISTS `tmp_system_config_package_package_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_system_config_package_menu_ids`;
END//
DELIMITER ;

CALL ensure_system_config_package_menu();

DROP PROCEDURE IF EXISTS ensure_system_config_package_menu;
