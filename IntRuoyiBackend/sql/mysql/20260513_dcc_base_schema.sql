-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- DCC v1 backend foundation schema for MySQL.
-- Safe to run repeatedly: creates missing tables only and inserts only missing menu permissions.

CREATE TABLE IF NOT EXISTS `dcc_file_directory` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `parent_id` bigint DEFAULT NULL,
  `code` varchar(64) NOT NULL,
  `name` varchar(128) NOT NULL,
  `active` tinyint NOT NULL DEFAULT 1,
  `sort` int NOT NULL DEFAULT 0,
  `remark` varchar(255) DEFAULT NULL,
  `access_rule_manually_bound` tinyint NOT NULL DEFAULT 0,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_file_directory_parent_code` (`parent_id`, `code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC file directory';

CREATE TABLE IF NOT EXISTS `dcc_directory_access_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `directory_id` bigint NOT NULL,
  `subject_type` varchar(32) NOT NULL,
  `subject_id` bigint NOT NULL,
  `can_query` tinyint NOT NULL DEFAULT 0,
  `can_preview` tinyint NOT NULL DEFAULT 0,
  `can_download` tinyint NOT NULL DEFAULT 0,
  `active` tinyint NOT NULL DEFAULT 1,
  `change_reason` varchar(255) DEFAULT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_dcc_access_rule_directory` (`directory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC directory access rule';

CREATE TABLE IF NOT EXISTS `dcc_file_category` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(64) NOT NULL,
  `name` varchar(128) NOT NULL,
  `parent_id` bigint DEFAULT NULL,
  `active` tinyint NOT NULL DEFAULT 1,
  `sort` int NOT NULL DEFAULT 0,
  `source` varchar(32) NOT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `lifecycle_stage` varchar(32) NOT NULL COMMENT 'Lifecycle stage: PLAN/INPUT/OUTPUT/VERIFICATION/VALIDATION/TRANSFER',
  `distribution_required` tinyint NOT NULL DEFAULT 0,
  `training_required` tinyint NOT NULL DEFAULT 0,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_file_category_tenant_code` (`tenant_id`, `code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC file category';

CREATE TABLE IF NOT EXISTS `dcc_file_category_permission_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `category_id` bigint NOT NULL,
  `action_type` varchar(32) NOT NULL,
  `subject_type` varchar(32) NOT NULL,
  `subject_id` bigint NOT NULL,
  `active` tinyint NOT NULL DEFAULT 1,
  `remark` varchar(255) DEFAULT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_category_permission_subject` (`category_id`, `action_type`, `subject_type`, `subject_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC file category permission rule';

CREATE TABLE IF NOT EXISTS `dcc_file_category_distribution_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `category_id` bigint NOT NULL,
  `department_id` bigint NOT NULL,
  `distribution_medium` varchar(32) NOT NULL DEFAULT 'PUBLIC_FOLDER',
  `active` tinyint NOT NULL DEFAULT 1,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_category_distribution_department` (`category_id`, `department_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC file category distribution rule';

CREATE TABLE IF NOT EXISTS `dcc_file_category_training_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `category_id` bigint NOT NULL,
  `department_id` bigint NOT NULL,
  `active` tinyint NOT NULL DEFAULT 1,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_category_training_department` (`category_id`, `department_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC file category training rule';

CREATE TABLE IF NOT EXISTS `dcc_category_directory_binding` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `category_id` bigint NOT NULL,
  `directory_id` bigint NOT NULL,
  `active` tinyint NOT NULL DEFAULT 1,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_category_directory_binding` (`category_id`, `directory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC category directory binding';

CREATE TABLE IF NOT EXISTS `dcc_approval_position` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(64) NOT NULL,
  `name` varchar(128) NOT NULL,
  `active` tinyint NOT NULL DEFAULT 1,
  `source` varchar(32) NOT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_approval_position_tenant_code` (`tenant_id`, `code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC approval position';

CREATE TABLE IF NOT EXISTS `dcc_position_assignment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `position_id` bigint NOT NULL,
  `assignment_type` varchar(32) NOT NULL,
  `system_post_id` bigint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `role_id` bigint DEFAULT NULL,
  `post_id` bigint DEFAULT NULL,
  `department_id` bigint DEFAULT NULL,
  `active` tinyint NOT NULL DEFAULT 1,
  `change_reason` varchar(255) DEFAULT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_dcc_position_assignment_position` (`position_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC position assignment';

CREATE TABLE IF NOT EXISTS `dcc_category_approval_route` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `category_id` bigint NOT NULL,
  `version_no` int NOT NULL,
  `active` tinyint NOT NULL DEFAULT 1,
  `effective_time` datetime DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_category_route_version` (`category_id`, `version_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC category approval route';

CREATE TABLE IF NOT EXISTS `dcc_category_approval_route_node` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `route_id` bigint NOT NULL,
  `stage_no` int NOT NULL,
  `stage_code` varchar(64) DEFAULT NULL,
  `stage_name` varchar(128) NOT NULL,
  `stage_order` int DEFAULT NULL,
  `candidate_source_type` varchar(32) NOT NULL,
  `candidate_source_id` bigint DEFAULT NULL,
  `candidate_source_ids` varchar(1000) DEFAULT NULL,
  `approve_method` varchar(32) NOT NULL,
  `approve_ratio` int DEFAULT NULL,
  `require_all_approvals` tinyint NOT NULL DEFAULT 0,
  `required` tinyint NOT NULL DEFAULT 1,
  `sort` int NOT NULL DEFAULT 0,
  `stage_type` varchar(32) DEFAULT NULL COMMENT '规则阶段类型：DOC_CONTROL/SIGNOFF/APPROVAL',
  `subject_label` varchar(255) DEFAULT NULL COMMENT '主体标签',
  `marker` varchar(32) DEFAULT NULL COMMENT '标记',
  `subject_type` varchar(32) DEFAULT NULL COMMENT '主体类型',
  `subject_id` bigint DEFAULT NULL COMMENT '主体ID',
  `subject_name` varchar(255) DEFAULT NULL COMMENT '主体名称',
  `subject_department_path` varchar(500) DEFAULT NULL COMMENT '对应部门路径',
  `rule_remark` varchar(255) DEFAULT NULL COMMENT '规则备注',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_dcc_route_node_route` (`route_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC category approval route node';

CREATE TABLE IF NOT EXISTS `dcc_controlled_file_master` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `category_id` bigint NOT NULL,
  `directory_id` bigint DEFAULT NULL COMMENT 'DCC directory for this logical document chain',
  `file_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `file_number` varchar(64) NOT NULL,
  `current_active_controlled_file_id` bigint DEFAULT NULL,
  `status` varchar(32) NOT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_controlled_file_master_chain` (`category_id`, `directory_id`, `file_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC logical document chain';

CREATE TABLE IF NOT EXISTS `dcc_controlled_file` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `master_id` bigint NOT NULL,
  `category_id` bigint NOT NULL,
  `directory_id` bigint NOT NULL,
  `source_file_id` bigint NOT NULL,
  `original_file_id` bigint NOT NULL,
  `drawing_pdf_file_id` bigint DEFAULT NULL,
  `training_record_file_id` bigint DEFAULT NULL,
  `published_file_id` bigint DEFAULT NULL,
  `stamped_file_id` bigint DEFAULT NULL,
  `file_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `title` varchar(256) NOT NULL,
  `file_number` varchar(64) NOT NULL,
  `product_master_id` bigint DEFAULT NULL,
  `product_code` varchar(255) DEFAULT NULL,
  `product_name` varchar(255) DEFAULT NULL,
  `dcc_project_code_id` bigint DEFAULT NULL,
  `project_code_recognition_type` varchar(32) DEFAULT NULL,
  `project_code_recognition_text` varchar(255) DEFAULT NULL,
  `project_code_recognized_by` bigint DEFAULT NULL,
  `project_code_recognized_time` datetime DEFAULT NULL,
  `file_type_level1` varchar(64) DEFAULT NULL,
  `file_type_level2` varchar(128) DEFAULT NULL,
  `file_type_level3` varchar(128) DEFAULT NULL,
  `file_type_level4` varchar(128) DEFAULT NULL,
  `file_type_level5` varchar(128) DEFAULT NULL,
  `need_training` bit(1) NOT NULL DEFAULT b'0',
  `process_type` varchar(32) NOT NULL DEFAULT 'CONTROLLED_FILE',
  `change_type` varchar(32) NOT NULL DEFAULT 'NEW',
  `version_no` varchar(64) NOT NULL,
  `effective_date` date DEFAULT NULL,
  `remark` varchar(1024) DEFAULT NULL,
  `status` varchar(64) NOT NULL,
  `submitter_id` bigint NOT NULL,
  `requester_id` bigint NOT NULL,
  `process_instance_id` varchar(64) DEFAULT NULL,
  `process_definition_key` varchar(128) DEFAULT NULL,
  `submitted_time` datetime DEFAULT NULL,
  `approved_time` datetime DEFAULT NULL,
  `published_time` datetime DEFAULT NULL,
  `rejected_time` datetime DEFAULT NULL,
  `stamped_time` datetime DEFAULT NULL,
  `obsoleted_by` bigint DEFAULT NULL,
  `obsoleted_time` datetime DEFAULT NULL,
  `obsolete_reason` varchar(255) DEFAULT NULL,
  `superseded_by_file_id` bigint DEFAULT NULL,
  `reject_reason` varchar(255) DEFAULT NULL,
  `finalization_error` varchar(500) DEFAULT NULL,
  `checked_out_by` bigint DEFAULT NULL COMMENT '当前检出人用户ID',
  `checked_out_time` datetime DEFAULT NULL COMMENT '当前检出时间',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_dcc_controlled_file_master` (`master_id`),
  KEY `idx_dcc_controlled_file_category` (`category_id`),
  KEY `idx_dcc_controlled_file_directory` (`directory_id`),
  KEY `idx_dcc_controlled_file_status` (`status`),
  KEY `idx_dcc_controlled_file_project_code` (`tenant_id`, `dcc_project_code_id`),
  KEY `idx_dcc_controlled_file_type_level` (`tenant_id`, `file_type_level1`, `file_type_level2`),
  KEY `idx_dcc_controlled_file_checkout` (`tenant_id`, `checked_out_by`, `checked_out_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC controlled file revision';

CREATE TABLE IF NOT EXISTS `dcc_controlled_file_print_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `controlled_file_id` BIGINT NOT NULL,
  `file_number` VARCHAR(64) NOT NULL,
  `version_no` VARCHAR(64) NOT NULL,
  `print_no` VARCHAR(64) NOT NULL,
  `purpose` VARCHAR(255) NOT NULL,
  `copies` INT NOT NULL,
  `receiving_department` VARCHAR(128) NOT NULL,
  `use_location` VARCHAR(128) NOT NULL,
  `print_user_id` BIGINT NOT NULL,
  `print_user_name` VARCHAR(128) DEFAULT NULL,
  `print_time` DATETIME NOT NULL,
  `approval_status` VARCHAR(32) NOT NULL,
  `approval_user_id` BIGINT DEFAULT NULL,
  `approval_user_name` VARCHAR(128) DEFAULT NULL,
  `approval_time` DATETIME DEFAULT NULL,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `creator` VARCHAR(64) DEFAULT NULL,
  `updater` VARCHAR(64) DEFAULT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_controlled_print_no` (`tenant_id`, `print_no`, `deleted`),
  KEY `idx_dcc_controlled_print_file` (`tenant_id`, `controlled_file_id`, `print_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC controlled file print record';

CREATE TABLE IF NOT EXISTS `dcc_controlled_file_route_snapshot` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `controlled_file_id` bigint NOT NULL,
  `route_version_no` int NOT NULL,
  `stage_no` int NOT NULL,
  `stage_code` varchar(64) DEFAULT NULL,
  `stage_name` varchar(128) DEFAULT NULL,
  `stage_order` int DEFAULT NULL,
  `candidate_source_type` varchar(32) NOT NULL,
  `candidate_source_id` bigint DEFAULT NULL,
  `candidate_source_ids` varchar(1000) DEFAULT NULL,
  `resolved_user_ids` varchar(1000) DEFAULT NULL,
  `approve_method` varchar(32) NOT NULL,
  `approve_ratio` int DEFAULT NULL,
  `require_all_approvals` tinyint NOT NULL DEFAULT 0,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_dcc_route_snapshot_file` (`controlled_file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC controlled file route snapshot';

CREATE TABLE IF NOT EXISTS `dcc_external_file_review` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `controlled_file_id` bigint NOT NULL COMMENT 'DCC 主记录编号',
  `external_source` varchar(128) NOT NULL COMMENT '外来来源',
  `external_owner` varchar(128) NOT NULL COMMENT '外来归属或责任方',
  `review_reason` varchar(500) NOT NULL COMMENT '评审原因',
  `participant_user_ids` varchar(500) NOT NULL COMMENT '参与人用户编号，逗号分隔',
  `review_conclusion` varchar(64) DEFAULT NULL COMMENT '评审结论',
  `conclusion_comment` varchar(1000) DEFAULT NULL COMMENT '结论说明',
  `output_file_id` bigint DEFAULT NULL COMMENT '输出物文件编号',
  `closed_time` datetime DEFAULT NULL COMMENT '闭环时间',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `creator` varchar(64) DEFAULT NULL COMMENT '创建者',
  `updater` varchar(64) DEFAULT NULL COMMENT '更新者',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_external_file_review_file` (`controlled_file_id`, `tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC 外来文件评审扩展';

CREATE TABLE IF NOT EXISTS `dcc_controlled_file_signature` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `controlled_file_id` bigint NOT NULL,
  `revision_id` bigint DEFAULT NULL,
  `version_no` varchar(64) DEFAULT NULL,
  `task_id` varchar(64) NOT NULL,
  `actor_id` bigint NOT NULL,
  `actor_username_snapshot` varchar(64) DEFAULT NULL,
  `actor_nickname_snapshot` varchar(64) DEFAULT NULL,
  `actor_dept_id_snapshot` bigint DEFAULT NULL,
  `actor_dept_name_snapshot` varchar(128) DEFAULT NULL,
  `actor_post_names_snapshot` varchar(512) DEFAULT NULL,
  `actor_role_names_snapshot` varchar(512) DEFAULT NULL,
  `signature_purpose` varchar(128) DEFAULT NULL,
  `authorization_basis` varchar(500) DEFAULT NULL,
  `authentication_method` varchar(64) DEFAULT NULL,
  `record_version_snapshot` varchar(64) DEFAULT NULL,
  `record_hash_snapshot` varchar(128) DEFAULT NULL,
  `client_ip_snapshot` varchar(64) DEFAULT NULL,
  `user_agent_snapshot` varchar(512) DEFAULT NULL,
  `snapshot_status` varchar(32) DEFAULT NULL,
  `action_type` varchar(32) NOT NULL,
  `meaning_code` varchar(64) DEFAULT NULL,
  `meaning_label` varchar(128) DEFAULT NULL,
  `signature_mode` varchar(32) NOT NULL,
  `password_verified` tinyint NOT NULL DEFAULT 0,
  `comment` varchar(500) DEFAULT NULL,
  `signed_at` datetime DEFAULT NULL,
  `source_file_id` bigint DEFAULT NULL,
  `source_file_hash` varchar(128) DEFAULT NULL,
  `source_file_hash_algorithm` varchar(32) DEFAULT NULL,
  `source_file_hash_status` varchar(32) DEFAULT 'HISTORICAL_UNBOUND',
  `controlled_copy_file_id` bigint DEFAULT NULL,
  `controlled_copy_hash` varchar(128) DEFAULT NULL,
  `controlled_copy_hash_algorithm` varchar(32) DEFAULT NULL,
  `controlled_copy_hash_status` varchar(32) DEFAULT 'NOT_APPLICABLE',
  `signature_image_id` bigint DEFAULT NULL,
  `signature_image_version_no` int DEFAULT NULL,
  `signature_image_file_id` bigint DEFAULT NULL,
  `signature_image_file_url` varchar(512) DEFAULT NULL,
  `signature_image_sha256` varchar(128) DEFAULT NULL,
  `signature_image_content_type` varchar(128) DEFAULT NULL,
  `signature_image_file_size` bigint DEFAULT NULL,
  `signature_image_status_snapshot` varchar(32) DEFAULT NULL,
  `signature_image_verified_status` varchar(32) DEFAULT NULL,
  `evidence_payload_version` varchar(32) DEFAULT NULL,
  `evidence_key_version` varchar(64) DEFAULT NULL,
  `evidence_hash` varchar(128) DEFAULT NULL,
  `evidence_hash_algorithm` varchar(32) DEFAULT NULL,
  `evidence_status` varchar(32) DEFAULT 'HISTORICAL_UNBOUND',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_dcc_signature_file` (`controlled_file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC controlled file signature evidence';

CREATE TABLE IF NOT EXISTS `dcc_electronic_signature_image` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `version_no` int NOT NULL,
  `file_id` bigint NOT NULL,
  `file_url` varchar(512) NOT NULL,
  `storage_path` varchar(512) NOT NULL,
  `file_name` varchar(255) NOT NULL,
  `content_type` varchar(128) NOT NULL,
  `file_size` bigint NOT NULL,
  `sha256` varchar(128) NOT NULL,
  `image_status` varchar(32) NOT NULL,
  `active` tinyint NOT NULL DEFAULT 0,
  `uploaded_by` bigint NOT NULL,
  `uploaded_at` datetime NOT NULL,
  `enabled_at` datetime DEFAULT NULL,
  `disabled_at` datetime DEFAULT NULL,
  `disable_reason` varchar(500) DEFAULT NULL,
  `referenced_count` int NOT NULL DEFAULT 0,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_signature_image_user_version` (`tenant_id`, `user_id`, `version_no`, `deleted`),
  KEY `idx_dcc_signature_image_user_active` (`tenant_id`, `user_id`, `active`, `deleted`),
  KEY `idx_dcc_signature_image_file` (`file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC electronic signature image versions';

CREATE TABLE IF NOT EXISTS `dcc_electronic_signature_authorization` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `electronic_signature_enabled` tinyint NOT NULL DEFAULT 1,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_esign_authorization_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC electronic signature authorization';

CREATE TABLE IF NOT EXISTS `dcc_approval_print_template` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `template_file_id` bigint NOT NULL,
  `template_file_name` varchar(255) NOT NULL,
  `template_file_content_type` varchar(255) DEFAULT NULL,
  `active` tinyint NOT NULL DEFAULT 1,
  `remark` varchar(500) DEFAULT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_dcc_approval_print_template_tenant_active` (`tenant_id`, `active`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC approval print template';

CREATE TABLE IF NOT EXISTS `dcc_controlled_file_distribution` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `controlled_file_id` bigint NOT NULL,
  `department_id` bigint NOT NULL,
  `distribution_medium` varchar(32) NOT NULL DEFAULT 'PUBLIC_FOLDER',
  `status` varchar(32) NOT NULL,
  `acknowledged_by` bigint DEFAULT NULL,
  `acknowledged_at` datetime DEFAULT NULL,
  `recovered_by` bigint DEFAULT NULL,
  `recovered_at` datetime DEFAULT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_dcc_distribution_file` (`controlled_file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC controlled file distribution';

CREATE TABLE IF NOT EXISTS `dcc_controlled_file_distribution_recipient` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `distribution_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `message_job_id` bigint DEFAULT NULL,
  `read_at` datetime DEFAULT NULL,
  `acknowledged_at` datetime DEFAULT NULL,
  `ack_comment` varchar(1000) DEFAULT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_dcc_distribution_recipient_distribution` (`distribution_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC controlled file distribution recipient';

CREATE TABLE IF NOT EXISTS `dcc_controlled_file_training` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `controlled_file_id` bigint NOT NULL,
  `department_id` bigint NOT NULL,
  `status` varchar(32) NOT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_dcc_training_file` (`controlled_file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC controlled file training';

CREATE TABLE IF NOT EXISTS `dcc_controlled_file_training_assignment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `training_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `message_job_id` bigint DEFAULT NULL,
  `status` varchar(32) NOT NULL,
  `acknowledged_at` datetime DEFAULT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_dcc_training_assignment_training` (`training_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC controlled file training assignment';

CREATE TABLE IF NOT EXISTS `dcc_controlled_file_training_progress` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `controlled_file_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `required_view_seconds` int NOT NULL DEFAULT 600,
  `accumulated_view_seconds` int NOT NULL DEFAULT 0,
  `first_viewed_at` datetime DEFAULT NULL,
  `last_viewed_at` datetime DEFAULT NULL,
  `acknowledged_at` datetime DEFAULT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_training_progress_file_user` (`controlled_file_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC controlled file user training progress';

CREATE TABLE IF NOT EXISTS `dcc_controlled_file_training_view_session` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `training_progress_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `client_session_id` varchar(64) NOT NULL,
  `started_at` datetime NOT NULL,
  `last_heartbeat_at` datetime DEFAULT NULL,
  `ended_at` datetime DEFAULT NULL,
  `accumulated_seconds` int NOT NULL DEFAULT 0,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_dcc_training_view_session_progress` (`training_progress_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC controlled file training focused-view sessions';

CREATE TABLE IF NOT EXISTS `dcc_controlled_file_message_job` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `business_type` varchar(32) NOT NULL,
  `business_id` bigint NOT NULL,
  `template_code` varchar(64) NOT NULL,
  `recipient_user_id` bigint NOT NULL,
  `status` varchar(32) NOT NULL,
  `error_message` varchar(500) DEFAULT NULL,
  `sent_at` datetime DEFAULT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_dcc_message_job_business` (`business_type`, `business_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC controlled file message job';

CREATE TABLE IF NOT EXISTS `dcc_controlled_file_nas_transfer_task` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `operator_user_id` bigint NOT NULL,
  `template_category_id` bigint NOT NULL,
  `dcc_project_code_id` bigint DEFAULT NULL COMMENT 'DCC project code selected for DCC submit',
  `product_master_id` bigint DEFAULT NULL COMMENT 'MDM product selected for DCC submit',
  `effective_date` date NOT NULL,
  `selected_nas_paths_json` longtext NOT NULL,
  `source_type` varchar(32) NOT NULL DEFAULT 'NAS' COMMENT 'NAS or LOCAL_FOLDER',
  `expected_file_count` bigint NOT NULL DEFAULT 0 COMMENT 'Expected files for LOCAL_FOLDER upload session',
  `expected_total_bytes` bigint NOT NULL DEFAULT 0 COMMENT 'Expected bytes for LOCAL_FOLDER upload session',
  `uploaded_file_count` bigint NOT NULL DEFAULT 0 COMMENT 'Uploaded files for LOCAL_FOLDER upload session',
  `uploaded_total_bytes` bigint NOT NULL DEFAULT 0 COMMENT 'Uploaded bytes for LOCAL_FOLDER upload session',
  `upload_completed_at` datetime DEFAULT NULL COMMENT 'LOCAL_FOLDER upload completion time',
  `status` varchar(32) NOT NULL,
  `next_check_at` datetime DEFAULT NULL,
  `last_run_at` datetime DEFAULT NULL,
  `completed_at` datetime DEFAULT NULL,
  `last_failure_message` varchar(2048) DEFAULT NULL,
  `failure_report_path` varchar(512) DEFAULT NULL,
  `failure_report_generated_at` varchar(64) DEFAULT NULL,
  `failure_report_error` varchar(512) DEFAULT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_dcc_nas_transfer_task_status` (`status`, `next_check_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC NAS transfer async task';

CREATE TABLE IF NOT EXISTS `dcc_controlled_file_nas_transfer_task_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `parent_item_id` bigint DEFAULT NULL,
  `item_type` varchar(16) NOT NULL,
  `nas_path` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `item_name` varchar(255) NOT NULL,
  `source_file_id` bigint DEFAULT NULL COMMENT 'infra_file.id for LOCAL_FOLDER file items',
  `status` varchar(32) NOT NULL,
  `attempt_count` int NOT NULL DEFAULT 0,
  `failure_stage` varchar(32) DEFAULT NULL,
  `last_error` varchar(512) DEFAULT NULL,
  `resolved_directory_id` bigint DEFAULT NULL,
  `resolved_category_id` bigint DEFAULT NULL,
  `directory_outcome` varchar(16) DEFAULT NULL,
  `category_outcome` varchar(16) DEFAULT NULL,
  `preview_download_only` bit(1) NOT NULL DEFAULT b'0',
  `last_attempt_at` datetime DEFAULT NULL,
  `completed_at` datetime DEFAULT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_nas_transfer_task_item_path` (`task_id`, `nas_path`),
  KEY `idx_dcc_nas_transfer_task_item_status` (`task_id`, `status`, `id`),
  KEY `idx_dcc_nas_transfer_task_item_parent` (`parent_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC NAS transfer async task item';

CREATE TABLE IF NOT EXISTS `dcc_controlled_file_local_folder_upload_chunk` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint NOT NULL COMMENT 'dcc_controlled_file_nas_transfer_task.id',
  `relative_path` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT 'Browser local folder relative path',
  `file_name` varchar(255) NOT NULL COMMENT 'Original file name',
  `file_size` bigint NOT NULL COMMENT 'Original file byte size',
  `chunk_index` int NOT NULL COMMENT 'Zero-based chunk index',
  `total_chunks` int NOT NULL COMMENT 'Total chunks of the original file',
  `chunk_size` bigint NOT NULL COMMENT 'Received chunk byte size',
  `chunk_sha256` varchar(64) NOT NULL COMMENT 'SHA-256 hex digest of the received chunk',
  `chunk_temp_path` varchar(1024) NOT NULL COMMENT 'Server-side persisted temporary chunk path',
  `status` varchar(32) NOT NULL DEFAULT 'COMPLETED' COMMENT 'Chunk upload status',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_local_folder_chunk_position` (`task_id`, `relative_path`, `chunk_index`),
  KEY `idx_dcc_local_folder_chunk_file` (`task_id`, `relative_path`),
  KEY `idx_dcc_local_folder_chunk_status` (`task_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC local folder resumable upload chunks';

CREATE TABLE IF NOT EXISTS `dcc_controlled_file_obsolete_audit` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `controlled_file_id` bigint NOT NULL,
  `operator_id` bigint NOT NULL,
  `obsolete_reason` varchar(255) NOT NULL,
  `status_before` varchar(32) NOT NULL,
  `status_after` varchar(32) NOT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_dcc_obsolete_audit_file` (`controlled_file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC controlled file obsolete audit';

CREATE TABLE IF NOT EXISTS `dcc_controlled_file_stamp` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `controlled_file_id` bigint NOT NULL,
  `stamp_type` varchar(32) NOT NULL,
  `template_id` varchar(64) NOT NULL,
  `renderer_type` varchar(32) NOT NULL,
  `stamp_text` varchar(255) DEFAULT NULL,
  `output_format` varchar(32) NOT NULL,
  `page_positions_json` varchar(2000) DEFAULT NULL,
  `source_file_id` bigint NOT NULL,
  `output_file_id` bigint DEFAULT NULL,
  `status` varchar(32) NOT NULL,
  `error_message` varchar(500) DEFAULT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_dcc_file_stamp_file` (`controlled_file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC controlled file stamp record';

CREATE TABLE IF NOT EXISTS `dcc_controlled_file_access_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `controlled_file_id` bigint DEFAULT NULL,
  `access_event_id` bigint DEFAULT NULL,
  `access_event_code` varchar(64) DEFAULT NULL,
  `watermark_trace_code` varchar(64) DEFAULT NULL,
  `file_version_no` varchar(64) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `action_type` varchar(32) NOT NULL,
  `purpose` varchar(64) DEFAULT NULL,
  `result` varchar(32) NOT NULL,
  `failure_code` varchar(64) DEFAULT NULL,
  `reason` varchar(255) DEFAULT NULL,
  `source_ip` varchar(64) DEFAULT NULL,
  `request_id` varchar(128) DEFAULT NULL,
  `user_agent` varchar(512) DEFAULT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_dcc_access_log_file` (`controlled_file_id`),
  KEY `idx_dcc_access_log_event` (`access_event_id`),
  KEY `idx_dcc_access_log_event_code` (`access_event_code`),
  KEY `idx_dcc_access_log_request` (`request_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC controlled file access log';

CREATE TABLE IF NOT EXISTS `dcc_controlled_file_access_event` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `access_event_code` varchar(64) NOT NULL,
  `controlled_file_id` bigint NOT NULL,
  `file_version_no` varchar(64) NOT NULL,
  `user_id` bigint NOT NULL,
  `access_type` varchar(32) NOT NULL,
  `purpose` varchar(64) NOT NULL,
  `result` varchar(32) NOT NULL,
  `failure_code` varchar(64) DEFAULT NULL,
  `failure_reason` varchar(500) DEFAULT NULL,
  `source_ip` varchar(64) DEFAULT NULL,
  `user_agent` varchar(512) DEFAULT NULL,
  `request_id` varchar(128) DEFAULT NULL,
  `occurred_at` datetime NOT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_protection_access_event_code` (`tenant_id`, `access_event_code`),
  KEY `idx_dcc_protection_access_event_file` (`controlled_file_id`, `file_version_no`),
  KEY `idx_dcc_protection_access_event_user_time` (`user_id`, `occurred_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC controlled file access event';

CREATE TABLE IF NOT EXISTS `dcc_controlled_file_watermark_trace` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `trace_code` varchar(64) NOT NULL,
  `access_event_id` bigint NOT NULL,
  `access_event_code` varchar(64) NOT NULL,
  `controlled_file_id` bigint NOT NULL,
  `file_number` varchar(64) NOT NULL,
  `file_version_no` varchar(64) NOT NULL,
  `user_id` bigint NOT NULL,
  `user_identifier` varchar(64) DEFAULT NULL,
  `user_display_name` varchar(128) DEFAULT NULL,
  `dept_id` bigint DEFAULT NULL,
  `dept_name` varchar(128) DEFAULT NULL,
  `tenant_name` varchar(128) DEFAULT NULL,
  `privacy_mode` varchar(32) NOT NULL,
  `watermark_payload_json` longtext NOT NULL,
  `issued_at` datetime NOT NULL,
  `expires_at` datetime DEFAULT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_protection_watermark_trace_code` (`tenant_id`, `trace_code`),
  KEY `idx_dcc_protection_watermark_event` (`access_event_id`),
  KEY `idx_dcc_protection_watermark_file` (`controlled_file_id`, `file_version_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC controlled file watermark trace';

CREATE TABLE IF NOT EXISTS `dcc_controlled_file_upload_policy` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `policy_code` varchar(64) NOT NULL,
  `scope_type` varchar(32) NOT NULL,
  `category_id` bigint DEFAULT NULL,
  `purpose` varchar(64) DEFAULT NULL,
  `max_bytes` bigint NOT NULL,
  `enabled` bit(1) NOT NULL,
  `priority` int NOT NULL,
  `policy_version` varchar(64) NOT NULL,
  `effective_from` datetime DEFAULT NULL,
  `effective_to` datetime DEFAULT NULL,
  `change_reason` varchar(500) NOT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_protection_upload_policy_code` (`tenant_id`, `policy_code`),
  UNIQUE KEY `uk_dcc_protection_upload_policy_scope` (`tenant_id`, `scope_type`, `category_id`, `purpose`, `policy_version`),
  KEY `idx_dcc_protection_upload_policy_lookup` (`tenant_id`, `scope_type`, `enabled`, `priority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC controlled file upload size policy';

CREATE TABLE IF NOT EXISTS `dcc_controlled_file_temporary_file` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `upload_ticket` varchar(64) NOT NULL,
  `session_id` varchar(128) NOT NULL,
  `purpose` varchar(64) NOT NULL,
  `uploader_id` bigint NOT NULL,
  `original_file_name` varchar(255) NOT NULL,
  `content_type` varchar(255) DEFAULT NULL,
  `file_size` bigint NOT NULL,
  `file_sha256` varchar(128) NOT NULL,
  `storage_file_id` bigint NOT NULL,
  `status` varchar(32) NOT NULL,
  `expire_time` datetime NOT NULL,
  `bound_controlled_file_id` bigint DEFAULT NULL,
  `bound_time` datetime DEFAULT NULL,
  `cleanup_status` varchar(32) NOT NULL,
  `cleanup_reason` varchar(500) DEFAULT NULL,
  `cleanup_time` datetime DEFAULT NULL,
  `request_id` varchar(128) DEFAULT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_protection_upload_ticket` (`tenant_id`, `upload_ticket`),
  KEY `idx_dcc_protection_temp_session` (`tenant_id`, `session_id`, `purpose`, `uploader_id`),
  KEY `idx_dcc_protection_temp_request` (`tenant_id`, `request_id`, `uploader_id`),
  KEY `idx_dcc_protection_temp_status` (`tenant_id`, `status`, `expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC controlled file temporary upload file';

CREATE TABLE IF NOT EXISTS `dcc_controlled_file_download_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `download_request_id` varchar(64) NOT NULL,
  `access_event_id` bigint NOT NULL,
  `access_event_code` varchar(64) NOT NULL,
  `controlled_file_id` bigint NOT NULL,
  `file_version_no` varchar(64) NOT NULL,
  `user_id` bigint NOT NULL,
  `policy_version` varchar(64) NOT NULL,
  `encryption_status` varchar(32) NOT NULL,
  `encryption_policy_version` varchar(64) DEFAULT NULL,
  `artifact_id` varchar(128) DEFAULT NULL,
  `cipher_file_ref` varchar(255) DEFAULT NULL,
  `plain_sha256` varchar(128) DEFAULT NULL,
  `cipher_sha256` varchar(128) DEFAULT NULL,
  `failure_code` varchar(64) DEFAULT NULL,
  `failure_reason` varchar(500) DEFAULT NULL,
  `requested_at` datetime NOT NULL,
  `encrypted_at` datetime DEFAULT NULL,
  `returned_at` datetime DEFAULT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_protection_download_request` (`tenant_id`, `download_request_id`),
  KEY `idx_dcc_protection_download_event` (`access_event_id`),
  KEY `idx_dcc_protection_download_file` (`controlled_file_id`, `file_version_no`),
  KEY `idx_dcc_protection_download_user_time` (`user_id`, `requested_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC controlled file encrypted download record';

CREATE TABLE IF NOT EXISTS `dcc_project_code` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_master_id` bigint DEFAULT NULL COMMENT 'MDM product master id',
  `doc_control_no` varchar(64) DEFAULT NULL,
  `project_name` varchar(255) NOT NULL,
  `project_code` varchar(64) NOT NULL DEFAULT '',
  `category` varchar(128) DEFAULT NULL,
  `commissioned_production` varchar(128) DEFAULT NULL,
  `project_leader` varchar(128) DEFAULT NULL,
  `project_engineer` varchar(128) DEFAULT NULL,
  `storage_location` varchar(128) DEFAULT NULL,
  `priority` varchar(64) DEFAULT NULL,
  `status` varchar(32) NOT NULL,
  `last_import_batch_id` bigint DEFAULT NULL,
  `batch_record_total_recognition_json` longtext DEFAULT NULL COMMENT 'Word batch-record total recognition JSON',
  `associated_file_count` bigint NOT NULL DEFAULT 0,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_project_code_tenant_project` (`tenant_id`, `project_name`, `project_code`),
  KEY `idx_dcc_project_code_product` (`tenant_id`, `product_master_id`),
  KEY `idx_dcc_project_code_status` (`tenant_id`, `status`),
  KEY `idx_dcc_project_code_category` (`tenant_id`, `category`),
  KEY `idx_dcc_project_code_priority` (`tenant_id`, `priority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC project code basic data';

CREATE TABLE IF NOT EXISTS `dcc_product_onboarding_request` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_master_id` bigint DEFAULT NULL COMMENT 'Existing or generated MDM product id',
  `product_code` varchar(64) DEFAULT NULL COMMENT 'MDM product code snapshot',
  `dcc_product_code` varchar(14) DEFAULT NULL COMMENT 'DCC product code snapshot',
  `product_name_cn` varchar(255) DEFAULT NULL COMMENT 'Chinese product name snapshot',
  `product_name_en` varchar(255) DEFAULT NULL COMMENT 'English product name snapshot',
  `model_specification` varchar(255) DEFAULT NULL COMMENT 'Model/specification snapshot',
  `product_category` varchar(128) DEFAULT NULL COMMENT 'MDM product category snapshot',
  `doc_control_no` varchar(64) DEFAULT NULL,
  `project_name` varchar(255) NOT NULL,
  `project_code` varchar(64) NOT NULL DEFAULT '',
  `category` varchar(128) DEFAULT NULL,
  `commissioned_production` varchar(128) DEFAULT NULL,
  `project_leader` varchar(128) DEFAULT NULL,
  `project_engineer` varchar(128) DEFAULT NULL,
  `storage_location` varchar(128) DEFAULT NULL,
  `priority` varchar(64) DEFAULT NULL,
  `status` varchar(32) NOT NULL,
  `applicant_user_id` bigint NOT NULL,
  `approver_user_id` bigint DEFAULT NULL,
  `approved_time` datetime DEFAULT NULL,
  `generated_project_code_id` bigint DEFAULT NULL,
  `reject_reason` varchar(512) DEFAULT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_product_onboarding_pending_project` (`tenant_id`, `project_name`, `project_code`, `status`, `deleted`),
  KEY `idx_dcc_product_onboarding_status` (`tenant_id`, `status`, `deleted`),
  KEY `idx_dcc_product_onboarding_product` (`tenant_id`, `product_master_id`, `deleted`),
  KEY `idx_dcc_product_onboarding_generated` (`tenant_id`, `generated_project_code_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC product onboarding request';

CREATE TABLE IF NOT EXISTS `dcc_project_code_alias_mapping` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_code_id` bigint NOT NULL,
  `alias_text` varchar(255) NOT NULL,
  `normalized_alias_text` varchar(255) NOT NULL,
  `alias_source` varchar(32) NOT NULL,
  `status` varchar(32) NOT NULL,
  `active` bit(1) NOT NULL DEFAULT b'1',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_project_alias_tenant_text` (`tenant_id`, `normalized_alias_text`, `alias_source`, `project_code_id`),
  KEY `idx_dcc_project_alias_status` (`tenant_id`, `status`, `active`),
  KEY `idx_dcc_project_alias_project` (`tenant_id`, `project_code_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC project code confirmed alias and directory mapping';

CREATE TABLE IF NOT EXISTS `dcc_project_code_import_batch` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `status` varchar(32) NOT NULL,
  `total_count` int NOT NULL DEFAULT 0,
  `create_count` int NOT NULL DEFAULT 0,
  `update_count` int NOT NULL DEFAULT 0,
  `disable_count` int NOT NULL DEFAULT 0,
  `unchanged_count` int NOT NULL DEFAULT 0,
  `failure_count` int NOT NULL DEFAULT 0,
  `confirmed_at` datetime DEFAULT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_dcc_project_code_import_batch_status` (`tenant_id`, `status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC project code import batch';

CREATE TABLE IF NOT EXISTS `dcc_project_code_import_row` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_id` bigint NOT NULL,
  `row_no` int NOT NULL,
  `doc_control_no` varchar(64) DEFAULT NULL,
  `project_name` varchar(255) DEFAULT NULL,
  `project_code` varchar(64) NOT NULL DEFAULT '',
  `category` varchar(128) DEFAULT NULL,
  `commissioned_production` varchar(128) DEFAULT NULL,
  `project_leader` varchar(128) DEFAULT NULL,
  `project_engineer` varchar(128) DEFAULT NULL,
  `storage_location` varchar(128) DEFAULT NULL,
  `priority` varchar(64) DEFAULT NULL,
  `current_status` varchar(32) DEFAULT NULL,
  `import_action` varchar(32) NOT NULL,
  `failure_reason` varchar(500) DEFAULT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_dcc_project_code_import_row_batch` (`tenant_id`, `batch_id`, `row_no`),
  KEY `idx_dcc_project_code_import_row_action` (`tenant_id`, `batch_id`, `import_action`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC project code import preview row';

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6800, '文控中心', '', 1, 70, 0, '/dcc', 'ep:files', '', 'DccRoot', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 6800 OR `path` = '/dcc');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6801, '文档目录', 'dcc:controlled-file:directory:manage', 2, 1, 6800, 'controlled-file/directories', 'ep:folder', 'dcc/controlled-file/directories/index', 'DccControlledFileDirectories', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'dcc:controlled-file:directory:manage');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6802, 'DCC访问规则', 'dcc:controlled-file:access-rule:manage', 2, 2, 6800, 'controlled-file/access-rules', 'ep:lock', 'dcc/controlled-file/access-rules/index', 'DccControlledFileAccessRules', 1, b'0', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'dcc:controlled-file:access-rule:manage');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6803, '文控权限', 'dcc:controlled-file:category:manage', 2, 3, 6800, 'controlled-file/categories', 'ep:collection-tag', 'dcc/controlled-file/categories/index', 'DccControlledFileCategories', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'dcc:controlled-file:category:manage');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 990200, '基础数据', '', 1, 35, 0, '/mdm', 'ep:coin', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 990200 OR `path` = '/mdm');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 990210, 'DCC项目代码', 'dcc:project-code:query', 2, 20, 990200, 'project-code', 'ep:data-analysis', 'dcc/controlled-file/basic-data/project-code/index', 'DccProjectCodeBasicDataPage', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 990210 OR (`parent_id` = 990200 AND `path` = 'project-code') OR `permission` = 'dcc:project-code:query');

UPDATE `system_menu`
SET `name` = 'DCC项目代码',
    `parent_id` = 990200,
    `path` = 'project-code',
    `icon` = 'ep:data-analysis',
    `component` = 'dcc/controlled-file/basic-data/project-code/index',
    `component_name` = 'DccProjectCodeBasicDataPage',
    `permission` = 'dcc:project-code:query',
    `type` = 2,
    `sort` = 20,
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE `permission` = 'dcc:project-code:query'
  AND `deleted` = b'0';

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 990216, 'DCC产品目录', '', 2, 21, 990200, 'product-catalog', 'ep:goods', 'dcc/controlled-file/basic-data/product-catalog/index', 'DccProductCatalogBasicDataPage', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 990216 OR (`parent_id` = 990200 AND `path` = 'product-catalog'));

UPDATE `system_menu`
SET `name` = 'DCC产品目录',
    `permission` = '',
    `parent_id` = 990200,
    `path` = 'product-catalog',
    `icon` = 'ep:goods',
    `component` = 'dcc/controlled-file/basic-data/product-catalog/index',
    `component_name` = 'DccProductCatalogBasicDataPage',
    `type` = 2,
    `sort` = 21,
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE `id` = 990216
  AND `deleted` = b'0';

SET @dcc_project_code_menu_id := (
    SELECT `id`
    FROM `system_menu`
    WHERE `path` = 'project-code'
      AND `deleted` = b'0'
    LIMIT 1
);

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 990211, 'DCC项目代码新增', 'dcc:project-code:create', 3, 1, @dcc_project_code_menu_id, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @dcc_project_code_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 990211 OR `permission` = 'dcc:project-code:create');

UPDATE `system_menu`
SET `name` = 'DCC项目代码新增',
    `parent_id` = @dcc_project_code_menu_id,
    `type` = 3,
    `sort` = 1,
    `path` = '',
    `icon` = '',
    `component` = '',
    `component_name` = '',
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE @dcc_project_code_menu_id IS NOT NULL
  AND `permission` = 'dcc:project-code:create'
  AND `deleted` = b'0';

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 990212, 'DCC项目代码编辑', 'dcc:project-code:update', 3, 2, @dcc_project_code_menu_id, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @dcc_project_code_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 990212 OR `permission` = 'dcc:project-code:update');

UPDATE `system_menu`
SET `name` = 'DCC项目代码编辑',
    `parent_id` = @dcc_project_code_menu_id,
    `type` = 3,
    `sort` = 2,
    `path` = '',
    `icon` = '',
    `component` = '',
    `component_name` = '',
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE @dcc_project_code_menu_id IS NOT NULL
  AND `permission` = 'dcc:project-code:update'
  AND `deleted` = b'0';

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 990213, 'DCC项目代码删除', 'dcc:project-code:delete', 3, 3, @dcc_project_code_menu_id, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @dcc_project_code_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 990213 OR `permission` = 'dcc:project-code:delete');

UPDATE `system_menu`
SET `name` = 'DCC项目代码删除',
    `parent_id` = @dcc_project_code_menu_id,
    `type` = 3,
    `sort` = 3,
    `path` = '',
    `icon` = '',
    `component` = '',
    `component_name` = '',
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE @dcc_project_code_menu_id IS NOT NULL
  AND `permission` = 'dcc:project-code:delete'
  AND `deleted` = b'0';

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 990214, 'DCC项目代码导入', 'dcc:project-code:import', 3, 4, @dcc_project_code_menu_id, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @dcc_project_code_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 990214 OR `permission` = 'dcc:project-code:import');

UPDATE `system_menu`
SET `name` = 'DCC项目代码导入',
    `parent_id` = @dcc_project_code_menu_id,
    `type` = 3,
    `sort` = 4,
    `path` = '',
    `icon` = '',
    `component` = '',
    `component_name` = '',
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE @dcc_project_code_menu_id IS NOT NULL
  AND `permission` = 'dcc:project-code:import'
  AND `deleted` = b'0';

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 990215, 'DCC项目代码导出', 'dcc:project-code:export', 3, 5, @dcc_project_code_menu_id, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @dcc_project_code_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 990215 OR `permission` = 'dcc:project-code:export');

UPDATE `system_menu`
SET `name` = 'DCC项目代码导出',
    `parent_id` = @dcc_project_code_menu_id,
    `type` = 3,
    `sort` = 5,
    `path` = '',
    `icon` = '',
    `component` = '',
    `component_name` = '',
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE @dcc_project_code_menu_id IS NOT NULL
  AND `permission` = 'dcc:project-code:export'
  AND `deleted` = b'0';

INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `updater`, `tenant_id`)
SELECT src.`role_id`, target_menu.`id`, src.`creator`, src.`updater`, src.`tenant_id`
FROM `system_role_menu` src
JOIN `system_menu` source_menu
  ON source_menu.`path` = 'controlled-file/categories'
 AND source_menu.`deleted` = b'0'
JOIN `system_menu` target_menu
  ON (
      target_menu.`path` IN ('project-code', 'product-catalog')
      OR target_menu.`permission` IN (
          'dcc:project-code:create',
          'dcc:project-code:update',
          'dcc:project-code:delete',
          'dcc:project-code:import',
          'dcc:project-code:export'
      )
 )
 AND target_menu.`deleted` = b'0'
WHERE src.`menu_id` = source_menu.`id`
  AND src.`deleted` = b'0'
  AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` existing
      WHERE existing.`role_id` = src.`role_id`
        AND existing.`menu_id` = target_menu.`id`
        AND existing.`deleted` = b'0'
  );

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6804, '审批角色', 'dcc:controlled-file:position:manage', 2, 3, 101, 'approval-role', 'ep:user', 'dcc/controlled-file/positions/index', 'DccControlledFilePositions', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'dcc:controlled-file:position:manage');

UPDATE `system_menu`
SET `name` = '审批角色',
    `type` = 2,
    `sort` = 3,
    `parent_id` = 101,
    `path` = 'approval-role',
    `icon` = 'ep:user',
    `component` = 'dcc/controlled-file/positions/index',
    `component_name` = 'DccControlledFilePositions',
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE `permission` = 'dcc:controlled-file:position:manage'
  AND `deleted` = b'0';

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6805, '上传审批', 'dcc:controlled-file:route:manage', 2, 5, 6800, 'controlled-file/routes', 'ep:share', 'dcc/controlled-file/routes/index', 'DccControlledFileRoutes', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'dcc:controlled-file:route:manage');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6806, '文件上传', 'dcc:controlled-file:submit', 2, 6, 6800, 'controlled-file/upload', 'ep:upload', 'dcc/controlled-file/upload/index', 'DccControlledFileUpload', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'dcc:controlled-file:submit');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6807, '受控浏览', 'dcc:controlled-file:query', 2, 7, 6800, 'controlled-file/browser', 'ep:document', 'dcc/controlled-file/browser/index', 'DccControlledFileBrowser', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'dcc:controlled-file:query' AND `path` = 'controlled-file/browser');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6811, 'DCC受控下载', 'dcc:controlled-file:download', 3, 2, 6807, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'dcc:controlled-file:download');

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 'DCC受控打印', 'dcc:controlled-file:print', 3, 4, 6807, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'dcc:controlled-file:print');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6812, 'DCC盖章重试', 'dcc:controlled-file:stamp:retry', 3, 3, 6807, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'dcc:controlled-file:stamp:retry');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6814, 'DCC审批任务', 'dcc:controlled-file:query', 2, 9, 6800, 'controlled-file/approval-tasks', 'ep:checked', 'dcc/controlled-file/approval-tasks/index', 'DccControlledFileApprovalTasks', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `path` = 'controlled-file/approval-tasks');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6815, 'DCC电子签名管理', 'dcc:controlled-file:signature:manage', 2, 12, 6800, 'controlled-file/signatures', 'ep:management', 'dcc/controlled-file/signatures/index', 'DccControlledFileSignatures', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 6815 OR `path` = 'controlled-file/signatures');

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '我的培训', 'dcc:controlled-file:training:mine', 2, 13, 6800, 'controlled-file/training-mine', 'ep:reading', 'dcc/controlled-file/training/mine/index', 'DccControlledFileTrainingMine', 0, b'0', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `path` = 'controlled-file/training-mine');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6817, '模板配置', 'dcc:controlled-file:print-template:manage', 2, 14, 6800, 'controlled-file/print-template', 'ep:printer', 'dcc/controlled-file/print-template/index', 'DccApprovalPrintTemplate', 0, b'0', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu`
  WHERE `id` = 6817
     OR `permission` = 'dcc:controlled-file:print-template:manage'
     OR `path` = 'controlled-file/print-template'
);
CREATE TABLE IF NOT EXISTS `dcc_nas_acl_snapshot` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `transfer_task_id` bigint DEFAULT NULL,
  `snapshot_key` char(64) NOT NULL,
  `server` varchar(255) NOT NULL,
  `share` varchar(255) NOT NULL,
  `root_paths_json` longtext NOT NULL,
  `status` varchar(32) NOT NULL,
  `normalization_version` varchar(32) NOT NULL,
  `total_directory_count` bigint NOT NULL DEFAULT 0,
  `snapshotted_directory_count` bigint NOT NULL DEFAULT 0,
  `failed_directory_count` bigint NOT NULL DEFAULT 0,
  `started_at` datetime DEFAULT NULL,
  `completed_at` datetime DEFAULT NULL,
  `failure_code` varchar(64) DEFAULT NULL,
  `failure_message` varchar(1024) DEFAULT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_nas_acl_snapshot_key` (`tenant_id`, `snapshot_key`),
  KEY `idx_dcc_nas_acl_snapshot_task` (`tenant_id`, `transfer_task_id`, `id`),
  KEY `idx_dcc_nas_acl_snapshot_status` (`tenant_id`, `status`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='NAS ACL snapshot collection';

CREATE TABLE IF NOT EXISTS `dcc_nas_acl_directory_snapshot` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `snapshot_id` bigint NOT NULL,
  `transfer_task_id` bigint DEFAULT NULL,
  `transfer_task_item_id` bigint DEFAULT NULL,
  `dcc_directory_id` bigint DEFAULT NULL,
  `parent_snapshot_id` bigint DEFAULT NULL,
  `depth` int NOT NULL DEFAULT 0,
  `nas_path` varchar(1024) NOT NULL,
  `path_hash` char(64) NOT NULL,
  `item_name` varchar(255) NOT NULL,
  `descriptor_id` bigint DEFAULT NULL,
  `collect_status` varchar(32) NOT NULL,
  `failure_code` varchar(64) DEFAULT NULL,
  `failure_message` varchar(1024) DEFAULT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_nas_acl_dir_snapshot_path` (`snapshot_id`, `path_hash`),
  KEY `idx_dcc_nas_acl_dir_snapshot_task_item` (`tenant_id`, `transfer_task_item_id`),
  KEY `idx_dcc_nas_acl_dir_snapshot_dcc_dir` (`tenant_id`, `dcc_directory_id`),
  KEY `idx_dcc_nas_acl_dir_snapshot_descriptor` (`tenant_id`, `descriptor_id`),
  KEY `idx_dcc_nas_acl_dir_snapshot_status` (`tenant_id`, `snapshot_id`, `collect_status`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='NAS ACL directory node snapshot';

CREATE TABLE IF NOT EXISTS `dcc_nas_acl_descriptor` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `descriptor_hash` char(64) NOT NULL,
  `owner_sid` varchar(184) DEFAULT NULL,
  `group_sid` varchar(184) DEFAULT NULL,
  `control_flags` varchar(128) DEFAULT NULL,
  `dacl_present` bit(1) NOT NULL DEFAULT b'0',
  `dacl_protected` bit(1) NOT NULL DEFAULT b'0',
  `sacl_present` bit(1) NOT NULL DEFAULT b'0',
  `raw_descriptor_sha256` char(64) DEFAULT NULL,
  `raw_descriptor_blob` longblob DEFAULT NULL,
  `normalized_descriptor_json` longtext NOT NULL,
  `capture_capability` varchar(32) NOT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_nas_acl_descriptor_hash` (`tenant_id`, `descriptor_hash`),
  KEY `idx_dcc_nas_acl_descriptor_owner` (`tenant_id`, `owner_sid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Deduplicated NAS ACL descriptor';

CREATE TABLE IF NOT EXISTS `dcc_nas_acl_ace` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `descriptor_id` bigint NOT NULL,
  `ace_index` int NOT NULL,
  `ace_hash` char(64) NOT NULL,
  `ace_type` varchar(32) NOT NULL,
  `ace_flags` int DEFAULT NULL,
  `access_mask` bigint NOT NULL,
  `trustee_sid` varchar(184) NOT NULL,
  `trustee_sid_hash` char(64) NOT NULL,
  `inherited` bit(1) NOT NULL DEFAULT b'0',
  `inheritance_flags` varchar(128) DEFAULT NULL,
  `propagation_flags` varchar(128) DEFAULT NULL,
  `object_type_guid` varchar(64) DEFAULT NULL,
  `inherited_object_type_guid` varchar(64) DEFAULT NULL,
  `raw_ace_json` longtext NOT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_nas_acl_ace_order` (`descriptor_id`, `ace_index`),
  KEY `idx_dcc_nas_acl_ace_hash` (`tenant_id`, `ace_hash`),
  KEY `idx_dcc_nas_acl_ace_sid` (`tenant_id`, `trustee_sid_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='NAS ACL ACE detail';

CREATE TABLE IF NOT EXISTS `dcc_nas_acl_identity_mapping` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sid` varchar(184) NOT NULL,
  `sid_hash` char(64) NOT NULL,
  `domain_name` varchar(128) DEFAULT NULL,
  `account_name` varchar(128) DEFAULT NULL,
  `account_display_name` varchar(255) DEFAULT NULL,
  `account_type` varchar(32) NOT NULL,
  `mapping_status` varchar(32) NOT NULL,
  `dcc_subject_type` varchar(32) DEFAULT NULL,
  `dcc_subject_id` bigint DEFAULT NULL,
  `mapping_method` varchar(32) DEFAULT NULL,
  `verified_at` datetime DEFAULT NULL,
  `mapped_by_user_id` bigint DEFAULT NULL,
  `block_reason` varchar(512) DEFAULT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_nas_acl_identity_sid` (`tenant_id`, `sid_hash`),
  KEY `idx_dcc_nas_acl_identity_subject` (`tenant_id`, `dcc_subject_type`, `dcc_subject_id`),
  KEY `idx_dcc_nas_acl_identity_status` (`tenant_id`, `mapping_status`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='NAS SID to DCC subject mapping';

CREATE TABLE IF NOT EXISTS `dcc_nas_acl_restore_plan` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `snapshot_id` bigint NOT NULL,
  `transfer_task_id` bigint DEFAULT NULL,
  `plan_key` char(64) NOT NULL,
  `target_model` varchar(32) NOT NULL,
  `status` varchar(32) NOT NULL,
  `semantic_policy_version` varchar(64) DEFAULT NULL,
  `identity_mapping_version` varchar(64) DEFAULT NULL,
  `validation_summary_json` longtext DEFAULT NULL,
  `created_by_user_id` bigint NOT NULL,
  `started_at` datetime DEFAULT NULL,
  `completed_at` datetime DEFAULT NULL,
  `failure_code` varchar(64) DEFAULT NULL,
  `failure_message` varchar(1024) DEFAULT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_nas_acl_restore_plan_key` (`tenant_id`, `plan_key`),
  KEY `idx_dcc_nas_acl_restore_plan_snapshot` (`tenant_id`, `snapshot_id`, `id`),
  KEY `idx_dcc_nas_acl_restore_plan_task` (`tenant_id`, `transfer_task_id`, `id`),
  KEY `idx_dcc_nas_acl_restore_plan_status` (`tenant_id`, `status`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='NAS ACL restore plan';

CREATE TABLE IF NOT EXISTS `dcc_nas_acl_restore_plan_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `plan_id` bigint NOT NULL,
  `directory_snapshot_id` bigint NOT NULL,
  `transfer_task_item_id` bigint DEFAULT NULL,
  `dcc_directory_id` bigint DEFAULT NULL,
  `dcc_category_id` bigint DEFAULT NULL,
  `source_descriptor_id` bigint NOT NULL,
  `planned_operations_hash` char(64) DEFAULT NULL,
  `planned_operations_json` longtext DEFAULT NULL,
  `status` varchar(32) NOT NULL,
  `block_reason` varchar(1024) DEFAULT NULL,
  `expected_after_hash` varchar(128) DEFAULT NULL,
  `actual_after_hash` varchar(128) DEFAULT NULL,
  `verified_at` datetime DEFAULT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_nas_acl_restore_item_dir` (`plan_id`, `directory_snapshot_id`),
  KEY `idx_dcc_nas_acl_restore_item_status` (`tenant_id`, `plan_id`, `status`, `id`),
  KEY `idx_dcc_nas_acl_restore_item_dcc_dir` (`tenant_id`, `dcc_directory_id`),
  KEY `idx_dcc_nas_acl_restore_item_descriptor` (`tenant_id`, `source_descriptor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='NAS ACL restore plan item';

CREATE TABLE IF NOT EXISTS `dcc_nas_acl_restore_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `plan_id` bigint NOT NULL,
  `plan_item_id` bigint NOT NULL,
  `attempt_no` int NOT NULL,
  `action_type` varchar(32) NOT NULL,
  `status` varchar(32) NOT NULL,
  `before_hash` varchar(128) DEFAULT NULL,
  `expected_after_hash` varchar(128) DEFAULT NULL,
  `actual_after_hash` varchar(128) DEFAULT NULL,
  `request_payload_hash` char(64) DEFAULT NULL,
  `error_code` varchar(64) DEFAULT NULL,
  `error_message` varchar(1024) DEFAULT NULL,
  `started_at` datetime DEFAULT NULL,
  `completed_at` datetime DEFAULT NULL,
  `operator_user_id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_nas_acl_restore_log_attempt` (`plan_item_id`, `attempt_no`, `action_type`),
  KEY `idx_dcc_nas_acl_restore_log_plan` (`tenant_id`, `plan_id`, `id`),
  KEY `idx_dcc_nas_acl_restore_log_status` (`tenant_id`, `status`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='NAS ACL restore execution log';
