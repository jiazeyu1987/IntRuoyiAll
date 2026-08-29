-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260816_mdm_enterprise_company_scope; type=data; riskLevel=medium
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_mdm_associated_company_menu;
DELIMITER $$
CREATE PROCEDURE ensure_mdm_associated_company_menu()
BEGIN
  IF EXISTS (
      SELECT 1
      FROM `system_tenant_package` AS `package`
      WHERE `package`.`deleted` = b'0'
        AND NOT JSON_VALID(`package`.`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot merge MDM associated company menu';
  END IF;

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
   `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 990200, CONVERT(UNHEX('E59FBAE7A180E695B0E68DAE') USING utf8mb4) COLLATE utf8mb4_unicode_ci,
         '', 1, 35, 0, '/mdm', 'ep:coin', NULL, NULL,
         0, b'1', b'1', b'1', 'mdm-associated-company', NOW(), 'mdm-associated-company', NOW(), b'0'
  WHERE NOT EXISTS (
    SELECT 1 FROM `system_menu` WHERE `deleted` = b'0' AND (`id` = 990200 OR `path` = '/mdm')
  );

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
   `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 990249, CONVERT(UNHEX('E585B3E88194E585ACE58FB8') USING utf8mb4) COLLATE utf8mb4_unicode_ci,
         'mdm:enterprise:query', 2, 15, 990200, 'enterprise', 'ep:office-building',
         'mdm/enterprise/index', 'MdmEnterprise',
         0, b'1', b'1', b'1', 'mdm-associated-company', NOW(), 'mdm-associated-company', NOW(), b'0'
  WHERE EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 990200 AND `deleted` = b'0')
    AND NOT EXISTS (
      SELECT 1
      FROM `system_menu`
      WHERE `deleted` = b'0'
        AND (`id` = 990249
          OR (`parent_id` = 990200 AND `path` = 'enterprise')
          OR `permission` = 'mdm:enterprise:query')
    );

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
   `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 990250, CONVERT(UNHEX('E585B3E88194E585ACE58FB8E696B0E5A29E') USING utf8mb4) COLLATE utf8mb4_unicode_ci,
         'mdm:enterprise:create', 3, 20, 990249, '', '', '', '',
         0, b'1', b'1', b'1', 'mdm-associated-company', NOW(), 'mdm-associated-company', NOW(), b'0'
  WHERE EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 990249 AND `deleted` = b'0')
    AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `deleted` = b'0' AND `permission` = 'mdm:enterprise:create');

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
   `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 990251, CONVERT(UNHEX('E585B3E88194E585ACE58FB8E4BFAEE694B9') USING utf8mb4) COLLATE utf8mb4_unicode_ci,
         'mdm:enterprise:update', 3, 30, 990249, '', '', '', '',
         0, b'1', b'1', b'1', 'mdm-associated-company', NOW(), 'mdm-associated-company', NOW(), b'0'
  WHERE EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 990249 AND `deleted` = b'0')
    AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `deleted` = b'0' AND `permission` = 'mdm:enterprise:update');

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
   `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 990252, CONVERT(UNHEX('E585B3E88194E585ACE58FB8E588A0E999A4') USING utf8mb4) COLLATE utf8mb4_unicode_ci,
         'mdm:enterprise:delete', 3, 40, 990249, '', '', '', '',
         0, b'1', b'1', b'1', 'mdm-associated-company', NOW(), 'mdm-associated-company', NOW(), b'0'
  WHERE EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 990249 AND `deleted` = b'0')
    AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `deleted` = b'0' AND `permission` = 'mdm:enterprise:delete');

  IF NOT EXISTS (
    SELECT 1
      FROM `system_menu`
    WHERE `id` = 990249
      AND `deleted` = b'0'
      AND `parent_id` = 990200
      AND `path` = 'enterprise'
      AND `component` = 'mdm/enterprise/index'
      AND `component_name` = 'MdmEnterprise'
      AND `permission` = 'mdm:enterprise:query'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'MDM associated company menu contract mismatch';
  END IF;

  IF NOT EXISTS (
    SELECT 1
      FROM `system_users` AS `user`
      INNER JOIN `system_user_role` AS `user_role`
        ON `user_role`.`user_id` = `user`.`id`
       AND `user_role`.`tenant_id` = `user`.`tenant_id`
       AND `user_role`.`deleted` = b'0'
      INNER JOIN `system_role` AS `role`
        ON `role`.`id` = `user_role`.`role_id`
       AND `role`.`tenant_id` = `user_role`.`tenant_id`
       AND `role`.`deleted` = b'0'
       AND `role`.`code` = 'super_admin'
     WHERE `user`.`tenant_id` = 1
       AND `user`.`username` = 'admin'
       AND `user`.`status` = 0
       AND `user`.`deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing tenant 1 admin super_admin binding for MDM associated company menu';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mdm_assoc_company_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mdm_assoc_company_menu_ids` (
    `menu_id` BIGINT NOT NULL PRIMARY KEY
  );

  INSERT INTO `tmp_mdm_assoc_company_menu_ids` (`menu_id`) VALUES
    (990249), (990250), (990251), (990252);

  DROP TEMPORARY TABLE IF EXISTS `tmp_mdm_assoc_company_target_packages`;
  CREATE TEMPORARY TABLE `tmp_mdm_assoc_company_target_packages` AS
  SELECT DISTINCT `package`.`id` AS `package_id`
  FROM `system_tenant_package` AS `package`
  INNER JOIN JSON_TABLE(
      `package`.`menu_ids`,
      '$[*]' COLUMNS (`menu_id` BIGINT PATH '$')
  ) AS `existing_menu`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`)
    AND `existing_menu`.`menu_id` IN (990200, 990201, 990210, 990216);

  DROP TEMPORARY TABLE IF EXISTS `tmp_mdm_assoc_company_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mdm_assoc_company_package_menu_ids` (
    `package_id` BIGINT NOT NULL,
    `menu_id` BIGINT NOT NULL,
    PRIMARY KEY (`package_id`, `menu_id`)
  );

  INSERT IGNORE INTO `tmp_mdm_assoc_company_package_menu_ids` (`package_id`, `menu_id`)
  SELECT `target_package`.`package_id`, `existing_menu`.`menu_id`
  FROM `tmp_mdm_assoc_company_target_packages` AS `target_package`
  INNER JOIN `system_tenant_package` AS `package`
      ON `package`.`id` = `target_package`.`package_id`
  INNER JOIN JSON_TABLE(
      `package`.`menu_ids`,
      '$[*]' COLUMNS (`menu_id` BIGINT PATH '$')
  ) AS `existing_menu`;

  INSERT IGNORE INTO `tmp_mdm_assoc_company_package_menu_ids` (`package_id`, `menu_id`)
  SELECT `target_package`.`package_id`, `menu`.`menu_id`
  FROM `tmp_mdm_assoc_company_target_packages` AS `target_package`
  CROSS JOIN `tmp_mdm_assoc_company_menu_ids` AS `menu`;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mdm_assoc_company_package_menu_json`;
  CREATE TEMPORARY TABLE `tmp_mdm_assoc_company_package_menu_json` AS
  SELECT
      `package_id`,
      JSON_ARRAYAGG(`menu_id`) AS `menu_ids`
  FROM (
      SELECT `package_id`, `menu_id`
      FROM `tmp_mdm_assoc_company_package_menu_ids`
      ORDER BY `package_id`, `menu_id`
  ) AS `ordered_menu`
  GROUP BY `package_id`;

  UPDATE `system_tenant_package` AS `package`
  INNER JOIN `tmp_mdm_assoc_company_package_menu_json` AS `merged`
      ON `merged`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = `merged`.`menu_ids`,
      `package`.`updater` = 'mdm-associated-company',
      `package`.`update_time` = NOW()
  WHERE `package`.`deleted` = b'0';

  DROP TEMPORARY TABLE IF EXISTS `tmp_mdm_assoc_company_target_roles`;
  CREATE TEMPORARY TABLE `tmp_mdm_assoc_company_target_roles` AS
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
  LEFT JOIN `tmp_mdm_assoc_company_target_packages` AS `target_package`
      ON `target_package`.`package_id` = `tenant`.`package_id`
  WHERE `tenant`.`deleted` = b'0'
    AND (`target_package`.`package_id` IS NOT NULL OR `tenant`.`id` = 1);

  UPDATE `system_role_menu` AS `role_menu`
  INNER JOIN `tmp_mdm_assoc_company_target_roles` AS `target_role`
      ON `target_role`.`tenant_id` = `role_menu`.`tenant_id`
     AND `target_role`.`role_id` = `role_menu`.`role_id`
  INNER JOIN `tmp_mdm_assoc_company_menu_ids` AS `menu`
      ON `menu`.`menu_id` = `role_menu`.`menu_id`
  SET `role_menu`.`deleted` = b'0',
      `role_menu`.`updater` = 'mdm-associated-company',
      `role_menu`.`update_time` = NOW()
  WHERE `role_menu`.`deleted` = b'1';

  INSERT INTO `system_role_menu`
    (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT
      `target_role`.`role_id`,
      `menu`.`menu_id`,
      'mdm-associated-company',
      NOW(),
      'mdm-associated-company',
      NOW(),
      b'0',
      `target_role`.`tenant_id`
  FROM `tmp_mdm_assoc_company_target_roles` AS `target_role`
  CROSS JOIN `tmp_mdm_assoc_company_menu_ids` AS `menu`
  WHERE NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` AS `existing`
      WHERE `existing`.`tenant_id` = `target_role`.`tenant_id`
        AND `existing`.`role_id` = `target_role`.`role_id`
        AND `existing`.`menu_id` = `menu`.`menu_id`
        AND `existing`.`deleted` = b'0'
  );
END$$
DELIMITER ;

CALL ensure_mdm_associated_company_menu();
DROP PROCEDURE IF EXISTS ensure_mdm_associated_company_menu;
