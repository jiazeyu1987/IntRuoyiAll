-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260719_business_approval_policy; type=menu; riskLevel=low
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_business_approval_policy_menu;

DELIMITER //
CREATE PROCEDURE ensure_business_approval_policy_menu()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 1186
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing workflow management parent menu 1186 for business approval policy';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_business_approval_policy_menu_defs`;
  CREATE TEMPORARY TABLE `tmp_business_approval_policy_menu_defs` (
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

  INSERT INTO `tmp_business_approval_policy_menu_defs`
    (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`)
  VALUES
    (605071300, '业务审批策略', 'bpm:business-approval-policy:query', 2, 92, 1186, 'business-approval-policy', 'ep:operation', 'bpm/businessApprovalPolicy/index', 'BpmBusinessApprovalPolicy', 0, b'1', b'1', b'1'),
    (605071301, '业务审批策略查询', 'bpm:business-approval-policy:query', 3, 1, 605071300, '', '', '', '', 0, b'1', b'1', b'1'),
    (605071302, '业务审批策略创建', 'bpm:business-approval-policy:create', 3, 2, 605071300, '', '', '', '', 0, b'1', b'1', b'1'),
    (605071303, '业务审批策略发布', 'bpm:business-approval-policy:publish', 3, 3, 605071300, '', '', '', '', 0, b'1', b'1', b'1'),
    (605071304, '业务审批策略停用', 'bpm:business-approval-policy:disable', 3, 4, 605071300, '', '', '', '', 0, b'1', b'1', b'1');

  IF EXISTS (
    SELECT 1
    FROM `system_menu` AS `existing`
    JOIN `tmp_business_approval_policy_menu_defs` AS `def`
      ON `def`.`id` = `existing`.`id`
    WHERE `existing`.`deleted` <> b'0'
       OR COALESCE(`existing`.`permission`, '') <> `def`.`permission`
       OR `existing`.`type` <> `def`.`type`
       OR `existing`.`parent_id` <> `def`.`parent_id`
       OR COALESCE(`existing`.`path`, '') <> `def`.`path`
       OR COALESCE(`existing`.`component`, '') <> `def`.`component`
       OR COALESCE(`existing`.`component_name`, '') <> `def`.`component_name`
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Conflicting business approval policy system_menu id or permission exists';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_menu` AS `existing`
    JOIN `tmp_business_approval_policy_menu_defs` AS `def`
      ON `def`.`permission` <> ''
     AND `def`.`permission` = `existing`.`permission`
     AND `def`.`id` <> `existing`.`id`
    WHERE `existing`.`deleted` = b'0'
      AND `existing`.`id` NOT IN (605071300, 605071301, 605071302, 605071303, 605071304)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Conflicting business approval policy system_menu id or permission exists';
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
  FROM `tmp_business_approval_policy_menu_defs` AS `def`
  LEFT JOIN `system_menu` AS `existing`
    ON `existing`.`id` = `def`.`id`
  WHERE `existing`.`id` IS NULL;

  UPDATE `system_menu` AS `existing`
  JOIN `tmp_business_approval_policy_menu_defs` AS `def`
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

  DROP TEMPORARY TABLE IF EXISTS `tmp_business_approval_policy_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_business_approval_policy_menu_ids` AS
  SELECT `id`
  FROM `system_menu`
  WHERE `deleted` = b'0'
    AND `id` IN (605071300, 605071301, 605071302, 605071303, 605071304);

  IF (SELECT COUNT(*) FROM `tmp_business_approval_policy_menu_ids`) <> 5 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing or duplicated business approval policy menu rows';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_tenant_package` AS `package`
    WHERE `package`.`deleted` = b'0'
      AND NOT JSON_VALID(`package`.`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot merge business approval policy menus';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_business_approval_policy_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_business_approval_policy_package_menu_ids` (
    `package_id` bigint NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`package_id`, `menu_id`)
  );

  INSERT IGNORE INTO `tmp_business_approval_policy_package_menu_ids` (`package_id`, `menu_id`)
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

  INSERT IGNORE INTO `tmp_business_approval_policy_package_menu_ids` (`package_id`, `menu_id`)
  SELECT
    `package`.`id`,
    `policy_menu`.`id`
  FROM `system_tenant_package` AS `package`
  CROSS JOIN `tmp_business_approval_policy_menu_ids` AS `policy_menu`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`)
    AND JSON_CONTAINS(`package`.`menu_ids`, CAST('1186' AS JSON), '$');

  UPDATE `system_tenant_package` AS `package`
  JOIN (
    SELECT DISTINCT
      `package_id`,
      JSON_ARRAYAGG(`menu_id`) OVER (
        PARTITION BY `package_id`
        ORDER BY `menu_id`
        ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
      ) AS `menu_ids`
    FROM `tmp_business_approval_policy_package_menu_ids`
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
    `policy_menu`.`id`,
    '1',
    NOW(),
    '1',
    NOW(),
    b'0',
    `role`.`tenant_id`
  FROM `system_role` AS `role`
  CROSS JOIN `tmp_business_approval_policy_menu_ids` AS `policy_menu`
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
      OR `role`.`code` = 'bpm_admin'
      OR (
        `role`.`code` = 'tenant_admin'
        AND `package`.`id` IS NOT NULL
        AND JSON_CONTAINS(`package`.`menu_ids`, CAST(`policy_menu`.`id` AS JSON), '$')
      )
    )
    AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` AS `existing`
      WHERE `existing`.`role_id` = `role`.`id`
        AND `existing`.`menu_id` = `policy_menu`.`id`
        AND `existing`.`deleted` = b'0'
    );

  DROP TEMPORARY TABLE IF EXISTS `tmp_business_approval_policy_package_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_business_approval_policy_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_business_approval_policy_menu_defs`;
END//
DELIMITER ;

CALL ensure_business_approval_policy_menu();

DROP PROCEDURE IF EXISTS ensure_business_approval_policy_menu;
