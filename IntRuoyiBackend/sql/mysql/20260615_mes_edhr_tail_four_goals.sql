-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260612_mes_edhr_multi_batch_route; type=schema; riskLevel=high
-- eDHR goals 55-58 schema: internal record metadata, selected signature time, operation audit, object ACL.
-- This migration is idempotent and does not fabricate historical operation audit events.

DROP PROCEDURE IF EXISTS ensure_mes_edhr_tail_goal_table;
DROP PROCEDURE IF EXISTS ensure_mes_edhr_tail_goal_column;
DROP PROCEDURE IF EXISTS ensure_mes_edhr_tail_goal_index;

DELIMITER $$

CREATE PROCEDURE ensure_mes_edhr_tail_goal_table(IN p_table_name varchar(128))
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table_name
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing required eDHR base table for tail goals migration';
  END IF;
END$$

CREATE PROCEDURE ensure_mes_edhr_tail_goal_column(
  IN p_table_name varchar(128),
  IN p_column_name varchar(128),
  IN p_column_definition text
)
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table_name
      AND COLUMN_NAME = p_column_name
  ) THEN
    SET @mes_edhr_tail_goal_sql = CONCAT(
      'ALTER TABLE `', p_table_name, '` ADD COLUMN `', p_column_name, '` ', p_column_definition
    );
    PREPARE stmt FROM @mes_edhr_tail_goal_sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$

CREATE PROCEDURE ensure_mes_edhr_tail_goal_index(
  IN p_table_name varchar(128),
  IN p_index_name varchar(128),
  IN p_index_definition text
)
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table_name
      AND INDEX_NAME = p_index_name
  ) THEN
    SET @mes_edhr_tail_goal_sql = CONCAT(
      'ALTER TABLE `', p_table_name, '` ADD ', p_index_definition
    );
    PREPARE stmt FROM @mes_edhr_tail_goal_sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$

DELIMITER ;

CALL ensure_mes_edhr_tail_goal_table('mes_pro_route_use_process_batch_record');
CALL ensure_mes_edhr_tail_goal_table('mes_pro_edhr_batch_execution_task');
CALL ensure_mes_edhr_tail_goal_table('mes_pro_batch_record_execution');
CALL ensure_mes_edhr_tail_goal_table('mes_pro_batch_record_execution_signature');

CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_route_use_process_batch_record',
  'form_slot_type',
  'varchar(32) NOT NULL DEFAULT ''MAIN'' COMMENT ''表单槽位类型：MAIN/LOSS_REPORT/PROCESS_INSPECTION/PARAMETER_RECORD'' AFTER `batch_record_report_id`'
);
CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_route_use_process_batch_record',
  'record_category',
  'varchar(32) NOT NULL DEFAULT ''BATCH_RECORD'' COMMENT ''记录类型：BATCH_RECORD/INTERNAL_RECORD'' AFTER `form_slot_type`'
);
CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_route_use_process_batch_record',
  'validation_profile',
  'varchar(32) NOT NULL DEFAULT ''CONTROLLED_BATCH'' COMMENT ''校验策略：CONTROLLED_BATCH/INTERNAL_TRACE'' AFTER `record_category`'
);
CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_route_use_process_batch_record',
  'permission_scope_id',
  'bigint DEFAULT NULL COMMENT ''对象级权限范围ID'' AFTER `validation_profile`'
);
CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_route_use_process_batch_record',
  'record_category_snapshot_hash',
  'char(64) DEFAULT NULL COMMENT ''记录类型、校验策略和权限范围摘要'' AFTER `permission_scope_id`'
);
CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_route_use_process_batch_record',
  'required_policy',
  'varchar(32) NOT NULL DEFAULT ''REQUIRED'' COMMENT ''必填策略：REQUIRED/CONDITIONAL/OPTIONAL'' AFTER `record_category_snapshot_hash`'
);
CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_route_use_process_batch_record',
  'required_condition_json',
  'json DEFAULT NULL COMMENT ''条件必填规则JSON'' AFTER `required_policy`'
);
CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_route_use_process_batch_record',
  'owner_role_key',
  'varchar(32) NOT NULL DEFAULT ''PRODUCTION'' COMMENT ''表单责任角色：PRODUCTION/QUALITY/EQUIPMENT'' AFTER `required_condition_json`'
);
CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_route_use_process_batch_record',
  'archive_visibility',
  'varchar(32) NOT NULL DEFAULT ''FINAL_DHR'' COMMENT ''归档可见性：FINAL_DHR/INTERNAL_REVIEW/AUDIT_ONLY/ATTACHMENT_REFERENCE'' AFTER `owner_role_key`'
);
CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_route_use_process_batch_record',
  'slot_config_snapshot_hash',
  'char(64) DEFAULT NULL COMMENT ''表单槽位配置快照哈希'' AFTER `archive_visibility`'
);
CALL ensure_mes_edhr_tail_goal_index(
  'mes_pro_route_use_process_batch_record',
  'idx_mes_pro_route_record_scope',
  'KEY `idx_mes_pro_route_record_scope` (`tenant_id`, `record_category`, `permission_scope_id`, `deleted`)'
);

CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_edhr_batch_execution_task',
  'form_slot_type',
  'varchar(32) NOT NULL DEFAULT ''MAIN'' COMMENT ''表单槽位类型：MAIN/LOSS_REPORT/PROCESS_INSPECTION/PARAMETER_RECORD'' AFTER `batch_record_report_name`'
);
CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_edhr_batch_execution_task',
  'record_category',
  'varchar(32) NOT NULL DEFAULT ''BATCH_RECORD'' COMMENT ''执行快照记录类型'' AFTER `execution_mode`'
);
CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_edhr_batch_execution_task',
  'validation_profile',
  'varchar(32) NOT NULL DEFAULT ''CONTROLLED_BATCH'' COMMENT ''执行快照校验策略'' AFTER `record_category`'
);
CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_edhr_batch_execution_task',
  'permission_scope_id',
  'bigint DEFAULT NULL COMMENT ''执行快照权限范围ID'' AFTER `validation_profile`'
);
CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_edhr_batch_execution_task',
  'route_binding_id',
  'bigint DEFAULT NULL COMMENT ''来源路线记录表绑定ID'' AFTER `permission_scope_id`'
);
CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_edhr_batch_execution_task',
  'route_binding_snapshot_hash',
  'char(64) DEFAULT NULL COMMENT ''来源路线记录表绑定快照摘要'' AFTER `route_binding_id`'
);
CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_edhr_batch_execution_task',
  'required_policy',
  'varchar(32) NOT NULL DEFAULT ''REQUIRED'' COMMENT ''必填策略：REQUIRED/CONDITIONAL/OPTIONAL'' AFTER `route_binding_snapshot_hash`'
);
CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_edhr_batch_execution_task',
  'required_condition_json',
  'json DEFAULT NULL COMMENT ''条件必填规则JSON'' AFTER `required_policy`'
);
CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_edhr_batch_execution_task',
  'owner_role_key',
  'varchar(32) NOT NULL DEFAULT ''PRODUCTION'' COMMENT ''表单责任角色：PRODUCTION/QUALITY/EQUIPMENT'' AFTER `required_condition_json`'
);
CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_edhr_batch_execution_task',
  'archive_visibility',
  'varchar(32) NOT NULL DEFAULT ''FINAL_DHR'' COMMENT ''归档可见性：FINAL_DHR/INTERNAL_REVIEW/AUDIT_ONLY/ATTACHMENT_REFERENCE'' AFTER `owner_role_key`'
);
CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_edhr_batch_execution_task',
  'slot_config_snapshot_hash',
  'char(64) DEFAULT NULL COMMENT ''表单槽位配置快照哈希'' AFTER `archive_visibility`'
);
CALL ensure_mes_edhr_tail_goal_index(
  'mes_pro_edhr_batch_execution_task',
  'idx_mes_pro_edhr_task_record_scope',
  'KEY `idx_mes_pro_edhr_task_record_scope` (`tenant_id`, `record_category`, `permission_scope_id`, `deleted`)'
);

CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_batch_record_execution',
  'form_slot_type',
  'varchar(32) NOT NULL DEFAULT ''MAIN'' COMMENT ''表单槽位类型：MAIN/LOSS_REPORT/PROCESS_INSPECTION/PARAMETER_RECORD'' AFTER `batch_record_report_id`'
);
CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_batch_record_execution',
  'record_category',
  'varchar(32) NOT NULL DEFAULT ''BATCH_RECORD'' COMMENT ''执行记录类型'' AFTER `form_slot_type`'
);
CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_batch_record_execution',
  'validation_profile',
  'varchar(32) NOT NULL DEFAULT ''CONTROLLED_BATCH'' COMMENT ''执行校验策略'' AFTER `record_category`'
);
CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_batch_record_execution',
  'permission_scope_id',
  'bigint DEFAULT NULL COMMENT ''对象级权限范围ID'' AFTER `validation_profile`'
);
CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_batch_record_execution',
  'route_binding_id',
  'bigint DEFAULT NULL COMMENT ''来源路线记录表绑定ID'' AFTER `permission_scope_id`'
);
CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_batch_record_execution',
  'route_binding_snapshot_hash',
  'char(64) DEFAULT NULL COMMENT ''来源路线记录表绑定快照摘要'' AFTER `route_binding_id`'
);
CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_batch_record_execution',
  'archive_visibility',
  'varchar(32) NOT NULL DEFAULT ''FINAL_DHR'' COMMENT ''归档可见性：FINAL_DHR/INTERNAL_REVIEW/AUDIT_ONLY/ATTACHMENT_REFERENCE'' AFTER `route_binding_snapshot_hash`'
);
CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_batch_record_execution',
  'slot_config_snapshot_hash',
  'char(64) DEFAULT NULL COMMENT ''槽位配置快照摘要'' AFTER `archive_visibility`'
);
CALL ensure_mes_edhr_tail_goal_index(
  'mes_pro_batch_record_execution',
  'idx_mes_pro_bre_record_scope',
  'KEY `idx_mes_pro_bre_record_scope` (`tenant_id`, `record_category`, `permission_scope_id`, `deleted`)'
);

CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_batch_record_execution_signature',
  'selected_signed_at',
  'datetime DEFAULT NULL COMMENT ''用户选择的签名显示时间'' AFTER `signed_at`'
);
CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_batch_record_execution_signature',
  'signature_display_at',
  'datetime DEFAULT NULL COMMENT ''打印、归档和签名历史展示时间'' AFTER `selected_signed_at`'
);
CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_batch_record_execution_signature',
  'signature_time_mode',
  'varchar(32) NOT NULL DEFAULT ''SERVER_TIME'' COMMENT ''签名时间模式：SERVER_TIME/USER_SELECTED'' AFTER `signature_display_at`'
);
CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_batch_record_execution_signature',
  'selected_time_zone',
  'varchar(64) DEFAULT NULL COMMENT ''选择签名时间对应时区'' AFTER `signature_time_mode`'
);
CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_batch_record_execution_signature',
  'selected_time_reason',
  'varchar(500) DEFAULT NULL COMMENT ''选择签名时间原因'' AFTER `selected_time_zone`'
);
CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_batch_record_execution_signature',
  'selected_time_policy_version',
  'varchar(64) DEFAULT NULL COMMENT ''签名时间策略版本'' AFTER `selected_time_reason`'
);
CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_batch_record_execution_signature',
  'selected_time_audit_hash',
  'char(64) DEFAULT NULL COMMENT ''签名时间选择审计摘要'' AFTER `selected_time_policy_version`'
);
CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_batch_record_execution_signature',
  'actor_username_snapshot',
  'varchar(64) DEFAULT NULL COMMENT ''签名当时账号快照'' AFTER `selected_time_audit_hash`'
);
CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_batch_record_execution_signature',
  'actor_nickname_snapshot',
  'varchar(64) DEFAULT NULL COMMENT ''签名当时昵称快照'' AFTER `actor_username_snapshot`'
);
CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_batch_record_execution_signature',
  'actor_dept_id_snapshot',
  'bigint DEFAULT NULL COMMENT ''签名当时部门ID快照'' AFTER `actor_nickname_snapshot`'
);
CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_batch_record_execution_signature',
  'actor_dept_name_snapshot',
  'varchar(128) DEFAULT NULL COMMENT ''签名当时部门名称快照'' AFTER `actor_dept_id_snapshot`'
);
CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_batch_record_execution_signature',
  'actor_post_names_snapshot',
  'varchar(512) DEFAULT NULL COMMENT ''签名当时岗位快照'' AFTER `actor_dept_name_snapshot`'
);
CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_batch_record_execution_signature',
  'actor_role_names_snapshot',
  'varchar(512) DEFAULT NULL COMMENT ''签名当时角色快照'' AFTER `actor_post_names_snapshot`'
);
CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_batch_record_execution_signature',
  'signature_purpose',
  'varchar(128) DEFAULT NULL COMMENT ''签名目的标签'' AFTER `actor_role_names_snapshot`'
);
CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_batch_record_execution_signature',
  'authorization_basis',
  'varchar(500) DEFAULT NULL COMMENT ''签名权限依据快照'' AFTER `signature_purpose`'
);
CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_batch_record_execution_signature',
  'authentication_method',
  'varchar(64) DEFAULT NULL COMMENT ''签名认证方式'' AFTER `authorization_basis`'
);
CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_batch_record_execution_signature',
  'record_version_snapshot',
  'varchar(64) DEFAULT NULL COMMENT ''业务记录版本快照'' AFTER `authentication_method`'
);
CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_batch_record_execution_signature',
  'record_hash_snapshot',
  'varchar(128) DEFAULT NULL COMMENT ''业务记录摘要快照'' AFTER `record_version_snapshot`'
);
CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_batch_record_execution_signature',
  'client_ip_snapshot',
  'varchar(64) DEFAULT NULL COMMENT ''签名客户端IP快照'' AFTER `record_hash_snapshot`'
);
CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_batch_record_execution_signature',
  'user_agent_snapshot',
  'varchar(512) DEFAULT NULL COMMENT ''签名客户端User-Agent快照'' AFTER `client_ip_snapshot`'
);
CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_batch_record_execution_signature',
  'snapshot_status',
  'varchar(32) DEFAULT NULL COMMENT ''签名快照状态'' AFTER `user_agent_snapshot`'
);

UPDATE `mes_pro_batch_record_execution_signature`
SET `signature_display_at` = `signed_at`,
    `signature_time_mode` = 'SERVER_TIME'
WHERE `signature_display_at` IS NULL;

CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_edhr_batch_execution_signature',
  'selected_signed_at',
  'datetime DEFAULT NULL COMMENT ''用户选择的签名显示时间'' AFTER `signed_at`'
);
CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_edhr_batch_execution_signature',
  'signature_display_at',
  'datetime DEFAULT NULL COMMENT ''打印、归档和签名历史展示时间'' AFTER `selected_signed_at`'
);
CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_edhr_batch_execution_signature',
  'signature_time_mode',
  'varchar(32) NOT NULL DEFAULT ''SERVER_TIME'' COMMENT ''签名时间模式：SERVER_TIME/USER_SELECTED'' AFTER `signature_display_at`'
);
CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_edhr_batch_execution_signature',
  'selected_time_zone',
  'varchar(64) DEFAULT NULL COMMENT ''选择签名时间对应时区'' AFTER `signature_time_mode`'
);
CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_edhr_batch_execution_signature',
  'selected_time_reason',
  'varchar(500) DEFAULT NULL COMMENT ''选择签名时间原因'' AFTER `selected_time_zone`'
);
CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_edhr_batch_execution_signature',
  'selected_time_policy_version',
  'varchar(64) DEFAULT NULL COMMENT ''签名时间策略版本'' AFTER `selected_time_reason`'
);
CALL ensure_mes_edhr_tail_goal_column(
  'mes_pro_edhr_batch_execution_signature',
  'selected_time_audit_hash',
  'char(64) DEFAULT NULL COMMENT ''签名时间选择审计摘要'' AFTER `selected_time_policy_version`'
);
UPDATE `mes_pro_edhr_batch_execution_signature`
SET `signature_display_at` = `signed_at`,
    `signature_time_mode` = 'SERVER_TIME'
WHERE `signature_display_at` IS NULL;

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_operation_audit_event` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `request_id` varchar(128) NOT NULL COMMENT '请求ID',
  `object_type` varchar(64) NOT NULL COMMENT '对象类型',
  `object_id` varchar(128) NOT NULL COMMENT '对象ID',
  `batch_execution_id` bigint DEFAULT NULL COMMENT '批次执行ID',
  `execution_id` bigint DEFAULT NULL COMMENT '批记录执行ID',
  `work_task_id` bigint DEFAULT NULL COMMENT '工作任务ID',
  `route_id` bigint DEFAULT NULL COMMENT '工艺路线ID',
  `route_process_id` bigint DEFAULT NULL COMMENT '路线工序ID',
  `report_id` varchar(64) DEFAULT NULL COMMENT '记录表报表ID',
  `record_category` varchar(32) DEFAULT NULL COMMENT '记录类型',
  `operation_type` varchar(64) NOT NULL COMMENT '操作类型',
  `action_name` varchar(128) DEFAULT NULL COMMENT '操作名称',
  `actor_user_id` bigint DEFAULT NULL COMMENT '操作用户ID',
  `actor_username` varchar(64) DEFAULT NULL COMMENT '操作用户账号',
  `permission_code` varchar(128) DEFAULT NULL COMMENT '接口权限编码',
  `permission_decision` varchar(32) DEFAULT NULL COMMENT '对象权限决策',
  `matched_rule_ids` varchar(500) DEFAULT NULL COMMENT '命中权限规则ID集合',
  `result_status` varchar(32) NOT NULL COMMENT '结果状态：SUCCESS/FAILED/DENIED',
  `failure_code` varchar(128) DEFAULT NULL COMMENT '失败编码',
  `failure_message` varchar(500) DEFAULT NULL COMMENT '失败说明',
  `before_summary_hash` char(64) DEFAULT NULL COMMENT '操作前摘要',
  `after_summary_hash` char(64) DEFAULT NULL COMMENT '操作后摘要',
  `metadata_json` longtext DEFAULT NULL COMMENT '审计元数据JSON',
  `occurred_at` datetime NOT NULL COMMENT '发生时间',
  `previous_audit_hash` char(64) DEFAULT NULL COMMENT '上一审计摘要',
  `audit_hash` char(64) NOT NULL COMMENT '当前审计摘要',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_mes_pro_edhr_audit_object` (`tenant_id`, `object_type`, `object_id`, `occurred_at`),
  KEY `idx_mes_pro_edhr_audit_execution` (`tenant_id`, `execution_id`, `operation_type`, `occurred_at`),
  KEY `idx_mes_pro_edhr_audit_actor` (`tenant_id`, `actor_user_id`, `occurred_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES eDHR 操作审计事件';

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_permission_scope` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `scope_name` varchar(128) NOT NULL COMMENT '权限范围名称',
  `object_type` varchar(64) NOT NULL COMMENT '对象类型',
  `object_id` varchar(128) NOT NULL COMMENT '对象ID',
  `parent_scope_id` bigint DEFAULT NULL COMMENT '父权限范围ID',
  `status` varchar(32) NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
  `version` int NOT NULL DEFAULT 1 COMMENT '版本号',
  `create_user_id` bigint DEFAULT NULL COMMENT '创建用户ID',
  `update_user_id` bigint DEFAULT NULL COMMENT '更新用户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_edhr_perm_scope_object` (`tenant_id`, `object_type`, `object_id`, `deleted`),
  KEY `idx_mes_pro_edhr_perm_scope_parent` (`tenant_id`, `parent_scope_id`, `status`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES eDHR 对象级权限范围';

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_permission_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `scope_id` bigint NOT NULL COMMENT '权限范围ID',
  `subject_type` varchar(32) NOT NULL COMMENT '主体类型：USER/ROLE/DEPT',
  `subject_id` bigint NOT NULL COMMENT '主体ID',
  `ability` varchar(64) NOT NULL COMMENT '能力：VIEW/FILL/SIGN/APPROVE/ARCHIVE/ROUTE_EDIT/PERMISSION_ADMIN/AUDIT_VIEW',
  `decision` varchar(32) NOT NULL COMMENT '决策：ALLOW/DENY',
  `priority` int NOT NULL DEFAULT 100 COMMENT '优先级',
  `effective_from` datetime DEFAULT NULL COMMENT '生效时间',
  `effective_to` datetime DEFAULT NULL COMMENT '失效时间',
  `status` varchar(32) NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
  `version` int NOT NULL DEFAULT 1 COMMENT '版本号',
  `create_user_id` bigint DEFAULT NULL COMMENT '创建用户ID',
  `update_user_id` bigint DEFAULT NULL COMMENT '更新用户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_mes_pro_edhr_perm_rule_scope` (`tenant_id`, `scope_id`, `ability`, `status`, `deleted`),
  KEY `idx_mes_pro_edhr_perm_rule_subject` (`tenant_id`, `subject_type`, `subject_id`, `ability`, `status`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES eDHR 对象级权限规则';

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900241, 'eDHR操作审计', 'mes:pro-edhr-operation-audit:query', 2, 60, 900220,
       '/mes/pro/feedback/edhr-operation-audit', 'ep:document-checked',
       'mes/pro/edhr/OperationAuditPage', 'MesProEdhrOperationAuditPage', 0, b'1', b'1', b'1',
       'system', NOW(), 'system', NOW(), b'0'
WHERE EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900220 AND `deleted` = b'0')
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900241);

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900242, 'eDHR操作审计查询', 'mes:pro-edhr-operation-audit:query', 3, 1, 900241,
       '', '', '', '', 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
WHERE EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900241 AND `deleted` = b'0')
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900242);

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900243, 'eDHR对象权限', 'mes:pro-edhr-permission-scope:evaluate', 2, 61, 900220,
       '/mes/pro/feedback/edhr-permission-matrix', 'ep:key',
       'mes/pro/edhr/PermissionMatrixPage', 'MesProEdhrPermissionMatrixPage', 0, b'1', b'1', b'1',
       'system', NOW(), 'system', NOW(), b'0'
WHERE EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900220 AND `deleted` = b'0')
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900243);

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900244, 'eDHR对象权限查询', 'mes:pro-edhr-permission-scope:query', 3, 1, 900243,
       '', '', '', '', 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
WHERE EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900243 AND `deleted` = b'0')
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900244);

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900245, 'eDHR对象权限保存', 'mes:pro-edhr-permission-scope:save', 3, 2, 900243,
       '', '', '', '', 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
WHERE EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900243 AND `deleted` = b'0')
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900245);

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900246, 'eDHR对象权限评估', 'mes:pro-edhr-permission-scope:evaluate', 3, 3, 900243,
       '', '', '', '', 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
WHERE EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900243 AND `deleted` = b'0')
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900246);

INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT `role`.`id`, `menu`.`menu_id`, 'system', NOW(), 'system', NOW(), b'0', `role`.`tenant_id`
FROM `system_role` `role`
JOIN (
  SELECT 900241 AS `menu_id` UNION ALL
  SELECT 900242 UNION ALL
  SELECT 900243 UNION ALL
  SELECT 900244 UNION ALL
  SELECT 900245 UNION ALL
  SELECT 900246
) `menu`
WHERE `role`.`code` = 'tenant_admin'
  AND `role`.`status` = 0
  AND `role`.`deleted` = b'0'
  AND EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = `menu`.`menu_id` AND `deleted` = b'0')
  AND NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` `role_menu`
    WHERE `role_menu`.`role_id` = `role`.`id`
      AND `role_menu`.`menu_id` = `menu`.`menu_id`
      AND `role_menu`.`tenant_id` = `role`.`tenant_id`
      AND `role_menu`.`deleted` = b'0'
  );

DROP PROCEDURE IF EXISTS ensure_mes_edhr_tail_goal_index;
DROP PROCEDURE IF EXISTS ensure_mes_edhr_tail_goal_column;
DROP PROCEDURE IF EXISTS ensure_mes_edhr_tail_goal_table;
