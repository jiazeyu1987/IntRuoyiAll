-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
CREATE TABLE IF NOT EXISTS `mes_pro_batch_record_cell_link_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '规则ID',
  `scope_type` varchar(32) NOT NULL COMMENT '作用范围类型',
  `scope_id` bigint NOT NULL COMMENT '作用范围ID',
  `route_id` bigint DEFAULT NULL COMMENT '工艺路线ID',
  `batch_record_definition_id` bigint DEFAULT NULL COMMENT '批记录定义ID',
  `batch_record_version_id` bigint DEFAULT NULL COMMENT '批记录版本ID',
  `source_type` varchar(32) NOT NULL DEFAULT 'BATCH_RECORD_CELL' COMMENT '来源类型',
  `source_report_id` varchar(64) NOT NULL COMMENT '源表单报表ID',
  `source_report_name` varchar(100) NOT NULL COMMENT '源表单名称快照',
  `source_row_index` int NOT NULL COMMENT '源单元格行坐标',
  `source_column_index` int NOT NULL COMMENT '源单元格列坐标',
  `source_cell_key` varchar(32) NOT NULL COMMENT '源单元格Key',
  `source_field_code` varchar(64) DEFAULT NULL COMMENT '来源字段编码',
  `source_field_name` varchar(100) DEFAULT NULL COMMENT '来源字段名称快照',
  `source_label` varchar(255) DEFAULT NULL COMMENT '源单元格标签快照',
  `source_value_type` varchar(32) DEFAULT NULL COMMENT '源单元格值类型快照',
  `target_report_id` varchar(64) NOT NULL COMMENT '目标表单报表ID',
  `target_report_name` varchar(100) NOT NULL COMMENT '目标表单名称快照',
  `target_row_index` int NOT NULL COMMENT '目标单元格行坐标',
  `target_column_index` int NOT NULL COMMENT '目标单元格列坐标',
  `target_cell_key` varchar(32) NOT NULL COMMENT '目标单元格Key',
  `target_label` varchar(255) DEFAULT NULL COMMENT '目标单元格标签快照',
  `target_value_type` varchar(32) DEFAULT NULL COMMENT '目标单元格值类型快照',
  `aggregation_strategy` varchar(32) DEFAULT NULL COMMENT '多源聚合策略：SUM/LIST/DISTINCT_LIST/FIRST/LAST/MIN/MAX',
  `overwrite_policy` varchar(32) NOT NULL DEFAULT 'ONLY_WHEN_EMPTY' COMMENT '覆盖策略',
  `template_snapshot_hash` char(64) NOT NULL COMMENT '配置时模板快照哈希',
  `rule_version` bigint NOT NULL COMMENT '规则版本',
  `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `active_pair_unique_flag` tinyint GENERATED ALWAYS AS (
    CASE WHEN `deleted` = b'0' THEN 1 ELSE NULL END
  ) STORED,
  `active_target_unique_flag` tinyint GENERATED ALWAYS AS (
    CASE WHEN `deleted` = b'0' AND `enabled` = b'1' THEN 1 ELSE NULL END
  ) STORED,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_batch_record_cell_link_pair` (`tenant_id`, `scope_type`, `scope_id`, `source_report_id`, `source_cell_key`, `target_report_id`, `target_cell_key`, `active_pair_unique_flag`),
  UNIQUE KEY `uk_mes_batch_record_cell_link_target` (`tenant_id`, `scope_type`, `scope_id`, `target_report_id`, `target_cell_key`, `active_target_unique_flag`),
  KEY `idx_mes_batch_record_cell_link_source` (`tenant_id`, `scope_type`, `scope_id`, `source_report_id`, `source_cell_key`, `deleted`),
  KEY `idx_mes_batch_record_cell_link_target` (`tenant_id`, `scope_type`, `scope_id`, `target_report_id`, `target_cell_key`, `enabled`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='批记录跨表单单元格链接规则';

ALTER TABLE `mes_pro_batch_record_cell_link_rule`
  MODIFY COLUMN `batch_record_definition_id` bigint DEFAULT NULL COMMENT '批记录定义ID',
  MODIFY COLUMN `batch_record_version_id` bigint DEFAULT NULL COMMENT '批记录版本ID';

DROP PROCEDURE IF EXISTS ensure_mes_batch_record_cell_link_rule_aggregation_strategy;
DELIMITER $$
CREATE PROCEDURE ensure_mes_batch_record_cell_link_rule_aggregation_strategy()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_batch_record_cell_link_rule'
      AND COLUMN_NAME = 'aggregation_strategy'
  ) THEN
    ALTER TABLE `mes_pro_batch_record_cell_link_rule`
      ADD COLUMN `aggregation_strategy` varchar(32) DEFAULT NULL COMMENT '多源聚合策略：SUM/LIST/DISTINCT_LIST/FIRST/LAST/MIN/MAX' AFTER `target_value_type`;
  END IF;
END$$
DELIMITER ;

CALL ensure_mes_batch_record_cell_link_rule_aggregation_strategy();

DROP PROCEDURE IF EXISTS ensure_mes_batch_record_cell_link_rule_aggregation_strategy;

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 605071101, '批记录单元格链接查询', 'mes:pro-batch-record-cell-link:query', 3, 1, 0, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-batch-record-cell-link:query' AND `deleted` = b'0');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 605071102, '批记录单元格链接维护', 'mes:pro-batch-record-cell-link:update', 3, 2, 0, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-batch-record-cell-link:update' AND `deleted` = b'0');
