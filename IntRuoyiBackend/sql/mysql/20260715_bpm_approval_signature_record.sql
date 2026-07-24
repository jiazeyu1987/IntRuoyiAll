-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260714_unified_signature_records_menu; type=schema; riskLevel=medium
-- 为审批中心直接审核动作建立统一电子签名记录表，覆盖 BPM 原生审批与 MES 报工审批。

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `bpm_approval_signature_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `module_code` varchar(64) NOT NULL COMMENT '审批模块编码：BPM/MES_FEEDBACK等',
  `source_task_type` varchar(128) NOT NULL COMMENT '来源任务类型',
  `source_task_id` varchar(128) DEFAULT NULL COMMENT '来源任务ID',
  `business_key` varchar(128) DEFAULT NULL COMMENT '业务键',
  `process_instance_id` varchar(128) DEFAULT NULL COMMENT '流程实例ID',
  `signer_user_id` bigint NOT NULL COMMENT '签名人用户ID',
  `review_result` varchar(32) NOT NULL COMMENT '审批结果：APPROVE/REJECT',
  `reason` varchar(500) DEFAULT NULL COMMENT '签名意见或驳回原因',
  `password_verified` bit(1) NOT NULL DEFAULT b'1' COMMENT '签名密码已通过校验',
  `signature_image_id` bigint DEFAULT NULL COMMENT '签名图片版本ID快照',
  `signature_image_version_no` int DEFAULT NULL COMMENT '签名图片版本号快照',
  `signature_image_file_id` bigint DEFAULT NULL COMMENT '签名图片文件ID快照',
  `signature_image_file_url` varchar(512) DEFAULT NULL COMMENT '签名图片文件URL快照',
  `signature_image_sha256` varchar(128) DEFAULT NULL COMMENT '签名图片SHA-256快照',
  `signature_image_content_type` varchar(128) DEFAULT NULL COMMENT '签名图片内容类型快照',
  `signature_image_file_size` bigint DEFAULT NULL COMMENT '签名图片大小快照',
  `signature_image_status_snapshot` varchar(32) DEFAULT NULL COMMENT '签名图片状态快照',
  `signature_image_verified_status` varchar(32) DEFAULT NULL COMMENT '签名图片校验状态',
  `signed_at` datetime NOT NULL COMMENT '签名时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_bpm_approval_signature_module_task` (`tenant_id`, `module_code`, `source_task_type`, `source_task_id`),
  KEY `idx_bpm_approval_signature_signer` (`tenant_id`, `signer_user_id`, `signed_at`),
  KEY `idx_bpm_approval_signature_signed_at` (`tenant_id`, `signed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='统一审批电子签名记录';
