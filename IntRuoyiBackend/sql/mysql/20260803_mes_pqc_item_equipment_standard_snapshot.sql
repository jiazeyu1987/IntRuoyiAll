-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260802_mes_pqc_inspection_task; type=schema; riskLevel=medium
-- MES M7: PQC item-level equipment, equipment number, acceptance standard, method and submission snapshot.
-- Static schema contract:
-- `standard_lower_limit` decimal(18,6) DEFAULT NULL COMMENT '接收标准下限'
-- `standard_upper_limit` decimal(18,6) DEFAULT NULL COMMENT '接收标准上限'
-- `standard_unit` varchar(32) DEFAULT NULL COMMENT '接收标准单位'
-- `equipment_required` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否必须选择检验设备'
-- `selected_equipment_id` bigint NOT NULL COMMENT '实际检验设备ID快照'
-- `selected_equipment_number` varchar(64) NOT NULL COMMENT '实际检验设备编号快照'
-- `standard_lower_limit` decimal(18,6) DEFAULT NULL COMMENT '提交时接收标准下限快照'
-- `standard_upper_limit` decimal(18,6) DEFAULT NULL COMMENT '提交时接收标准上限快照'

DROP PROCEDURE IF EXISTS ensure_mes_pqc_item_snapshot_column;

DELIMITER //
CREATE PROCEDURE ensure_mes_pqc_item_snapshot_column(IN target_table VARCHAR(64), IN target_column VARCHAR(64), IN ddl_statement TEXT)
BEGIN
  SELECT COUNT(*)
  INTO @mes_pqc_item_snapshot_column_count
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = target_table
    AND COLUMN_NAME = target_column;

  SET @mes_pqc_item_snapshot_column_sql = IF(
    @mes_pqc_item_snapshot_column_count = 0,
    ddl_statement,
    CONCAT('SELECT ''', target_table, '.', target_column, ' already exists'' AS migration_status')
  );

  PREPARE mes_pqc_item_snapshot_column_stmt FROM @mes_pqc_item_snapshot_column_sql;
  EXECUTE mes_pqc_item_snapshot_column_stmt;
  DEALLOCATE PREPARE mes_pqc_item_snapshot_column_stmt;
END//
DELIMITER ;

CALL ensure_mes_pqc_item_snapshot_column('mes_qa_inspection_regulation_item', 'standard_lower_limit',
  'ALTER TABLE `mes_qa_inspection_regulation_item` ADD COLUMN `standard_lower_limit` decimal(18,6) DEFAULT NULL COMMENT ''接收标准下限'' AFTER `standard_text`');
CALL ensure_mes_pqc_item_snapshot_column('mes_qa_inspection_regulation_item', 'standard_upper_limit',
  'ALTER TABLE `mes_qa_inspection_regulation_item` ADD COLUMN `standard_upper_limit` decimal(18,6) DEFAULT NULL COMMENT ''接收标准上限'' AFTER `standard_lower_limit`');
CALL ensure_mes_pqc_item_snapshot_column('mes_qa_inspection_regulation_item', 'standard_unit',
  'ALTER TABLE `mes_qa_inspection_regulation_item` ADD COLUMN `standard_unit` varchar(32) DEFAULT NULL COMMENT ''接收标准单位'' AFTER `standard_upper_limit`');
CALL ensure_mes_pqc_item_snapshot_column('mes_qa_inspection_regulation_item', 'standard_precision',
  'ALTER TABLE `mes_qa_inspection_regulation_item` ADD COLUMN `standard_precision` int DEFAULT NULL COMMENT ''接收标准小数位数'' AFTER `standard_unit`');
CALL ensure_mes_pqc_item_snapshot_column('mes_qa_inspection_regulation_item', 'equipment_required',
  'ALTER TABLE `mes_qa_inspection_regulation_item` ADD COLUMN `equipment_required` bit(1) NOT NULL DEFAULT b''1'' COMMENT ''是否必须选择检验设备'' AFTER `standard_precision`');

CREATE TABLE IF NOT EXISTS `mes_qa_inspection_regulation_item_equipment` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `regulation_version_id` bigint NOT NULL COMMENT 'QA检验规程版本ID',
    `inspection_type` varchar(32) NOT NULL COMMENT '检验类型：FIRST/PATROL/FINAL',
    `item_code` varchar(64) NOT NULL COMMENT '检验项目编码',
    `equipment_id` bigint NOT NULL COMMENT 'MES设备台账ID',
    `equipment_code` varchar(64) NOT NULL COMMENT '设备编码快照',
    `equipment_name` varchar(128) NOT NULL COMMENT '设备名称快照',
    `equipment_number` varchar(64) NOT NULL COMMENT '设备编号/出厂编号/台账编码快照',
    `default_flag` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否默认设备',
    `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mes_qa_regulation_item_equipment` (`tenant_id`, `regulation_version_id`, `inspection_type`, `item_code`, `equipment_id`, `equipment_number`, `deleted`),
    KEY `idx_mes_qa_regulation_item_equipment_item` (`tenant_id`, `regulation_version_id`, `inspection_type`, `item_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES QA 检验规程项目设备';

CALL ensure_mes_pqc_item_snapshot_column('mes_pqc_inspection_piece_detail', 'selected_equipment_id',
  'ALTER TABLE `mes_pqc_inspection_piece_detail` ADD COLUMN `selected_equipment_id` bigint NOT NULL COMMENT ''实际检验设备ID快照'' AFTER `standard_text`');
CALL ensure_mes_pqc_item_snapshot_column('mes_pqc_inspection_piece_detail', 'selected_equipment_code',
  'ALTER TABLE `mes_pqc_inspection_piece_detail` ADD COLUMN `selected_equipment_code` varchar(64) NOT NULL COMMENT ''实际检验设备编码快照'' AFTER `selected_equipment_id`');
CALL ensure_mes_pqc_item_snapshot_column('mes_pqc_inspection_piece_detail', 'selected_equipment_name',
  'ALTER TABLE `mes_pqc_inspection_piece_detail` ADD COLUMN `selected_equipment_name` varchar(128) NOT NULL COMMENT ''实际检验设备名称快照'' AFTER `selected_equipment_code`');
CALL ensure_mes_pqc_item_snapshot_column('mes_pqc_inspection_piece_detail', 'selected_equipment_number',
  'ALTER TABLE `mes_pqc_inspection_piece_detail` ADD COLUMN `selected_equipment_number` varchar(64) NOT NULL COMMENT ''实际检验设备编号快照'' AFTER `selected_equipment_name`');
CALL ensure_mes_pqc_item_snapshot_column('mes_pqc_inspection_piece_detail', 'standard_lower_limit',
  'ALTER TABLE `mes_pqc_inspection_piece_detail` ADD COLUMN `standard_lower_limit` decimal(18,6) DEFAULT NULL COMMENT ''提交时接收标准下限快照'' AFTER `selected_equipment_number`');
CALL ensure_mes_pqc_item_snapshot_column('mes_pqc_inspection_piece_detail', 'standard_upper_limit',
  'ALTER TABLE `mes_pqc_inspection_piece_detail` ADD COLUMN `standard_upper_limit` decimal(18,6) DEFAULT NULL COMMENT ''提交时接收标准上限快照'' AFTER `standard_lower_limit`');
CALL ensure_mes_pqc_item_snapshot_column('mes_pqc_inspection_piece_detail', 'standard_unit',
  'ALTER TABLE `mes_pqc_inspection_piece_detail` ADD COLUMN `standard_unit` varchar(32) DEFAULT NULL COMMENT ''提交时接收标准单位快照'' AFTER `standard_upper_limit`');
CALL ensure_mes_pqc_item_snapshot_column('mes_pqc_inspection_piece_detail', 'standard_precision',
  'ALTER TABLE `mes_pqc_inspection_piece_detail` ADD COLUMN `standard_precision` int DEFAULT NULL COMMENT ''提交时接收标准小数位数快照'' AFTER `standard_unit`');

DROP PROCEDURE IF EXISTS ensure_mes_pqc_item_snapshot_column;
