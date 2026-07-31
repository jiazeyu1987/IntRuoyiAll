-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260711_mes_batch_record_cell_link_rule; type=schema; riskLevel=low
ALTER TABLE `mes_pro_batch_record_cell_link_rule`
  ADD COLUMN `source_type` varchar(32) NOT NULL DEFAULT 'BATCH_RECORD_CELL' COMMENT '来源类型' AFTER `batch_record_version_id`,
  ADD COLUMN `source_field_code` varchar(64) DEFAULT NULL COMMENT '来源字段编码' AFTER `source_cell_key`,
  ADD COLUMN `source_field_name` varchar(100) DEFAULT NULL COMMENT '来源字段名称快照' AFTER `source_field_code`;
