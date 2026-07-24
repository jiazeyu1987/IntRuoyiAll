-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- eDHR controlled final archive schema.
-- Safe to run repeatedly: creates MES-owned archive metadata and event tables.

DROP PROCEDURE IF EXISTS ensure_mes_batch_record_execution_archive;
DELIMITER $$
CREATE PROCEDURE ensure_mes_batch_record_execution_archive()
BEGIN
  CREATE TABLE IF NOT EXISTS `mes_pro_batch_record_execution_archive` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `execution_id` bigint NOT NULL COMMENT '执行记录ID',
    `archive_code` varchar(64) NOT NULL COMMENT '归档编号',
    `archive_version` int NOT NULL COMMENT '归档版本',
    `artifact_type` varchar(16) NOT NULL COMMENT '归档文件类型',
    `archive_status` varchar(16) NOT NULL COMMENT '归档状态',
    `file_id` bigint DEFAULT NULL COMMENT 'Infra文件ID',
    `file_name` varchar(255) DEFAULT NULL COMMENT '文件名',
    `content_type` varchar(128) DEFAULT NULL COMMENT 'MIME类型',
    `file_size` bigint DEFAULT NULL COMMENT '文件大小',
    `sha256` char(64) DEFAULT NULL COMMENT '文件SHA-256摘要',
    `render_source_version` varchar(64) DEFAULT NULL COMMENT '渲染源版本',
    `execution_snapshot_hash` char(64) DEFAULT NULL COMMENT '执行快照摘要',
    `cell_values_hash` char(64) DEFAULT NULL COMMENT '单元格值摘要',
    `signature_hash` char(64) DEFAULT NULL COMMENT '签名摘要',
    `approval_snapshot_id` bigint DEFAULT NULL COMMENT '审批快照ID',
    `approval_snapshot_hash` char(64) DEFAULT NULL COMMENT '审批快照摘要',
    `seal_signature_id` bigint DEFAULT NULL COMMENT '封存签名ID',
    `generated_by` bigint NOT NULL COMMENT '生成人',
    `generated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
    `sealed_by` bigint DEFAULT NULL COMMENT '封存人',
    `sealed_at` datetime DEFAULT NULL COMMENT '封存时间',
    `failure_reason` varchar(500) DEFAULT NULL COMMENT '失败原因',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_archive_code` (`tenant_id`, `archive_code`),
    KEY `idx_execution_type_version` (`tenant_id`, `execution_id`, `artifact_type`, `archive_version`),
    KEY `idx_execution_status` (`tenant_id`, `execution_id`, `archive_status`),
    KEY `idx_generated_time` (`tenant_id`, `generated_at`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 执行归档记录';

  CREATE TABLE IF NOT EXISTS `mes_pro_batch_record_execution_archive_event` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `archive_id` bigint NOT NULL COMMENT '归档ID',
    `execution_id` bigint NOT NULL COMMENT '执行记录ID',
    `event_type` varchar(32) NOT NULL COMMENT '事件类型',
    `actor_id` bigint DEFAULT NULL COMMENT '操作人',
    `event_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '事件时间',
    `client_ip` varchar(64) DEFAULT NULL COMMENT '客户端IP',
    `user_agent` varchar(512) DEFAULT NULL COMMENT 'User-Agent',
    `message` varchar(500) DEFAULT NULL COMMENT '事件说明',
    `metadata_json` text COMMENT '结构化元数据',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_archive_event_archive_id` (`tenant_id`, `archive_id`),
    KEY `idx_archive_event_execution_type` (`tenant_id`, `execution_id`, `event_type`),
    KEY `idx_archive_event_time` (`tenant_id`, `event_time`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 执行归档事件';
END$$
DELIMITER ;

CALL ensure_mes_batch_record_execution_archive();

DROP PROCEDURE IF EXISTS ensure_mes_batch_record_execution_archive;
