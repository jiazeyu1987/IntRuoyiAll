-- T7：统一电子签名历史查询与周期复核批次
-- 说明：复核批次由服务端调度或受控后台入口创建；客户端不得上传样本、哈希或复核结论作为事实源。

CREATE TABLE IF NOT EXISTS `system_electronic_signature_review` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '电子签名复核批次编号',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `review_type` varchar(32) NOT NULL COMMENT '复核类型，QUARTERLY-季度，SPECIAL-专项',
  `review_scope` varchar(128) NOT NULL COMMENT '复核范围',
  `trigger_reason` varchar(256) NOT NULL COMMENT '触发原因',
  `status` varchar(32) NOT NULL COMMENT '复核状态',
  `owner_user_id` bigint NOT NULL COMMENT '责任人用户编号',
  `backup_owner_user_id` bigint NOT NULL COMMENT '替补责任人用户编号',
  `sop_version` varchar(64) NOT NULL COMMENT '适用 SOP 版本',
  `training_evidence_id` varchar(128) NOT NULL COMMENT '培训证据编号',
  `planned_at` datetime NOT NULL COMMENT '计划生成时间',
  `due_at` datetime NOT NULL COMMENT '复核截止时间',
  `escalated_at` datetime DEFAULT NULL COMMENT '逾期升级时间',
  `escalated_to_user_id` bigint DEFAULT NULL COMMENT '逾期升级接收人',
  `completed_at` datetime DEFAULT NULL COMMENT '完成时间',
  `completed_by` bigint DEFAULT NULL COMMENT '完成人',
  `sample_rule_json` json NOT NULL COMMENT '服务端样本规则',
  `result_summary_json` json DEFAULT NULL COMMENT '复核结果摘要',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_system_esign_review_due` (`tenant_id`, `status`, `due_at`, `deleted`),
  KEY `idx_system_esign_review_scope` (`tenant_id`, `review_type`, `review_scope`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='统一电子签名周期复核批次';
