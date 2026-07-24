-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=menu; riskLevel=low
SET NAMES utf8mb4;

INSERT INTO `system_menu` (
  `id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`,
  `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`,
  `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES
  (5900, 'NAS 管理', '', 2, 7, 1, 'nas', 'ep:connection', 'system/nas/index', 'SystemNasManagement', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `permission` = VALUES(`permission`),
  `type` = VALUES(`type`),
  `sort` = VALUES(`sort`),
  `parent_id` = VALUES(`parent_id`),
  `path` = VALUES(`path`),
  `icon` = VALUES(`icon`),
  `component` = VALUES(`component`),
  `component_name` = VALUES(`component_name`),
  `status` = VALUES(`status`),
  `visible` = VALUES(`visible`),
  `keep_alive` = VALUES(`keep_alive`),
  `always_show` = VALUES(`always_show`),
  `updater` = VALUES(`updater`),
  `update_time` = VALUES(`update_time`),
  `deleted` = VALUES(`deleted`);

INSERT INTO `system_menu` (
  `id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`,
  `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`,
  `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES
  (5901, 'NAS 配置查询', 'infra:nas:query', 3, 1, 5900, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (5902, 'NAS 配置保存', 'infra:nas:update', 3, 2, 5900, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (5903, 'NAS 连接测试', 'infra:nas:test', 3, 3, 5900, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `permission` = VALUES(`permission`),
  `type` = VALUES(`type`),
  `sort` = VALUES(`sort`),
  `parent_id` = VALUES(`parent_id`),
  `path` = VALUES(`path`),
  `icon` = VALUES(`icon`),
  `component` = VALUES(`component`),
  `component_name` = VALUES(`component_name`),
  `status` = VALUES(`status`),
  `visible` = VALUES(`visible`),
  `keep_alive` = VALUES(`keep_alive`),
  `always_show` = VALUES(`always_show`),
  `updater` = VALUES(`updater`),
  `update_time` = VALUES(`update_time`),
  `deleted` = VALUES(`deleted`);

DROP PROCEDURE IF EXISTS ensure_system_nas_management_tenant_package_menus;

DELIMITER //
CREATE PROCEDURE ensure_system_nas_management_tenant_package_menus()
BEGIN
  IF EXISTS (
    SELECT 1
    FROM `system_tenant_package` AS `package`
    WHERE `package`.`deleted` = b'0'
      AND NOT JSON_VALID(`package`.`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot merge NAS management menus';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_system_nas_management_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_system_nas_management_menu_ids` AS
  SELECT `id`
  FROM `system_menu`
  WHERE `id` IN (5900, 5901, 5902, 5903)
    AND `deleted` = b'0';

  IF (SELECT COUNT(*) FROM `tmp_system_nas_management_menu_ids`) <> 4 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing NAS management system_menu rows; cannot merge tenant package menu_ids';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_system_nas_management_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_system_nas_management_package_menu_ids` (
    `package_id` bigint NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`package_id`, `menu_id`)
  );

  INSERT IGNORE INTO `tmp_system_nas_management_package_menu_ids` (`package_id`, `menu_id`)
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

  INSERT IGNORE INTO `tmp_system_nas_management_package_menu_ids` (`package_id`, `menu_id`)
  SELECT
    `package`.`id`,
    `nas_menu`.`id`
  FROM `system_tenant_package` AS `package`
  CROSS JOIN `tmp_system_nas_management_menu_ids` AS `nas_menu`
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
    FROM `tmp_system_nas_management_package_menu_ids`
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
    `nas_menu`.`id`,
    '1',
    NOW(),
    '1',
    NOW(),
    b'0',
    `role`.`tenant_id`
  FROM `system_role` AS `role`
  JOIN `system_tenant` AS `tenant`
    ON `tenant`.`id` = `role`.`tenant_id`
   AND `tenant`.`deleted` = b'0'
  JOIN `system_tenant_package` AS `package`
    ON `package`.`id` = `tenant`.`package_id`
   AND `package`.`deleted` = b'0'
   AND JSON_VALID(`package`.`menu_ids`)
  CROSS JOIN `tmp_system_nas_management_menu_ids` AS `nas_menu`
  WHERE `role`.`code` = 'tenant_admin'
    AND `role`.`deleted` = b'0'
    AND JSON_CONTAINS(`package`.`menu_ids`, CAST(`nas_menu`.`id` AS JSON), '$')
    AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` AS `existing`
      WHERE `existing`.`role_id` = `role`.`id`
        AND `existing`.`menu_id` = `nas_menu`.`id`
        AND `existing`.`deleted` = b'0'
    );

  DROP TEMPORARY TABLE IF EXISTS `tmp_system_nas_management_package_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_system_nas_management_menu_ids`;
END//
DELIMITER ;

CALL ensure_system_nas_management_tenant_package_menus();

DROP PROCEDURE IF EXISTS ensure_system_nas_management_tenant_package_menus;
