-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- MES schema repair for MySQL.
-- Covers the tables touched by MES material item pages and MES home statistics.
-- Safe to run repeatedly: creates missing tables only and does not delete data.

CREATE TABLE IF NOT EXISTS `mes_md_item_type` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(64) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `parent_id` bigint DEFAULT 0,
  `item_or_product` varchar(32) DEFAULT NULL,
  `sort` int DEFAULT 0,
  `status` tinyint DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_mes_md_item_type_tenant_id` (`tenant_id`),
  KEY `idx_mes_md_item_type_parent_id` (`parent_id`),
  KEY `idx_mes_md_item_type_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MesMdItemTypeDO';

CREATE TABLE IF NOT EXISTS `mes_md_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(64) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `specification` varchar(512) DEFAULT NULL,
  `unit_measure_id` bigint DEFAULT NULL,
  `item_type_id` bigint DEFAULT NULL,
  `status` tinyint DEFAULT NULL,
  `safe_stock_flag` bit(1) DEFAULT NULL,
  `min_stock` decimal(24,6) DEFAULT NULL,
  `max_stock` decimal(24,6) DEFAULT NULL,
  `high_value` bit(1) DEFAULT NULL,
  `batch_flag` bit(1) DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_mes_md_item_tenant_id` (`tenant_id`),
  KEY `idx_mes_md_item_code` (`code`),
  KEY `idx_mes_md_item_item_type_id` (`item_type_id`),
  KEY `idx_mes_md_item_unit_measure_id` (`unit_measure_id`),
  KEY `idx_mes_md_item_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MesMdItemDO';

CREATE TABLE IF NOT EXISTS `mes_pro_work_order` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(64) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `type` tinyint DEFAULT NULL,
  `order_source_type` tinyint DEFAULT NULL,
  `order_source_code` varchar(64) DEFAULT NULL,
  `product_id` bigint DEFAULT NULL,
  `quantity` decimal(14,2) DEFAULT NULL,
  `quantity_produced` decimal(14,2) DEFAULT NULL,
  `quantity_changed` decimal(14,2) DEFAULT NULL,
  `quantity_scheduled` decimal(14,2) DEFAULT NULL,
  `client_id` bigint DEFAULT NULL,
  `vendor_id` bigint DEFAULT NULL,
  `batch_code` varchar(64) DEFAULT NULL,
  `request_date` datetime DEFAULT NULL,
  `parent_id` bigint DEFAULT 0,
  `finish_date` datetime DEFAULT NULL,
  `cancel_date` datetime DEFAULT NULL,
  `status` tinyint DEFAULT NULL,
  `temporary_frozen` bit(1) NOT NULL DEFAULT b'0',
  `remark` varchar(500) DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_mes_pro_work_order_tenant_id` (`tenant_id`),
  KEY `idx_mes_pro_work_order_code` (`code`),
  KEY `idx_mes_pro_work_order_status` (`status`),
  KEY `idx_mes_pro_work_order_product_id` (`product_id`),
  KEY `idx_mes_pro_work_order_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MesProWorkOrderDO';

CREATE TABLE IF NOT EXISTS `mes_pro_schedule_calendar_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `skip_statutory_holidays` bit(1) NOT NULL DEFAULT b'0',
  `weekend_rest_mode` varchar(16) NOT NULL,
  `date_shift_mode_by_date_json` text DEFAULT NULL,
  `temporary_freeze_enabled` bit(1) NOT NULL DEFAULT b'0',
  `remark` varchar(500) DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_schedule_calendar_rule_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES schedule calendar rule';

SET @mes_pro_work_order_has_temporary_frozen := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'mes_pro_work_order'
    AND COLUMN_NAME = 'temporary_frozen'
);
SET @mes_pro_work_order_temporary_frozen_sql := IF(
  @mes_pro_work_order_has_temporary_frozen = 0,
  'ALTER TABLE `mes_pro_work_order` ADD COLUMN `temporary_frozen` bit(1) NOT NULL DEFAULT b''0'' AFTER `status`',
  'SELECT 1'
);
PREPARE mes_pro_work_order_temporary_frozen_stmt FROM @mes_pro_work_order_temporary_frozen_sql;
EXECUTE mes_pro_work_order_temporary_frozen_stmt;
DEALLOCATE PREPARE mes_pro_work_order_temporary_frozen_stmt;

SET @mes_pro_schedule_calendar_rule_has_temporary_freeze_enabled := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'mes_pro_schedule_calendar_rule'
    AND COLUMN_NAME = 'temporary_freeze_enabled'
);
SET @mes_pro_schedule_calendar_rule_temporary_freeze_enabled_sql := IF(
  @mes_pro_schedule_calendar_rule_has_temporary_freeze_enabled = 0,
  'ALTER TABLE `mes_pro_schedule_calendar_rule` ADD COLUMN `temporary_freeze_enabled` bit(1) NOT NULL DEFAULT b''0'' AFTER `date_shift_mode_by_date_json`',
  'SELECT 1'
);
PREPARE mes_pro_schedule_calendar_rule_temporary_freeze_enabled_stmt FROM @mes_pro_schedule_calendar_rule_temporary_freeze_enabled_sql;
EXECUTE mes_pro_schedule_calendar_rule_temporary_freeze_enabled_stmt;
DEALLOCATE PREPARE mes_pro_schedule_calendar_rule_temporary_freeze_enabled_stmt;

CREATE TABLE IF NOT EXISTS `mes_pro_feedback` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(64) DEFAULT NULL,
  `type` tinyint DEFAULT NULL,
  `channel` varchar(32) DEFAULT NULL,
  `feedback_time` datetime DEFAULT NULL,
  `workstation_id` bigint DEFAULT NULL,
  `route_id` bigint DEFAULT NULL,
  `process_id` bigint DEFAULT NULL,
  `work_order_id` bigint DEFAULT NULL,
  `task_id` bigint DEFAULT NULL,
  `schedule_order_id` bigint DEFAULT NULL,
  `schedule_order_process_id` bigint DEFAULT NULL,
  `source_import_record_id` bigint DEFAULT NULL,
  `item_id` bigint DEFAULT NULL,
  `expire_date` datetime DEFAULT NULL,
  `lot_number` varchar(64) DEFAULT NULL,
  `scheduled_quantity` decimal(14,2) DEFAULT NULL,
  `feedback_quantity` decimal(14,2) DEFAULT NULL,
  `qualified_quantity` decimal(14,2) DEFAULT NULL,
  `unqualified_quantity` decimal(14,2) DEFAULT NULL,
  `uncheck_quantity` decimal(14,2) DEFAULT NULL,
  `labor_scrap_quantity` decimal(14,2) DEFAULT NULL,
  `material_scrap_quantity` decimal(14,2) DEFAULT NULL,
  `other_scrap_quantity` decimal(14,2) DEFAULT NULL,
  `feedback_user_id` bigint DEFAULT NULL,
  `approve_user_id` bigint DEFAULT NULL,
  `status` tinyint DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_mes_pro_feedback_tenant_id` (`tenant_id`),
  KEY `idx_mes_pro_feedback_status_time` (`status`, `feedback_time`),
  KEY `idx_mes_pro_feedback_work_order_id` (`work_order_id`),
  KEY `idx_mes_pro_feedback_task_id` (`task_id`),
  KEY `idx_mes_pro_feedback_source_import_record_id` (`source_import_record_id`),
  KEY `idx_mes_pro_feedback_item_id` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MesProFeedbackDO';

CREATE TABLE IF NOT EXISTS `mes_pro_feedback_import_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `source_file_name` varchar(255) NOT NULL,
  `source_file_sha256` char(64) NOT NULL,
  `sheet_name` varchar(128) NOT NULL,
  `row_no` int NOT NULL,
  `feedback_id` bigint NOT NULL,
  `attribution_status` varchar(32) NOT NULL DEFAULT 'PENDING',
  `task_code` varchar(64) NOT NULL,
  `work_order_code` varchar(64) DEFAULT NULL,
  `item_code` varchar(64) DEFAULT NULL,
  `process_code` varchar(64) DEFAULT NULL,
  `source_payload_json` longtext DEFAULT NULL,
  `schedule_order_id` bigint DEFAULT NULL,
  `schedule_order_process_id` bigint DEFAULT NULL,
  `attribution_target_type` varchar(64) DEFAULT NULL,
  `candidate_count` int DEFAULT NULL,
  `progress_source_type` varchar(64) DEFAULT NULL,
  `progress_quantity` decimal(18,6) DEFAULT NULL,
  `progress_applied_time` datetime DEFAULT NULL,
  `progress_warning_code` varchar(64) DEFAULT NULL,
  `progress_warning_message` varchar(500) DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_mes_pro_feedback_import_record_source_row` (`source_file_sha256`, `sheet_name`, `row_no`),
  KEY `idx_mes_feedback_import_record_direct_progress` (`tenant_id`, `schedule_order_id`, `progress_source_type`, `attribution_status`, `schedule_order_process_id`),
  KEY `idx_mes_pro_feedback_import_record_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MesProFeedbackImportRecordDO';

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_flow_intervention` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `intervention_code` varchar(64) NOT NULL COMMENT '干预编号',
  `business_object_type` varchar(64) NOT NULL COMMENT '业务对象类型',
  `business_object_id` varchar(128) NOT NULL COMMENT '业务对象ID',
  `business_object_code` varchar(128) DEFAULT NULL COMMENT '业务对象编号',
  `flow_instance_id` varchar(128) DEFAULT NULL COMMENT '流程实例ID',
  `intervention_action` varchar(32) NOT NULL COMMENT '干预动作',
  `intervention_status` varchar(32) NOT NULL COMMENT '干预状态',
  `from_status` varchar(32) NOT NULL COMMENT '原状态',
  `to_status` varchar(32) NOT NULL COMMENT '目标状态',
  `source_task_id` varchar(128) DEFAULT NULL COMMENT '来源任务ID',
  `target_task_id` varchar(128) DEFAULT NULL COMMENT '目标任务ID',
  `node_key` varchar(128) DEFAULT NULL COMMENT '节点标识',
  `target_user_id` bigint DEFAULT NULL COMMENT '目标处理人',
  `requested_by` bigint DEFAULT NULL COMMENT '申请人',
  `requested_at` datetime NOT NULL COMMENT '申请时间',
  `reason_category` varchar(64) DEFAULT NULL COMMENT '原因分类',
  `reason` varchar(500) NOT NULL COMMENT '原因',
  `authorization_basis` varchar(500) DEFAULT NULL COMMENT '授权依据',
  `signoff_evidence_hash` char(64) NOT NULL COMMENT '签核证据摘要',
  `idempotency_key` varchar(128) NOT NULL COMMENT '幂等键',
  `integrity_check_result` varchar(32) NOT NULL COMMENT '完整性复检结果',
  `integrity_check_snapshot_json` longtext DEFAULT NULL COMMENT '完整性复检快照JSON',
  `evidence_hash` char(64) NOT NULL COMMENT '干预证据摘要',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_edhr_flow_intervention_idempotency` (`tenant_id`, `business_object_type`, `business_object_id`, `intervention_action`, `idempotency_key`, `deleted`),
  KEY `idx_mes_pro_edhr_flow_intervention_object` (`tenant_id`, `business_object_type`, `business_object_id`, `requested_at`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES eDHR 流程干预';

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_flow_event` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `business_object_type` varchar(64) NOT NULL COMMENT '业务对象类型',
  `business_object_id` varchar(128) NOT NULL COMMENT '业务对象ID',
  `business_object_code` varchar(128) DEFAULT NULL COMMENT '业务对象编号',
  `intervention_id` bigint DEFAULT NULL COMMENT '流程干预ID',
  `flow_instance_id` varchar(128) DEFAULT NULL COMMENT '流程实例ID',
  `task_id` varchar(128) DEFAULT NULL COMMENT '流程任务ID',
  `node_key` varchar(128) DEFAULT NULL COMMENT '节点标识',
  `event_type` varchar(32) NOT NULL COMMENT '事件类型',
  `from_status` varchar(32) NOT NULL COMMENT '原状态',
  `to_status` varchar(32) NOT NULL COMMENT '目标状态',
  `actor_user_id` bigint DEFAULT NULL COMMENT '操作人',
  `target_user_id` bigint DEFAULT NULL COMMENT '目标处理人',
  `permission_code` varchar(128) NOT NULL COMMENT '权限编码',
  `permission_decision` varchar(32) NOT NULL COMMENT '权限判定',
  `reason` varchar(500) DEFAULT NULL COMMENT '原因',
  `signoff_evidence_hash` char(64) DEFAULT NULL COMMENT '签核证据摘要',
  `integrity_check_result` varchar(32) NOT NULL COMMENT '完整性复检结果',
  `integrity_check_snapshot_json` longtext DEFAULT NULL COMMENT '完整性复检快照JSON',
  `event_snapshot_json` longtext DEFAULT NULL COMMENT '事件快照JSON',
  `evidence_hash` char(64) NOT NULL COMMENT '事件证据摘要',
  `occurred_at` datetime NOT NULL COMMENT '事件发生时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_mes_pro_edhr_flow_event_object` (`tenant_id`, `business_object_type`, `business_object_id`, `occurred_at`, `deleted`),
  KEY `idx_mes_pro_edhr_flow_event_instance` (`tenant_id`, `flow_instance_id`, `task_id`, `occurred_at`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES eDHR 流程事件';

CREATE TABLE IF NOT EXISTS `mes_dv_machinery` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(64) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `brand` varchar(128) DEFAULT NULL,
  `specification` varchar(255) DEFAULT NULL,
  `machinery_type_id` bigint DEFAULT NULL,
  `workshop_id` bigint DEFAULT NULL,
  `process_name` varchar(255) DEFAULT NULL,
  `standard_hourly_capacity` decimal(18,6) DEFAULT NULL,
  `status` tinyint DEFAULT NULL,
  `last_mainten_time` datetime DEFAULT NULL,
  `last_check_time` datetime DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_mes_dv_machinery_tenant_id` (`tenant_id`),
  KEY `idx_mes_dv_machinery_code` (`code`),
  KEY `idx_mes_dv_machinery_status` (`status`),
  KEY `idx_mes_dv_machinery_type_id` (`machinery_type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MesDvMachineryDO';

CREATE TABLE IF NOT EXISTS `mes_dv_machinery_process` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `machinery_id` bigint DEFAULT NULL,
  `process_id` bigint DEFAULT NULL,
  `machinery_code` varchar(64) DEFAULT NULL,
  `line_name` varchar(255) DEFAULT NULL,
  `process_name` varchar(255) DEFAULT NULL,
  `device_name` varchar(255) DEFAULT NULL,
  `device_quantity` decimal(18,6) DEFAULT NULL,
  `ten_half_hour_daily_capacity` decimal(18,6) DEFAULT NULL,
  `standard_hourly_capacity` decimal(18,6) DEFAULT NULL,
  `source_row_no` int DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_mes_dv_machinery_process_machinery_id` (`machinery_id`),
  KEY `idx_mes_dv_machinery_process_code` (`machinery_code`),
  KEY `idx_mes_dv_machinery_process_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MesDvMachineryProcessDO';

CREATE TABLE IF NOT EXISTS `mes_pro_andon_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `config_id` bigint DEFAULT NULL,
  `workstation_id` bigint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `work_order_id` bigint DEFAULT NULL,
  `process_id` bigint DEFAULT NULL,
  `reason` varchar(500) DEFAULT NULL,
  `level` tinyint DEFAULT NULL,
  `status` tinyint DEFAULT NULL,
  `handle_time` datetime DEFAULT NULL,
  `handler_user_id` bigint DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_mes_pro_andon_record_tenant_id` (`tenant_id`),
  KEY `idx_mes_pro_andon_record_status` (`status`),
  KEY `idx_mes_pro_andon_record_work_order_id` (`work_order_id`),
  KEY `idx_mes_pro_andon_record_workstation_id` (`workstation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MesProAndonRecordDO';

CREATE TABLE IF NOT EXISTS `mes_dv_repair` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(64) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `machinery_id` bigint DEFAULT NULL,
  `require_date` datetime DEFAULT NULL,
  `finish_date` datetime DEFAULT NULL,
  `confirm_date` datetime DEFAULT NULL,
  `result` tinyint DEFAULT NULL,
  `accepted_user_id` bigint DEFAULT NULL,
  `confirm_user_id` bigint DEFAULT NULL,
  `source_doc_type` int DEFAULT NULL,
  `source_doc_id` bigint DEFAULT NULL,
  `source_doc_code` varchar(64) DEFAULT NULL,
  `status` tinyint DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_mes_dv_repair_tenant_id` (`tenant_id`),
  KEY `idx_mes_dv_repair_code` (`code`),
  KEY `idx_mes_dv_repair_status` (`status`),
  KEY `idx_mes_dv_repair_machinery_id` (`machinery_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MesDvRepairDO';
