-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260903_dcc_controlled_file_related_file,20260906_dcc_new_file_lifecycle_p4; type=schema; riskLevel=medium
-- Immutable publication follow-up ledger for publications created after this migration.

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `dcc_publication_followup_batch` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `published_controlled_file_id` BIGINT NOT NULL COMMENT '已发布受控文件版本 ID',
  `published_master_id` BIGINT NOT NULL COMMENT '已发布文件 Master ID',
  `previous_active_controlled_file_id` BIGINT NULL COMMENT '被替代的正式版本 ID',
  `dcc_project_code_id` BIGINT NULL COMMENT '发布时 DCC 项目代码 ID 上下文',
  `category_id` BIGINT NOT NULL COMMENT '发布时文件类别 ID 上下文',
  `directory_id` BIGINT NULL COMMENT '发布时目录 ID 上下文',
  `file_type_taxonomy_leaf_id` BIGINT NULL COMMENT '发布时分类叶子 ID 上下文',
  `file_number_snapshot` VARCHAR(128) NOT NULL COMMENT '发布时文件编号',
  `file_name_snapshot` VARCHAR(512) NOT NULL COMMENT '发布时文件名称',
  `version_no_snapshot` VARCHAR(64) NOT NULL COMMENT '发布版本号',
  `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '后续批次状态',
  `published_at` DATETIME NOT NULL COMMENT '发布时间',
  `creation_token` VARCHAR(36) NOT NULL COMMENT '并发幂等创建令牌',
  `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) NULL COMMENT '创建者',
  `updater` VARCHAR(64) NULL COMMENT '更新者',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_pub_followup_file` (`tenant_id`, `published_controlled_file_id`, `deleted`),
  KEY `idx_dcc_pub_followup_master` (`tenant_id`, `published_master_id`, `published_at`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC 发布后续批次';

CREATE TABLE IF NOT EXISTS `dcc_publication_visibility_rule_snapshot` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `batch_id` BIGINT NOT NULL COMMENT '发布后续批次 ID',
  `source_type` VARCHAR(48) NOT NULL COMMENT 'FILE_REQUESTER/CURRENT_VIEW_MATRIX/PUBLIC_FOLDER_DISTRIBUTION',
  `source_rule_id` BIGINT NOT NULL COMMENT '正式来源记录 ID',
  `source_scope` VARCHAR(32) NULL COMMENT '正式来源范围',
  `subject_type` VARCHAR(32) NOT NULL COMMENT 'USER/DEPT/ROLE/POST/DCC_POSITION',
  `subject_id` BIGINT NULL COMMENT '正式主体 ID',
  `dcc_project_code_id` BIGINT NULL COMMENT '项目代码上下文，不是 VIEW 授权来源',
  `category_id` BIGINT NOT NULL COMMENT '类别上下文，不是独立 VIEW 授权来源',
  `directory_id` BIGINT NULL COMMENT '目录上下文，不是独立 VIEW 授权来源',
  `source_summary` VARCHAR(512) NOT NULL COMMENT '来源摘要',
  `resolution_status` VARCHAR(32) NOT NULL COMMENT 'RESOLVED/EMPTY/UNRESOLVED/FILTERED_BY_ASSIGNMENT',
  `resolution_message` VARCHAR(1000) NULL COMMENT '解析或硬范围过滤说明',
  `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) NULL COMMENT '创建者',
  `updater` VARCHAR(64) NULL COMMENT '更新者',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_pub_visibility_rule` (`tenant_id`, `batch_id`, `source_type`, `source_rule_id`, `deleted`),
  KEY `idx_dcc_pub_visibility_rule_batch` (`tenant_id`, `batch_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC 发布业务可见规则快照';

CREATE TABLE IF NOT EXISTS `dcc_publication_visibility_user_snapshot` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `batch_id` BIGINT NOT NULL COMMENT '发布后续批次 ID',
  `rule_snapshot_id` BIGINT NOT NULL COMMENT '可见规则快照 ID',
  `user_id` BIGINT NOT NULL COMMENT '发布时业务可见用户 ID',
  `user_name_snapshot` VARCHAR(128) NULL COMMENT '发布时用户名称',
  `dept_id_snapshot` BIGINT NULL COMMENT '发布时部门 ID',
  `dept_name_snapshot` VARCHAR(128) NULL COMMENT '发布时部门名称',
  `user_status_snapshot` INT NULL COMMENT '发布时用户状态',
  `resolution_reason` VARCHAR(512) NULL COMMENT '规则解析原因',
  `assignment_scope_result` VARCHAR(32) NOT NULL COMMENT 'ALLOWED，已应用项目分配硬范围',
  `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) NULL COMMENT '创建者',
  `updater` VARCHAR(64) NULL COMMENT '更新者',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_pub_visibility_user` (`tenant_id`, `batch_id`, `rule_snapshot_id`, `user_id`, `deleted`),
  KEY `idx_dcc_pub_visibility_user_page` (`tenant_id`, `batch_id`, `user_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC 发布业务可见用户快照';

CREATE TABLE IF NOT EXISTS `dcc_publication_notification_candidate` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `batch_id` BIGINT NOT NULL COMMENT '发布后续批次 ID',
  `user_id` BIGINT NOT NULL COMMENT '通知候选用户 ID',
  `user_name_snapshot` VARCHAR(128) NULL COMMENT '发布时用户名称',
  `dept_id_snapshot` BIGINT NULL COMMENT '发布时部门 ID',
  `dept_name_snapshot` VARCHAR(128) NULL COMMENT '发布时部门名称',
  `user_status_snapshot` INT NULL COMMENT '发布时用户状态',
  `resolution_status` VARCHAR(32) NOT NULL COMMENT 'ACTIVE/INACTIVE/MISSING',
  `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) NULL COMMENT '创建者',
  `updater` VARCHAR(64) NULL COMMENT '更新者',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_pub_candidate_user` (`tenant_id`, `batch_id`, `user_id`, `deleted`),
  KEY `idx_dcc_pub_candidate_batch` (`tenant_id`, `batch_id`, `resolution_status`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC 发布通知候选';

CREATE TABLE IF NOT EXISTS `dcc_publication_notification_candidate_reason` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `batch_id` BIGINT NOT NULL COMMENT '发布后续批次 ID',
  `candidate_id` BIGINT NOT NULL COMMENT '通知候选 ID',
  `reason_type` VARCHAR(48) NOT NULL COMMENT 'FILE_OWNER/FORMAL_DISTRIBUTION/RELATED_FILE_OWNER',
  `source_id` BIGINT NOT NULL COMMENT '来源文件、分发收件人或相关 Master ID',
  `related_master_id` BIGINT NULL COMMENT '相关文件 Master ID',
  `reason_summary` VARCHAR(512) NOT NULL COMMENT '候选原因摘要',
  `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) NULL COMMENT '创建者',
  `updater` VARCHAR(64) NULL COMMENT '更新者',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_pub_candidate_reason` (`tenant_id`, `candidate_id`, `reason_type`, `source_id`, `deleted`),
  KEY `idx_dcc_pub_candidate_reason_batch` (`tenant_id`, `batch_id`, `candidate_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC 发布通知候选原因';

CREATE TABLE IF NOT EXISTS `dcc_publication_relation_snapshot` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `batch_id` BIGINT NOT NULL COMMENT '发布后续批次 ID',
  `related_master_id` BIGINT NOT NULL COMMENT '相关文件 Master ID',
  `related_active_controlled_file_id` BIGINT NULL COMMENT '发布时相关正式版本 ID',
  `related_file_number_snapshot` VARCHAR(128) NULL COMMENT '相关文件编号快照',
  `related_file_name_snapshot` VARCHAR(512) NULL COMMENT '相关文件名称快照',
  `related_version_no_snapshot` VARCHAR(64) NULL COMMENT '相关正式版本号快照',
  `responsible_user_id_snapshot` BIGINT NULL COMMENT '相关正式版本责任人 ID',
  `responsible_user_name_snapshot` VARCHAR(128) NULL COMMENT '相关责任人名称',
  `responsible_user_status_snapshot` INT NULL COMMENT '相关责任人状态',
  `resolution_status` VARCHAR(32) NOT NULL COMMENT 'RESOLVED/NO_ACTIVE_VERSION',
  `frozen_at` DATETIME NOT NULL COMMENT '关系冻结时间',
  `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) NULL COMMENT '创建者',
  `updater` VARCHAR(64) NULL COMMENT '更新者',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_pub_relation_master` (`tenant_id`, `batch_id`, `related_master_id`, `deleted`),
  KEY `idx_dcc_pub_relation_file` (`tenant_id`, `related_active_controlled_file_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC 发布相关 Master 快照';

CREATE TABLE IF NOT EXISTS `dcc_publication_relation_direction_snapshot` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `batch_id` BIGINT NOT NULL COMMENT '发布后续批次 ID',
  `relation_snapshot_id` BIGINT NOT NULL COMMENT '相关 Master 快照 ID',
  `direction` VARCHAR(16) NOT NULL COMMENT 'FORWARD/REVERSE',
  `source_relation_id` BIGINT NOT NULL COMMENT '原始关联记录 ID',
  `source_controlled_file_id` BIGINT NOT NULL COMMENT '原始关系来源版本 ID',
  `target_controlled_file_id` BIGINT NOT NULL COMMENT '原始关系目标版本 ID',
  `relation_source_snapshot` VARCHAR(32) NOT NULL COMMENT '原始关系来源类型',
  `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) NULL COMMENT '创建者',
  `updater` VARCHAR(64) NULL COMMENT '更新者',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_pub_relation_direction` (`tenant_id`, `batch_id`, `relation_snapshot_id`, `direction`, `source_relation_id`, `deleted`),
  KEY `idx_dcc_pub_relation_direction_batch` (`tenant_id`, `batch_id`, `direction`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC 发布关系方向快照';
