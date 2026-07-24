-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260610_mes_schedule_order_p1; type=schema; riskLevel=medium
-- MES 排产工单冻结、修改、删除追溯：新增冻结字段、操作日志表和删除权限。

SET @schema_name = DATABASE();

SET @required_table_count = (
  SELECT COUNT(1) FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME IN (
      'mes_pro_schedule_order',
      'system_menu',
      'system_tenant_package',
      'system_role_menu',
      'system_role',
      'system_tenant'
    )
);
SET @required_table_sql = IF(
  @required_table_count = 6,
  'SELECT 1',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''schedule order freeze audit migration missing required base tables'''
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
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''schedule order freeze audit migration missing schedule order parent menu 5580'''
);
PREPARE schedule_order_parent_menu_stmt FROM @schedule_order_parent_menu_sql;
EXECUTE schedule_order_parent_menu_stmt;
DEALLOCATE PREPARE schedule_order_parent_menu_stmt;

SET @schedule_order_frozen_exists = (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mes_pro_schedule_order' AND COLUMN_NAME = 'frozen'
);
SET @schedule_order_frozen_sql = IF(
  @schedule_order_frozen_exists = 0,
  'ALTER TABLE `mes_pro_schedule_order` ADD COLUMN `frozen` bit(1) NOT NULL DEFAULT b''0'' COMMENT ''是否冻结'' AFTER `progress_percent`',
  'SELECT 1'
);
PREPARE schedule_order_frozen_stmt FROM @schedule_order_frozen_sql;
EXECUTE schedule_order_frozen_stmt;
DEALLOCATE PREPARE schedule_order_frozen_stmt;

SET @schedule_order_frozen_time_exists = (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mes_pro_schedule_order' AND COLUMN_NAME = 'frozen_time'
);
SET @schedule_order_frozen_time_sql = IF(
  @schedule_order_frozen_time_exists = 0,
  'ALTER TABLE `mes_pro_schedule_order` ADD COLUMN `frozen_time` datetime NULL COMMENT ''冻结时间'' AFTER `frozen`',
  'SELECT 1'
);
PREPARE schedule_order_frozen_time_stmt FROM @schedule_order_frozen_time_sql;
EXECUTE schedule_order_frozen_time_stmt;
DEALLOCATE PREPARE schedule_order_frozen_time_stmt;

SET @schedule_order_frozen_by_exists = (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mes_pro_schedule_order' AND COLUMN_NAME = 'frozen_by'
);
SET @schedule_order_frozen_by_sql = IF(
  @schedule_order_frozen_by_exists = 0,
  'ALTER TABLE `mes_pro_schedule_order` ADD COLUMN `frozen_by` bigint NULL COMMENT ''冻结人'' AFTER `frozen_time`',
  'SELECT 1'
);
PREPARE schedule_order_frozen_by_stmt FROM @schedule_order_frozen_by_sql;
EXECUTE schedule_order_frozen_by_stmt;
DEALLOCATE PREPARE schedule_order_frozen_by_stmt;

SET @schedule_order_freeze_reason_exists = (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mes_pro_schedule_order' AND COLUMN_NAME = 'freeze_reason'
);
SET @schedule_order_freeze_reason_sql = IF(
  @schedule_order_freeze_reason_exists = 0,
  'ALTER TABLE `mes_pro_schedule_order` ADD COLUMN `freeze_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT ''冻结原因'' AFTER `frozen_by`',
  'SELECT 1'
);
PREPARE schedule_order_freeze_reason_stmt FROM @schedule_order_freeze_reason_sql;
EXECUTE schedule_order_freeze_reason_stmt;
DEALLOCATE PREPARE schedule_order_freeze_reason_stmt;

CREATE TABLE IF NOT EXISTS `mes_pro_schedule_order_operation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `schedule_order_id` bigint NOT NULL COMMENT '排产工单ID',
  `schedule_order_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '排产工单编码快照',
  `operation_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '操作类型',
  `before_snapshot_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '操作前快照',
  `after_snapshot_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '操作后快照',
  `reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '操作原因',
  `operator_id` bigint NULL COMMENT '操作人',
  `operator_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '操作人名称',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_mes_pro_schedule_order_operation_log_order` (`tenant_id` ASC, `schedule_order_id` ASC, `create_time` DESC) USING BTREE,
  INDEX `idx_mes_pro_schedule_order_operation_log_type` (`tenant_id` ASC, `operation_type` ASC, `create_time` DESC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'MES 排产工单操作追溯';

SET @existing_delete_menu_id = (
  SELECT MIN(`id`)
  FROM `system_menu`
  WHERE `permission` = 'mes:pro-schedule-order:delete'
);
SET @preferred_delete_menu_id_blocked = (
  SELECT COUNT(1)
  FROM `system_menu`
  WHERE `id` = 5586
    AND `permission` <> 'mes:pro-schedule-order:delete'
);
SET @delete_menu_id = IF(
  @existing_delete_menu_id IS NOT NULL,
  @existing_delete_menu_id,
  IF(@preferred_delete_menu_id_blocked = 0, 5586, (SELECT COALESCE(MAX(`id`), 0) + 1 FROM `system_menu`))
);

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT @delete_menu_id, '排产工单删除', 'mes:pro-schedule-order:delete', 3, 6, 5580, '', '', '', '', 0, b'1', b'1', b'1', 'schedule-order-freeze-audit', NOW(), 'schedule-order-freeze-audit', NOW(), b'0'
WHERE @existing_delete_menu_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-schedule-order:delete' AND `deleted` = b'0');

UPDATE `system_menu`
SET `name` = '排产工单删除',
    `permission` = 'mes:pro-schedule-order:delete',
    `type` = 3,
    `sort` = 6,
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
    `updater` = 'schedule-order-freeze-audit',
    `update_time` = NOW()
WHERE `id` = @delete_menu_id
  AND `permission` = 'mes:pro-schedule-order:delete';

DROP TEMPORARY TABLE IF EXISTS `tmp_mes_schedule_order_delete_permission_menu`;
CREATE TEMPORARY TABLE `tmp_mes_schedule_order_delete_permission_menu` AS
SELECT `id`
FROM `system_menu`
WHERE `permission` = 'mes:pro-schedule-order:delete'
  AND `deleted` = b'0';

DROP TEMPORARY TABLE IF EXISTS `tmp_mes_schedule_order_delete_permission_packages`;
CREATE TEMPORARY TABLE `tmp_mes_schedule_order_delete_permission_packages` AS
SELECT DISTINCT `package`.`id`
FROM `system_tenant_package` AS `package`
JOIN JSON_TABLE(CAST(`package`.`menu_ids` AS JSON), '$[*]' COLUMNS (`menu_id` bigint PATH '$')) AS `menu_json`
WHERE `package`.`deleted` = b'0'
  AND JSON_VALID(`package`.`menu_ids`)
  AND `menu_json`.`menu_id` = 5580;

INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT `role`.`id`, `menu`.`id`, 'schedule-order-freeze-audit', NOW(), 'schedule-order-freeze-audit', NOW(), b'0', `role`.`tenant_id`
FROM `system_role` AS `role`
JOIN `system_tenant` AS `tenant` ON `tenant`.`id` = `role`.`tenant_id` AND `tenant`.`deleted` = b'0'
JOIN `tmp_mes_schedule_order_delete_permission_packages` AS `target_package` ON `target_package`.`id` = `tenant`.`package_id`
CROSS JOIN `tmp_mes_schedule_order_delete_permission_menu` AS `menu`
WHERE `role`.`deleted` = b'0'
  AND `role`.`code` = 'tenant_admin';

UPDATE `system_tenant_package` AS `package`
JOIN `tmp_mes_schedule_order_delete_permission_packages` AS `target_package` ON `target_package`.`id` = `package`.`id`
CROSS JOIN `tmp_mes_schedule_order_delete_permission_menu` AS `menu`
SET `package`.`menu_ids` = CAST(JSON_ARRAY_APPEND(CAST(`package`.`menu_ids` AS JSON), '$', `menu`.`id`) AS CHAR),
    `package`.`updater` = 'schedule-order-freeze-audit',
    `package`.`update_time` = NOW()
WHERE JSON_VALID(`package`.`menu_ids`)
  AND NOT JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST(CONCAT('', `menu`.`id`) AS JSON), '$');

DROP TEMPORARY TABLE IF EXISTS `tmp_mes_schedule_order_delete_permission_menu`;
DROP TEMPORARY TABLE IF EXISTS `tmp_mes_schedule_order_delete_permission_packages`;
