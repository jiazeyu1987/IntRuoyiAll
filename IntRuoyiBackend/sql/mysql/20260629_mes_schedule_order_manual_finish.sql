-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260624_mes_schedule_order_freeze_audit,20260629_mes_smart_scheduling_role_scope; type=schema; riskLevel=medium
-- MES 排产工单人工完成与撤销人工完成：新增人工完成字段与权限绑定。

SET NAMES utf8mb4;

SET @schema_name = DATABASE();

SET @required_table_count = (
  SELECT COUNT(1) FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME IN (
      'mes_pro_schedule_order',
      'system_menu',
      'system_role',
      'system_role_menu',
      'system_tenant',
      'system_tenant_package'
    )
);
SET @required_table_sql = IF(
  @required_table_count = 6,
  'SELECT 1',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''schedule order manual finish migration missing required base tables'''
);
PREPARE required_table_stmt FROM @required_table_sql;
EXECUTE required_table_stmt;
DEALLOCATE PREPARE required_table_stmt;

SET @schedule_order_parent_menu_count = (
  SELECT COUNT(1) FROM `system_menu`
  WHERE `id` = 5580
    AND `permission` = 'mes:pro-schedule-order:query'
    AND `deleted` = b'0'
);
SET @schedule_order_parent_menu_sql = IF(
  @schedule_order_parent_menu_count = 1,
  'SELECT 1',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''schedule order manual finish migration missing schedule order parent menu 5580'''
);
PREPARE schedule_order_parent_menu_stmt FROM @schedule_order_parent_menu_sql;
EXECUTE schedule_order_parent_menu_stmt;
DEALLOCATE PREPARE schedule_order_parent_menu_stmt;

SET @manual_finished_exists = (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'mes_pro_schedule_order'
    AND COLUMN_NAME = 'manual_finished'
);
SET @manual_finished_sql = IF(
  @manual_finished_exists = 0,
  'ALTER TABLE `mes_pro_schedule_order` ADD COLUMN `manual_finished` bit(1) NOT NULL DEFAULT b''0'' COMMENT ''是否人工完成'' AFTER `freeze_reason`',
  'SELECT 1'
);
PREPARE manual_finished_stmt FROM @manual_finished_sql;
EXECUTE manual_finished_stmt;
DEALLOCATE PREPARE manual_finished_stmt;

SET @manual_finished_time_exists = (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'mes_pro_schedule_order'
    AND COLUMN_NAME = 'manual_finished_time'
);
SET @manual_finished_time_sql = IF(
  @manual_finished_time_exists = 0,
  'ALTER TABLE `mes_pro_schedule_order` ADD COLUMN `manual_finished_time` datetime NULL COMMENT ''人工完成时间'' AFTER `manual_finished`',
  'SELECT 1'
);
PREPARE manual_finished_time_stmt FROM @manual_finished_time_sql;
EXECUTE manual_finished_time_stmt;
DEALLOCATE PREPARE manual_finished_time_stmt;

SET @manual_finished_by_exists = (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'mes_pro_schedule_order'
    AND COLUMN_NAME = 'manual_finished_by'
);
SET @manual_finished_by_sql = IF(
  @manual_finished_by_exists = 0,
  'ALTER TABLE `mes_pro_schedule_order` ADD COLUMN `manual_finished_by` bigint NULL COMMENT ''人工完成人'' AFTER `manual_finished_time`',
  'SELECT 1'
);
PREPARE manual_finished_by_stmt FROM @manual_finished_by_sql;
EXECUTE manual_finished_by_stmt;
DEALLOCATE PREPARE manual_finished_by_stmt;

SET @manual_finished_reason_exists = (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'mes_pro_schedule_order'
    AND COLUMN_NAME = 'manual_finished_reason'
);
SET @manual_finished_reason_sql = IF(
  @manual_finished_reason_exists = 0,
  'ALTER TABLE `mes_pro_schedule_order` ADD COLUMN `manual_finished_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT ''人工完成原因'' AFTER `manual_finished_by`',
  'SELECT 1'
);
PREPARE manual_finished_reason_stmt FROM @manual_finished_reason_sql;
EXECUTE manual_finished_reason_stmt;
DEALLOCATE PREPARE manual_finished_reason_stmt;

SET @existing_manual_finish_menu_id = (
  SELECT MIN(`id`)
  FROM `system_menu`
  WHERE `permission` = 'mes:pro-schedule-order:manual-finish'
);
SET @preferred_manual_finish_menu_id_blocked = (
  SELECT COUNT(1)
  FROM `system_menu`
  WHERE `id` = 5587
    AND `permission` <> 'mes:pro-schedule-order:manual-finish'
);
SET @manual_finish_menu_id = IF(
  @existing_manual_finish_menu_id IS NOT NULL,
  @existing_manual_finish_menu_id,
  IF(@preferred_manual_finish_menu_id_blocked = 0, 5587, (SELECT COALESCE(MAX(`id`), 0) + 1 FROM `system_menu`))
);

SET @existing_revoke_menu_id = (
  SELECT MIN(`id`)
  FROM `system_menu`
  WHERE `permission` = 'mes:pro-schedule-order:revoke-complete'
);
SET @preferred_revoke_menu_id_blocked = (
  SELECT COUNT(1)
  FROM `system_menu`
  WHERE `id` = 5588
    AND `permission` <> 'mes:pro-schedule-order:revoke-complete'
);
SET @revoke_menu_id = IF(
  @existing_revoke_menu_id IS NOT NULL,
  @existing_revoke_menu_id,
  IF(@preferred_revoke_menu_id_blocked = 0, 5588, (SELECT COALESCE(MAX(`id`), 0) + 1 FROM `system_menu`))
);

INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT @manual_finish_menu_id, '排产工单人工完成', 'mes:pro-schedule-order:manual-finish', 3, 7, 5580, '', '', '', '', 0, b'1', b'1', b'1', 'schedule-order-manual-finish', NOW(), 'schedule-order-manual-finish', NOW(), b'0'
WHERE @existing_manual_finish_menu_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-schedule-order:manual-finish' AND `deleted` = b'0');

INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT @revoke_menu_id, '排产工单撤销人工完成', 'mes:pro-schedule-order:revoke-complete', 3, 8, 5580, '', '', '', '', 0, b'1', b'1', b'1', 'schedule-order-manual-finish', NOW(), 'schedule-order-manual-finish', NOW(), b'0'
WHERE @existing_revoke_menu_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-schedule-order:revoke-complete' AND `deleted` = b'0');

UPDATE `system_menu`
SET `name` = '排产工单人工完成',
    `permission` = 'mes:pro-schedule-order:manual-finish',
    `type` = 3,
    `sort` = 7,
    `parent_id` = 5580,
    `path` = '',
    `icon` = '',
    `component` = '',
    `component_name` = '',
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `deleted` = b'0',
    `updater` = 'schedule-order-manual-finish',
    `update_time` = NOW()
WHERE `id` = @manual_finish_menu_id
  AND `permission` = 'mes:pro-schedule-order:manual-finish';

UPDATE `system_menu`
SET `name` = '排产工单撤销人工完成',
    `permission` = 'mes:pro-schedule-order:revoke-complete',
    `type` = 3,
    `sort` = 8,
    `parent_id` = 5580,
    `path` = '',
    `icon` = '',
    `component` = '',
    `component_name` = '',
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `deleted` = b'0',
    `updater` = 'schedule-order-manual-finish',
    `update_time` = NOW()
WHERE `id` = @revoke_menu_id
  AND `permission` = 'mes:pro-schedule-order:revoke-complete';

DROP TEMPORARY TABLE IF EXISTS `tmp_mes_schedule_order_manual_finish_menu`;
CREATE TEMPORARY TABLE `tmp_mes_schedule_order_manual_finish_menu` AS
SELECT `id`
FROM `system_menu`
WHERE `permission` = 'mes:pro-schedule-order:manual-finish'
  AND `deleted` = b'0';

DROP TEMPORARY TABLE IF EXISTS `tmp_mes_schedule_order_revoke_complete_menu`;
CREATE TEMPORARY TABLE `tmp_mes_schedule_order_revoke_complete_menu` AS
SELECT `id`
FROM `system_menu`
WHERE `permission` = 'mes:pro-schedule-order:revoke-complete'
  AND `deleted` = b'0';

DROP TEMPORARY TABLE IF EXISTS `tmp_mes_schedule_order_target_packages`;
CREATE TEMPORARY TABLE `tmp_mes_schedule_order_target_packages` AS
SELECT DISTINCT `package`.`id`
FROM `system_tenant_package` AS `package`
JOIN JSON_TABLE(CAST(`package`.`menu_ids` AS JSON), '$[*]' COLUMNS (`menu_id` bigint PATH '$')) AS `menu_json`
WHERE `package`.`deleted` = b'0'
  AND JSON_VALID(`package`.`menu_ids`)
  AND `menu_json`.`menu_id` = 5580;

INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT `role`.`id`, `menu`.`id`, 'schedule-order-manual-finish', NOW(), 'schedule-order-manual-finish', NOW(), b'0', `role`.`tenant_id`
FROM `system_role` AS `role`
JOIN `system_tenant` AS `tenant` ON `tenant`.`id` = `role`.`tenant_id` AND `tenant`.`deleted` = b'0'
JOIN `tmp_mes_schedule_order_target_packages` AS `target_package` ON `target_package`.`id` = `tenant`.`package_id`
CROSS JOIN `tmp_mes_schedule_order_manual_finish_menu` AS `menu`
WHERE `role`.`deleted` = b'0'
  AND (`role`.`name` = '排产员' OR `role`.`code` = 'mes_scheduler')
  AND NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` AS `existing`
    WHERE `existing`.`role_id` = `role`.`id`
      AND `existing`.`menu_id` = `menu`.`id`
      AND `existing`.`tenant_id` = `role`.`tenant_id`
      AND `existing`.`deleted` = b'0'
  );

UPDATE `system_role_menu` AS `role_menu`
JOIN `system_role` AS `role`
  ON `role`.`id` = `role_menu`.`role_id`
 AND `role`.`tenant_id` = `role_menu`.`tenant_id`
JOIN `system_tenant` AS `tenant`
  ON `tenant`.`id` = `role`.`tenant_id`
 AND `tenant`.`deleted` = b'0'
JOIN `tmp_mes_schedule_order_target_packages` AS `target_package`
  ON `target_package`.`id` = `tenant`.`package_id`
JOIN `tmp_mes_schedule_order_manual_finish_menu` AS `menu`
  ON `menu`.`id` = `role_menu`.`menu_id`
SET `role_menu`.`deleted` = b'0',
    `role_menu`.`updater` = 'schedule-order-manual-finish',
    `role_menu`.`update_time` = NOW()
WHERE `role_menu`.`deleted` = b'1'
  AND (`role`.`name` = '排产员' OR `role`.`code` = 'mes_scheduler');

INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT `role`.`id`, `menu`.`id`, 'schedule-order-manual-finish', NOW(), 'schedule-order-manual-finish', NOW(), b'0', `role`.`tenant_id`
FROM `system_role` AS `role`
JOIN `system_tenant` AS `tenant` ON `tenant`.`id` = `role`.`tenant_id` AND `tenant`.`deleted` = b'0'
JOIN `tmp_mes_schedule_order_target_packages` AS `target_package` ON `target_package`.`id` = `tenant`.`package_id`
CROSS JOIN `tmp_mes_schedule_order_revoke_complete_menu` AS `menu`
WHERE `role`.`deleted` = b'0'
  AND `role`.`code` IN ('tenant_admin', 'super_admin')
  AND NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` AS `existing`
    WHERE `existing`.`role_id` = `role`.`id`
      AND `existing`.`menu_id` = `menu`.`id`
      AND `existing`.`tenant_id` = `role`.`tenant_id`
      AND `existing`.`deleted` = b'0'
  );

UPDATE `system_role_menu` AS `role_menu`
JOIN `system_role` AS `role`
  ON `role`.`id` = `role_menu`.`role_id`
 AND `role`.`tenant_id` = `role_menu`.`tenant_id`
JOIN `system_tenant` AS `tenant`
  ON `tenant`.`id` = `role`.`tenant_id`
 AND `tenant`.`deleted` = b'0'
JOIN `tmp_mes_schedule_order_target_packages` AS `target_package`
  ON `target_package`.`id` = `tenant`.`package_id`
JOIN `tmp_mes_schedule_order_revoke_complete_menu` AS `menu`
  ON `menu`.`id` = `role_menu`.`menu_id`
SET `role_menu`.`deleted` = b'0',
    `role_menu`.`updater` = 'schedule-order-manual-finish',
    `role_menu`.`update_time` = NOW()
WHERE `role_menu`.`deleted` = b'1'
  AND `role`.`code` IN ('tenant_admin', 'super_admin');

UPDATE `system_tenant_package` AS `package`
JOIN `tmp_mes_schedule_order_target_packages` AS `target_package` ON `target_package`.`id` = `package`.`id`
CROSS JOIN `tmp_mes_schedule_order_manual_finish_menu` AS `menu`
SET `package`.`menu_ids` = CAST(JSON_ARRAY_APPEND(CAST(`package`.`menu_ids` AS JSON), '$', `menu`.`id`) AS CHAR),
    `package`.`updater` = 'schedule-order-manual-finish',
    `package`.`update_time` = NOW()
WHERE JSON_VALID(`package`.`menu_ids`)
  AND NOT JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST(CONCAT('', `menu`.`id`) AS JSON), '$');

UPDATE `system_tenant_package` AS `package`
JOIN `tmp_mes_schedule_order_target_packages` AS `target_package` ON `target_package`.`id` = `package`.`id`
CROSS JOIN `tmp_mes_schedule_order_revoke_complete_menu` AS `menu`
SET `package`.`menu_ids` = CAST(JSON_ARRAY_APPEND(CAST(`package`.`menu_ids` AS JSON), '$', `menu`.`id`) AS CHAR),
    `package`.`updater` = 'schedule-order-manual-finish',
    `package`.`update_time` = NOW()
WHERE JSON_VALID(`package`.`menu_ids`)
  AND NOT JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST(CONCAT('', `menu`.`id`) AS JSON), '$');

DROP TEMPORARY TABLE IF EXISTS `tmp_mes_schedule_order_manual_finish_menu`;
DROP TEMPORARY TABLE IF EXISTS `tmp_mes_schedule_order_revoke_complete_menu`;
DROP TEMPORARY TABLE IF EXISTS `tmp_mes_schedule_order_target_packages`;
