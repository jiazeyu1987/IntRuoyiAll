-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260911_dcc_upload_ticket_category_guard; type=schema; riskLevel=medium

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `dcc_controlled_file_attachment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `controlled_file_id` bigint NOT NULL COMMENT '受控文件版本 ID',
  `storage_file_id` bigint NOT NULL COMMENT 'infra_file.id',
  `original_file_name` varchar(512) NOT NULL COMMENT '附件原始文件名',
  `content_type` varchar(255) DEFAULT NULL COMMENT 'MIME 类型',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小',
  `file_sha256` varchar(64) DEFAULT NULL COMMENT '文件 SHA-256',
  `sort_no` int NOT NULL DEFAULT 0 COMMENT '排序号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_dcc_file_attachment_file` (`tenant_id`, `controlled_file_id`, `deleted`),
  KEY `idx_dcc_file_attachment_storage` (`tenant_id`, `storage_file_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC 受控文件普通附件';

INSERT INTO `dcc_controlled_file_upload_policy` (
  `policy_code`,
  `scope_type`,
  `category_id`,
  `purpose`,
  `max_bytes`,
  `enabled`,
  `priority`,
  `policy_version`,
  `effective_from`,
  `effective_to`,
  `change_reason`,
  `tenant_id`,
  `creator`,
  `create_time`,
  `updater`,
  `update_time`,
  `deleted`
)
SELECT
  CONCAT('DCC_UPLOAD_DEFAULT_ATTACHMENT_V1_T', t.`id`),
  'PURPOSE',
  NULL,
  'ATTACHMENT',
  10485760,
  b'1',
  2,
  'v1',
  NULL,
  NULL,
  'Default DCC ATTACHMENT upload size policy seed',
  t.`id`,
  'system',
  NOW(),
  'system',
  NOW(),
  b'0'
FROM `system_tenant` t
WHERE NOT EXISTS (
  SELECT 1
  FROM `dcc_controlled_file_upload_policy` p
  WHERE p.`tenant_id` = t.`id`
    AND p.`scope_type` = 'PURPOSE'
    AND p.`purpose` = 'ATTACHMENT'
    AND p.`deleted` = b'0'
);
