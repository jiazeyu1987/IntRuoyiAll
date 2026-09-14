-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260813_dcc_signature_binding_object_key; type=schema; riskLevel=medium
-- Audit every business-approved reissue of historical DCC signature evidence hashes.

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `dcc_controlled_file_signature_reissue_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志编号',
  `controlled_file_id` bigint NOT NULL COMMENT '受控文件编号',
  `signature_id` bigint NOT NULL COMMENT '签名记录编号',
  `before_evidence_hash` varchar(128) NOT NULL COMMENT '重新封存前证据哈希',
  `before_evidence_key_version` varchar(128) NOT NULL COMMENT '重新封存前密钥版本',
  `before_evidence_status` varchar(32) NOT NULL COMMENT '重新封存前证据状态',
  `after_evidence_hash` varchar(128) NOT NULL COMMENT '重新封存后证据哈希',
  `after_evidence_key_version` varchar(128) NOT NULL COMMENT '重新封存后密钥版本',
  `after_evidence_status` varchar(32) NOT NULL COMMENT '重新封存后证据状态',
  `reissued_by` bigint NOT NULL COMMENT '重新封存操作人',
  `reissued_at` datetime NOT NULL COMMENT '重新封存时间',
  `request_id` varchar(128) NOT NULL COMMENT '审计请求号',
  `reason` varchar(1024) NOT NULL COMMENT '业务批准原因',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_dcc_signature_reissue_file` (`tenant_id`, `controlled_file_id`, `deleted`),
  KEY `idx_dcc_signature_reissue_signature` (`tenant_id`, `signature_id`, `deleted`),
  UNIQUE KEY `uk_dcc_signature_reissue_request` (`tenant_id`, `signature_id`, `request_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC签名证据重新封存审计日志';
