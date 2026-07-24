-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260624_mes_scheduler_workbench_permission_split,20260617_mes_scheduler_role_smart_scheduling_tab; type=data; riskLevel=medium
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_mes_smart_scheduling_role_scope;

DELIMITER //
CREATE PROCEDURE ensure_mes_smart_scheduling_role_scope()
BEGIN
  DECLARE v_scheduler_role_id BIGINT DEFAULT NULL;
  DECLARE v_workshop_director_role_id BIGINT DEFAULT NULL;
  DECLARE v_team_leader_role_id BIGINT DEFAULT NULL;

  IF (
    SELECT COUNT(*)
    FROM `system_menu`
    WHERE `id` IN (900120, 5590, 5580, 5550, 5262, 900121, 900122, 5540, 900104, 5985, 5551, 5552, 5553, 5532, 5535, 5555, 5969, 900200)
      AND `deleted` = b'0'
      AND `status` = 0
  ) <> 18 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing MES smart scheduling role-scope menu baseline';
  END IF;

  UPDATE `system_role`
  SET `name` = '排产员',
      `code` = 'mes_scheduler',
      `sort` = 237,
      `data_scope` = 2,
      `data_scope_dept_ids` = '',
      `status` = 0,
      `type` = 2,
      `remark` = 'MES 智能排产排产员',
      `deleted` = b'0',
      `updater` = 'codex',
      `update_time` = NOW()
  WHERE `tenant_id` = 1
    AND (`name` = '排产员' OR `code` = 'mes_scheduler');

  INSERT INTO `system_role` (
    `id`, `name`, `code`, `sort`, `data_scope`, `data_scope_dept_ids`, `status`, `type`, `remark`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    (
      SELECT COALESCE(MAX(`existing_role`.`id`), 910236) + 1
      FROM `system_role` AS `existing_role`
    ),
    '排产员', 'mes_scheduler', 237, 2, '', 0, 2, 'MES 智能排产排产员',
    'codex', NOW(), 'codex', NOW(), b'0', 1
  FROM DUAL
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_role`
    WHERE `deleted` = b'0'
      AND `tenant_id` = 1
      AND (`name` = '排产员' OR `code` = 'mes_scheduler')
  );

  SELECT `id`
  INTO v_scheduler_role_id
  FROM `system_role`
  WHERE `deleted` = b'0'
    AND `tenant_id` = 1
    AND `status` = 0
    AND (`name` = '排产员' OR `code` = 'mes_scheduler')
  ORDER BY CASE WHEN `code` = 'mes_scheduler' THEN 0 ELSE 1 END, `id`
  LIMIT 1;

  UPDATE `system_role`
  SET `name` = '车间主任',
      `code` = 'mes_workshop_director',
      `sort` = 238,
      `data_scope` = 2,
      `data_scope_dept_ids` = '',
      `status` = 0,
      `type` = 2,
      `remark` = 'MES 智能排产车间主任',
      `deleted` = b'0',
      `updater` = 'codex',
      `update_time` = NOW()
  WHERE `tenant_id` = 1
    AND (`name` = '车间主任' OR `code` = 'mes_workshop_director');

  INSERT INTO `system_role` (
    `id`, `name`, `code`, `sort`, `data_scope`, `data_scope_dept_ids`, `status`, `type`, `remark`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    (
      SELECT COALESCE(MAX(`existing_role`.`id`), 910237) + 1
      FROM `system_role` AS `existing_role`
    ),
    '车间主任', 'mes_workshop_director', 238, 2, '', 0, 2, 'MES 智能排产车间主任',
    'codex', NOW(), 'codex', NOW(), b'0', 1
  FROM DUAL
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_role`
    WHERE `deleted` = b'0'
      AND `tenant_id` = 1
      AND (`name` = '车间主任' OR `code` = 'mes_workshop_director')
  );

  SELECT `id`
  INTO v_workshop_director_role_id
  FROM `system_role`
  WHERE `deleted` = b'0'
    AND `tenant_id` = 1
    AND `status` = 0
    AND (`name` = '车间主任' OR `code` = 'mes_workshop_director')
  ORDER BY CASE WHEN `code` = 'mes_workshop_director' THEN 0 ELSE 1 END, `id`
  LIMIT 1;

  UPDATE `system_role`
  SET `name` = '班组长',
      `code` = 'mes_team_leader',
      `sort` = 239,
      `data_scope` = 2,
      `data_scope_dept_ids` = '',
      `status` = 0,
      `type` = 2,
      `remark` = 'MES 智能排产报工班组长',
      `deleted` = b'0',
      `updater` = 'codex',
      `update_time` = NOW()
  WHERE `tenant_id` = 1
    AND (`name` = '班组长' OR `code` = 'mes_team_leader');

  INSERT INTO `system_role` (
    `id`, `name`, `code`, `sort`, `data_scope`, `data_scope_dept_ids`, `status`, `type`, `remark`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    (
      SELECT COALESCE(MAX(`existing_role`.`id`), 910238) + 1
      FROM `system_role` AS `existing_role`
    ),
    '班组长', 'mes_team_leader', 239, 2, '', 0, 2, 'MES 智能排产报工班组长',
    'codex', NOW(), 'codex', NOW(), b'0', 1
  FROM DUAL
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_role`
    WHERE `deleted` = b'0'
      AND `tenant_id` = 1
      AND (`name` = '班组长' OR `code` = 'mes_team_leader')
  );

  SELECT `id`
  INTO v_team_leader_role_id
  FROM `system_role`
  WHERE `deleted` = b'0'
    AND `tenant_id` = 1
    AND (`name` = '班组长' OR `code` = 'mes_team_leader')
  ORDER BY CASE WHEN `code` = 'mes_team_leader' THEN 0 ELSE 1 END, `id`
  LIMIT 1;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_role_scope_target_tenants`;
  CREATE TEMPORARY TABLE `tmp_mes_role_scope_target_tenants` (
    `tenant_id` bigint NOT NULL,
    PRIMARY KEY (`tenant_id`)
  );

  INSERT IGNORE INTO `tmp_mes_role_scope_target_tenants` (`tenant_id`)
  VALUES (1);

  INSERT IGNORE INTO `tmp_mes_role_scope_target_tenants` (`tenant_id`)
  SELECT DISTINCT `role`.`tenant_id`
  FROM `system_role` AS `role`
  JOIN `system_tenant` AS `tenant`
    ON `tenant`.`id` = `role`.`tenant_id`
   AND `tenant`.`deleted` = b'0'
  JOIN `system_tenant_package` AS `tenant_package`
    ON `tenant_package`.`id` = `tenant`.`package_id`
   AND `tenant_package`.`deleted` = b'0'
   AND JSON_VALID(`tenant_package`.`menu_ids`)
  WHERE `role`.`deleted` = b'0'
    AND `role`.`tenant_id` <> 1
    AND `role`.`status` = 0
    AND (`role`.`name` = '排产员' OR `role`.`code` = 'mes_scheduler')
    AND JSON_CONTAINS(CAST(`tenant_package`.`menu_ids` AS JSON), CAST('900120' AS JSON), '$');

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_role_scope_targets`;
  CREATE TEMPORARY TABLE `tmp_mes_role_scope_targets` (
    `scope_key` varchar(64) NOT NULL,
    `role_id` bigint NOT NULL,
    `tenant_id` bigint NOT NULL,
    PRIMARY KEY (`scope_key`, `role_id`, `tenant_id`)
  );

  INSERT INTO `tmp_mes_role_scope_targets` (`scope_key`, `role_id`, `tenant_id`)
  SELECT 'scheduler', v_scheduler_role_id, 1
  WHERE v_scheduler_role_id IS NOT NULL;

  INSERT IGNORE INTO `tmp_mes_role_scope_targets` (`scope_key`, `role_id`, `tenant_id`)
  SELECT 'scheduler', `role`.`id`, `role`.`tenant_id`
  FROM `system_role` AS `role`
  JOIN `tmp_mes_role_scope_target_tenants` AS `target_tenant`
    ON `target_tenant`.`tenant_id` = `role`.`tenant_id`
  WHERE `role`.`deleted` = b'0'
    AND `role`.`tenant_id` <> 1
    AND `role`.`status` = 0
    AND (`role`.`name` = '排产员' OR `role`.`code` = 'mes_scheduler');

  INSERT INTO `tmp_mes_role_scope_targets` (`scope_key`, `role_id`, `tenant_id`)
  SELECT 'workshop_director', v_workshop_director_role_id, 1
  WHERE v_workshop_director_role_id IS NOT NULL;

  INSERT IGNORE INTO `tmp_mes_role_scope_targets` (`scope_key`, `role_id`, `tenant_id`)
  SELECT 'workshop_director', `role`.`id`, `role`.`tenant_id`
  FROM `system_role` AS `role`
  JOIN `tmp_mes_role_scope_target_tenants` AS `target_tenant`
    ON `target_tenant`.`tenant_id` = `role`.`tenant_id`
  WHERE `role`.`deleted` = b'0'
    AND `role`.`tenant_id` <> 1
    AND `role`.`status` = 0
    AND (`role`.`name` = '车间主任' OR `role`.`code` = 'mes_workshop_director');

  INSERT INTO `tmp_mes_role_scope_targets` (`scope_key`, `role_id`, `tenant_id`)
  SELECT 'team_leader', v_team_leader_role_id, 1
  WHERE v_team_leader_role_id IS NOT NULL;

  INSERT IGNORE INTO `tmp_mes_role_scope_targets` (`scope_key`, `role_id`, `tenant_id`)
  SELECT 'team_leader', `role`.`id`, `role`.`tenant_id`
  FROM `system_role` AS `role`
  JOIN `tmp_mes_role_scope_target_tenants` AS `target_tenant`
    ON `target_tenant`.`tenant_id` = `role`.`tenant_id`
  WHERE `role`.`deleted` = b'0'
    AND `role`.`tenant_id` <> 1
    AND `role`.`status` = 0
    AND (`role`.`name` = '班组长' OR `role`.`code` = 'mes_team_leader');

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_role_scope_allowed_menu`;
  CREATE TEMPORARY TABLE `tmp_mes_role_scope_allowed_menu` (
    `scope_key` varchar(64) NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`scope_key`, `menu_id`)
  );

  INSERT INTO `tmp_mes_role_scope_allowed_menu` (`scope_key`, `menu_id`)
  SELECT 'scheduler', `menu_id`
  FROM (
    SELECT 5100 AS `menu_id`
    UNION ALL SELECT 5101
    UNION ALL SELECT 5160
    UNION ALL SELECT 5161
    UNION ALL SELECT 5170
    UNION ALL SELECT 5171
    UNION ALL SELECT 5300
    UNION ALL SELECT 5310
    UNION ALL SELECT 5311
    UNION ALL SELECT 5320
    UNION ALL SELECT 5321
    UNION ALL SELECT 5700
    UNION ALL SELECT 5720
    UNION ALL SELECT 5721
    UNION ALL SELECT 5530
    UNION ALL SELECT 5531
    UNION ALL SELECT 900120
    UNION ALL SELECT 5590
    UNION ALL SELECT 900170
    UNION ALL SELECT 900180
    UNION ALL SELECT 900181
    UNION ALL SELECT 900182
    UNION ALL SELECT 5580
    UNION ALL SELECT 5581
    UNION ALL SELECT 5582
    UNION ALL SELECT 5584
    UNION ALL SELECT 5585
    UNION ALL SELECT 5587
    UNION ALL SELECT 5550
    UNION ALL SELECT 5552
    UNION ALL SELECT 5553
    UNION ALL SELECT 5555
    UNION ALL SELECT 5969
    UNION ALL SELECT 5262
    UNION ALL SELECT 900121
    UNION ALL SELECT 900122
    UNION ALL SELECT 5540
    UNION ALL SELECT 5541
    UNION ALL SELECT 5583
    UNION ALL SELECT 5532
    UNION ALL SELECT 5535
    UNION ALL SELECT 900200
    UNION ALL SELECT 5985
  ) AS `scheduler_menu_ids`;

  INSERT INTO `tmp_mes_role_scope_allowed_menu` (`scope_key`, `menu_id`)
  SELECT 'workshop_director', `menu_id`
  FROM (
    SELECT 5100 AS `menu_id`
    UNION ALL SELECT 5101
    UNION ALL SELECT 5160
    UNION ALL SELECT 5161
    UNION ALL SELECT 5170
    UNION ALL SELECT 5171
    UNION ALL SELECT 5300
    UNION ALL SELECT 5310
    UNION ALL SELECT 5311
    UNION ALL SELECT 5320
    UNION ALL SELECT 5321
    UNION ALL SELECT 5700
    UNION ALL SELECT 5720
    UNION ALL SELECT 5721
    UNION ALL SELECT 5530
    UNION ALL SELECT 5531
    UNION ALL SELECT 900120
    UNION ALL SELECT 5580
    UNION ALL SELECT 5581
    UNION ALL SELECT 5550
    UNION ALL SELECT 900121
  ) AS `workshop_director_menu_ids`;

  INSERT INTO `tmp_mes_role_scope_allowed_menu` (`scope_key`, `menu_id`)
  SELECT 'team_leader', `menu_id`
  FROM (
    SELECT 900120 AS `menu_id`
    UNION ALL SELECT 5530
    UNION ALL SELECT 5531
    UNION ALL SELECT 5720
    UNION ALL SELECT 5721
    UNION ALL SELECT 5550
    UNION ALL SELECT 5551
    UNION ALL SELECT 5552
    UNION ALL SELECT 5553
  ) AS `team_leader_menu_ids`;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_role_scope_effective_allowed_menu`;
  CREATE TEMPORARY TABLE `tmp_mes_role_scope_effective_allowed_menu` (
    `scope_key` varchar(64) NOT NULL,
    `role_id` bigint NOT NULL,
    `tenant_id` bigint NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`scope_key`, `role_id`, `tenant_id`, `menu_id`)
  );

  INSERT INTO `tmp_mes_role_scope_effective_allowed_menu` (`scope_key`, `role_id`, `tenant_id`, `menu_id`)
  SELECT
    `target_role`.`scope_key`,
    `target_role`.`role_id`,
    `target_role`.`tenant_id`,
    `allowed_menu`.`menu_id`
  FROM `tmp_mes_role_scope_targets` AS `target_role`
  JOIN `tmp_mes_role_scope_allowed_menu` AS `allowed_menu`
    ON `allowed_menu`.`scope_key` = `target_role`.`scope_key`
  WHERE `target_role`.`tenant_id` = 1;

  INSERT INTO `tmp_mes_role_scope_effective_allowed_menu` (`scope_key`, `role_id`, `tenant_id`, `menu_id`)
  SELECT
    `target_role`.`scope_key`,
    `target_role`.`role_id`,
    `target_role`.`tenant_id`,
    `allowed_menu`.`menu_id`
  FROM `tmp_mes_role_scope_targets` AS `target_role`
  JOIN `system_tenant` AS `tenant`
    ON `tenant`.`id` = `target_role`.`tenant_id`
   AND `tenant`.`deleted` = b'0'
  JOIN `system_tenant_package` AS `tenant_package`
    ON `tenant_package`.`id` = `tenant`.`package_id`
   AND `tenant_package`.`deleted` = b'0'
   AND JSON_VALID(`tenant_package`.`menu_ids`)
  JOIN `tmp_mes_role_scope_allowed_menu` AS `allowed_menu`
    ON `allowed_menu`.`scope_key` = `target_role`.`scope_key`
  WHERE `target_role`.`tenant_id` <> 1
    AND JSON_CONTAINS(CAST(`tenant_package`.`menu_ids` AS JSON), CAST(CONCAT('', `allowed_menu`.`menu_id`) AS JSON), '$');

  UPDATE `system_role_menu` AS `role_menu`
  JOIN `tmp_mes_role_scope_targets` AS `target_role`
    ON `target_role`.`role_id` = `role_menu`.`role_id`
   AND `target_role`.`tenant_id` = `role_menu`.`tenant_id`
  JOIN `system_menu` AS `menu`
    ON `menu`.`id` = `role_menu`.`menu_id`
   AND `menu`.`deleted` = b'0'
  LEFT JOIN `tmp_mes_role_scope_effective_allowed_menu` AS `effective_allowed_menu`
    ON `effective_allowed_menu`.`scope_key` = `target_role`.`scope_key`
   AND `effective_allowed_menu`.`role_id` = `target_role`.`role_id`
   AND `effective_allowed_menu`.`tenant_id` = `target_role`.`tenant_id`
   AND `effective_allowed_menu`.`menu_id` = `role_menu`.`menu_id`
  SET `role_menu`.`deleted` = b'1',
      `role_menu`.`updater` = 'mes-smart-role-scope',
      `role_menu`.`update_time` = NOW()
  WHERE `role_menu`.`deleted` = b'0'
    AND (
      `menu`.`id` = 900104
      OR `menu`.`parent_id` = 900120
      OR `menu`.`parent_id` IN (5590, 5580, 5550, 900121)
    )
    AND `effective_allowed_menu`.`menu_id` IS NULL;

  UPDATE `system_role_menu` AS `role_menu`
  JOIN `tmp_mes_role_scope_targets` AS `target_role`
    ON `target_role`.`role_id` = `role_menu`.`role_id`
   AND `target_role`.`tenant_id` = `role_menu`.`tenant_id`
  JOIN `tmp_mes_role_scope_effective_allowed_menu` AS `effective_allowed_menu`
    ON `effective_allowed_menu`.`scope_key` = `target_role`.`scope_key`
   AND `effective_allowed_menu`.`role_id` = `target_role`.`role_id`
   AND `effective_allowed_menu`.`tenant_id` = `target_role`.`tenant_id`
   AND `effective_allowed_menu`.`menu_id` = `role_menu`.`menu_id`
  SET `role_menu`.`deleted` = b'0',
      `role_menu`.`updater` = 'mes-smart-role-scope',
      `role_menu`.`update_time` = NOW()
  WHERE `role_menu`.`deleted` = b'1';

  INSERT INTO `system_role_menu` (
    `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    `target_role`.`role_id`,
    `effective_allowed_menu`.`menu_id`,
    'mes-smart-role-scope',
    NOW(),
    'mes-smart-role-scope',
    NOW(),
    b'0',
    `target_role`.`tenant_id`
  FROM `tmp_mes_role_scope_targets` AS `target_role`
  JOIN `tmp_mes_role_scope_effective_allowed_menu` AS `effective_allowed_menu`
    ON `effective_allowed_menu`.`scope_key` = `target_role`.`scope_key`
   AND `effective_allowed_menu`.`role_id` = `target_role`.`role_id`
   AND `effective_allowed_menu`.`tenant_id` = `target_role`.`tenant_id`
  JOIN `system_menu` AS `menu`
    ON `menu`.`id` = `effective_allowed_menu`.`menu_id`
   AND `menu`.`deleted` = b'0'
   AND `menu`.`status` = 0
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` AS `existing`
    WHERE `existing`.`role_id` = `target_role`.`role_id`
      AND `existing`.`menu_id` = `effective_allowed_menu`.`menu_id`
      AND `existing`.`tenant_id` = `target_role`.`tenant_id`
      AND `existing`.`deleted` = b'0'
  );

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_role_scope_effective_allowed_menu`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_role_scope_targets`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_role_scope_target_tenants`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_role_scope_allowed_menu`;
END//
DELIMITER ;

CALL ensure_mes_smart_scheduling_role_scope();

DROP PROCEDURE IF EXISTS ensure_mes_smart_scheduling_role_scope;
