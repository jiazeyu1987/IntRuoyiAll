-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260726_mes_batch_record_cell_link_work_order_source; type=schema; riskLevel=low
ALTER TABLE `mes_pro_batch_record_cell_link_rule`
  MODIFY COLUMN `source_cell_key` varchar(128) NOT NULL COMMENT '源单元格Key',
  MODIFY COLUMN `source_field_code` varchar(1024) DEFAULT NULL COMMENT '来源字段编码',
  MODIFY COLUMN `source_field_name` varchar(255) DEFAULT NULL COMMENT '来源字段名称快照';
