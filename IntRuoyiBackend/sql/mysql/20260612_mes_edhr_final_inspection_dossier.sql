-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- eDHR 批次成品检卷宗项契约
-- Fail fast: 成品检/OQC 必须作为批次级受控卷宗项存在，不使用普通附件或空清单代替。

DROP PROCEDURE IF EXISTS ensure_mes_edhr_final_inspection_dossier_table;

DELIMITER $$

CREATE PROCEDURE ensure_mes_edhr_final_inspection_dossier_table(IN p_table_name varchar(128))
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table_name
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing required table for eDHR final inspection dossier migration';
  END IF;
END$$

DELIMITER ;

CALL ensure_mes_edhr_final_inspection_dossier_table('mes_pro_edhr_batch_execution');
CALL ensure_mes_edhr_final_inspection_dossier_table('mes_qc_oqc');

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_batch_dossier_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `batch_execution_id` bigint NOT NULL COMMENT 'eDHR批次执行ID',
  `item_type` varchar(32) NOT NULL COMMENT '卷宗项类型：FINAL_INSPECTION',
  `item_key` varchar(64) NOT NULL COMMENT '卷宗项业务键',
  `item_name` varchar(128) NOT NULL COMMENT '卷宗项名称',
  `required_flag` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否必需',
  `item_status` varchar(32) NOT NULL COMMENT '卷宗项状态：PENDING/COMPLETED/BLOCKED',
  `source_doc_type` varchar(32) DEFAULT NULL COMMENT '来源单据类型：OQC',
  `source_doc_id` bigint DEFAULT NULL COMMENT '来源单据ID',
  `source_doc_code` varchar(128) DEFAULT NULL COMMENT '来源单据编号',
  `source_doc_status` varchar(64) DEFAULT NULL COMMENT '来源单据状态',
  `source_doc_result` varchar(64) DEFAULT NULL COMMENT '来源单据检验结果',
  `source_doc_hash` char(64) DEFAULT NULL COMMENT '来源单据受控HASH',
  `completed_at` datetime DEFAULT NULL COMMENT '业务完成时间',
  `verified_at` datetime DEFAULT NULL COMMENT 'eDHR校验时间',
  `blocker_code` varchar(128) DEFAULT NULL COMMENT '阻塞编码',
  `blocker_message` varchar(512) DEFAULT NULL COMMENT '阻塞说明',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_edhr_batch_dossier_item` (`tenant_id`, `batch_execution_id`, `item_type`, `item_key`, `deleted`),
  KEY `idx_mes_pro_edhr_batch_dossier_batch` (`tenant_id`, `batch_execution_id`, `item_status`, `deleted`),
  KEY `idx_mes_pro_edhr_batch_dossier_source` (`tenant_id`, `source_doc_type`, `source_doc_id`, `source_doc_code`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='eDHR批次卷宗项';

DROP PROCEDURE IF EXISTS ensure_mes_edhr_final_inspection_dossier_table;
