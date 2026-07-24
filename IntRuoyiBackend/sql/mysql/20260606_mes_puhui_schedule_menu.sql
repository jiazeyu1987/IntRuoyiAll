-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=menu; riskLevel=low
-- MES 璞慧排产菜单迁移。
-- 仅新增前端菜单入口；排产数据仍由浏览器 localStorage 持有。

DROP PROCEDURE IF EXISTS ensure_mes_puhui_schedule_menu;
DELIMITER $$
CREATE PROCEDURE ensure_mes_puhui_schedule_menu()
BEGIN
  DECLARE v_puhui_parent_menu_id BIGINT DEFAULT NULL;
  DECLARE v_puhui_permission VARCHAR(255) DEFAULT '';
  DECLARE v_puhui_path VARCHAR(255) DEFAULT 'puhui-schedule';

  IF NOT EXISTS (
      SELECT 1
      FROM `system_menu`
      WHERE `id` = 5700
        AND `deleted` = b'0'
        AND `name` = '生产管理'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing MES production management menu 5700; cannot add Puhui schedule menu';
  END IF;

  SELECT `parent_id`
    INTO v_puhui_parent_menu_id
  FROM `system_menu`
  WHERE `id` = 5540
    AND `deleted` = b'0'
    AND `name` = '生产排产'
  LIMIT 1;

  IF v_puhui_parent_menu_id IS NULL THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing MES production schedule menu 5540; cannot add Puhui schedule menu';
  END IF;

  IF NOT EXISTS (
      SELECT 1
      FROM `system_menu`
      WHERE `id` = v_puhui_parent_menu_id
        AND `id` IN (5700, 900120)
        AND `deleted` = b'0'
        AND `name` IN ('生产管理', '智能排产')
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'MES production schedule menu 5540 has unsupported parent; cannot add Puhui schedule menu';
  END IF;

  IF v_puhui_parent_menu_id = 900120 THEN
    SET v_puhui_permission = 'mes:pro-puhui-schedule:query';
    SET v_puhui_path = '/mes/pro/puhui-schedule';
  END IF;

  IF EXISTS (
      SELECT 1
      FROM `system_menu`
      WHERE `id` = 900104
        AND `deleted` = b'0'
        AND NOT (
          `parent_id` = v_puhui_parent_menu_id
          AND `path` IN ('puhui-schedule', '/mes/pro/puhui-schedule')
          AND `component` = 'mes/pro/puhui-schedule/index'
        )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'system_menu id 900104 is already used by another menu';
  END IF;

  IF EXISTS (
      SELECT 1
      FROM `system_menu`
      WHERE `parent_id` = v_puhui_parent_menu_id
        AND `path` IN ('puhui-schedule', '/mes/pro/puhui-schedule')
        AND `deleted` = b'0'
        AND `id` <> 900104
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Puhui schedule menu path already exists with a different id';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900104) THEN
    -- deterministic sibling shift: sort >= 5
    UPDATE `system_menu`
    SET `sort` = `sort` + 1,
        `updater` = 'mes-puhui-schedule-menu',
        `update_time` = NOW()
    WHERE `parent_id` = v_puhui_parent_menu_id
      AND `deleted` = b'0'
      AND `sort` >= 5;

    INSERT INTO `system_menu`
      (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
    VALUES
      (900104, '璞慧排产', v_puhui_permission, 2, 5, v_puhui_parent_menu_id, v_puhui_path, 'ep:calendar', 'mes/pro/puhui-schedule/index', 'MesProPuhuiSchedule', 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0');
  ELSE
    UPDATE `system_menu`
    SET `name` = '璞慧排产',
        `permission` = v_puhui_permission,
        `type` = 2,
        `sort` = 5,
        `parent_id` = v_puhui_parent_menu_id,
        `path` = v_puhui_path,
        `icon` = 'ep:calendar',
        `component` = 'mes/pro/puhui-schedule/index',
        `component_name` = 'MesProPuhuiSchedule',
        `status` = 0,
        `visible` = b'1',
        `keep_alive` = b'1',
        `always_show` = b'1',
        `updater` = 'mes-puhui-schedule-menu',
        `update_time` = NOW(),
        `deleted` = b'0'
    WHERE `id` = 900104;
  END IF;

  IF EXISTS (
      SELECT 1
      FROM `system_tenant_package` AS `package`
      WHERE `package`.`deleted` = b'0'
        AND NOT JSON_VALID(`package`.`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot merge Puhui schedule menu';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_puhui_schedule_target_packages`;
  CREATE TEMPORARY TABLE `tmp_mes_puhui_schedule_target_packages` AS
  SELECT DISTINCT `package`.`id` AS `package_id`
  FROM `system_tenant_package` AS `package`
  INNER JOIN JSON_TABLE(
      `package`.`menu_ids`,
      '$[*]' COLUMNS (`menu_id` BIGINT PATH '$')
  ) AS `existing_menu`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`)
    AND `existing_menu`.`menu_id` IN (5700, 900120, 5540);

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_puhui_schedule_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_puhui_schedule_package_menu_ids` (
      `package_id` BIGINT NOT NULL,
      `menu_id` BIGINT NOT NULL,
      PRIMARY KEY (`package_id`, `menu_id`)
  );

  INSERT IGNORE INTO `tmp_mes_puhui_schedule_package_menu_ids` (`package_id`, `menu_id`)
  SELECT `target_package`.`package_id`, `existing_menu`.`menu_id`
  FROM `tmp_mes_puhui_schedule_target_packages` AS `target_package`
  INNER JOIN `system_tenant_package` AS `package`
      ON `package`.`id` = `target_package`.`package_id`
  INNER JOIN JSON_TABLE(
      `package`.`menu_ids`,
      '$[*]' COLUMNS (`menu_id` BIGINT PATH '$')
  ) AS `existing_menu`;

  INSERT IGNORE INTO `tmp_mes_puhui_schedule_package_menu_ids` (`package_id`, `menu_id`)
  SELECT `package_id`, 900104
  FROM `tmp_mes_puhui_schedule_target_packages`;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_puhui_schedule_package_menu_json`;
  CREATE TEMPORARY TABLE `tmp_mes_puhui_schedule_package_menu_json` AS
  SELECT
      `package_id`,
      JSON_ARRAYAGG(`menu_id`) AS `menu_ids`
  FROM (
      SELECT `package_id`, `menu_id`
      FROM `tmp_mes_puhui_schedule_package_menu_ids`
      ORDER BY `package_id`, `menu_id`
  ) AS `ordered_menu`
  GROUP BY `package_id`;

  UPDATE `system_tenant_package` AS `package`
  INNER JOIN `tmp_mes_puhui_schedule_package_menu_json` AS `merged`
      ON `merged`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = `merged`.`menu_ids`,
      `package`.`updater` = 'mes-puhui-schedule-menu',
      `package`.`update_time` = NOW()
  WHERE `package`.`deleted` = b'0';

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_puhui_schedule_target_roles`;
  CREATE TEMPORARY TABLE `tmp_mes_puhui_schedule_target_roles` AS
  SELECT DISTINCT
      `tenant`.`id` AS `tenant_id`,
      `role`.`id` AS `role_id`
  FROM `system_tenant` AS `tenant`
  INNER JOIN `tmp_mes_puhui_schedule_target_packages` AS `target_package`
      ON `target_package`.`package_id` = `tenant`.`package_id`
  INNER JOIN `system_role` AS `role`
      ON `role`.`tenant_id` = `tenant`.`id`
     AND `role`.`code` = 'tenant_admin'
     AND `role`.`deleted` = b'0'
  WHERE `tenant`.`deleted` = b'0';

  UPDATE `system_role_menu` AS `role_menu`
  INNER JOIN `tmp_mes_puhui_schedule_target_roles` AS `target_role`
      ON `target_role`.`tenant_id` = `role_menu`.`tenant_id`
     AND `target_role`.`role_id` = `role_menu`.`role_id`
     AND `role_menu`.`menu_id` = 900104
  SET `role_menu`.`deleted` = b'0',
      `role_menu`.`updater` = 'mes-puhui-schedule-menu',
      `role_menu`.`update_time` = NOW()
  WHERE `role_menu`.`deleted` = b'1';

  INSERT INTO `system_role_menu`
    (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT
      `target_role`.`role_id`,
      900104,
      'mes-puhui-schedule-menu',
      NOW(),
      'mes-puhui-schedule-menu',
      NOW(),
      b'0',
      `target_role`.`tenant_id`
  FROM `tmp_mes_puhui_schedule_target_roles` AS `target_role`
  WHERE NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` AS `existing`
      WHERE `existing`.`tenant_id` = `target_role`.`tenant_id`
        AND `existing`.`role_id` = `target_role`.`role_id`
        AND `existing`.`menu_id` = 900104
        AND `existing`.`deleted` = b'0'
  );
END$$
DELIMITER ;

CALL ensure_mes_puhui_schedule_menu();
DROP PROCEDURE IF EXISTS ensure_mes_puhui_schedule_menu;
