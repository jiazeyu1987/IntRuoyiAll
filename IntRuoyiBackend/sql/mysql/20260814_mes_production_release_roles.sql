-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260611_mes_edhr_work_task_flow,20260618_mes_edhr_release_precheck_engine,20260707_system_role_category_management; type=permission; riskLevel=medium
-- MIG-RF-0: exact production-release roles, permissions, and initial members for the unique tenant containing both users.

SET NAMES utf8mb4;
START TRANSACTION;

DROP PROCEDURE IF EXISTS ensure_mes_production_release_roles_20260814;
DELIMITER //
CREATE PROCEDURE ensure_mes_production_release_roles_20260814()
BEGIN
  DECLARE v_target_tenant_id BIGINT DEFAULT NULL;
  DECLARE v_work_task_parent_menu_id BIGINT DEFAULT NULL;
  DECLARE v_role_category_id BIGINT DEFAULT NULL;
  DECLARE v_inserted_rows INT DEFAULT 0;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_production_release_target_tenant`;
  CREATE TEMPORARY TABLE `tmp_mes_production_release_target_tenant` (
    `tenant_id` bigint NOT NULL PRIMARY KEY
  ) ENGINE=Memory;

  INSERT INTO `tmp_mes_production_release_target_tenant` (`tenant_id`)
  SELECT `user`.`tenant_id`
    FROM `system_users` AS `user`
    JOIN `system_tenant` AS `tenant`
      ON `tenant`.`id` = `user`.`tenant_id`
     AND `tenant`.`deleted` = b'0'
   WHERE `user`.`username` IN ('zhulijiang', 'xujianhai')
     AND `user`.`status` = 0
     AND `user`.`deleted` = b'0'
   GROUP BY `user`.`tenant_id`
  HAVING COUNT(DISTINCT `user`.`username`) = 2;

  IF (SELECT COUNT(*) FROM `tmp_mes_production_release_target_tenant`) <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Ambiguous production release target tenant: expected one enabled tenant containing both initial users';
  END IF;

  SELECT `tenant_id`
    INTO v_target_tenant_id
    FROM `tmp_mes_production_release_target_tenant`;

  IF (
    SELECT COUNT(*)
      FROM `system_users` AS `user`
     WHERE `user`.`tenant_id` = v_target_tenant_id
       AND `user`.`username` IN ('zhulijiang', 'xujianhai')
       AND `user`.`status` = 0
       AND `user`.`deleted` = b'0'
  ) <> 2 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Duplicate or disabled initial production release user';
  END IF;

  IF (
    SELECT COUNT(*)
      FROM `system_menu` AS `parent`
     WHERE `parent`.`permission` = 'mes:pro-edhr-work-task:query'
       AND `parent`.`type` = 2
       AND `parent`.`status` = 0
       AND `parent`.`deleted` = b'0'
  ) <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing or ambiguous enabled eDHR work-task page menu';
  END IF;

  SELECT `parent`.`id`
    INTO v_work_task_parent_menu_id
    FROM `system_menu` AS `parent`
   WHERE `parent`.`permission` = 'mes:pro-edhr-work-task:query'
     AND `parent`.`type` = 2
     AND `parent`.`status` = 0
     AND `parent`.`deleted` = b'0';

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_production_release_pqc_button`;
  CREATE TEMPORARY TABLE `tmp_mes_production_release_pqc_button` (
    `permission` varchar(128) NOT NULL PRIMARY KEY,
    `name` varchar(50) NOT NULL,
    `sort` int NOT NULL
  ) ENGINE=Memory;

  INSERT INTO `tmp_mes_production_release_pqc_button` (`permission`, `name`, `sort`)
  VALUES
    ('mes:pro-production-release:query', '生产放行查询', 10),
    ('mes:pro-production-release:pqc-approve', 'PQC生产放行通过', 20),
    ('mes:pro-production-release:pqc-reject', 'PQC生产放行驳回', 30);

  IF EXISTS (
    SELECT 1
      FROM `system_menu` AS `menu`
      JOIN `tmp_mes_production_release_pqc_button` AS `button`
        ON `button`.`permission` = `menu`.`permission`
     GROUP BY `menu`.`permission`
    HAVING COUNT(*) > 1
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Duplicate production release PQC permission menu';
  END IF;

  UPDATE `system_menu` AS `menu`
    JOIN `tmp_mes_production_release_pqc_button` AS `button`
      ON `button`.`permission` = `menu`.`permission`
     SET `menu`.`name` = `button`.`name`,
         `menu`.`type` = 3,
         `menu`.`sort` = `button`.`sort`,
         `menu`.`parent_id` = v_work_task_parent_menu_id,
         `menu`.`path` = '',
         `menu`.`icon` = '',
         `menu`.`component` = '',
         `menu`.`component_name` = '',
         `menu`.`status` = 0,
         `menu`.`visible` = b'1',
         `menu`.`keep_alive` = b'1',
         `menu`.`always_show` = b'1',
         `menu`.`deleted` = b'0',
         `menu`.`updater` = 'mes-production-release-roles',
         `menu`.`update_time` = NOW();

  INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`
  )
  SELECT
    `button`.`name`, `button`.`permission`, 3, `button`.`sort`, v_work_task_parent_menu_id,
    '', '', '', '', 0, b'1', b'1', b'1',
    'mes-production-release-roles', NOW(), 'mes-production-release-roles', NOW(), b'0'
    FROM `tmp_mes_production_release_pqc_button` AS `button`
   WHERE NOT EXISTS (
     SELECT 1
       FROM `system_menu` AS `existing`
      WHERE `existing`.`permission` = `button`.`permission`
   );

  IF EXISTS (
    SELECT 1
      FROM `tmp_mes_production_release_pqc_button` AS `button`
     WHERE NOT EXISTS (
       SELECT 1
         FROM `system_menu` AS `menu`
        WHERE `menu`.`permission` = `button`.`permission`
          AND `menu`.`parent_id` = v_work_task_parent_menu_id
          AND `menu`.`type` = 3
          AND `menu`.`status` = 0
          AND `menu`.`deleted` = b'0'
     )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing production release role permission menu';
  END IF;

  IF (
    SELECT COUNT(*)
      FROM `system_role_category` AS `category`
     WHERE `category`.`tenant_id` = v_target_tenant_id
       AND `category`.`code` = 'menu'
       AND `category`.`status` = 0
       AND `category`.`deleted` = b'0'
  ) <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing or ambiguous menu role category for production release target tenant';
  END IF;

  SELECT `category`.`id`
    INTO v_role_category_id
    FROM `system_role_category` AS `category`
   WHERE `category`.`tenant_id` = v_target_tenant_id
     AND `category`.`code` = 'menu'
     AND `category`.`status` = 0
     AND `category`.`deleted` = b'0';

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_production_release_desired_role`;
  CREATE TEMPORARY TABLE `tmp_mes_production_release_desired_role` (
    `role_code` varchar(100) NOT NULL PRIMARY KEY,
    `role_name` varchar(30) NOT NULL,
    `role_sort` int NOT NULL,
    `initial_username` varchar(30) NOT NULL
  ) ENGINE=Memory;

  INSERT INTO `tmp_mes_production_release_desired_role`
    (`role_code`, `role_name`, `role_sort`, `initial_username`)
  VALUES
    ('MES_PQC_RELEASE_OWNER', 'PQC负责人', 840, 'zhulijiang'),
    ('MES_MANAGEMENT_REPRESENTATIVE', '管理者代表', 841, 'xujianhai');

  IF EXISTS (
    SELECT 1
      FROM `system_role` AS `role`
      JOIN `tmp_mes_production_release_desired_role` AS `desired`
        ON `desired`.`role_code` = `role`.`code`
     WHERE `role`.`tenant_id` = v_target_tenant_id
     GROUP BY `role`.`code`
    HAVING COUNT(*) > 1
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Duplicate production release role code in target tenant';
  END IF;

  UPDATE `system_role` AS `role`
    JOIN `tmp_mes_production_release_desired_role` AS `desired`
      ON `desired`.`role_code` = `role`.`code`
     SET `role`.`name` = `desired`.`role_name`,
         `role`.`sort` = `desired`.`role_sort`,
         `role`.`category_id` = v_role_category_id,
         `role`.`data_scope` = 1,
         `role`.`data_scope_dept_ids` = '',
         `role`.`status` = 0,
         `role`.`type` = 2,
         `role`.`remark` = '生产放行冻结角色，权限由MIG-RF-0精确维护',
         `role`.`deleted` = b'0',
         `role`.`updater` = 'mes-production-release-roles',
         `role`.`update_time` = NOW()
   WHERE `role`.`tenant_id` = v_target_tenant_id;

  INSERT INTO `system_role` (
    `name`, `code`, `sort`, `category_id`, `data_scope`, `data_scope_dept_ids`,
    `status`, `type`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    `desired`.`role_name`, `desired`.`role_code`, `desired`.`role_sort`, v_role_category_id, 1, '',
    0, 2, '生产放行冻结角色，权限由MIG-RF-0精确维护',
    'mes-production-release-roles', NOW(), 'mes-production-release-roles', NOW(), b'0', v_target_tenant_id
    FROM `tmp_mes_production_release_desired_role` AS `desired`
   WHERE NOT EXISTS (
     SELECT 1
       FROM `system_role` AS `existing`
      WHERE `existing`.`tenant_id` = v_target_tenant_id
        AND `existing`.`code` = `desired`.`role_code`
   );

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_production_release_role`;
  CREATE TEMPORARY TABLE `tmp_mes_production_release_role` (
    `role_id` bigint NOT NULL PRIMARY KEY,
    `role_code` varchar(100) NOT NULL UNIQUE
  ) ENGINE=Memory;

  INSERT INTO `tmp_mes_production_release_role` (`role_id`, `role_code`)
  SELECT `role`.`id`, `role`.`code`
    FROM `system_role` AS `role`
    JOIN `tmp_mes_production_release_desired_role` AS `desired`
      ON `desired`.`role_code` = `role`.`code`
   WHERE `role`.`tenant_id` = v_target_tenant_id
     AND `role`.`status` = 0
     AND `role`.`deleted` = b'0';

  IF (SELECT COUNT(*) FROM `tmp_mes_production_release_role`) <> 2 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Production release role creation or recovery failed';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_production_release_required_permission`;
  CREATE TEMPORARY TABLE `tmp_mes_production_release_required_permission` (
    `role_code` varchar(100) NOT NULL,
    `permission` varchar(128) NOT NULL,
    PRIMARY KEY (`role_code`, `permission`)
  ) ENGINE=Memory;

  INSERT INTO `tmp_mes_production_release_required_permission` (`role_code`, `permission`)
  VALUES
    ('MES_PQC_RELEASE_OWNER', 'mes:pro-edhr-work-task:query'),
    ('MES_PQC_RELEASE_OWNER', 'mes:pro-production-release:query'),
    ('MES_PQC_RELEASE_OWNER', 'mes:pro-production-release:pqc-approve'),
    ('MES_PQC_RELEASE_OWNER', 'mes:pro-production-release:pqc-reject'),
    ('MES_MANAGEMENT_REPRESENTATIVE', 'mes:pro-edhr-work-task:query'),
    ('MES_MANAGEMENT_REPRESENTATIVE', 'mes:pro-edhr-release:query'),
    ('MES_MANAGEMENT_REPRESENTATIVE', 'mes:pro-edhr-release:approve');

  IF EXISTS (
    SELECT 1
      FROM `tmp_mes_production_release_required_permission` AS `required`
     WHERE NOT EXISTS (
       SELECT 1
         FROM `system_menu` AS `menu`
        WHERE `menu`.`permission` = `required`.`permission`
          AND `menu`.`status` = 0
          AND `menu`.`deleted` = b'0'
     )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing production release role permission menu';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_production_release_required_role_menu`;
  CREATE TEMPORARY TABLE `tmp_mes_production_release_required_role_menu` (
    `role_code` varchar(100) NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`role_code`, `menu_id`)
  ) ENGINE=Memory;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_production_release_required_role_menu_source`;
  CREATE TEMPORARY TABLE `tmp_mes_production_release_required_role_menu_source` (
    `role_code` varchar(100) NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`role_code`, `menu_id`)
  ) ENGINE=Memory;

  INSERT INTO `tmp_mes_production_release_required_role_menu` (`role_code`, `menu_id`)
  SELECT `required`.`role_code`, `menu`.`id`
    FROM `tmp_mes_production_release_required_permission` AS `required`
    JOIN `system_menu` AS `menu`
      ON `menu`.`permission` = `required`.`permission`
     AND `menu`.`status` = 0
     AND `menu`.`deleted` = b'0';

  REPEAT
    TRUNCATE TABLE `tmp_mes_production_release_required_role_menu_source`;
    INSERT INTO `tmp_mes_production_release_required_role_menu_source` (`role_code`, `menu_id`)
    SELECT `required_menu`.`role_code`, `required_menu`.`menu_id`
      FROM `tmp_mes_production_release_required_role_menu` AS `required_menu`;

    INSERT IGNORE INTO `tmp_mes_production_release_required_role_menu` (`role_code`, `menu_id`)
    SELECT `required_menu`.`role_code`, `parent`.`id`
      FROM `tmp_mes_production_release_required_role_menu_source` AS `required_menu`
      JOIN `system_menu` AS `child`
        ON `child`.`id` = `required_menu`.`menu_id`
       AND `child`.`deleted` = b'0'
      JOIN `system_menu` AS `parent`
        ON `parent`.`id` = `child`.`parent_id`
       AND `parent`.`status` = 0
       AND `parent`.`deleted` = b'0'
     WHERE COALESCE(`parent`.`permission`, '') = '';
    SET v_inserted_rows = ROW_COUNT();
  UNTIL v_inserted_rows = 0 END REPEAT;

  UPDATE `system_role_menu` AS `role_menu`
    JOIN `tmp_mes_production_release_role` AS `role`
      ON `role`.`role_id` = `role_menu`.`role_id`
     SET `role_menu`.`deleted` = b'1',
         `role_menu`.`updater` = 'mes-production-release-roles',
         `role_menu`.`update_time` = NOW()
   WHERE `role_menu`.`deleted` = b'0';

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_production_release_role_menu_keep`;
  CREATE TEMPORARY TABLE `tmp_mes_production_release_role_menu_keep` AS
  SELECT MIN(`role_menu`.`id`) AS `keep_id`
    FROM `tmp_mes_production_release_role` AS `role`
    JOIN `tmp_mes_production_release_required_role_menu` AS `required_menu`
      ON `required_menu`.`role_code` = `role`.`role_code`
    JOIN `system_role_menu` AS `role_menu`
      ON `role_menu`.`role_id` = `role`.`role_id`
     AND `role_menu`.`menu_id` = `required_menu`.`menu_id`
   GROUP BY `role`.`role_id`, `required_menu`.`menu_id`;

  UPDATE `system_role_menu` AS `role_menu`
    JOIN `tmp_mes_production_release_role_menu_keep` AS `keep`
      ON `keep`.`keep_id` = `role_menu`.`id`
     SET `role_menu`.`deleted` = b'0',
         `role_menu`.`tenant_id` = v_target_tenant_id,
         `role_menu`.`updater` = 'mes-production-release-roles',
         `role_menu`.`update_time` = NOW();

  INSERT INTO `system_role_menu` (
    `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    `role`.`role_id`, `required_menu`.`menu_id`,
    'mes-production-release-roles', NOW(), 'mes-production-release-roles', NOW(), b'0', v_target_tenant_id
    FROM `tmp_mes_production_release_role` AS `role`
    JOIN `tmp_mes_production_release_required_role_menu` AS `required_menu`
      ON `required_menu`.`role_code` = `role`.`role_code`
   WHERE NOT EXISTS (
     SELECT 1
       FROM `system_role_menu` AS `existing`
      WHERE `existing`.`role_id` = `role`.`role_id`
        AND `existing`.`menu_id` = `required_menu`.`menu_id`
        AND `existing`.`deleted` = b'0'
   );

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_production_release_initial_binding`;
  CREATE TEMPORARY TABLE `tmp_mes_production_release_initial_binding` AS
  SELECT `user`.`id` AS `user_id`, `role`.`role_id`, `desired`.`role_code`
    FROM `tmp_mes_production_release_desired_role` AS `desired`
    JOIN `tmp_mes_production_release_role` AS `role`
      ON `role`.`role_code` = `desired`.`role_code`
    JOIN `system_users` AS `user`
      ON `user`.`tenant_id` = v_target_tenant_id
     AND `user`.`username` = `desired`.`initial_username`
     AND `user`.`status` = 0
     AND `user`.`deleted` = b'0';

  IF (SELECT COUNT(*) FROM `tmp_mes_production_release_initial_binding`) <> 2 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Production release initial user resolution failed';
  END IF;

  UPDATE `system_user_role` AS `user_role`
    JOIN `tmp_mes_production_release_initial_binding` AS `binding`
      ON `binding`.`user_id` = `user_role`.`user_id`
     AND `binding`.`role_id` = `user_role`.`role_id`
     SET `user_role`.`deleted` = b'1',
         `user_role`.`updater` = 'mes-production-release-roles',
         `user_role`.`update_time` = NOW()
   WHERE `user_role`.`deleted` = b'0';

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_production_release_user_role_keep`;
  CREATE TEMPORARY TABLE `tmp_mes_production_release_user_role_keep` AS
  SELECT MIN(`user_role`.`id`) AS `keep_id`
    FROM `tmp_mes_production_release_initial_binding` AS `binding`
    JOIN `system_user_role` AS `user_role`
      ON `user_role`.`user_id` = `binding`.`user_id`
     AND `user_role`.`role_id` = `binding`.`role_id`
   GROUP BY `binding`.`user_id`, `binding`.`role_id`;

  UPDATE `system_user_role` AS `user_role`
    JOIN `tmp_mes_production_release_user_role_keep` AS `keep`
      ON `keep`.`keep_id` = `user_role`.`id`
     SET `user_role`.`deleted` = b'0',
         `user_role`.`tenant_id` = v_target_tenant_id,
         `user_role`.`updater` = 'mes-production-release-roles',
         `user_role`.`update_time` = NOW();

  INSERT INTO `system_user_role` (
    `user_id`, `role_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    `binding`.`user_id`, `binding`.`role_id`,
    'mes-production-release-roles', NOW(), 'mes-production-release-roles', NOW(), b'0', v_target_tenant_id
    FROM `tmp_mes_production_release_initial_binding` AS `binding`
   WHERE NOT EXISTS (
     SELECT 1
       FROM `system_user_role` AS `existing`
      WHERE `existing`.`user_id` = `binding`.`user_id`
        AND `existing`.`role_id` = `binding`.`role_id`
        AND `existing`.`deleted` = b'0'
   );

  IF EXISTS (
    SELECT 1
      FROM `tmp_mes_production_release_role` AS `role`
      JOIN `tmp_mes_production_release_required_permission` AS `required`
        ON `required`.`role_code` = `role`.`role_code`
     WHERE NOT EXISTS (
       SELECT 1
         FROM `system_role_menu` AS `role_menu`
         JOIN `system_menu` AS `menu`
           ON `menu`.`id` = `role_menu`.`menu_id`
          AND `menu`.`permission` = `required`.`permission`
          AND `menu`.`status` = 0
          AND `menu`.`deleted` = b'0'
        WHERE `role_menu`.`role_id` = `role`.`role_id`
          AND `role_menu`.`deleted` = b'0'
     )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Production release role permission set mismatch';
  END IF;

  IF EXISTS (
     SELECT 1
       FROM `tmp_mes_production_release_role` AS `role`
      JOIN `system_role_menu` AS `role_menu`
        ON `role_menu`.`role_id` = `role`.`role_id`
       AND `role_menu`.`deleted` = b'0'
      JOIN `system_menu` AS `menu`
        ON `menu`.`id` = `role_menu`.`menu_id`
       AND `menu`.`deleted` = b'0'
     WHERE COALESCE(`menu`.`permission`, '') <> ''
       AND NOT EXISTS (
         SELECT 1
           FROM `tmp_mes_production_release_required_permission` AS `required`
          WHERE `required`.`role_code` = `role`.`role_code`
            AND `required`.`permission` = `menu`.`permission`
       )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Production release role permission set mismatch';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM `tmp_mes_production_release_initial_binding` AS `binding`
     WHERE (
       SELECT COUNT(*)
         FROM `system_user_role` AS `user_role`
        WHERE `user_role`.`user_id` = `binding`.`user_id`
          AND `user_role`.`role_id` = `binding`.`role_id`
          AND `user_role`.`deleted` = b'0'
     ) <> 1
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Production release initial user role binding incomplete';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_production_release_user_role_keep`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_production_release_initial_binding`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_production_release_role_menu_keep`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_production_release_required_role_menu_source`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_production_release_required_role_menu`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_production_release_required_permission`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_production_release_role`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_production_release_desired_role`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_production_release_pqc_button`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_production_release_target_tenant`;
END//
DELIMITER ;

CALL ensure_mes_production_release_roles_20260814();

DROP PROCEDURE IF EXISTS ensure_mes_production_release_roles_20260814;

COMMIT;
