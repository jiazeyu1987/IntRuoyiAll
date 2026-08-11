-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260528_dcc_controlled_file_protection; type=schema; riskLevel=medium
-- Persist immutable signature-to-controlled-copy binding evidence.

CREATE TABLE IF NOT EXISTS `dcc_controlled_file_signature_binding` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '绑定事件编号',
  `signature_id` bigint NOT NULL COMMENT '签名记录编号',
  `controlled_file_id` bigint NOT NULL COMMENT '受控文件编号',
  `original_evidence_hash` varchar(128) NOT NULL COMMENT '签名时原始证据摘要',
  `controlled_copy_file_id` bigint NOT NULL COMMENT '发布受控副本文件编号',
  `controlled_copy_sha256` char(64) NOT NULL COMMENT '发布受控副本 SHA-256',
  `controlled_copy_hash_algorithm` varchar(32) NOT NULL DEFAULT 'SHA256' COMMENT '受控副本摘要算法',
  `bound_at` datetime NOT NULL COMMENT '绑定时间',
  `bound_by` bigint DEFAULT NULL COMMENT '绑定操作者',
  `binding_event_key` varchar(128) NOT NULL COMMENT '最终化事件键',
  `binding_payload_version` varchar(32) NOT NULL DEFAULT 'v1' COMMENT '绑定载荷版本',
  `binding_hash_algorithm` varchar(32) NOT NULL DEFAULT 'SHA256' COMMENT '绑定事件摘要算法',
  `binding_hash` char(64) NOT NULL COMMENT '绑定事件摘要',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_signature_binding_signature` (`tenant_id`, `signature_id`, `deleted`),
  KEY `idx_dcc_signature_binding_file` (`tenant_id`, `controlled_file_id`),
  KEY `idx_dcc_signature_binding_copy` (`tenant_id`, `controlled_copy_file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC 签名证据受控副本绑定事件';
