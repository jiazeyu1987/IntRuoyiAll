CREATE TABLE IF NOT EXISTS `gxp_audit_event` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '审计事件编号',
    `tenant_id` bigint NOT NULL COMMENT '租户编号',
    `ledger_sequence` bigint NOT NULL COMMENT '租户内审计账本序号',
    `operation_id` varchar(128) NOT NULL COMMENT 'GxP 操作登记编号',
    `domain` varchar(64) NOT NULL COMMENT '业务域',
    `subject_type` varchar(64) NOT NULL COMMENT '对象类型',
    `subject_id` varchar(256) NOT NULL COMMENT '对象编号',
    `subject_version` varchar(128) NOT NULL COMMENT '对象版本',
    `action` varchar(64) NOT NULL COMMENT '操作类型',
    `reason` varchar(500) NOT NULL COMMENT '变更原因',
    `actor_id` bigint NOT NULL COMMENT '操作人编号',
    `actor_username` varchar(64) NOT NULL COMMENT '操作人账号',
    `actor_display_name` varchar(128) NOT NULL COMMENT '操作人显示名',
    `server_occurred_at` datetime NOT NULL COMMENT '服务器审计时间',
    `before_state` varchar(32) NOT NULL COMMENT '前状态 ABSENT/PRESENT/VOIDED',
    `before_object_version` varchar(128) NULL COMMENT '前对象版本',
    `before_state_json` longtext NULL COMMENT '前状态规范化 JSON',
    `after_state` varchar(32) NOT NULL COMMENT '后状态 ABSENT/PRESENT/VOIDED',
    `after_object_version` varchar(128) NULL COMMENT '后对象版本',
    `after_state_json` longtext NULL COMMENT '后状态规范化 JSON',
    `policy_version` varchar(128) NOT NULL COMMENT '审计策略版本',
    `idempotency_key` varchar(128) NOT NULL COMMENT '幂等键',
    `request_id` varchar(128) NULL COMMENT '请求编号',
    `signature_record_id` varchar(128) NULL COMMENT '电子签名记录编号',
    `signature_content_hash` varchar(128) NULL COMMENT '电子签名内容 Hash',
    `idempotency_payload_hash` char(64) NOT NULL COMMENT '幂等载荷 Hash',
    `canonical_event_json` longtext NOT NULL COMMENT '规范化审计事件 JSON',
    `previous_event_hash` char(64) NULL COMMENT '前序事件 Hash',
    `event_hash` char(64) NOT NULL COMMENT '事件 Hash',
    `algorithm` varchar(32) NOT NULL COMMENT 'Hash 算法',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_gxp_audit_event_sequence` (`tenant_id`, `ledger_sequence`),
    UNIQUE KEY `uk_gxp_audit_event_idempotency` (`tenant_id`, `idempotency_key`),
    KEY `idx_gxp_audit_event_subject` (`tenant_id`, `domain`, `subject_type`, `subject_id`, `server_occurred_at`),
    KEY `idx_gxp_audit_event_operation` (`tenant_id`, `operation_id`, `server_occurred_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='GxP 统一审计事件只追加账本';

CREATE TABLE IF NOT EXISTS `gxp_audit_policy_version` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '策略版本编号',
    `tenant_id` bigint NOT NULL COMMENT '租户编号',
    `policy_version` varchar(128) NOT NULL COMMENT '策略版本',
    `policy_hash` char(64) NOT NULL COMMENT '策略文件 Hash',
    `approved_by` bigint NOT NULL COMMENT '批准人',
    `approved_at` datetime NOT NULL COMMENT '批准时间',
    `approval_reference` varchar(256) NOT NULL COMMENT '批准依据',
    `coverage_report_hash` char(64) NOT NULL COMMENT '覆盖报告 Hash',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_gxp_audit_policy_version` (`tenant_id`, `policy_version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='GxP 审计策略版本登记';

CREATE TABLE IF NOT EXISTS `gxp_audit_policy_operation` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '策略操作登记编号',
    `tenant_id` bigint NOT NULL COMMENT '租户编号',
    `policy_version` varchar(128) NOT NULL COMMENT '策略版本',
    `operation_id` varchar(128) NOT NULL COMMENT 'GxP 操作登记编号',
    `source_type` varchar(64) NOT NULL COMMENT '入口类型 SERVICE_METHOD/JOB/MIGRATION/SCRIPT',
    `source_locator` varchar(512) NOT NULL COMMENT '源码或受控命令定位',
    `domain` varchar(64) NOT NULL COMMENT '业务域',
    `subject_type` varchar(64) NOT NULL COMMENT '对象类型',
    `action_type` varchar(64) NOT NULL COMMENT '操作类型',
    `reason_policy` varchar(64) NOT NULL COMMENT '原因策略',
    `signature_policy` varchar(64) NOT NULL COMMENT '签名策略',
    `state_policy` varchar(64) NOT NULL COMMENT '状态快照策略',
    `retention_class` varchar(64) NOT NULL COMMENT '保存分类',
    `test_ids` varchar(512) NOT NULL COMMENT '覆盖测试编号',
    `owner` varchar(128) NOT NULL COMMENT '业务 owner',
    `applicability` varchar(64) NOT NULL COMMENT 'GXP/NOT_APPLICABLE',
    `active` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否当前有效',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_gxp_audit_policy_operation` (`tenant_id`, `operation_id`, `policy_version`),
    KEY `idx_gxp_audit_policy_operation_source` (`tenant_id`, `source_type`, `source_locator`),
    KEY `idx_gxp_audit_policy_operation_active` (`tenant_id`, `active`, `operation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='GxP 审计策略操作登记';

CREATE TABLE IF NOT EXISTS `gxp_audit_coverage_report` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '覆盖报告编号',
    `tenant_id` bigint NOT NULL COMMENT '租户编号',
    `policy_version` varchar(128) NOT NULL COMMENT '策略版本',
    `source_commit` varchar(64) NOT NULL COMMENT '源码 commit',
    `registry_sha256` char(64) NOT NULL COMMENT '策略登记 Hash',
    `discovered_inventory_sha256` char(64) NOT NULL COMMENT '实际写入口清单 Hash',
    `registered_count` int NOT NULL COMMENT '登记项数量',
    `not_applicable_count` int NOT NULL COMMENT '已批准不适用数量',
    `gap_count` int NOT NULL COMMENT '缺口数量',
    `test_mapping_count` int NOT NULL COMMENT '测试映射数量',
    `report_sha256` char(64) NOT NULL COMMENT '覆盖报告 Hash',
    `generated_at_utc` datetime NOT NULL COMMENT '生成时间 UTC',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_gxp_audit_coverage_report` (`tenant_id`, `policy_version`, `source_commit`),
    KEY `idx_gxp_audit_coverage_report_policy` (`tenant_id`, `policy_version`, `generated_at_utc`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='GxP 审计覆盖登记报告';

CREATE TABLE IF NOT EXISTS `gxp_audit_daily_manifest` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日清单编号',
    `tenant_id` bigint NOT NULL COMMENT '租户编号',
    `business_date` date NOT NULL COMMENT '业务日期',
    `first_sequence` bigint NOT NULL COMMENT '首个账本序号',
    `last_sequence` bigint NOT NULL COMMENT '最后账本序号',
    `event_count` int NOT NULL COMMENT '事件数量',
    `merkle_root` char(64) NOT NULL COMMENT 'Merkle Root',
    `previous_manifest_hash` char(64) NULL COMMENT '前一清单 Hash',
    `manifest_hash` char(64) NOT NULL COMMENT '清单 Hash',
    `sealed_at_utc` datetime NOT NULL COMMENT '封存时间 UTC',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_gxp_audit_daily_manifest` (`tenant_id`, `business_date`),
    UNIQUE KEY `uk_gxp_audit_daily_manifest_hash` (`tenant_id`, `manifest_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='GxP 审计每日封存清单';

CREATE TABLE IF NOT EXISTS `gxp_audit_seal_watermark` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '封存水位编号',
    `tenant_id` bigint NOT NULL COMMENT '租户编号',
    `watermark_type` varchar(64) NOT NULL COMMENT '水位类型',
    `sealed_through_sequence` bigint NOT NULL COMMENT '已封存至账本序号',
    `last_manifest_hash` char(64) NOT NULL COMMENT '最新清单 Hash',
    `unsealed_event_count` int NOT NULL COMMENT '未封存事件数',
    `watermark_hash` char(64) NOT NULL COMMENT '水位 Hash',
    `generated_at_utc` datetime NOT NULL COMMENT '生成时间 UTC',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_gxp_audit_seal_watermark_hash` (`tenant_id`, `watermark_hash`),
    KEY `idx_gxp_audit_seal_watermark_sequence` (`tenant_id`, `watermark_type`, `sealed_through_sequence`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='GxP 审计封存水位';

DROP TRIGGER IF EXISTS `trg_gxp_audit_event_no_update`;
DROP TRIGGER IF EXISTS `trg_gxp_audit_event_no_delete`;
DROP TRIGGER IF EXISTS `trg_gxp_audit_daily_manifest_no_update`;
DROP TRIGGER IF EXISTS `trg_gxp_audit_daily_manifest_no_delete`;
DROP TRIGGER IF EXISTS `trg_gxp_audit_seal_watermark_no_update`;
DROP TRIGGER IF EXISTS `trg_gxp_audit_seal_watermark_no_delete`;
DELIMITER $$
CREATE TRIGGER `trg_gxp_audit_event_no_update`
BEFORE UPDATE ON `gxp_audit_event`
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'GxP audit event is append-only';
END$$
CREATE TRIGGER `trg_gxp_audit_event_no_delete`
BEFORE DELETE ON `gxp_audit_event`
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'GxP audit event is append-only';
END$$
CREATE TRIGGER `trg_gxp_audit_daily_manifest_no_update`
BEFORE UPDATE ON `gxp_audit_daily_manifest`
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'GxP audit daily manifest is append-only';
END$$
CREATE TRIGGER `trg_gxp_audit_daily_manifest_no_delete`
BEFORE DELETE ON `gxp_audit_daily_manifest`
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'GxP audit daily manifest is append-only';
END$$
CREATE TRIGGER `trg_gxp_audit_seal_watermark_no_update`
BEFORE UPDATE ON `gxp_audit_seal_watermark`
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'GxP audit seal watermark is append-only';
END$$
CREATE TRIGGER `trg_gxp_audit_seal_watermark_no_delete`
BEFORE DELETE ON `gxp_audit_seal_watermark`
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'GxP audit seal watermark is append-only';
END$$
DELIMITER ;
