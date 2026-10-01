-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260908_gxp_audit_trail_core; type=schema; riskLevel=high
-- GXP_EVENT_V2 is an additive migration. It does not rewrite V1 event payloads or delete audit rows.

DROP PROCEDURE IF EXISTS `ensure_gxp_audit_v2_column`;
DROP PROCEDURE IF EXISTS `ensure_gxp_audit_v2_index`;
DELIMITER $$
CREATE PROCEDURE `ensure_gxp_audit_v2_column`(
    IN target_table varchar(64),
    IN target_column varchar(64),
    IN ddl_statement text
)
BEGIN
    SELECT COUNT(1) INTO @gxp_v2_column_count
      FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = target_table
       AND COLUMN_NAME = target_column;
    IF @gxp_v2_column_count = 0 THEN
        SET @gxp_v2_column_sql = ddl_statement;
        PREPARE gxp_v2_column_stmt FROM @gxp_v2_column_sql;
        EXECUTE gxp_v2_column_stmt;
        DEALLOCATE PREPARE gxp_v2_column_stmt;
    END IF;
END$$

CREATE PROCEDURE `ensure_gxp_audit_v2_index`(
    IN target_table varchar(64),
    IN target_index varchar(64),
    IN ddl_statement text
)
BEGIN
    SELECT COUNT(1) INTO @gxp_v2_index_count
      FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = target_table
       AND INDEX_NAME = target_index;
    IF @gxp_v2_index_count = 0 THEN
        SET @gxp_v2_index_sql = ddl_statement;
        PREPARE gxp_v2_index_stmt FROM @gxp_v2_index_sql;
        EXECUTE gxp_v2_index_stmt;
        DEALLOCATE PREPARE gxp_v2_index_stmt;
    END IF;
END$$
DELIMITER ;

CALL ensure_gxp_audit_v2_column('gxp_audit_event', 'event_schema_version',
    'ALTER TABLE `gxp_audit_event` ADD COLUMN `event_schema_version` smallint unsigned NULL COMMENT ''事件协议版本'' AFTER `algorithm`');
CALL ensure_gxp_audit_v2_column('gxp_audit_event', 'canonicalization_version',
    'ALTER TABLE `gxp_audit_event` ADD COLUMN `canonicalization_version` varchar(32) CHARACTER SET ascii NULL COMMENT ''规范化版本'' AFTER `event_schema_version`');
CALL ensure_gxp_audit_v2_column('gxp_audit_event', 'result_status',
    'ALTER TABLE `gxp_audit_event` ADD COLUMN `result_status` varchar(16) CHARACTER SET ascii NULL COMMENT ''结果状态'' AFTER `canonicalization_version`');
CALL ensure_gxp_audit_v2_column('gxp_audit_event', 'reason_code',
    'ALTER TABLE `gxp_audit_event` ADD COLUMN `reason_code` varchar(64) CHARACTER SET ascii NULL COMMENT ''原因代码'' AFTER `result_status`');
CALL ensure_gxp_audit_v2_column('gxp_audit_event', 'reason_source',
    'ALTER TABLE `gxp_audit_event` ADD COLUMN `reason_source` varchar(16) CHARACTER SET ascii NULL COMMENT ''原因来源'' AFTER `reason_code`');
CALL ensure_gxp_audit_v2_column('gxp_audit_event', 'transaction_id',
    'ALTER TABLE `gxp_audit_event` ADD COLUMN `transaction_id` varchar(64) CHARACTER SET ascii NULL COMMENT ''服务器事务标识'' AFTER `reason_source`');
CALL ensure_gxp_audit_v2_column('gxp_audit_event', 'authenticated_actor_json',
    'ALTER TABLE `gxp_audit_event` ADD COLUMN `authenticated_actor_json` longtext NULL COMMENT ''认证身份快照'' AFTER `transaction_id`');
CALL ensure_gxp_audit_v2_column('gxp_audit_event', 'performed_by_json',
    'ALTER TABLE `gxp_audit_event` ADD COLUMN `performed_by_json` longtext NULL COMMENT ''实际执行人快照'' AFTER `authenticated_actor_json`');
CALL ensure_gxp_audit_v2_column('gxp_audit_event', 'source_type',
    'ALTER TABLE `gxp_audit_event` ADD COLUMN `source_type` varchar(64) CHARACTER SET ascii NULL COMMENT ''入口类型'' AFTER `performed_by_json`');
CALL ensure_gxp_audit_v2_column('gxp_audit_event', 'source_locator',
    'ALTER TABLE `gxp_audit_event` ADD COLUMN `source_locator` varchar(512) CHARACTER SET ascii NULL COMMENT ''入口定位'' AFTER `source_type`');
CALL ensure_gxp_audit_v2_column('gxp_audit_event', 'trace_id',
    'ALTER TABLE `gxp_audit_event` ADD COLUMN `trace_id` varchar(128) CHARACTER SET ascii NULL COMMENT ''追踪标识'' AFTER `source_locator`');
CALL ensure_gxp_audit_v2_column('gxp_audit_event', 'error_code',
    'ALTER TABLE `gxp_audit_event` ADD COLUMN `error_code` varchar(128) CHARACTER SET ascii NULL COMMENT ''失败错误码'' AFTER `trace_id`');
CALL ensure_gxp_audit_v2_column('gxp_audit_event', 'attempted_operation_id',
    'ALTER TABLE `gxp_audit_event` ADD COLUMN `attempted_operation_id` varchar(128) CHARACTER SET ascii NULL COMMENT ''失败目标动作'' AFTER `error_code`');
CALL ensure_gxp_audit_v2_column('gxp_audit_event', 'relation_manifest_json',
    'ALTER TABLE `gxp_audit_event` ADD COLUMN `relation_manifest_json` longtext NULL COMMENT ''规范关联清单'' AFTER `attempted_operation_id`');
CALL ensure_gxp_audit_v2_column('gxp_audit_event', 'evidence_manifest_json',
    'ALTER TABLE `gxp_audit_event` ADD COLUMN `evidence_manifest_json` longtext NULL COMMENT ''规范证据清单'' AFTER `relation_manifest_json`');
CALL ensure_gxp_audit_v2_column('gxp_audit_event', 'state_payload_hash',
    'ALTER TABLE `gxp_audit_event` ADD COLUMN `state_payload_hash` char(64) CHARACTER SET ascii NULL COMMENT ''状态载荷 Hash'' AFTER `evidence_manifest_json`');

ALTER TABLE `gxp_audit_event`
    MODIFY COLUMN `reason` varchar(2000) NOT NULL COMMENT '变更原因',
    MODIFY COLUMN `server_occurred_at` datetime(3) NOT NULL COMMENT '服务器审计时间';
CALL ensure_gxp_audit_v2_index('gxp_audit_event', 'uk_gxp_event_tenant_id',
    'ALTER TABLE `gxp_audit_event` ADD UNIQUE KEY `uk_gxp_event_tenant_id` (`tenant_id`, `id`)');
CALL ensure_gxp_audit_v2_index('gxp_audit_event', 'idx_gxp_event_result',
    'ALTER TABLE `gxp_audit_event` ADD KEY `idx_gxp_event_result` (`tenant_id`, `result_status`, `ledger_sequence`)');
CALL ensure_gxp_audit_v2_index('gxp_audit_event', 'idx_gxp_event_request',
    'ALTER TABLE `gxp_audit_event` ADD KEY `idx_gxp_event_request` (`tenant_id`, `request_id`)');

CALL ensure_gxp_audit_v2_column('gxp_audit_policy_version', 'schema_version',
    'ALTER TABLE `gxp_audit_policy_version` ADD COLUMN `schema_version` varchar(32) CHARACTER SET ascii NULL COMMENT ''策略 schema 版本'' AFTER `coverage_report_hash`');
CALL ensure_gxp_audit_v2_column('gxp_audit_policy_version', 'canonical_policy_json',
    'ALTER TABLE `gxp_audit_policy_version` ADD COLUMN `canonical_policy_json` longtext NULL COMMENT ''规范策略快照'' AFTER `schema_version`');
CALL ensure_gxp_audit_v2_column('gxp_audit_policy_version', 'artifact_hash',
    'ALTER TABLE `gxp_audit_policy_version` ADD COLUMN `artifact_hash` char(64) CHARACTER SET ascii NULL COMMENT ''策略原始文件 Hash'' AFTER `canonical_policy_json`');

CREATE TABLE IF NOT EXISTS `gxp_audit_event_relation` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '关联索引编号',
    `tenant_id` bigint NOT NULL COMMENT '租户编号',
    `event_id` bigint NOT NULL COMMENT '审计事件编号',
    `relation_type` varchar(32) CHARACTER SET ascii NOT NULL COMMENT '关联类型',
    `target_type` varchar(64) CHARACTER SET ascii NOT NULL COMMENT '目标类型',
    `target_id` varchar(128) CHARACTER SET ascii NOT NULL COMMENT '目标稳定编号',
    `target_version` varchar(128) CHARACTER SET ascii DEFAULT NULL COMMENT '目标版本',
    `target_hash` char(64) CHARACTER SET ascii DEFAULT NULL COMMENT '目标 Hash',
    `created_at_utc` datetime(3) NOT NULL COMMENT '创建时间 UTC',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_gxp_relation` (`tenant_id`, `event_id`, `relation_type`, `target_type`, `target_id`),
    KEY `idx_gxp_relation_target` (`tenant_id`, `relation_type`, `target_type`, `target_id`, `event_id`),
    CONSTRAINT `fk_gxp_relation_event` FOREIGN KEY (`tenant_id`, `event_id`)
        REFERENCES `gxp_audit_event` (`tenant_id`, `id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='GxP 审计事件规范关联索引';

CREATE TABLE IF NOT EXISTS `gxp_audit_policy_activation` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '策略激活编号',
    `tenant_id` bigint NOT NULL COMMENT '租户编号',
    `policy_version` varchar(128) CHARACTER SET ascii NOT NULL COMMENT '策略版本',
    `policy_hash` char(64) CHARACTER SET ascii NOT NULL COMMENT '策略 Hash',
    `previous_activation_hash` char(64) CHARACTER SET ascii DEFAULT NULL COMMENT '前激活 Hash',
    `request_id` varchar(128) CHARACTER SET ascii NOT NULL COMMENT '激活请求编号',
    `actor_id` bigint NOT NULL COMMENT '批准/激活人',
    `approval_reference` varchar(256) NOT NULL COMMENT '批准依据',
    `activated_at_utc` datetime(3) NOT NULL COMMENT '激活时间 UTC',
    `effective_after_sequence` bigint NOT NULL COMMENT '生效前账本序号',
    `canonical_activation_json` longtext NOT NULL COMMENT '规范激活快照',
    `activation_hash` char(64) CHARACTER SET ascii NOT NULL COMMENT '激活 Hash',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_gxp_activation_request` (`tenant_id`, `request_id`),
    UNIQUE KEY `uk_gxp_activation_hash` (`tenant_id`, `activation_hash`),
    KEY `idx_gxp_activation_tenant_id` (`tenant_id`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='GxP 审计策略激活历史';

CREATE TABLE IF NOT EXISTS `gxp_audit_legacy_fact_baseline` (
    `tenant_id` bigint NOT NULL COMMENT '租户编号',
    `source_type` varchar(64) CHARACTER SET ascii NOT NULL COMMENT '事实来源类型',
    `source_id` varchar(128) CHARACTER SET ascii NOT NULL COMMENT '事实来源编号',
    `source_hash` char(64) CHARACTER SET ascii NOT NULL COMMENT '事实来源 Hash',
    `policy_version` varchar(128) CHARACTER SET ascii NOT NULL COMMENT '策略版本',
    `captured_at_utc` datetime(3) NOT NULL COMMENT '采集时间 UTC',
    `approval_reference` varchar(256) NOT NULL COMMENT '批准依据',
    `manifest_hash` char(64) CHARACTER SET ascii NOT NULL COMMENT '基线清单 Hash',
    PRIMARY KEY (`tenant_id`, `source_type`, `source_id`, `policy_version`),
    KEY `idx_gxp_legacy_baseline_source` (`tenant_id`, `source_type`, `source_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='GxP 历史事实基线';

DROP TRIGGER IF EXISTS `trg_gxp_audit_event_relation_no_update`;
DROP TRIGGER IF EXISTS `trg_gxp_audit_event_relation_no_delete`;
DROP TRIGGER IF EXISTS `trg_gxp_audit_policy_activation_no_update`;
DROP TRIGGER IF EXISTS `trg_gxp_audit_policy_activation_no_delete`;
DROP TRIGGER IF EXISTS `trg_gxp_audit_legacy_baseline_no_update`;
DROP TRIGGER IF EXISTS `trg_gxp_audit_legacy_baseline_no_delete`;
DELIMITER $$
CREATE TRIGGER `trg_gxp_audit_event_relation_no_update`
BEFORE UPDATE ON `gxp_audit_event_relation` FOR EACH ROW
BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'GxP audit event relation is append-only'; END$$
CREATE TRIGGER `trg_gxp_audit_event_relation_no_delete`
BEFORE DELETE ON `gxp_audit_event_relation` FOR EACH ROW
BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'GxP audit event relation is append-only'; END$$
CREATE TRIGGER `trg_gxp_audit_policy_activation_no_update`
BEFORE UPDATE ON `gxp_audit_policy_activation` FOR EACH ROW
BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'GxP audit policy activation is append-only'; END$$
CREATE TRIGGER `trg_gxp_audit_policy_activation_no_delete`
BEFORE DELETE ON `gxp_audit_policy_activation` FOR EACH ROW
BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'GxP audit policy activation is append-only'; END$$
CREATE TRIGGER `trg_gxp_audit_legacy_baseline_no_update`
BEFORE UPDATE ON `gxp_audit_legacy_fact_baseline` FOR EACH ROW
BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'GxP legacy baseline is append-only'; END$$
CREATE TRIGGER `trg_gxp_audit_legacy_baseline_no_delete`
BEFORE DELETE ON `gxp_audit_legacy_fact_baseline` FOR EACH ROW
BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'GxP legacy baseline is append-only'; END$$
DELIMITER ;

DROP PROCEDURE IF EXISTS `ensure_gxp_audit_v2_column`;
DROP PROCEDURE IF EXISTS `ensure_gxp_audit_v2_index`;
