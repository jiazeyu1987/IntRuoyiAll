-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- BPM base schema repair for MySQL.
-- Generated from yudao-module-bpm DO annotations and fields.
-- Safe to run repeatedly: creates missing tables only and does not delete data.

CREATE TABLE IF NOT EXISTS `bpm_category` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `code` varchar(255) DEFAULT NULL,
  `description` varchar(512) DEFAULT NULL,
  `status` int DEFAULT NULL,
  `sort` int DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_bpm_category_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='BpmCategoryDO';

CREATE TABLE IF NOT EXISTS `bpm_form` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `status` int DEFAULT NULL,
  `conf` longtext DEFAULT NULL,
  `fields` longtext,
  `remark` varchar(512) DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_bpm_form_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='BpmFormDO';

CREATE TABLE IF NOT EXISTS `bpm_oa_leave` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL,
  `type` int DEFAULT NULL,
  `reason` varchar(512) DEFAULT NULL,
  `start_time` datetime DEFAULT NULL,
  `end_time` datetime DEFAULT NULL,
  `day` bigint DEFAULT NULL,
  `status` int DEFAULT NULL,
  `process_instance_id` varchar(128) DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_bpm_oa_leave_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='BpmOALeaveDO';

CREATE TABLE IF NOT EXISTS `bpm_process_definition_info` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `process_definition_id` varchar(128) DEFAULT NULL,
  `model_id` varchar(128) DEFAULT NULL,
  `model_type` int DEFAULT NULL,
  `category` varchar(128) DEFAULT NULL,
  `icon` varchar(255) DEFAULT NULL,
  `description` varchar(512) DEFAULT NULL,
  `form_type` int DEFAULT NULL,
  `form_id` bigint DEFAULT NULL,
  `form_conf` longtext DEFAULT NULL,
  `form_fields` longtext,
  `form_custom_create_path` varchar(512) DEFAULT NULL,
  `form_custom_view_path` varchar(512) DEFAULT NULL,
  `simple_model` longtext DEFAULT NULL,
  `visible` bit(1) DEFAULT NULL,
  `sort` bigint DEFAULT NULL,
  `start_user_ids` longtext,
  `start_dept_ids` longtext,
  `manager_user_ids` longtext,
  `allow_cancel_running_process` bit(1) DEFAULT NULL,
  `allow_withdraw_task` bit(1) DEFAULT NULL,
  `process_id_rule` longtext,
  `auto_approval_type` int DEFAULT NULL,
  `title_setting` longtext,
  `summary_setting` longtext,
  `process_before_trigger_setting` longtext,
  `process_after_trigger_setting` longtext,
  `task_before_trigger_setting` longtext,
  `task_after_trigger_setting` longtext,
  `print_template_setting` longtext,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_bpm_process_definition_info_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='BpmProcessDefinitionInfoDO';

CREATE TABLE IF NOT EXISTS `bpm_process_expression` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `status` int DEFAULT NULL,
  `expression` longtext DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_bpm_process_expression_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='BpmProcessExpressionDO';

CREATE TABLE IF NOT EXISTS `bpm_process_instance_copy` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `start_user_id` bigint DEFAULT NULL,
  `process_instance_name` varchar(128) DEFAULT NULL,
  `process_instance_id` varchar(128) DEFAULT NULL,
  `process_definition_id` varchar(128) DEFAULT NULL,
  `category` varchar(128) DEFAULT NULL,
  `activity_id` varchar(128) DEFAULT NULL,
  `activity_name` varchar(128) DEFAULT NULL,
  `task_id` varchar(128) DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `reason` varchar(512) DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_bpm_process_instance_copy_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='BpmProcessInstanceCopyDO';

CREATE TABLE IF NOT EXISTS `bpm_process_listener` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `status` int DEFAULT NULL,
  `type` varchar(64) DEFAULT NULL,
  `event` varchar(64) DEFAULT NULL,
  `value_type` varchar(64) DEFAULT NULL,
  `value` varchar(255) DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_bpm_process_listener_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='BpmProcessListenerDO';

CREATE TABLE IF NOT EXISTS `bpm_user_group` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `description` varchar(512) DEFAULT NULL,
  `status` int DEFAULT NULL,
  `user_ids` longtext,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_bpm_user_group_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='BpmUserGroupDO';
