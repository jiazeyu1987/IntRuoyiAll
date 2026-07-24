-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260628_srm_t6_nas_locator; type=schema; riskLevel=medium
-- SRM T6 NAS locator blacklist config permission menu.

SET NAMES utf8mb4;
SET @SRM_NAS_BLACKLIST_MENU_ID := 991105;

DROP PROCEDURE IF EXISTS ensure_srm_t6_nas_locator_blacklist_config;

DELIMITER $$
CREATE PROCEDURE ensure_srm_t6_nas_locator_blacklist_config()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `id` = 991100
      AND `component` = 'srm/nas-locator/index'
      AND `component_name` = 'SrmNasLocator'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = '缺少 SRM T6 NAS定位 路由菜单，禁止安装黑名单按钮权限';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `id` = @SRM_NAS_BLACKLIST_MENU_ID
      AND `permission` <> 'srm:nas-locator:config'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'SRM T6 NAS定位 黑名单菜单 ID 已被其他权限占用';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_tenant_package`
    WHERE `deleted` = b'0'
      AND `menu_ids` IS NOT NULL
      AND `menu_ids` <> ''
      AND JSON_VALID(`menu_ids`) = 0
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON';
  END IF;

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT @SRM_NAS_BLACKLIST_MENU_ID, 'NAS定位黑名单', 'srm:nas-locator:config', 3, 4, 991100, '', '', '', '', 0, b'1', b'1', b'1', 'srm-t6', NOW(), 'srm-t6', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = @SRM_NAS_BLACKLIST_MENU_ID);

  UPDATE `system_menu`
  SET `name` = 'NAS定位黑名单',
      `permission` = 'srm:nas-locator:config',
      `type` = 3,
      `sort` = 4,
      `parent_id` = 991100,
      `updater` = 'srm-t6',
      `update_time` = NOW()
  WHERE `id` = @SRM_NAS_BLACKLIST_MENU_ID;

  DROP TEMPORARY TABLE IF EXISTS `tmp_srm_t6_blacklist_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_srm_t6_blacklist_menu_ids` AS
  SELECT `id`
  FROM `system_menu`
  WHERE `deleted` = b'0'
    AND `id` = @SRM_NAS_BLACKLIST_MENU_ID;

  DROP TEMPORARY TABLE IF EXISTS `tmp_srm_t6_blacklist_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_srm_t6_blacklist_package_menu_ids` (
    `package_id` bigint NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`package_id`, `menu_id`)
  );

  INSERT INTO `tmp_srm_t6_blacklist_package_menu_ids` (`package_id`, `menu_id`)
  SELECT DISTINCT
    `package`.`id`,
    CAST(`existing_menu`.`menu_id` AS UNSIGNED)
  FROM `system_tenant_package` AS `package`
  JOIN JSON_TABLE(
    `package`.`menu_ids`,
    '$[*]' COLUMNS (`menu_id` bigint PATH '$')
  ) AS `existing_menu`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`)
  UNION
  SELECT
    `package`.`id`,
    `menu`.`id`
  FROM `system_tenant_package` AS `package`
  CROSS JOIN `tmp_srm_t6_blacklist_menu_ids` AS `menu`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`)
    AND JSON_CONTAINS(`package`.`menu_ids`, CAST('991100' AS JSON), '$');

  UPDATE `system_tenant_package` AS `package`
  JOIN (
    SELECT DISTINCT
      `package_id`,
      JSON_ARRAYAGG(`menu_id`) OVER (
        PARTITION BY `package_id`
        ORDER BY `menu_id`
        ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
      ) AS `menu_ids`
    FROM `tmp_srm_t6_blacklist_package_menu_ids`
  ) AS `merged`
    ON `merged`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = `merged`.`menu_ids`,
      `package`.`updater` = 'srm-t6',
      `package`.`update_time` = NOW();

  INSERT INTO `system_role_menu`
  (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT
    `role`.`id`,
    `menu`.`id`,
    'srm-t6',
    NOW(),
    'srm-t6',
    NOW(),
    b'0',
    `role`.`tenant_id`
  FROM `system_role` AS `role`
  CROSS JOIN `tmp_srm_t6_blacklist_menu_ids` AS `menu`
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
      OR `role`.`code` = 'srm_admin'
      OR (
        `role`.`code` = 'tenant_admin'
        AND `package`.`id` IS NOT NULL
        AND JSON_CONTAINS(`package`.`menu_ids`, CAST(`menu`.`id` AS JSON), '$')
      )
    )
    AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` AS `existing`
      WHERE `existing`.`role_id` = `role`.`id`
        AND `existing`.`menu_id` = `menu`.`id`
        AND `existing`.`deleted` = b'0'
    );

  DROP TEMPORARY TABLE IF EXISTS `tmp_srm_t6_blacklist_package_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_srm_t6_blacklist_menu_ids`;
END$$
DELIMITER ;

CALL ensure_srm_t6_nas_locator_blacklist_config();
DROP PROCEDURE IF EXISTS ensure_srm_t6_nas_locator_blacklist_config;
