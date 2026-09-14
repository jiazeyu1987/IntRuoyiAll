-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260514_mes_batch_record_report,20260612_mes_edhr_multi_batch_route; type=schema; riskLevel=medium
-- eDHR batch record Word import version approval phase one.
-- Non-destructive migration: only creates version-control tables and adds nullable version references.

DROP PROCEDURE IF EXISTS add_mes_edhr_column_if_missing;
DROP PROCEDURE IF EXISTS add_mes_edhr_column_if_table_exists;
DELIMITER $$
CREATE PROCEDURE add_mes_edhr_column_if_missing(
  IN p_table_name varchar(128),
  IN p_column_name varchar(128),
  IN p_column_ddl text
)
BEGIN
  IF NOT EXISTS (
    SELECT 1
      FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = p_table_name
       AND column_name = p_column_name
  ) THEN
    SET @add_column_sql = CONCAT('ALTER TABLE `', p_table_name, '` ADD COLUMN ', p_column_ddl);
    PREPARE add_column_stmt FROM @add_column_sql;
    EXECUTE add_column_stmt;
    DEALLOCATE PREPARE add_column_stmt;
  END IF;
END$$
DELIMITER ;

DELIMITER $$
CREATE PROCEDURE add_mes_edhr_column_if_table_exists(
  IN p_table_name varchar(128),
  IN p_column_name varchar(128),
  IN p_column_ddl text
)
BEGIN
  IF EXISTS (
    SELECT 1
      FROM information_schema.tables
     WHERE table_schema = DATABASE()
       AND table_name = p_table_name
  ) AND NOT EXISTS (
    SELECT 1
      FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = p_table_name
       AND column_name = p_column_name
  ) THEN
    SET @add_column_sql = CONCAT('ALTER TABLE `', p_table_name, '` ADD COLUMN ', p_column_ddl);
    PREPARE add_column_stmt FROM @add_column_sql;
    EXECUTE add_column_stmt;
    DEALLOCATE PREPARE add_column_stmt;
  END IF;
END$$
DELIMITER ;

CREATE TABLE IF NOT EXISTS `mes_pro_batch_record_definition` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `batch_record_name` varchar(100) NOT NULL COMMENT '批记录名称',
  `route_key` varchar(32) NOT NULL DEFAULT 'LEGACY' COMMENT '识别路线',
  `current_version_id` bigint DEFAULT NULL COMMENT '当前生效版本ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_batch_record_definition_name_route` (`tenant_id`, `batch_record_name`, `route_key`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 批记录定义';

CREATE TABLE IF NOT EXISTS `mes_pro_batch_record_version` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `definition_id` bigint NOT NULL COMMENT '批记录定义ID',
  `version_no` varchar(32) NOT NULL COMMENT '版本号',
  `status` varchar(32) NOT NULL COMMENT '版本状态',
  `source_version_id` bigint DEFAULT NULL COMMENT '来源版本ID',
  `source_file_name` varchar(255) NOT NULL COMMENT '来源文件名',
  `source_file_sha256` char(64) NOT NULL COMMENT '来源文件SHA256',
  `route_id` bigint DEFAULT NULL COMMENT '版本路线ID',
  `source_route_id` bigint DEFAULT NULL COMMENT '来源路线ID',
  `approval_instance_id` varchar(128) DEFAULT NULL COMMENT '审批实例ID',
  `submitted_by` bigint DEFAULT NULL COMMENT '提交人',
  `submitted_at` datetime DEFAULT NULL COMMENT '提交时间',
  `approved_by` bigint DEFAULT NULL COMMENT '审批人',
  `approved_at` datetime DEFAULT NULL COMMENT '审批时间',
  `reject_reason` varchar(500) DEFAULT NULL COMMENT '拒绝原因',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  `pending_hash_scope` char(64) GENERATED ALWAYS AS (
    CASE WHEN `status` IN ('DRAFT', 'PRECHECK_PASSED', 'PENDING_APPROVAL')
      THEN `source_file_sha256`
      ELSE NULL
    END
  ) STORED COMMENT '待处理版本同源文件幂等范围',
  UNIQUE KEY `uk_mes_batch_record_version_no` (`tenant_id`, `definition_id`, `version_no`, `deleted`),
  UNIQUE KEY `uk_mes_batch_record_version_hash_pending` (`tenant_id`, `definition_id`, `pending_hash_scope`, `deleted`),
  KEY `idx_mes_batch_record_version_hash` (`tenant_id`, `definition_id`, `source_file_sha256`, `status`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 批记录版本';

CALL add_mes_edhr_column_if_missing('mes_pro_batch_record_version', 'pending_hash_scope',
  '`pending_hash_scope` char(64) GENERATED ALWAYS AS (CASE WHEN `status` IN (''DRAFT'', ''PRECHECK_PASSED'', ''PENDING_APPROVAL'') THEN `source_file_sha256` ELSE NULL END) STORED COMMENT ''待处理版本同源文件幂等范围''');

SET @drop_old_hash_unique := (
  SELECT IF(COUNT(*) > 0,
    'ALTER TABLE `mes_pro_batch_record_version` DROP INDEX `uk_mes_batch_record_version_hash_pending`',
    'SELECT 1')
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'mes_pro_batch_record_version'
    AND index_name = 'uk_mes_batch_record_version_hash_pending'
    AND column_name = 'source_file_sha256'
);
PREPARE stmt FROM @drop_old_hash_unique;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @create_pending_hash_unique := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `mes_pro_batch_record_version` ADD UNIQUE KEY `uk_mes_batch_record_version_hash_pending` (`tenant_id`, `definition_id`, `pending_hash_scope`, `deleted`)',
    'SELECT 1')
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'mes_pro_batch_record_version'
    AND index_name = 'uk_mes_batch_record_version_hash_pending'
);
PREPARE stmt FROM @create_pending_hash_unique;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @create_hash_lookup := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `mes_pro_batch_record_version` ADD KEY `idx_mes_batch_record_version_hash` (`tenant_id`, `definition_id`, `source_file_sha256`, `status`, `deleted`)',
    'SELECT 1')
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'mes_pro_batch_record_version'
    AND index_name = 'idx_mes_batch_record_version_hash'
);
PREPARE stmt FROM @create_hash_lookup;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `mes_pro_batch_record_version_migration_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `definition_id` bigint NOT NULL COMMENT '批记录定义ID',
  `version_id` bigint NOT NULL COMMENT '目标版本ID',
  `source_version_id` bigint DEFAULT NULL COMMENT '来源版本ID',
  `item_type` varchar(64) NOT NULL COMMENT '迁移项类型',
  `diff_group` varchar(64) NOT NULL DEFAULT 'TABLE' COMMENT '差异分组',
  `diff_type` varchar(32) NOT NULL DEFAULT 'UNCHANGED' COMMENT '差异类型',
  `source_logical_key` varchar(255) NOT NULL COMMENT '来源逻辑键',
  `target_logical_key` varchar(255) DEFAULT NULL COMMENT '目标逻辑键',
  `match_confidence` decimal(5,4) DEFAULT NULL COMMENT '匹配置信度',
  `match_evidence_json` longtext COMMENT '匹配证据JSON',
  `risk_level` varchar(32) NOT NULL COMMENT '风险等级',
  `rule_type` varchar(64) DEFAULT NULL COMMENT '规则类型',
  `business_owner_type` varchar(32) DEFAULT NULL COMMENT '业务负责人类型',
  `confirmed` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否已确认',
  `confirmed_by` bigint DEFAULT NULL COMMENT '确认人',
  `confirmed_at` datetime DEFAULT NULL COMMENT '确认时间',
  `confirm_comment` varchar(500) DEFAULT NULL COMMENT '确认意见',
  `confirm_idempotency_key` varchar(128) DEFAULT NULL COMMENT '确认幂等键',
  `message` varchar(500) DEFAULT NULL COMMENT '提示信息',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_mes_batch_record_migration_version` (`tenant_id`, `version_id`, `risk_level`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 批记录版本迁移证据';

CALL add_mes_edhr_column_if_missing('mes_pro_batch_record_version_migration_item', 'diff_group',
  '`diff_group` varchar(64) NOT NULL DEFAULT ''TABLE'' COMMENT ''差异分组''');
CALL add_mes_edhr_column_if_missing('mes_pro_batch_record_version_migration_item', 'diff_type',
  '`diff_type` varchar(32) NOT NULL DEFAULT ''UNCHANGED'' COMMENT ''差异类型''');
CALL add_mes_edhr_column_if_missing('mes_pro_batch_record_version_migration_item', 'rule_type',
  '`rule_type` varchar(64) DEFAULT NULL COMMENT ''规则类型''');
CALL add_mes_edhr_column_if_missing('mes_pro_batch_record_version_migration_item', 'business_owner_type',
  '`business_owner_type` varchar(32) DEFAULT NULL COMMENT ''业务负责人类型''');
CALL add_mes_edhr_column_if_missing('mes_pro_batch_record_version_migration_item', 'confirmed',
  '`confirmed` bit(1) NOT NULL DEFAULT b''0'' COMMENT ''是否已确认''');
CALL add_mes_edhr_column_if_missing('mes_pro_batch_record_version_migration_item', 'confirmed_by',
  '`confirmed_by` bigint DEFAULT NULL COMMENT ''确认人''');
CALL add_mes_edhr_column_if_missing('mes_pro_batch_record_version_migration_item', 'confirmed_at',
  '`confirmed_at` datetime DEFAULT NULL COMMENT ''确认时间''');
CALL add_mes_edhr_column_if_missing('mes_pro_batch_record_version_migration_item', 'confirm_comment',
  '`confirm_comment` varchar(500) DEFAULT NULL COMMENT ''确认意见''');
CALL add_mes_edhr_column_if_missing('mes_pro_batch_record_version_migration_item', 'confirm_idempotency_key',
  '`confirm_idempotency_key` varchar(128) DEFAULT NULL COMMENT ''确认幂等键''');

CREATE TABLE IF NOT EXISTS `mes_pro_batch_record_version_approval_event` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `definition_id` bigint NOT NULL COMMENT '批记录定义ID',
  `version_id` bigint NOT NULL COMMENT '版本ID',
  `approval_instance_id` varchar(128) DEFAULT NULL COMMENT '审批实例ID',
  `approval_event_id` varchar(128) NOT NULL COMMENT '审批事件ID',
  `approval_result` varchar(32) NOT NULL COMMENT '审批结果',
  `processed_result` varchar(32) NOT NULL COMMENT '处理结果',
  `actor_user_id` bigint DEFAULT NULL COMMENT '审批用户',
  `processed_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '处理时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_batch_record_approval_event` (`tenant_id`, `approval_instance_id`, `approval_event_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 批记录版本审批事件';

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_unified_change_request` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `change_code` varchar(64) NOT NULL COMMENT '变更单号',
  `controlled_object_type` varchar(64) NOT NULL COMMENT '受控对象类型',
  `controlled_object_id` varchar(64) NOT NULL COMMENT '受控对象ID',
  `controlled_object_code` varchar(128) NOT NULL COMMENT '受控对象编码',
  `current_version` varchar(64) NOT NULL COMMENT '当前版本',
  `target_version` varchar(64) NOT NULL COMMENT '目标版本',
  `change_type` varchar(64) NOT NULL COMMENT '变更类型',
  `change_status` varchar(32) NOT NULL COMMENT '变更状态',
  `risk_level` varchar(32) NOT NULL COMMENT '风险等级',
  `reason_category` varchar(64) DEFAULT NULL COMMENT '原因分类',
  `reason` varchar(1000) NOT NULL COMMENT '原因',
  `diff_snapshot_json` longtext NOT NULL COMMENT '差异快照',
  `impact_summary_json` longtext NOT NULL COMMENT '影响面摘要',
  `impact_recalculated_at` datetime DEFAULT NULL COMMENT '影响面重算时间',
  `impact_recalculation_hash` char(64) DEFAULT NULL COMMENT '影响面重算哈希',
  `requested_by` bigint DEFAULT NULL COMMENT '申请人',
  `requested_at` datetime DEFAULT NULL COMMENT '申请时间',
  `submitted_by` bigint DEFAULT NULL COMMENT '提交人',
  `submitted_at` datetime DEFAULT NULL COMMENT '提交时间',
  `approved_by` bigint DEFAULT NULL COMMENT '批准人',
  `approved_at` datetime DEFAULT NULL COMMENT '批准时间',
  `approval_opinion` varchar(1000) DEFAULT NULL COMMENT '批准意见',
  `approval_signoff_evidence_hash` char(64) DEFAULT NULL COMMENT '批准签核证据哈希',
  `effect_requested_by` bigint DEFAULT NULL COMMENT '生效申请人',
  `effect_requested_at` datetime DEFAULT NULL COMMENT '生效申请时间',
  `effect_signoff_evidence_hash` char(64) DEFAULT NULL COMMENT '生效签核证据哈希',
  `idempotency_key` varchar(128) NOT NULL COMMENT '幂等键',
  `evidence_hash` char(64) NOT NULL COMMENT '证据哈希',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_edhr_unified_change_code` (`tenant_id`, `change_code`, `deleted`),
  UNIQUE KEY `uk_mes_edhr_unified_change_idem` (`tenant_id`, `controlled_object_type`, `controlled_object_id`, `change_type`, `idempotency_key`, `deleted`),
  KEY `idx_mes_edhr_unified_change_object` (`tenant_id`, `controlled_object_type`, `controlled_object_id`, `change_type`, `change_status`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES eDHR 统一变更申请';

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_unified_change_impact` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `change_request_id` bigint NOT NULL COMMENT '统一变更申请ID',
  `impact_type` varchar(64) NOT NULL COMMENT '影响类型',
  `impact_object_type` varchar(64) NOT NULL COMMENT '影响对象类型',
  `impact_object_id` varchar(64) NOT NULL COMMENT '影响对象ID',
  `impact_object_code` varchar(128) DEFAULT NULL COMMENT '影响对象编码',
  `risk_level` varchar(32) NOT NULL COMMENT '风险等级',
  `responsibility_module` varchar(64) DEFAULT NULL COMMENT '责任模块',
  `requires_training` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否需要培训',
  `requires_revalidation` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否需要再验证',
  `requires_release_recheck` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否需要放行复核',
  `impact_detail` varchar(1000) DEFAULT NULL COMMENT '影响详情',
  `next_action` varchar(500) DEFAULT NULL COMMENT '下一步动作',
  `evidence_hash` char(64) NOT NULL COMMENT '证据哈希',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_mes_edhr_unified_change_impact_request` (`tenant_id`, `change_request_id`, `risk_level`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES eDHR 统一变更影响面';

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_unified_change_event` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `change_request_id` bigint NOT NULL COMMENT '统一变更申请ID',
  `event_type` varchar(64) NOT NULL COMMENT '事件类型',
  `from_status` varchar(32) DEFAULT NULL COMMENT '来源状态',
  `to_status` varchar(32) NOT NULL COMMENT '目标状态',
  `actor_user_id` bigint DEFAULT NULL COMMENT '操作人',
  `reason` varchar(1000) DEFAULT NULL COMMENT '原因',
  `signoff_evidence_hash` char(64) DEFAULT NULL COMMENT '签核证据哈希',
  `event_snapshot_json` longtext COMMENT '事件快照',
  `evidence_hash` char(64) NOT NULL COMMENT '证据哈希',
  `occurred_at` datetime NOT NULL COMMENT '发生时间',
  `idempotency_key` varchar(128) NOT NULL COMMENT '幂等键',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_edhr_unified_change_event_idem` (`tenant_id`, `change_request_id`, `event_type`, `idempotency_key`, `deleted`),
  KEY `idx_mes_edhr_unified_change_event_request` (`tenant_id`, `change_request_id`, `event_type`, `occurred_at`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES eDHR 统一变更事件';

CALL add_mes_edhr_column_if_missing('mes_pro_batch_record_report', 'batch_record_definition_id',
  '`batch_record_definition_id` bigint DEFAULT NULL COMMENT ''批记录定义ID''');
CALL add_mes_edhr_column_if_missing('mes_pro_batch_record_report', 'batch_record_version_id',
  '`batch_record_version_id` bigint DEFAULT NULL COMMENT ''批记录版本ID''');
CALL add_mes_edhr_column_if_missing('mes_pro_batch_record_report', 'product_name',
  '`product_name` varchar(128) DEFAULT NULL COMMENT ''产品名称''');

CALL add_mes_edhr_column_if_table_exists('mes_pro_route_use_process_batch_record', 'form_slot_type',
  '`form_slot_type` varchar(32) NOT NULL DEFAULT ''MAIN'' COMMENT ''表单槽位类型''');
CALL add_mes_edhr_column_if_table_exists('mes_pro_route_use_process_batch_record', 'owner_role_key',
  '`owner_role_key` varchar(32) NOT NULL DEFAULT ''PRODUCTION'' COMMENT ''填写责任角色''');
CALL add_mes_edhr_column_if_table_exists('mes_pro_route_use_process_batch_record', 'slot_config_snapshot_hash',
  '`slot_config_snapshot_hash` char(64) DEFAULT NULL COMMENT ''槽位配置快照哈希''');
CALL add_mes_edhr_column_if_table_exists('mes_pro_route_use_process_batch_record', 'batch_record_definition_id',
  '`batch_record_definition_id` bigint DEFAULT NULL COMMENT ''批记录定义ID''');
CALL add_mes_edhr_column_if_table_exists('mes_pro_route_use_process_batch_record', 'batch_record_version_id',
  '`batch_record_version_id` bigint DEFAULT NULL COMMENT ''批记录版本ID''');

CALL add_mes_edhr_column_if_table_exists('mes_pro_route_use_process_batch_record_legacy_20260709', 'form_slot_type',
  '`form_slot_type` varchar(32) NOT NULL DEFAULT ''MAIN'' COMMENT ''表单槽位类型''');
CALL add_mes_edhr_column_if_table_exists('mes_pro_route_use_process_batch_record_legacy_20260709', 'owner_role_key',
  '`owner_role_key` varchar(32) NOT NULL DEFAULT ''PRODUCTION'' COMMENT ''填写责任角色''');
CALL add_mes_edhr_column_if_table_exists('mes_pro_route_use_process_batch_record_legacy_20260709', 'slot_config_snapshot_hash',
  '`slot_config_snapshot_hash` char(64) DEFAULT NULL COMMENT ''槽位配置快照哈希''');
CALL add_mes_edhr_column_if_table_exists('mes_pro_route_use_process_batch_record_legacy_20260709', 'batch_record_definition_id',
  '`batch_record_definition_id` bigint DEFAULT NULL COMMENT ''批记录定义ID''');
CALL add_mes_edhr_column_if_table_exists('mes_pro_route_use_process_batch_record_legacy_20260709', 'batch_record_version_id',
  '`batch_record_version_id` bigint DEFAULT NULL COMMENT ''批记录版本ID''');

CALL add_mes_edhr_column_if_missing('mes_pro_batch_record_execution', 'batch_record_definition_id',
  '`batch_record_definition_id` bigint DEFAULT NULL COMMENT ''批记录定义ID''');
CALL add_mes_edhr_column_if_missing('mes_pro_batch_record_execution', 'batch_record_version_id',
  '`batch_record_version_id` bigint DEFAULT NULL COMMENT ''批记录版本ID''');
CALL add_mes_edhr_column_if_missing('mes_pro_batch_record_execution', 'form_slot_type',
  '`form_slot_type` varchar(32) NOT NULL DEFAULT ''MAIN'' COMMENT ''表单槽位类型''');
CALL add_mes_edhr_column_if_missing('mes_pro_batch_record_execution', 'permission_scope_id',
  '`permission_scope_id` bigint DEFAULT NULL COMMENT ''权限范围ID''');
CALL add_mes_edhr_column_if_missing('mes_pro_batch_record_execution', 'slot_config_snapshot_hash',
  '`slot_config_snapshot_hash` char(64) DEFAULT NULL COMMENT ''槽位配置快照哈希''');
CALL add_mes_edhr_column_if_missing('mes_pro_batch_record_execution', 'route_id',
  '`route_id` bigint DEFAULT NULL COMMENT ''路线ID''');

CALL add_mes_edhr_column_if_missing('mes_pro_edhr_batch_execution_task', 'batch_record_definition_id',
  '`batch_record_definition_id` bigint DEFAULT NULL COMMENT ''批记录定义ID''');
CALL add_mes_edhr_column_if_missing('mes_pro_edhr_batch_execution_task', 'batch_record_version_id',
  '`batch_record_version_id` bigint DEFAULT NULL COMMENT ''批记录版本ID''');
CALL add_mes_edhr_column_if_missing('mes_pro_edhr_batch_execution_task', 'form_slot_type',
  '`form_slot_type` varchar(32) NOT NULL DEFAULT ''MAIN'' COMMENT ''表单槽位类型''');
CALL add_mes_edhr_column_if_missing('mes_pro_edhr_batch_execution_task', 'owner_role_key',
  '`owner_role_key` varchar(32) NOT NULL DEFAULT ''PRODUCTION'' COMMENT ''填写责任角色''');
CALL add_mes_edhr_column_if_missing('mes_pro_edhr_batch_execution_task', 'slot_config_snapshot_hash',
  '`slot_config_snapshot_hash` char(64) DEFAULT NULL COMMENT ''槽位配置快照哈希''');

CALL add_mes_edhr_column_if_missing('mes_pro_edhr_process_form_permission_rule', 'batch_record_definition_id',
  '`batch_record_definition_id` bigint DEFAULT NULL COMMENT ''批记录定义ID''');
CALL add_mes_edhr_column_if_missing('mes_pro_edhr_process_form_permission_rule', 'batch_record_version_id',
  '`batch_record_version_id` bigint DEFAULT NULL COMMENT ''批记录版本ID''');
CALL add_mes_edhr_column_if_missing('mes_pro_edhr_process_form_permission_rule', 'rule_type',
  '`rule_type` varchar(32) NOT NULL DEFAULT ''FILL'' COMMENT ''规则类型''');
CALL add_mes_edhr_column_if_missing('mes_pro_edhr_process_form_permission_rule', 'scope_key',
  '`scope_key` varchar(64) NOT NULL DEFAULT ''ALL'' COMMENT ''责任范围标识''');
CALL add_mes_edhr_column_if_missing('mes_pro_edhr_process_form_permission_rule', 'fillable_scope_json',
  '`fillable_scope_json` json DEFAULT NULL COMMENT ''精确可填写范围 JSON''');
CALL add_mes_edhr_column_if_missing('mes_pro_edhr_process_form_permission_rule', 'signature_role',
  '`signature_role` varchar(64) DEFAULT NULL COMMENT ''签名角色''');
CALL add_mes_edhr_column_if_missing('mes_pro_edhr_process_form_permission_rule', 'due_minutes',
  '`due_minutes` int DEFAULT NULL COMMENT ''处理时限分钟''');

SET @drop_process_form_rule_unique := (
  SELECT IF(COUNT(*) > 0,
    'ALTER TABLE `mes_pro_edhr_process_form_permission_rule` DROP INDEX `uk_mes_pro_edhr_process_form_rule`',
    'SELECT 1')
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'mes_pro_edhr_process_form_permission_rule'
    AND index_name = 'uk_mes_pro_edhr_process_form_rule'
);
PREPARE stmt FROM @drop_process_form_rule_unique;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @create_process_form_rule_unique := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `mes_pro_edhr_process_form_permission_rule` ADD UNIQUE KEY `uk_mes_pro_edhr_process_form_rule` (`tenant_id`, `route_process_id`, `batch_record_report_id`, `batch_record_version_id`, `rule_type`, `scope_key`, `signature_cell_key`, `deleted`)',
    'SELECT 1')
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'mes_pro_edhr_process_form_permission_rule'
    AND index_name = 'uk_mes_pro_edhr_process_form_rule'
);
PREPARE stmt FROM @create_process_form_rule_unique;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @edhr_version_approval_menu_id := (
  SELECT `id`
    FROM `system_menu`
   WHERE `deleted` = b'0'
     AND `permission` = 'mes:pro-batch-record-template:version-approve'
   ORDER BY `id`
   LIMIT 1
);

SET @edhr_version_approval_menu_id := IFNULL(@edhr_version_approval_menu_id, (
  SELECT GREATEST(900023, IFNULL(MAX(`id`) + 1, 900023))
    FROM `system_menu`
));

INSERT INTO `system_menu` (
  `id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
  `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`
)
SELECT @edhr_version_approval_menu_id, '批记录升版审批', 'mes:pro-batch-record-template:version-approve',
       3, 21, 900002, '', '', '', '', 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
WHERE NOT EXISTS (
  SELECT 1
    FROM `system_menu`
   WHERE `deleted` = b'0'
     AND `permission` = 'mes:pro-batch-record-template:version-approve'
);

INSERT INTO `system_role_menu` (
  `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
)
SELECT r.`id`, m.`id`, 'system', NOW(), 'system', NOW(), b'0', r.`tenant_id`
  FROM `system_role` r
  JOIN `system_menu` m
    ON m.`deleted` = b'0'
   AND m.`permission` = 'mes:pro-batch-record-template:version-approve'
 WHERE r.`deleted` = b'0'
   AND r.`tenant_id` = 122
   AND r.`code` = 'edhr_rehearsal_approver_t1'
   AND NOT EXISTS (
       SELECT 1
         FROM `system_role_menu` rm
        WHERE rm.`deleted` = b'0'
          AND rm.`tenant_id` = r.`tenant_id`
          AND rm.`role_id` = r.`id`
           AND rm.`menu_id` = m.`id`
    );

INSERT INTO `system_menu` (
  `id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
  `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`
)
SELECT 900303, 'eDHR版本治理', 'mes:pro-batch-record-version:governance-query',
       2, 303, 900220, '/mes/pro/feedback/edhr-version-governance', '', 'mes/pro/edhr-version-governance/VersionGovernancePage',
       'MesProEdhrVersionGovernancePage', 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
WHERE NOT EXISTS (
  SELECT 1
    FROM `system_menu`
   WHERE `deleted` = b'0'
     AND (`id` = 900303 OR `permission` = 'mes:pro-batch-record-version:governance-query')
);

INSERT INTO `system_menu` (
  `id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
  `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`
)
SELECT 900304, 'eDHR版本治理确认', 'mes:pro-batch-record-version:confirm',
       3, 1, 900303, '', '', '', '', 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
WHERE NOT EXISTS (
  SELECT 1
    FROM `system_menu`
   WHERE `deleted` = b'0'
     AND (`id` = 900304 OR `permission` = 'mes:pro-batch-record-version:confirm')
);

INSERT INTO `system_menu` (
  `id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
  `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`
)
SELECT 900305, 'eDHR版本草稿重传', 'mes:pro-batch-record-version:import',
       3, 2, 900303, '', '', '', '', 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
WHERE NOT EXISTS (
  SELECT 1
    FROM `system_menu`
   WHERE `deleted` = b'0'
     AND (`id` = 900305 OR `permission` = 'mes:pro-batch-record-version:import')
);

INSERT INTO `system_menu` (
  `id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
  `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`
)
SELECT 900306, 'eDHR版本受控回滚', 'mes:pro-batch-record-version:rollback-request',
       3, 3, 900303, '', '', '', '', 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
WHERE NOT EXISTS (
  SELECT 1
    FROM `system_menu`
   WHERE `deleted` = b'0'
     AND (`id` = 900306 OR `permission` = 'mes:pro-batch-record-version:rollback-request')
);

INSERT INTO `system_role_menu` (
  `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
)
SELECT DISTINCT rm.`role_id`, vm.`id`, 'system', NOW(), 'system', NOW(), b'0', rm.`tenant_id`
  FROM `system_role_menu` rm
  JOIN `system_menu` parent_menu
    ON parent_menu.`id` = rm.`menu_id`
   AND parent_menu.`deleted` = b'0'
   AND parent_menu.`id` = 900220
  JOIN `system_menu` vm
    ON vm.`deleted` = b'0'
   AND vm.`permission` IN (
       'mes:pro-batch-record-version:governance-query',
       'mes:pro-batch-record-version:confirm',
       'mes:pro-batch-record-version:import',
       'mes:pro-batch-record-version:rollback-request'
   )
 WHERE rm.`deleted` = b'0'
   AND NOT EXISTS (
       SELECT 1
         FROM `system_role_menu` existing
        WHERE existing.`deleted` = b'0'
          AND existing.`tenant_id` = rm.`tenant_id`
          AND existing.`role_id` = rm.`role_id`
          AND existing.`menu_id` = vm.`id`
   );

DROP PROCEDURE IF EXISTS add_mes_edhr_column_if_missing;
DROP PROCEDURE IF EXISTS add_mes_edhr_column_if_table_exists;
