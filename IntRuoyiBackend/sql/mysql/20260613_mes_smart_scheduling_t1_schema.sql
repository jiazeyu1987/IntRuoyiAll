-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- MES 智能排产 T1：路线版本、排产配置、排产风险/进度、按天计划实际差异 schema。
-- 数据安全：本迁移只新增字段和表，不写历史默认迁移值；历史路线版本无法唯一归属时由后续数据审计阻塞处理。

SET @schema_name := DATABASE();

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `mes_pro_schedule_order` ADD COLUMN `route_status` tinyint NULL COMMENT ''路线状态：0有路线 1无路线'' AFTER `risk_status`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mes_pro_schedule_order' AND COLUMN_NAME = 'route_status'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `mes_pro_schedule_order` ADD COLUMN `auto_schedulable` bit(1) NULL COMMENT ''是否可自动排产'' AFTER `route_status`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mes_pro_schedule_order' AND COLUMN_NAME = 'auto_schedulable'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `mes_pro_schedule_order` ADD COLUMN `route_version_id` bigint NULL COMMENT ''路线版本ID'' AFTER `route_id`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mes_pro_schedule_order' AND COLUMN_NAME = 'route_version_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `mes_pro_schedule_order` ADD COLUMN `latest_start_time` datetime NULL COMMENT ''最晚开工时间'' AFTER `schedule_config_version`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mes_pro_schedule_order' AND COLUMN_NAME = 'latest_start_time'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `mes_pro_schedule_order` ADD COLUMN `planned_start_time` datetime NULL COMMENT ''计划开工时间'' AFTER `latest_start_time`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mes_pro_schedule_order' AND COLUMN_NAME = 'planned_start_time'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `mes_pro_schedule_order` ADD COLUMN `planned_end_time` datetime NULL COMMENT ''计划完成时间'' AFTER `planned_start_time`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mes_pro_schedule_order' AND COLUMN_NAME = 'planned_end_time'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `mes_pro_schedule_order` ADD COLUMN `start_risk_flag` bit(1) NULL COMMENT ''开工风险标记'' AFTER `planned_end_time`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mes_pro_schedule_order' AND COLUMN_NAME = 'start_risk_flag'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `mes_pro_schedule_order` ADD COLUMN `delay_risk_flag` bit(1) NULL COMMENT ''延期风险标记'' AFTER `start_risk_flag`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mes_pro_schedule_order' AND COLUMN_NAME = 'delay_risk_flag'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `mes_pro_schedule_order` ADD COLUMN `total_quantity` decimal(18,6) NULL COMMENT ''总数量'' AFTER `delay_risk_flag`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mes_pro_schedule_order' AND COLUMN_NAME = 'total_quantity'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `mes_pro_schedule_order` ADD COLUMN `completed_quantity` decimal(18,6) NULL COMMENT ''完成数量'' AFTER `total_quantity`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mes_pro_schedule_order' AND COLUMN_NAME = 'completed_quantity'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `mes_pro_schedule_order` ADD COLUMN `uncompleted_quantity` decimal(18,6) NULL COMMENT ''未完成数量'' AFTER `completed_quantity`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mes_pro_schedule_order' AND COLUMN_NAME = 'uncompleted_quantity'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `mes_pro_schedule_order` ADD COLUMN `progress_percent` decimal(10,6) NULL COMMENT ''进度百分比'' AFTER `uncompleted_quantity`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mes_pro_schedule_order' AND COLUMN_NAME = 'progress_percent'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `mes_pro_schedule_order_process` ADD COLUMN `route_version_id` bigint NULL COMMENT ''路线版本ID'' AFTER `route_process_id`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mes_pro_schedule_order_process' AND COLUMN_NAME = 'route_version_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `mes_pro_schedule_order_process` ADD COLUMN `route_schedule_config_id` bigint NULL COMMENT ''路线排产配置ID'' AFTER `route_version_id`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mes_pro_schedule_order_process' AND COLUMN_NAME = 'route_schedule_config_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `mes_pro_schedule_order_process` ADD COLUMN `capacity_mode` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT ''产能模式'' AFTER `capacity_source`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mes_pro_schedule_order_process' AND COLUMN_NAME = 'capacity_mode'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `mes_pro_schedule_order_process` ADD COLUMN `infinite_duration_quantity_factor` decimal(18,6) NULL COMMENT ''无限产能数量系数'' AFTER `hourly_capacity_total`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mes_pro_schedule_order_process' AND COLUMN_NAME = 'infinite_duration_quantity_factor'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `mes_pro_schedule_order_process` ADD COLUMN `infinite_duration_base_minutes` decimal(18,6) NULL COMMENT ''无限产能基础分钟'' AFTER `infinite_duration_quantity_factor`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mes_pro_schedule_order_process' AND COLUMN_NAME = 'infinite_duration_base_minutes'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `mes_pro_schedule_order_process` ADD COLUMN `progress_percent` decimal(10,6) NULL COMMENT ''进度百分比'' AFTER `remaining_quantity`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mes_pro_schedule_order_process' AND COLUMN_NAME = 'progress_percent'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `mes_pro_schedule_order_process` ADD COLUMN `night_shift_enabled` bit(1) NULL COMMENT ''是否启用夜班'' AFTER `progress_percent`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mes_pro_schedule_order_process' AND COLUMN_NAME = 'night_shift_enabled'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `mes_pro_schedule_order_process` ADD COLUMN `calendar_rule_id` bigint NULL COMMENT ''日历规则ID'' AFTER `night_shift_enabled`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mes_pro_schedule_order_process' AND COLUMN_NAME = 'calendar_rule_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `mes_pro_schedule_order_process` ADD COLUMN `key_process_flag` bit(1) NOT NULL DEFAULT b''0'' COMMENT ''是否关键工序'' AFTER `calendar_rule_id`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mes_pro_schedule_order_process' AND COLUMN_NAME = 'key_process_flag'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `mes_pro_schedule_order_process` ADD COLUMN `plan_date` date NULL COMMENT ''计划日期'' AFTER `key_process_flag`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mes_pro_schedule_order_process' AND COLUMN_NAME = 'plan_date'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `mes_pro_schedule_order_process` ADD COLUMN `actual_start_time` datetime NULL COMMENT ''实际开始时间'' AFTER `planned_end_time`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mes_pro_schedule_order_process' AND COLUMN_NAME = 'actual_start_time'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `mes_pro_schedule_order_process` ADD COLUMN `actual_end_time` datetime NULL COMMENT ''实际结束时间'' AFTER `actual_start_time`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mes_pro_schedule_order_process' AND COLUMN_NAME = 'actual_end_time'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `mes_pro_route_version` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `route_id` bigint NOT NULL COMMENT '路线ID',
  `version_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '版本号',
  `active` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否当前版本',
  `source_route_version_id` bigint NULL DEFAULT NULL COMMENT '来源路线版本ID',
  `route_snapshot_json` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '路线版本快照',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_mes_pro_route_version_no` (`tenant_id` ASC, `route_id` ASC, `version_no` ASC, `deleted` ASC) USING BTREE,
  INDEX `idx_mes_pro_route_version_active` (`tenant_id` ASC, `route_id` ASC, `active` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'MES 工艺路线版本';

CREATE TABLE IF NOT EXISTS `mes_pro_route_schedule_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `route_version_id` bigint NOT NULL COMMENT '路线版本ID',
  `item_id` bigint NULL DEFAULT NULL COMMENT '历史产品物料ID',
  `route_process_id` bigint NOT NULL COMMENT '路线工序ID',
  `capacity_mode` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '产能模式',
  `hourly_capacity` decimal(18,6) NULL DEFAULT NULL COMMENT '每小时产能',
  `infinite_duration_quantity_factor` decimal(18,6) NULL DEFAULT NULL COMMENT '无限产能数量系数',
  `infinite_duration_base_minutes` decimal(18,6) NULL DEFAULT NULL COMMENT '无限产能基础分钟',
  `night_shift_enabled` bit(1) NULL DEFAULT b'0' COMMENT '是否启用夜班',
  `calendar_rule_id` bigint NULL DEFAULT NULL COMMENT '日历规则ID',
  `config_version` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '配置版本',
  `copied_from_config_id` bigint NULL DEFAULT NULL COMMENT '复制来源配置ID',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_mes_pro_route_schedule_config_process` (`tenant_id` ASC, `route_version_id` ASC, `route_process_id` ASC, `deleted` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'MES 路线排产侧配置';

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `mes_pro_route_schedule_config` ADD COLUMN `item_id` bigint NULL COMMENT ''历史产品物料ID'' AFTER `route_version_id`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mes_pro_route_schedule_config' AND COLUMN_NAME = 'item_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) > 0,
    'ALTER TABLE `mes_pro_route_schedule_config` DROP INDEX `uk_mes_pro_route_schedule_config_item_process`',
    'SELECT 1')
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mes_pro_route_schedule_config' AND INDEX_NAME = 'uk_mes_pro_route_schedule_config_item_process'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @route_schedule_config_conflict_count := (
  SELECT COUNT(*)
  FROM (
    SELECT
      `tenant_id`,
      `route_version_id`,
      `route_process_id`
    FROM `mes_pro_route_schedule_config`
    WHERE `deleted` = b'0'
    GROUP BY `tenant_id`, `route_version_id`, `route_process_id`
    HAVING COUNT(DISTINCT CONCAT_WS('#',
      COALESCE(`capacity_mode`, ''),
      COALESCE(CAST(`hourly_capacity` AS CHAR), ''),
      COALESCE(CAST(`infinite_duration_quantity_factor` AS CHAR), ''),
      COALESCE(CAST(`infinite_duration_base_minutes` AS CHAR), ''),
      COALESCE(CAST(`night_shift_enabled` AS CHAR), ''),
      COALESCE(CAST(`calendar_rule_id` AS CHAR), ''),
      COALESCE(`config_version`, ''),
      COALESCE(`remark`, '')
    )) > 1
  ) conflicted
);
DROP TEMPORARY TABLE IF EXISTS `tmp_mes_pro_route_schedule_config_conflict_guard`;
CREATE TEMPORARY TABLE `tmp_mes_pro_route_schedule_config_conflict_guard` (
  `guard_id` tinyint NOT NULL PRIMARY KEY,
  `message` varchar(128) NOT NULL
);

INSERT INTO `tmp_mes_pro_route_schedule_config_conflict_guard` (`guard_id`, `message`)
VALUES (1, 'mes_pro_route_schedule_config conflict check passed');

INSERT INTO `tmp_mes_pro_route_schedule_config_conflict_guard` (`guard_id`, `message`)
SELECT 1, 'mes_pro_route_schedule_config has conflicting product-level configs'
WHERE @route_schedule_config_conflict_count > 0;

DROP TEMPORARY TABLE IF EXISTS `tmp_mes_pro_route_schedule_config_conflict_guard`;

UPDATE `mes_pro_route_schedule_config` target
JOIN (
  SELECT MIN(`id`) AS keep_id
  FROM `mes_pro_route_schedule_config`
  WHERE `deleted` = b'0'
  GROUP BY `tenant_id`, `route_version_id`, `route_process_id`
) kept ON kept.keep_id = target.`id`
SET target.`item_id` = NULL
WHERE target.`item_id` IS NOT NULL;

UPDATE `mes_pro_route_schedule_config` target
JOIN (
  SELECT `id`, ROW_NUMBER() OVER (
    PARTITION BY `tenant_id`, `route_version_id`, `route_process_id`
    ORDER BY `id`
  ) AS row_no
  FROM `mes_pro_route_schedule_config`
  WHERE `deleted` = b'0'
) ranked ON ranked.`id` = target.`id`
SET target.`deleted` = b'1'
WHERE ranked.row_no > 1;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `mes_pro_route_schedule_config` ADD UNIQUE INDEX `uk_mes_pro_route_schedule_config_process` (`tenant_id`, `route_version_id`, `route_process_id`, `deleted`) USING BTREE',
    'SELECT 1')
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mes_pro_route_schedule_config' AND INDEX_NAME = 'uk_mes_pro_route_schedule_config_process'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `mes_pro_schedule_order_daily_compare` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `schedule_order_id` bigint NOT NULL COMMENT '排产工单ID',
  `schedule_order_process_id` bigint NOT NULL COMMENT '排产工序快照ID',
  `process_id` bigint NULL DEFAULT NULL COMMENT '工序ID',
  `plan_date` date NOT NULL COMMENT '计划日期',
  `planned_quantity` decimal(18,6) NOT NULL DEFAULT 0 COMMENT '计划数量',
  `actual_quantity` decimal(18,6) NOT NULL DEFAULT 0 COMMENT '实际数量',
  `diff_quantity` decimal(18,6) NOT NULL DEFAULT 0 COMMENT '差异数量',
  `status` tinyint NOT NULL COMMENT '状态：0正常 1提前 2滞后',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_mes_pro_schedule_daily_compare` (`tenant_id` ASC, `schedule_order_process_id` ASC, `plan_date` ASC, `deleted` ASC) USING BTREE,
  INDEX `idx_mes_pro_schedule_daily_compare_order` (`tenant_id` ASC, `schedule_order_id` ASC, `plan_date` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'MES 排产工单按天计划实际差异';
