-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=menu; riskLevel=medium
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_system_codex_test_record_menu;

DELIMITER //
CREATE PROCEDURE ensure_system_codex_test_record_menu()
BEGIN
  DECLARE v_record_menu_id BIGINT DEFAULT NULL;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 1
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing system management root menu';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 901090
      AND (
        `deleted` <> b'0'
        OR `path` <> 'codex-test-record'
        OR `component` <> 'system/codex-test-record/index'
      )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Conflicting system_menu id exists for codex test record menu';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `id` <> 901090
      AND (`path` = 'codex-test-record' OR `component` = 'system/codex-test-record/index')
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Conflicting codex test record menu path or component exists';
  END IF;

  INSERT INTO `system_menu` (
    `id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`,
    `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
  )
  SELECT
    901090, '测试记录', 'system:codex-test:query', 2, 101, 1, 'codex-test-record',
    'ep:document', 'system/codex-test-record/index', 'SystemCodexTestRecord',
    0, b'1', b'1', b'1', 'codex', NOW(), 'codex', NOW(), b'0'
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND (`id` = 901090 OR `path` = 'codex-test-record' OR `component` = 'system/codex-test-record/index')
  );

  SELECT `id` INTO v_record_menu_id
  FROM `system_menu`
  WHERE `path` = 'codex-test-record'
    AND `component` = 'system/codex-test-record/index'
    AND `deleted` = b'0'
  ORDER BY `id`
  LIMIT 1;

  IF v_record_menu_id IS NULL THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing codex test record menu';
  END IF;

  UPDATE `system_menu`
  SET `name` = '测试记录',
      `permission` = 'system:codex-test:query',
      `type` = 2,
      `sort` = 101,
      `parent_id` = 1,
      `path` = 'codex-test-record',
      `icon` = 'ep:document',
      `component` = 'system/codex-test-record/index',
      `component_name` = 'SystemCodexTestRecord',
      `status` = 0,
      `visible` = b'1',
      `keep_alive` = b'1',
      `always_show` = b'1',
      `updater` = 'codex',
      `update_time` = NOW()
  WHERE `id` = v_record_menu_id;

  UPDATE `system_menu`
  SET `sort` = 102,
      `updater` = 'codex',
      `update_time` = NOW()
  WHERE `permission` = 'system:backup-plan:query'
    AND `deleted` = b'0';

  DROP TEMPORARY TABLE IF EXISTS `tmp_system_codex_test_record_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_system_codex_test_record_menu_ids` (
    `menu_id` BIGINT NOT NULL PRIMARY KEY
  ) ENGINE=Memory;

  INSERT INTO `tmp_system_codex_test_record_menu_ids` (`menu_id`)
  SELECT `id`
  FROM `system_menu`
  WHERE `id` = v_record_menu_id
    AND `path` = 'codex-test-record'
    AND `component` = 'system/codex-test-record/index'
    AND `component_name` = 'SystemCodexTestRecord'
    AND `sort` = 101
    AND `status` = 0
    AND `deleted` = b'0';

  IF (SELECT COUNT(*) FROM `tmp_system_codex_test_record_menu_ids`) <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing or duplicated codex test record menu';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_tenant_package` AS `package`
    WHERE `package`.`deleted` = b'0'
      AND NOT JSON_VALID(`package`.`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot merge codex test record menu';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_system_codex_test_record_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_system_codex_test_record_package_menu_ids` (
    `package_id` BIGINT NOT NULL,
    `menu_id` BIGINT NOT NULL,
    PRIMARY KEY (`package_id`, `menu_id`)
  ) ENGINE=Memory;

  INSERT IGNORE INTO `tmp_system_codex_test_record_package_menu_ids` (`package_id`, `menu_id`)
  SELECT
    `package`.`id`,
    CAST(`existing_menu`.`menu_id` AS UNSIGNED)
  FROM `system_tenant_package` AS `package`
  JOIN JSON_TABLE(
    `package`.`menu_ids`,
    '$[*]' COLUMNS (`menu_id` BIGINT PATH '$')
  ) AS `existing_menu`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`);

  INSERT IGNORE INTO `tmp_system_codex_test_record_package_menu_ids` (`package_id`, `menu_id`)
  SELECT
    `package`.`id`,
    `record_menu`.`menu_id`
  FROM `system_tenant_package` AS `package`
  CROSS JOIN `tmp_system_codex_test_record_menu_ids` AS `record_menu`
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
    FROM `tmp_system_codex_test_record_package_menu_ids`
  ) AS `merged`
    ON `merged`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = `merged`.`menu_ids`,
      `package`.`updater` = 'codex',
      `package`.`update_time` = NOW();

  INSERT INTO `system_role_menu` (
    `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    `role`.`id`,
    `record_menu`.`menu_id`,
    'codex',
    NOW(),
    'codex',
    NOW(),
    b'0',
    `role`.`tenant_id`
  FROM `system_role` AS `role`
  CROSS JOIN `tmp_system_codex_test_record_menu_ids` AS `record_menu`
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
      OR `role`.`code` = 'codex_test_admin'
      OR (
        `role`.`code` = 'tenant_admin'
        AND `package`.`id` IS NOT NULL
        AND JSON_CONTAINS(`package`.`menu_ids`, CAST(`record_menu`.`menu_id` AS JSON), '$')
      )
    )
    AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` AS `existing`
      WHERE `existing`.`role_id` = `role`.`id`
        AND `existing`.`menu_id` = `record_menu`.`menu_id`
        AND `existing`.`deleted` = b'0'
    );

  DROP TEMPORARY TABLE IF EXISTS `tmp_system_codex_test_record_package_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_system_codex_test_record_menu_ids`;
END//
DELIMITER ;

CALL ensure_system_codex_test_record_menu();

DROP PROCEDURE IF EXISTS ensure_system_codex_test_record_menu;
