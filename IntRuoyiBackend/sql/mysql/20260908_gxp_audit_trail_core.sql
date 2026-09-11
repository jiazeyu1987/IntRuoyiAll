-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
CREATE TABLE IF NOT EXISTS `gxp_audit_event` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '审计事件编号',
    `tenant_id` bigint NOT NULL COMMENT '租户编号',
    `ledger_sequence` bigint NOT NULL COMMENT '租户内审计账本序号',
    `operation_id` varchar(128) NOT NULL COMMENT 'GxP 操作登记编号',
    `domain` varchar(64) NOT NULL COMMENT '业务域',
    `subject_type` varchar(64) NOT NULL COMMENT '对象类型',
    `subject_id` varchar(2048) NOT NULL COMMENT '对象编号',
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
    `idempotency_key` varchar(512) NOT NULL COMMENT '幂等键',
    `request_id` varchar(128) NULL COMMENT '请求编号',
    `signature_record_id` varchar(128) NULL COMMENT '电子签名记录编号',
    `signature_content_hash` varchar(128) NULL COMMENT '电子签名内容 Hash',
    `idempotency_payload_hash` char(64) NOT NULL COMMENT '幂等载荷 Hash',
    `canonical_event_json` longtext NOT NULL COMMENT '规范化审计事件 JSON',
    `previous_event_hash` char(64) NULL COMMENT '前序事件 Hash',
    `event_hash` char(64) NOT NULL COMMENT '事件 Hash',
    `algorithm` varchar(32) NOT NULL COMMENT 'Hash 算法',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `creator` varchar(64) DEFAULT NULL COMMENT '创建者',
    `updater` varchar(64) DEFAULT NULL COMMENT '更新者',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_gxp_audit_event_sequence` (`tenant_id`, `ledger_sequence`),
    UNIQUE KEY `uk_gxp_audit_event_idempotency` (`tenant_id`, `idempotency_key`),
    KEY `idx_gxp_audit_event_subject` (`tenant_id`, `domain`, `subject_type`, `subject_id`(191), `server_occurred_at`),
    KEY `idx_gxp_audit_event_operation` (`tenant_id`, `operation_id`, `server_occurred_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='GxP 统一审计事件只追加账本';

CREATE TABLE IF NOT EXISTS `gxp_audit_ledger_sequence` (
    `tenant_id` bigint NOT NULL COMMENT '租户编号',
    `next_ledger_sequence` bigint NOT NULL COMMENT '下一个租户内审计账本序号',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='GxP 审计账本序号水位';

INSERT INTO `gxp_audit_ledger_sequence` (`tenant_id`, `next_ledger_sequence`)
SELECT `tenant_id`, COALESCE(MAX(`ledger_sequence`), 0) + 1
FROM `gxp_audit_event`
GROUP BY `tenant_id`
ON DUPLICATE KEY UPDATE
    `next_ledger_sequence` = GREATEST(`next_ledger_sequence`, VALUES(`next_ledger_sequence`));

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
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `creator` varchar(64) DEFAULT NULL COMMENT '创建者',
    `updater` varchar(64) DEFAULT NULL COMMENT '更新者',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
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

DROP PROCEDURE IF EXISTS `ensure_gxp_audit_core_column`;
DROP PROCEDURE IF EXISTS `ensure_gxp_audit_core_index`;
DROP PROCEDURE IF EXISTS `drop_gxp_audit_core_index`;

DELIMITER $$
CREATE PROCEDURE `ensure_gxp_audit_core_column`(
    IN target_table varchar(64),
    IN target_column varchar(64),
    IN ddl_statement text
)
BEGIN
    SELECT COUNT(1) INTO @gxp_audit_core_column_count
      FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = target_table
       AND COLUMN_NAME = target_column;
    SET @gxp_audit_core_column_sql = IF(@gxp_audit_core_column_count = 0, ddl_statement,
        CONCAT('SELECT ''', target_table, '.', target_column, ' already exists'' AS migration_status'));
    PREPARE gxp_audit_core_column_stmt FROM @gxp_audit_core_column_sql;
    EXECUTE gxp_audit_core_column_stmt;
    DEALLOCATE PREPARE gxp_audit_core_column_stmt;
END$$

CREATE PROCEDURE `ensure_gxp_audit_core_index`(
    IN target_table varchar(64),
    IN target_index varchar(64),
    IN ddl_statement text
)
BEGIN
    SELECT COUNT(1) INTO @gxp_audit_core_index_count
      FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = target_table
       AND INDEX_NAME = target_index;
    SET @gxp_audit_core_index_sql = IF(@gxp_audit_core_index_count = 0, ddl_statement,
        CONCAT('SELECT ''', target_table, '.', target_index, ' already exists'' AS migration_status'));
    PREPARE gxp_audit_core_index_stmt FROM @gxp_audit_core_index_sql;
    EXECUTE gxp_audit_core_index_stmt;
    DEALLOCATE PREPARE gxp_audit_core_index_stmt;
END$$

CREATE PROCEDURE `drop_gxp_audit_core_index`(
    IN target_table varchar(64),
    IN target_index varchar(64)
)
BEGIN
    SELECT COUNT(1) INTO @gxp_audit_core_drop_index_count
      FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = target_table
       AND INDEX_NAME = target_index;
    SET @gxp_audit_core_drop_index_sql = IF(@gxp_audit_core_drop_index_count = 0,
        CONCAT('SELECT ''', target_table, '.', target_index, ' already absent'' AS migration_status'),
        CONCAT('ALTER TABLE `', target_table, '` DROP INDEX `', target_index, '`'));
    PREPARE gxp_audit_core_drop_index_stmt FROM @gxp_audit_core_drop_index_sql;
    EXECUTE gxp_audit_core_drop_index_stmt;
    DEALLOCATE PREPARE gxp_audit_core_drop_index_stmt;
END$$
DELIMITER ;

CALL ensure_gxp_audit_core_column('gxp_audit_event', 'subject_id',
    'ALTER TABLE `gxp_audit_event` ADD COLUMN `subject_id` varchar(2048) NOT NULL DEFAULT '''' COMMENT ''对象编号'' AFTER `subject_type`');
CALL ensure_gxp_audit_core_column('gxp_audit_event', 'idempotency_key',
    'ALTER TABLE `gxp_audit_event` ADD COLUMN `idempotency_key` varchar(512) NOT NULL DEFAULT '''' COMMENT ''幂等键'' AFTER `policy_version`');
CALL ensure_gxp_audit_core_column('gxp_audit_event', 'create_time',
    'ALTER TABLE `gxp_audit_event` ADD COLUMN `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'' AFTER `algorithm`');
CALL ensure_gxp_audit_core_column('gxp_audit_event', 'update_time',
    'ALTER TABLE `gxp_audit_event` ADD COLUMN `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'' AFTER `create_time`');
CALL ensure_gxp_audit_core_column('gxp_audit_event', 'creator',
    'ALTER TABLE `gxp_audit_event` ADD COLUMN `creator` varchar(64) DEFAULT NULL COMMENT ''创建者'' AFTER `update_time`');
CALL ensure_gxp_audit_core_column('gxp_audit_event', 'updater',
    'ALTER TABLE `gxp_audit_event` ADD COLUMN `updater` varchar(64) DEFAULT NULL COMMENT ''更新者'' AFTER `creator`');
CALL ensure_gxp_audit_core_column('gxp_audit_event', 'deleted',
    'ALTER TABLE `gxp_audit_event` ADD COLUMN `deleted` bit(1) NOT NULL DEFAULT b''0'' COMMENT ''是否删除'' AFTER `updater`');

ALTER TABLE `gxp_audit_event`
    MODIFY COLUMN `subject_id` varchar(2048) NOT NULL COMMENT '对象编号',
    MODIFY COLUMN `idempotency_key` varchar(512) NOT NULL COMMENT '幂等键';

CALL drop_gxp_audit_core_index('gxp_audit_event', 'uk_gxp_audit_event_idempotency');
CALL drop_gxp_audit_core_index('gxp_audit_event', 'idx_gxp_audit_event_subject');
CALL ensure_gxp_audit_core_index('gxp_audit_event', 'uk_gxp_audit_event_idempotency',
    'ALTER TABLE `gxp_audit_event` ADD UNIQUE KEY `uk_gxp_audit_event_idempotency` (`tenant_id`, `idempotency_key`)');
CALL ensure_gxp_audit_core_index('gxp_audit_event', 'idx_gxp_audit_event_subject',
    'ALTER TABLE `gxp_audit_event` ADD KEY `idx_gxp_audit_event_subject` (`tenant_id`, `domain`, `subject_type`, `subject_id`(191), `server_occurred_at`)');

CALL ensure_gxp_audit_core_column('gxp_audit_policy_operation', 'create_time',
    'ALTER TABLE `gxp_audit_policy_operation` ADD COLUMN `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'' AFTER `active`');
CALL ensure_gxp_audit_core_column('gxp_audit_policy_operation', 'update_time',
    'ALTER TABLE `gxp_audit_policy_operation` ADD COLUMN `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'' AFTER `create_time`');
CALL ensure_gxp_audit_core_column('gxp_audit_policy_operation', 'creator',
    'ALTER TABLE `gxp_audit_policy_operation` ADD COLUMN `creator` varchar(64) DEFAULT NULL COMMENT ''创建者'' AFTER `update_time`');
CALL ensure_gxp_audit_core_column('gxp_audit_policy_operation', 'updater',
    'ALTER TABLE `gxp_audit_policy_operation` ADD COLUMN `updater` varchar(64) DEFAULT NULL COMMENT ''更新者'' AFTER `creator`');
CALL ensure_gxp_audit_core_column('gxp_audit_policy_operation', 'deleted',
    'ALTER TABLE `gxp_audit_policy_operation` ADD COLUMN `deleted` bit(1) NOT NULL DEFAULT b''0'' COMMENT ''是否删除'' AFTER `updater`');
CALL ensure_gxp_audit_core_index('gxp_audit_policy_operation', 'idx_gxp_audit_policy_operation_source',
    'ALTER TABLE `gxp_audit_policy_operation` ADD KEY `idx_gxp_audit_policy_operation_source` (`tenant_id`, `source_type`, `source_locator`)');
CALL ensure_gxp_audit_core_index('gxp_audit_policy_operation', 'idx_gxp_audit_policy_operation_active',
    'ALTER TABLE `gxp_audit_policy_operation` ADD KEY `idx_gxp_audit_policy_operation_active` (`tenant_id`, `active`, `operation_id`)');

DROP PROCEDURE IF EXISTS `ensure_gxp_audit_core_column`;
DROP PROCEDURE IF EXISTS `ensure_gxp_audit_core_index`;
DROP PROCEDURE IF EXISTS `drop_gxp_audit_core_index`;

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
