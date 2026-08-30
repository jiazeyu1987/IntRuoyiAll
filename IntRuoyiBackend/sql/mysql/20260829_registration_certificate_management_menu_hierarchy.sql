-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260816_dcc_registration_certificate_menu,20260829_mdm_associated_company_menu; type=data; riskLevel=medium
-- Purpose: Group supported registration certificate related menus under the registration certificate management parent and retire unavailable page menus.

SET NAMES utf8mb4;

START TRANSACTION;

DROP PROCEDURE IF EXISTS ensure_registration_certificate_management_menu_hierarchy;
DELIMITER $$
CREATE PROCEDURE ensure_registration_certificate_management_menu_hierarchy()
BEGIN
  IF NOT EXISTS (
      SELECT 1
        FROM `system_menu`
       WHERE `id` = 990200
         AND `deleted` = b'0'
         AND `path` = '/mdm'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing MDM basic data menu 990200 for registration certificate management hierarchy';
  END IF;

  IF EXISTS (
      SELECT 1
        FROM `system_menu`
       WHERE `id` IN (990230, 990231, 990232, 990249)
         AND `deleted` = b'1'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Registration certificate management target menu is soft deleted';
  END IF;

  IF EXISTS (
      SELECT 1
        FROM `system_menu`
       WHERE `id` = 990253
         AND `deleted` = b'0'
         AND NOT (
           `name` = '注册证管理'
           AND `permission` = ''
           AND `type` = 1
           AND `parent_id` = 990200
           AND `path` = 'registration-certificate-management'
           AND `component` IS NULL
           AND `component_name` IS NULL
         )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Registration certificate management parent menu contract mismatch';
  END IF;

  IF EXISTS (
      SELECT 1
        FROM `system_menu`
       WHERE `deleted` = b'0'
         AND `parent_id` = 990200
         AND `path` = 'registration-certificate-management'
         AND `id` <> 990253
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Registration certificate management parent menu path conflict';
  END IF;

  IF EXISTS (
      SELECT 1
        FROM `system_menu`
       WHERE `deleted` = b'0'
         AND `id` NOT IN (990230, 990231, 990232, 990249)
         AND `path` IN (
           '/mdm/registration-certificate',
           '/mdm/registration-certificate/detail/:id',
           '/mdm/registration-certificate/history/:id',
           '/mdm/enterprise'
         )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Registration certificate management child route conflict';
  END IF;

  IF NOT EXISTS (
      SELECT 1
        FROM `system_menu`
       WHERE `id` = 990230
         AND `deleted` = b'0'
         AND `name` = '注册证'
         AND `permission` = 'dcc:registration-certificate:query-current'
         AND `type` = 2
         AND `component` = 'dcc/registration-certificate/index/index'
         AND `component_name` = 'DccRegistrationCertificateIndex'
         AND (
           (`parent_id` = 990200 AND `path` = 'registration-certificate')
           OR (`parent_id` = 990253 AND `path` = '/mdm/registration-certificate')
         )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Registration certificate page menu contract mismatch';
  END IF;

  IF NOT EXISTS (
      SELECT 1
        FROM `system_menu`
       WHERE `id` = 990249
         AND `deleted` = b'0'
         AND `name` = '关联公司'
         AND `permission` = 'mdm:enterprise:query'
         AND `type` = 2
         AND `component` = 'mdm/enterprise/index'
         AND `component_name` = 'MdmEnterprise'
         AND (
           (`parent_id` = 990200 AND `path` = 'enterprise')
           OR (`parent_id` = 990253 AND `path` = '/mdm/enterprise')
         )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Associated company page menu contract mismatch';
  END IF;

  IF EXISTS (
      SELECT 1
        FROM `system_menu`
       WHERE `id` = 605071320
         AND `deleted` = b'0'
         AND NOT (
           `name` = '企业公司范围'
           AND `permission` = 'mdm:company-scope:query'
           AND `type` = 2
           AND `component` = 'mdm/company-scope/index'
           AND `component_name` = 'MdmCompanyScope'
         )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Registration certificate management retired menu contract mismatch: enterprise company scope';
  END IF;

  IF EXISTS (
      SELECT 1
        FROM `system_menu`
       WHERE `id` = 605071321
         AND `deleted` = b'0'
         AND NOT (
           `name` = '注册证历史导入'
           AND `permission` = 'dcc:registration-certificate:historical-import'
           AND `type` = 2
           AND `component` = 'dcc/registration-certificate/historical-import/index'
           AND `component_name` = 'DccRegistrationCertificateHistoricalImport'
         )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Registration certificate management retired menu contract mismatch: historical import';
  END IF;

  IF NOT EXISTS (
      SELECT 1
        FROM `system_menu`
       WHERE `id` = 990231
         AND `deleted` = b'0'
         AND `name` = '注册证详情'
         AND `permission` = 'dcc:registration-certificate:query-current'
         AND `type` = 2
         AND `component` = 'dcc/registration-certificate/detail/index'
         AND `component_name` = 'DccRegistrationCertificateDetail'
         AND (
           (`parent_id` = 990200 AND `path` = 'registration-certificate/detail/:id')
           OR (`parent_id` = 990253 AND `path` = '/mdm/registration-certificate/detail/:id')
         )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Registration certificate detail menu contract mismatch';
  END IF;

  IF NOT EXISTS (
      SELECT 1
        FROM `system_menu`
       WHERE `id` = 990232
         AND `deleted` = b'0'
         AND `name` = '注册证履历'
         AND `permission` = 'dcc:registration-certificate:query-current'
         AND `type` = 2
         AND `component` = 'dcc/registration-certificate/history/index'
         AND `component_name` = 'DccRegistrationCertificateHistory'
         AND (
           (`parent_id` = 990200 AND `path` = 'registration-certificate/history/:id')
           OR (`parent_id` = 990253 AND `path` = '/mdm/registration-certificate/history/:id')
         )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Registration certificate history menu contract mismatch';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM `system_tenant_package` AS `package`
     WHERE `package`.`deleted` = b'0'
       AND NOT JSON_VALID(`package`.`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON';
  END IF;

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
   `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 990253, '注册证管理', '', 1, 22, 990200, 'registration-certificate-management', 'ep:document-checked', NULL, NULL,
         0, b'1', b'1', b'1', 'registration-certificate-menu-hierarchy', NOW(),
         'registration-certificate-menu-hierarchy', NOW(), b'0'
  WHERE NOT EXISTS (
    SELECT 1
      FROM `system_menu`
     WHERE `id` = 990253
       AND `deleted` = b'0'
  );

  UPDATE `system_menu`
     SET `name` = '注册证管理',
         `permission` = '',
         `type` = 1,
         `sort` = 22,
         `parent_id` = 990200,
         `path` = 'registration-certificate-management',
         `icon` = 'ep:document-checked',
         `component` = NULL,
         `component_name` = NULL,
         `status` = 0,
         `visible` = b'1',
         `keep_alive` = b'1',
         `always_show` = b'1',
         `updater` = 'registration-certificate-menu-hierarchy',
         `update_time` = NOW()
   WHERE `id` = 990253
     AND `deleted` = b'0';

  UPDATE `system_menu`
     SET `parent_id` = 990253,
         `sort` = 10,
         `path` = '/mdm/enterprise',
         `updater` = 'registration-certificate-menu-hierarchy',
         `update_time` = NOW()
   WHERE `id` = 990249
     AND `deleted` = b'0';

  UPDATE `system_menu`
     SET `parent_id` = 990253,
         `sort` = 20,
         `path` = '/mdm/registration-certificate',
         `updater` = 'registration-certificate-menu-hierarchy',
         `update_time` = NOW()
   WHERE `id` = 990230
     AND `deleted` = b'0';

  UPDATE `system_menu`
     SET `parent_id` = 990253,
         `path` = '/mdm/registration-certificate/detail/:id',
         `updater` = 'registration-certificate-menu-hierarchy',
         `update_time` = NOW()
   WHERE `id` = 990231
     AND `deleted` = b'0';

  UPDATE `system_menu`
     SET `parent_id` = 990253,
         `path` = '/mdm/registration-certificate/history/:id',
         `updater` = 'registration-certificate-menu-hierarchy',
         `update_time` = NOW()
   WHERE `id` = 990232
     AND `deleted` = b'0';

  DROP TEMPORARY TABLE IF EXISTS `tmp_registration_certificate_management_retired_menu`;
  CREATE TEMPORARY TABLE `tmp_registration_certificate_management_retired_menu` (
    `menu_id` BIGINT NOT NULL PRIMARY KEY
  );

  INSERT IGNORE INTO `tmp_registration_certificate_management_retired_menu` (`menu_id`) VALUES
    (605071320), (605071321);

  DROP TEMPORARY TABLE IF EXISTS `tmp_registration_certificate_management_retired_candidate_menu`;
  CREATE TEMPORARY TABLE `tmp_registration_certificate_management_retired_candidate_menu` (
    `menu_id` BIGINT NOT NULL PRIMARY KEY
  );

  INSERT IGNORE INTO `tmp_registration_certificate_management_retired_candidate_menu` (`menu_id`)
  SELECT `child`.`id`
    FROM `system_menu` AS `child`
    INNER JOIN `tmp_registration_certificate_management_retired_menu` AS `parent`
      ON `parent`.`menu_id` = `child`.`parent_id`;

  INSERT IGNORE INTO `tmp_registration_certificate_management_retired_menu` (`menu_id`)
  SELECT `menu_id`
    FROM `tmp_registration_certificate_management_retired_candidate_menu`;

  DROP TEMPORARY TABLE IF EXISTS `tmp_registration_certificate_management_retired_candidate_menu`;
  CREATE TEMPORARY TABLE `tmp_registration_certificate_management_retired_candidate_menu` (
    `menu_id` BIGINT NOT NULL PRIMARY KEY
  );

  INSERT IGNORE INTO `tmp_registration_certificate_management_retired_candidate_menu` (`menu_id`)
  SELECT `child`.`id`
    FROM `system_menu` AS `child`
    INNER JOIN `tmp_registration_certificate_management_retired_menu` AS `parent`
      ON `parent`.`menu_id` = `child`.`parent_id`;

  INSERT IGNORE INTO `tmp_registration_certificate_management_retired_menu` (`menu_id`)
  SELECT `menu_id`
    FROM `tmp_registration_certificate_management_retired_candidate_menu`;

  UPDATE `system_menu` AS `menu`
  INNER JOIN `tmp_registration_certificate_management_retired_menu` AS `retired`
    ON `retired`.`menu_id` = `menu`.`id`
     SET `menu`.`deleted` = b'1',
         `menu`.`status` = 1,
         `menu`.`visible` = b'0',
         `menu`.`updater` = 'registration-certificate-menu-hierarchy',
         `menu`.`update_time` = NOW();

  DROP TEMPORARY TABLE IF EXISTS `tmp_registration_certificate_management_page_menu`;
  CREATE TEMPORARY TABLE `tmp_registration_certificate_management_page_menu` (
    `menu_id` BIGINT NOT NULL PRIMARY KEY
  );

  INSERT INTO `tmp_registration_certificate_management_page_menu` (`menu_id`) VALUES
    (990230), (990249);

  DROP TEMPORARY TABLE IF EXISTS `tmp_registration_certificate_management_parent_package_ids`;
  CREATE TEMPORARY TABLE `tmp_registration_certificate_management_parent_package_ids` (
    `package_id` BIGINT NOT NULL PRIMARY KEY
  );

  INSERT INTO `tmp_registration_certificate_management_parent_package_ids` (`package_id`)
  SELECT DISTINCT `package`.`id`
    FROM `system_tenant_package` AS `package`
    INNER JOIN JSON_TABLE(
      `package`.`menu_ids`,
      '$[*]' COLUMNS (`menu_id` BIGINT PATH '$')
    ) AS `existing_menu`
      ON 1 = 1
   WHERE `package`.`deleted` = b'0'
     AND `existing_menu`.`menu_id` IN (
       SELECT `menu_id`
         FROM `tmp_registration_certificate_management_page_menu`
     );

  DROP TEMPORARY TABLE IF EXISTS `tmp_registration_certificate_management_package_ids`;
  CREATE TEMPORARY TABLE `tmp_registration_certificate_management_package_ids` (
    `package_id` BIGINT NOT NULL PRIMARY KEY
  );

  INSERT INTO `tmp_registration_certificate_management_package_ids` (`package_id`)
  SELECT `package_id`
    FROM `tmp_registration_certificate_management_parent_package_ids`;

  INSERT IGNORE INTO `tmp_registration_certificate_management_package_ids` (`package_id`)
  SELECT DISTINCT `package`.`id`
    FROM `system_tenant_package` AS `package`
    INNER JOIN JSON_TABLE(
      `package`.`menu_ids`,
      '$[*]' COLUMNS (`menu_id` BIGINT PATH '$')
    ) AS `existing_menu`
      ON 1 = 1
   WHERE `package`.`deleted` = b'0'
     AND `existing_menu`.`menu_id` IN (
       SELECT `menu_id`
         FROM `tmp_registration_certificate_management_retired_menu`
     );

  DROP TEMPORARY TABLE IF EXISTS `tmp_registration_certificate_management_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_registration_certificate_management_package_menu_ids` (
    `package_id` BIGINT NOT NULL,
    `menu_id` BIGINT NOT NULL,
    PRIMARY KEY (`package_id`, `menu_id`)
  );

  INSERT IGNORE INTO `tmp_registration_certificate_management_package_menu_ids` (`package_id`, `menu_id`)
  SELECT `target_package`.`package_id`, `existing_menu`.`menu_id`
    FROM `tmp_registration_certificate_management_package_ids` AS `target_package`
    INNER JOIN `system_tenant_package` AS `package`
      ON `package`.`id` = `target_package`.`package_id`
    INNER JOIN JSON_TABLE(
      `package`.`menu_ids`,
      '$[*]' COLUMNS (`menu_id` BIGINT PATH '$')
    ) AS `existing_menu`
      ON 1 = 1
    LEFT JOIN `tmp_registration_certificate_management_retired_menu` AS `retired`
      ON `retired`.`menu_id` = `existing_menu`.`menu_id`
   WHERE `retired`.`menu_id` IS NULL;

  INSERT IGNORE INTO `tmp_registration_certificate_management_package_menu_ids` (`package_id`, `menu_id`)
  SELECT `package_id`, 990253
    FROM `tmp_registration_certificate_management_parent_package_ids`;

  DROP TEMPORARY TABLE IF EXISTS `tmp_registration_certificate_management_package_menu_json`;
  CREATE TEMPORARY TABLE `tmp_registration_certificate_management_package_menu_json` (
    `package_id` BIGINT NOT NULL PRIMARY KEY,
    `menu_ids` JSON NOT NULL
  );

  INSERT INTO `tmp_registration_certificate_management_package_menu_json` (`package_id`, `menu_ids`)
  SELECT `package_id`, JSON_ARRAY()
    FROM `tmp_registration_certificate_management_package_ids`;

  REPLACE INTO `tmp_registration_certificate_management_package_menu_json` (`package_id`, `menu_ids`)
  SELECT `package_id`, JSON_ARRAYAGG(`menu_id`) AS `menu_ids`
    FROM (
      SELECT `package_id`, `menu_id`
        FROM `tmp_registration_certificate_management_package_menu_ids`
       ORDER BY `package_id`, `menu_id`
    ) AS `ordered_menu`
   GROUP BY `package_id`;

  UPDATE `system_tenant_package` AS `package`
  INNER JOIN `tmp_registration_certificate_management_package_menu_json` AS `merged`
    ON `merged`.`package_id` = `package`.`id`
     SET `package`.`menu_ids` = `merged`.`menu_ids`,
         `package`.`updater` = 'registration-certificate-menu-hierarchy',
         `package`.`update_time` = NOW()
   WHERE `package`.`deleted` = b'0';

  DROP TEMPORARY TABLE IF EXISTS `tmp_registration_certificate_management_role_ids`;
  CREATE TEMPORARY TABLE `tmp_registration_certificate_management_role_ids` (
    `tenant_id` BIGINT NOT NULL,
    `role_id` BIGINT NOT NULL,
    PRIMARY KEY (`tenant_id`, `role_id`)
  );

  INSERT INTO `tmp_registration_certificate_management_role_ids` (`tenant_id`, `role_id`)
  SELECT DISTINCT `role_menu`.`tenant_id`, `role_menu`.`role_id`
    FROM `system_role_menu` AS `role_menu`
   WHERE `role_menu`.`deleted` = b'0'
     AND `role_menu`.`menu_id` IN (
       SELECT `menu_id`
         FROM `tmp_registration_certificate_management_page_menu`
     );

  UPDATE `system_role_menu` AS `role_menu`
  INNER JOIN `tmp_registration_certificate_management_role_ids` AS `target_role`
    ON `target_role`.`tenant_id` = `role_menu`.`tenant_id`
   AND `target_role`.`role_id` = `role_menu`.`role_id`
     SET `role_menu`.`deleted` = b'0',
         `role_menu`.`updater` = 'registration-certificate-menu-hierarchy',
         `role_menu`.`update_time` = NOW()
   WHERE `role_menu`.`menu_id` = 990253
     AND `role_menu`.`deleted` = b'1';

  INSERT INTO `system_role_menu`
    (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT `target_role`.`role_id`, 990253,
         'registration-certificate-menu-hierarchy', NOW(),
         'registration-certificate-menu-hierarchy', NOW(), b'0', `target_role`.`tenant_id`
    FROM `tmp_registration_certificate_management_role_ids` AS `target_role`
   WHERE NOT EXISTS (
      SELECT 1
        FROM `system_role_menu` AS `existing`
       WHERE `existing`.`tenant_id` = `target_role`.`tenant_id`
         AND `existing`.`role_id` = `target_role`.`role_id`
         AND `existing`.`menu_id` = 990253
         AND `existing`.`deleted` = b'0'
   );

  UPDATE `system_role_menu` AS `role_menu`
  INNER JOIN `tmp_registration_certificate_management_retired_menu` AS `retired`
    ON `retired`.`menu_id` = `role_menu`.`menu_id`
     SET `role_menu`.`deleted` = b'1',
         `role_menu`.`updater` = 'registration-certificate-menu-hierarchy',
         `role_menu`.`update_time` = NOW()
   WHERE `role_menu`.`deleted` = b'0';

  IF NOT EXISTS (
      SELECT 1
        FROM `system_menu`
       WHERE `id` = 990253
         AND `deleted` = b'0'
         AND `name` = '注册证管理'
         AND `permission` = ''
         AND `type` = 1
         AND `parent_id` = 990200
         AND `path` = 'registration-certificate-management'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Registration certificate management parent menu final contract mismatch';
  END IF;

  IF EXISTS (
      SELECT 1
        FROM `system_menu`
       WHERE `deleted` = b'0'
         AND (
           (`id` = 990230 AND (`parent_id` <> 990253 OR `path` <> '/mdm/registration-certificate' OR `sort` <> 20))
           OR (`id` = 990249 AND (`parent_id` <> 990253 OR `path` <> '/mdm/enterprise' OR `sort` <> 10))
           OR (`id` = 990231 AND (`parent_id` <> 990253 OR `path` <> '/mdm/registration-certificate/detail/:id'))
           OR (`id` = 990232 AND (`parent_id` <> 990253 OR `path` <> '/mdm/registration-certificate/history/:id'))
         )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Registration certificate management target menu contract mismatch: child final contract';
  END IF;

  IF EXISTS (
      SELECT 1
        FROM `system_menu` AS `menu`
        INNER JOIN `tmp_registration_certificate_management_retired_menu` AS `retired`
          ON `retired`.`menu_id` = `menu`.`id`
       WHERE `menu`.`deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Registration certificate management retired menu final contract mismatch';
  END IF;

  IF EXISTS (
      SELECT 1
        FROM `system_tenant_package` AS `package`
        INNER JOIN JSON_TABLE(
          `package`.`menu_ids`,
          '$[*]' COLUMNS (`menu_id` BIGINT PATH '$')
        ) AS `existing_menu`
          ON 1 = 1
        INNER JOIN `tmp_registration_certificate_management_retired_menu` AS `retired`
          ON `retired`.`menu_id` = `existing_menu`.`menu_id`
       WHERE `package`.`deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Registration certificate management retired package final contract mismatch';
  END IF;

  IF EXISTS (
      SELECT 1
        FROM `system_role_menu` AS `role_menu`
        INNER JOIN `tmp_registration_certificate_management_retired_menu` AS `retired`
          ON `retired`.`menu_id` = `role_menu`.`menu_id`
       WHERE `role_menu`.`deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Registration certificate management retired role final contract mismatch';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_registration_certificate_management_page_menu`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_registration_certificate_management_retired_menu`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_registration_certificate_management_retired_candidate_menu`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_registration_certificate_management_parent_package_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_registration_certificate_management_package_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_registration_certificate_management_package_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_registration_certificate_management_package_menu_json`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_registration_certificate_management_role_ids`;
END$$
DELIMITER ;

CALL ensure_registration_certificate_management_menu_hierarchy();
DROP PROCEDURE IF EXISTS ensure_registration_certificate_management_menu_hierarchy;

COMMIT;
