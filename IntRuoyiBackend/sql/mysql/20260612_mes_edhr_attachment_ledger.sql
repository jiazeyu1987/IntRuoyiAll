-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
DROP PROCEDURE IF EXISTS ensure_mes_edhr_attachment_ledger_preconditions;
DELIMITER $$
CREATE PROCEDURE ensure_mes_edhr_attachment_ledger_preconditions()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_batch_record_execution'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'mes_pro_batch_record_execution is missing; cannot apply eDHR attachment ledger migration';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_edhr_batch_execution'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'mes_pro_edhr_batch_execution is missing; cannot apply eDHR attachment ledger migration';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_edhr_batch_execution_task'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'mes_pro_edhr_batch_execution_task is missing; cannot apply eDHR attachment ledger migration';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_edhr_work_task'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'mes_pro_edhr_work_task is missing; cannot apply eDHR attachment ledger migration';
  END IF;
END$$
DELIMITER ;

CALL ensure_mes_edhr_attachment_ledger_preconditions();

CREATE TABLE IF NOT EXISTS `mes_pro_batch_record_execution_attachment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `execution_id` bigint NOT NULL COMMENT '执行记录ID',
  `batch_execution_id` bigint NOT NULL COMMENT '批次执行ID',
  `batch_task_id` bigint NOT NULL COMMENT '批次工序任务ID',
  `work_task_id` bigint DEFAULT NULL COMMENT '工作任务ID',
  `row_index` int NOT NULL COMMENT '模板行索引',
  `column_index` int NOT NULL COMMENT '模板列索引',
  `field_key` varchar(128) NOT NULL COMMENT '字段键',
  `field_path` varchar(255) NOT NULL COMMENT '字段路径',
  `field_label` varchar(255) DEFAULT NULL COMMENT '字段标签',
  `attachment_type` varchar(32) NOT NULL COMMENT '附件类型：FILE/IMAGE',
  `attachment_group_key` varchar(64) NOT NULL COMMENT '附件组键',
  `attachment_action` varchar(32) NOT NULL COMMENT '附件动作：ADD/REPLACE/VOID',
  `version_no` int NOT NULL COMMENT '附件组版本号',
  `file_id` bigint NOT NULL COMMENT '文件ID',
  `file_url` varchar(1024) NOT NULL COMMENT '文件访问地址',
  `storage_config_id` bigint NOT NULL COMMENT '文件配置ID',
  `storage_path` varchar(512) NOT NULL COMMENT '存储路径',
  `file_name` varchar(255) NOT NULL COMMENT '文件名',
  `content_type` varchar(128) NOT NULL COMMENT 'Content-Type',
  `file_size` bigint NOT NULL COMMENT '文件大小',
  `sha256` char(64) NOT NULL COMMENT '文件SHA256',
  `storage_retention_json` json DEFAULT NULL COMMENT '存储保留证据',
  `storage_retention_hash` char(64) DEFAULT NULL COMMENT '存储保留证据HASH',
  `audit_batch_id` bigint DEFAULT NULL COMMENT '字段审计批次ID',
  `signature_id` bigint DEFAULT NULL COMMENT '签名ID',
  `previous_attachment_hash` char(64) DEFAULT NULL COMMENT '上一附件事件HASH',
  `attachment_hash` char(64) NOT NULL COMMENT '附件事件HASH',
  `operator_id` bigint NOT NULL COMMENT '操作人ID',
  `operator_name` varchar(64) NOT NULL COMMENT '操作人名称',
  `operated_at` datetime NOT NULL COMMENT '操作时间',
  `reason_category` varchar(64) NOT NULL COMMENT '原因分类',
  `reason_text` varchar(500) NOT NULL COMMENT '原因说明',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_bre_attach_version` (`tenant_id`, `execution_id`, `field_path`, `field_key`, `row_index`, `column_index`, `attachment_group_key`, `version_no`),
  UNIQUE KEY `uk_mes_pro_bre_attach_hash` (`tenant_id`, `execution_id`, `attachment_hash`),
  KEY `idx_mes_pro_bre_attach_execution_field` (`tenant_id`, `execution_id`, `field_path`, `field_key`, `row_index`, `column_index`),
  KEY `idx_mes_pro_bre_attach_batch` (`tenant_id`, `batch_execution_id`, `batch_task_id`),
  KEY `idx_mes_pro_bre_attach_work_task` (`tenant_id`, `work_task_id`),
  KEY `idx_mes_pro_bre_attach_signature` (`tenant_id`, `signature_id`, `audit_batch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='eDHR执行附件证据账本';

DROP PROCEDURE IF EXISTS ensure_mes_edhr_attachment_ledger_preconditions;
