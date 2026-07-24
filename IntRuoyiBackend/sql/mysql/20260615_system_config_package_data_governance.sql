-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=config; riskLevel=medium
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ROLLBACK GUIDE:
-- 1. Before applying this local governance script, create a table-scoped dump for:
--    system_menu, system_role_menu, system_tenant_package, system_dept, system_post,
--    system_user_post, system_user_role, system_users, system_dict_type.
-- 2. Restore that dump to roll back this local data governance batch.
-- 3. This script is intentionally fail-fast and scoped to local tenant_id=1 / 芋道源码.

DROP PROCEDURE IF EXISTS govern_system_config_package_data;

DELIMITER //
CREATE PROCEDURE govern_system_config_package_data()
BEGIN
  DECLARE v_remaining_count int DEFAULT 0;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_tenant`
    WHERE `id` = 1
      AND `name` = '芋道源码'
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Expected source tenant 1 / 芋道源码 is not active';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_users`
    WHERE `id` = 1
      AND `tenant_id` = 1
      AND `username` = 'admin'
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Expected source user 芋道源码/admin is not active';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_tenant_package`
    WHERE `deleted` = b'0'
      AND NOT JSON_VALID(`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid tenant package menu_ids JSON before governance';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_users`
    WHERE `tenant_id` = 1
      AND `deleted` = b'0'
      AND `post_ids` IS NOT NULL
      AND NOT JSON_VALID(`post_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid active user post_ids JSON before governance';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_role`
    WHERE `tenant_id` = 1
      AND `deleted` = b'0'
      AND `data_scope_dept_ids` IS NOT NULL
      AND `data_scope_dept_ids` <> ''
      AND NOT JSON_VALID(`data_scope_dept_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid active role data_scope_dept_ids JSON before governance';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_config_governance_missing_menu_roots`;
  CREATE TEMPORARY TABLE `tmp_config_governance_missing_menu_roots` (
    `id` bigint NOT NULL PRIMARY KEY,
    `component` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL
  );

  INSERT INTO `tmp_config_governance_missing_menu_roots` (`id`, `component`) VALUES
    (2161, 'pay/demo/index'),
    (6402, 'wms/md/warehouse/index'),
    (6411, 'wms/md/item/brand/index'),
    (6510, 'im/manager/statistics/index'),
    (6515, 'im/manager/friend/request/index'),
    (6520, 'im/manager/friend/index'),
    (6530, 'im/manager/message/private/index'),
    (6540, 'im/manager/group/index'),
    (6545, 'im/manager/group/request/index'),
    (6550, 'im/manager/message/group/index'),
    (6560, 'im/manager/sensitiveword/index'),
    (6571, 'im/manager/face/pack/index'),
    (6580, 'im/manager/face/userItem/index'),
    (980109, 'showroom-frontstage/screen/views/ScreenHomeView'),
    (980110, 'showroom-frontstage/screen/views/ScreenCompanyView'),
    (980111, 'showroom-frontstage/screen/views/ScreenHallView'),
    (980112, 'showroom-frontstage/screen/views/ScreenProductView'),
    (980113, 'showroom-frontstage/screen/views/ScreenSettingsView'),
    (980114, 'showroom-frontstage/screen/views/ScreenNarrationView');

  IF EXISTS (
    SELECT 1
    FROM `system_menu` AS `menu`
    JOIN `tmp_config_governance_missing_menu_roots` AS `root`
      ON `root`.`id` = `menu`.`id`
    WHERE `menu`.`deleted` = b'0'
      AND COALESCE(`menu`.`component`, '') <> `root`.`component`
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Target menu id no longer matches expected missing component path';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_config_governance_retired_menu_tree`;
  CREATE TEMPORARY TABLE `tmp_config_governance_retired_menu_tree` (
    `id` bigint NOT NULL PRIMARY KEY
  );

  INSERT IGNORE INTO `tmp_config_governance_retired_menu_tree` (`id`)
  WITH RECURSIVE `menu_tree` AS (
    SELECT `menu`.`id`
    FROM `system_menu` AS `menu`
    JOIN `tmp_config_governance_missing_menu_roots` AS `root`
      ON `root`.`id` = `menu`.`id`
    WHERE `menu`.`deleted` = b'0'
    UNION DISTINCT
    SELECT `child`.`id`
    FROM `system_menu` AS `child`
    JOIN `menu_tree` AS `parent`
      ON `child`.`parent_id` = `parent`.`id`
    WHERE `child`.`deleted` = b'0'
  )
  SELECT `id`
  FROM `menu_tree`;

  DROP TEMPORARY TABLE IF EXISTS `tmp_config_governance_affected_packages`;
  CREATE TEMPORARY TABLE `tmp_config_governance_affected_packages` (
    `package_id` bigint NOT NULL PRIMARY KEY
  );

  INSERT IGNORE INTO `tmp_config_governance_affected_packages` (`package_id`)
  SELECT DISTINCT `package`.`id`
  FROM `system_tenant_package` AS `package`
  JOIN JSON_TABLE(
    `package`.`menu_ids`,
    '$[*]' COLUMNS (`menu_id` bigint PATH '$')
  ) AS `package_menu`
  JOIN `tmp_config_governance_retired_menu_tree` AS `retired`
    ON `retired`.`id` = `package_menu`.`menu_id`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`);

  DROP TEMPORARY TABLE IF EXISTS `tmp_config_governance_package_menu_json`;
  CREATE TEMPORARY TABLE `tmp_config_governance_package_menu_json` (
    `package_id` bigint NOT NULL,
    `menu_id` bigint NOT NULL
  );

  INSERT INTO `tmp_config_governance_package_menu_json` (`package_id`, `menu_id`)
  SELECT `package`.`id`, `package_menu`.`menu_id`
  FROM `system_tenant_package` AS `package`
  JOIN `tmp_config_governance_affected_packages` AS `affected`
    ON `affected`.`package_id` = `package`.`id`
  JOIN JSON_TABLE(
    `package`.`menu_ids`,
    '$[*]' COLUMNS (`menu_id` bigint PATH '$')
  ) AS `package_menu`
  LEFT JOIN `tmp_config_governance_retired_menu_tree` AS `retired`
    ON `retired`.`id` = `package_menu`.`menu_id`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`)
    AND `retired`.`id` IS NULL;

  UPDATE `system_tenant_package` AS `package`
  JOIN `tmp_config_governance_affected_packages` AS `affected`
    ON `affected`.`package_id` = `package`.`id`
  LEFT JOIN (
    SELECT
      `package_id`,
      JSON_ARRAYAGG(`menu_id`) AS `menu_ids`
    FROM `tmp_config_governance_package_menu_json`
    GROUP BY `package_id`
  ) AS `rebuilt`
    ON `rebuilt`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = COALESCE(CAST(`rebuilt`.`menu_ids` AS char), '[]'),
      `package`.`updater` = 'system-config-governance',
      `package`.`update_time` = NOW();

  UPDATE `system_role_menu` AS `role_menu`
  JOIN `tmp_config_governance_retired_menu_tree` AS `retired`
    ON `retired`.`id` = `role_menu`.`menu_id`
  SET `role_menu`.`deleted` = b'1',
      `role_menu`.`updater` = 'system-config-governance',
      `role_menu`.`update_time` = NOW()
  WHERE `role_menu`.`deleted` = b'0';

  UPDATE `system_menu` AS `menu`
  JOIN `tmp_config_governance_retired_menu_tree` AS `retired`
    ON `retired`.`id` = `menu`.`id`
  SET `menu`.`deleted` = b'1',
      `menu`.`updater` = 'system-config-governance',
      `menu`.`update_time` = NOW()
  WHERE `menu`.`deleted` = b'0';

  UPDATE `system_dept`
  SET `deleted` = b'0',
      `updater` = 'system-config-governance',
      `update_time` = NOW()
  WHERE `tenant_id` = 1
    AND `id` IN (100, 101, 102, 103, 104, 105, 107, 108, 112)
    AND `deleted` = b'1';

  UPDATE `system_dept` AS `dept`
  LEFT JOIN `system_users` AS `leader`
    ON `leader`.`id` = `dept`.`leader_user_id`
   AND `leader`.`tenant_id` = `dept`.`tenant_id`
   AND `leader`.`deleted` = b'0'
  SET `dept`.`leader_user_id` = NULL,
      `dept`.`updater` = 'system-config-governance',
      `dept`.`update_time` = NOW()
  WHERE `dept`.`tenant_id` = 1
    AND `dept`.`deleted` = b'0'
    AND `dept`.`leader_user_id` IS NOT NULL
    AND `leader`.`id` IS NULL;

  UPDATE `system_post`
  SET `deleted` = b'0',
      `updater` = 'system-config-governance',
      `update_time` = NOW()
  WHERE `tenant_id` = 1
    AND `id` = 2
    AND `deleted` = b'1';

  DROP TEMPORARY TABLE IF EXISTS `tmp_config_governance_affected_users`;
  CREATE TEMPORARY TABLE `tmp_config_governance_affected_users` (
    `user_id` bigint NOT NULL PRIMARY KEY
  );

  INSERT IGNORE INTO `tmp_config_governance_affected_users` (`user_id`)
  SELECT DISTINCT `user`.`id`
  FROM `system_users` AS `user`
  JOIN JSON_TABLE(
    `user`.`post_ids`,
    '$[*]' COLUMNS (`post_id` bigint PATH '$')
  ) AS `user_post`
  LEFT JOIN `system_post` AS `post`
    ON `post`.`id` = `user_post`.`post_id`
   AND `post`.`tenant_id` = `user`.`tenant_id`
   AND `post`.`deleted` = b'0'
  WHERE `user`.`tenant_id` = 1
    AND `user`.`deleted` = b'0'
    AND `user`.`post_ids` IS NOT NULL
    AND JSON_VALID(`user`.`post_ids`)
    AND `post`.`id` IS NULL;

  DROP TEMPORARY TABLE IF EXISTS `tmp_config_governance_user_post_json`;
  CREATE TEMPORARY TABLE `tmp_config_governance_user_post_json` (
    `user_id` bigint NOT NULL,
    `post_id` bigint NOT NULL
  );

  INSERT INTO `tmp_config_governance_user_post_json` (`user_id`, `post_id`)
  SELECT `user`.`id`, `user_post`.`post_id`
  FROM `system_users` AS `user`
  JOIN `tmp_config_governance_affected_users` AS `affected`
    ON `affected`.`user_id` = `user`.`id`
  JOIN JSON_TABLE(
    `user`.`post_ids`,
    '$[*]' COLUMNS (`post_id` bigint PATH '$')
  ) AS `user_post`
  JOIN `system_post` AS `post`
    ON `post`.`id` = `user_post`.`post_id`
   AND `post`.`tenant_id` = `user`.`tenant_id`
   AND `post`.`deleted` = b'0'
  WHERE `user`.`tenant_id` = 1
    AND `user`.`deleted` = b'0'
    AND JSON_VALID(`user`.`post_ids`);

  UPDATE `system_users` AS `user`
  JOIN `tmp_config_governance_affected_users` AS `affected`
    ON `affected`.`user_id` = `user`.`id`
  LEFT JOIN (
    SELECT
      `user_id`,
      JSON_ARRAYAGG(`post_id`) AS `post_ids`
    FROM `tmp_config_governance_user_post_json`
    GROUP BY `user_id`
  ) AS `rebuilt`
    ON `rebuilt`.`user_id` = `user`.`id`
  SET `user`.`post_ids` = COALESCE(CAST(`rebuilt`.`post_ids` AS char), '[]'),
      `user`.`updater` = 'system-config-governance',
      `user`.`update_time` = NOW();

  UPDATE `system_user_post` AS `user_post`
  LEFT JOIN `system_users` AS `user`
    ON `user`.`id` = `user_post`.`user_id`
   AND `user`.`tenant_id` = `user_post`.`tenant_id`
   AND `user`.`deleted` = b'0'
  LEFT JOIN `system_post` AS `post`
    ON `post`.`id` = `user_post`.`post_id`
   AND `post`.`tenant_id` = `user_post`.`tenant_id`
   AND `post`.`deleted` = b'0'
  SET `user_post`.`deleted` = b'1',
      `user_post`.`updater` = 'system-config-governance',
      `user_post`.`update_time` = NOW()
  WHERE `user_post`.`tenant_id` = 1
    AND `user_post`.`deleted` = b'0'
    AND (`user`.`id` IS NULL OR `post`.`id` IS NULL OR `user_post`.`post_id` = 1);

  UPDATE `system_user_role` AS `user_role`
  LEFT JOIN `system_users` AS `user`
    ON `user`.`id` = `user_role`.`user_id`
   AND `user`.`tenant_id` = `user_role`.`tenant_id`
   AND `user`.`deleted` = b'0'
  LEFT JOIN `system_role` AS `role`
    ON `role`.`id` = `user_role`.`role_id`
   AND `role`.`tenant_id` = `user_role`.`tenant_id`
   AND `role`.`deleted` = b'0'
  SET `user_role`.`deleted` = b'1',
      `user_role`.`updater` = 'system-config-governance',
      `user_role`.`update_time` = NOW()
  WHERE `user_role`.`tenant_id` = 1
    AND (`user_role`.`deleted` = b'0' OR `user_role`.`deleted` IS NULL)
    AND (`user`.`id` IS NULL OR `role`.`id` IS NULL);

  UPDATE `system_dict_type`
  SET `deleted` = b'0',
      `deleted_time` = NULL,
      `updater` = 'system-config-governance',
      `update_time` = NOW()
  WHERE `type` IN ('system_menu_type', 'system_data_scope', 'mes_wm_issue_status')
    AND `deleted` = b'1';

  INSERT INTO `system_dict_type` (
    `id`, `name`, `type`, `status`, `remark`, `creator`, `create_time`,
    `updater`, `update_time`, `deleted`, `deleted_time`
  )
  SELECT 910401, '菜单类型', 'system_menu_type', 0, '配置包治理恢复：字典数据已存在但类型缺失', 'system-config-governance', NOW(), 'system-config-governance', NOW(), b'0', NULL
  WHERE NOT EXISTS (SELECT 1 FROM `system_dict_type` WHERE `type` = 'system_menu_type');

  INSERT INTO `system_dict_type` (
    `id`, `name`, `type`, `status`, `remark`, `creator`, `create_time`,
    `updater`, `update_time`, `deleted`, `deleted_time`
  )
  SELECT 910402, '数据范围', 'system_data_scope', 0, '配置包治理恢复：字典数据已存在但类型缺失', 'system-config-governance', NOW(), 'system-config-governance', NOW(), b'0', NULL
  WHERE NOT EXISTS (SELECT 1 FROM `system_dict_type` WHERE `type` = 'system_data_scope');

  INSERT INTO `system_dict_type` (
    `id`, `name`, `type`, `status`, `remark`, `creator`, `create_time`,
    `updater`, `update_time`, `deleted`, `deleted_time`
  )
  SELECT 910403, 'MES 领料出库单状态', 'mes_wm_issue_status', 0, '配置包治理恢复：字典数据已存在但类型缺失', 'system-config-governance', NOW(), 'system-config-governance', NOW(), b'0', NULL
  WHERE NOT EXISTS (SELECT 1 FROM `system_dict_type` WHERE `type` = 'mes_wm_issue_status');

  SELECT COUNT(*) INTO v_remaining_count
  FROM `system_role_menu` AS `role_menu`
  JOIN `tmp_config_governance_retired_menu_tree` AS `retired`
    ON `retired`.`id` = `role_menu`.`menu_id`
  WHERE `role_menu`.`deleted` = b'0';
  IF v_remaining_count <> 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Remaining active role-menu retired menu after governance';
  END IF;

  SELECT COUNT(*) INTO v_remaining_count
  FROM `system_menu` AS `menu`
  JOIN `tmp_config_governance_retired_menu_tree` AS `retired`
    ON `retired`.`id` = `menu`.`id`
  WHERE `menu`.`deleted` = b'0';
  IF v_remaining_count <> 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Remaining active retired menu after governance';
  END IF;

  SELECT COUNT(*) INTO v_remaining_count
  FROM `system_tenant_package` AS `package`
  JOIN JSON_TABLE(
    `package`.`menu_ids`,
    '$[*]' COLUMNS (`menu_id` bigint PATH '$')
  ) AS `package_menu`
  JOIN `tmp_config_governance_retired_menu_tree` AS `retired`
    ON `retired`.`id` = `package_menu`.`menu_id`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`);
  IF v_remaining_count <> 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Remaining tenant package retired menu after governance';
  END IF;

  SELECT COUNT(*) INTO v_remaining_count
  FROM `system_users` AS `user`
  LEFT JOIN `system_dept` AS `dept`
    ON `dept`.`id` = `user`.`dept_id`
   AND `dept`.`tenant_id` = `user`.`tenant_id`
   AND `dept`.`deleted` = b'0'
  WHERE `user`.`tenant_id` = 1
    AND `user`.`deleted` = b'0'
    AND `user`.`dept_id` IS NOT NULL
    AND `dept`.`id` IS NULL;
  IF v_remaining_count <> 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Remaining active user missing dept after governance';
  END IF;

  SELECT COUNT(*) INTO v_remaining_count
  FROM `system_dept` AS `dept`
  LEFT JOIN `system_users` AS `leader`
    ON `leader`.`id` = `dept`.`leader_user_id`
   AND `leader`.`tenant_id` = `dept`.`tenant_id`
   AND `leader`.`deleted` = b'0'
  WHERE `dept`.`tenant_id` = 1
    AND `dept`.`deleted` = b'0'
    AND `dept`.`leader_user_id` IS NOT NULL
    AND `leader`.`id` IS NULL;
  IF v_remaining_count <> 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Remaining active dept missing leader after governance';
  END IF;

  SELECT COUNT(*) INTO v_remaining_count
  FROM `system_user_post` AS `user_post`
  LEFT JOIN `system_users` AS `user`
    ON `user`.`id` = `user_post`.`user_id`
   AND `user`.`tenant_id` = `user_post`.`tenant_id`
   AND `user`.`deleted` = b'0'
  LEFT JOIN `system_post` AS `post`
    ON `post`.`id` = `user_post`.`post_id`
   AND `post`.`tenant_id` = `user_post`.`tenant_id`
   AND `post`.`deleted` = b'0'
  WHERE `user_post`.`tenant_id` = 1
    AND `user_post`.`deleted` = b'0'
    AND (`user`.`id` IS NULL OR `post`.`id` IS NULL);
  IF v_remaining_count <> 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Remaining orphan user post after governance';
  END IF;

  SELECT COUNT(*) INTO v_remaining_count
  FROM `system_user_role` AS `user_role`
  LEFT JOIN `system_users` AS `user`
    ON `user`.`id` = `user_role`.`user_id`
   AND `user`.`tenant_id` = `user_role`.`tenant_id`
   AND `user`.`deleted` = b'0'
  LEFT JOIN `system_role` AS `role`
    ON `role`.`id` = `user_role`.`role_id`
   AND `role`.`tenant_id` = `user_role`.`tenant_id`
   AND `role`.`deleted` = b'0'
  WHERE `user_role`.`tenant_id` = 1
    AND (`user_role`.`deleted` = b'0' OR `user_role`.`deleted` IS NULL)
    AND (`user`.`id` IS NULL OR `role`.`id` IS NULL);
  IF v_remaining_count <> 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Remaining orphan user role after governance';
  END IF;

  SELECT COUNT(*) INTO v_remaining_count
  FROM `system_users` AS `user`
  JOIN JSON_TABLE(
    `user`.`post_ids`,
    '$[*]' COLUMNS (`post_id` bigint PATH '$')
  ) AS `user_post`
  LEFT JOIN `system_post` AS `post`
    ON `post`.`id` = `user_post`.`post_id`
   AND `post`.`tenant_id` = `user`.`tenant_id`
   AND `post`.`deleted` = b'0'
  WHERE `user`.`tenant_id` = 1
    AND `user`.`deleted` = b'0'
    AND `user`.`post_ids` IS NOT NULL
    AND JSON_VALID(`user`.`post_ids`)
    AND `post`.`id` IS NULL;
  IF v_remaining_count <> 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Remaining active user missing post_ids post after governance';
  END IF;

  SELECT COUNT(*) INTO v_remaining_count
  FROM `system_role` AS `role`
  JOIN JSON_TABLE(
    `role`.`data_scope_dept_ids`,
    '$[*]' COLUMNS (`dept_id` bigint PATH '$')
  ) AS `scope_dept`
  LEFT JOIN `system_dept` AS `dept`
    ON `dept`.`id` = `scope_dept`.`dept_id`
   AND `dept`.`tenant_id` = `role`.`tenant_id`
   AND `dept`.`deleted` = b'0'
  WHERE `role`.`tenant_id` = 1
    AND `role`.`deleted` = b'0'
    AND `role`.`data_scope_dept_ids` IS NOT NULL
    AND `role`.`data_scope_dept_ids` <> ''
    AND JSON_VALID(`role`.`data_scope_dept_ids`)
    AND `dept`.`id` IS NULL;
  IF v_remaining_count <> 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Remaining invalid role data scope dept after governance';
  END IF;

  SELECT COUNT(*) INTO v_remaining_count
  FROM (
    SELECT 'system_menu_type' AS `type`
    UNION ALL SELECT 'system_data_scope'
    UNION ALL SELECT 'mes_wm_issue_status'
  ) AS `expected`
  LEFT JOIN `system_dict_type` AS `dict_type`
    ON `dict_type`.`type` = `expected`.`type`
   AND `dict_type`.`deleted` = b'0'
  WHERE `dict_type`.`id` IS NULL;
  IF v_remaining_count <> 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing dict type remains after governance';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_config_governance_user_post_json`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_config_governance_affected_users`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_config_governance_package_menu_json`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_config_governance_affected_packages`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_config_governance_retired_menu_tree`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_config_governance_missing_menu_roots`;
END//
DELIMITER ;

CALL govern_system_config_package_data();

DROP PROCEDURE IF EXISTS govern_system_config_package_data;
