-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- Repair legacy DCC runtime schemas that were created before the current v1 table contract landed.
-- Safe to run repeatedly: create missing tables, add missing columns, and backfill legacy rows in place.

DROP PROCEDURE IF EXISTS ensure_dcc_column;
DELIMITER $$
CREATE PROCEDURE ensure_dcc_column(IN target_table VARCHAR(64), IN target_column VARCHAR(64), IN ddl_statement TEXT)
BEGIN
  IF NOT EXISTS (
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = target_table
        AND COLUMN_NAME = target_column
  ) THEN
    SET @ddl_statement = ddl_statement;
    PREPARE stmt FROM @ddl_statement;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;

CALL ensure_dcc_column(
  'dcc_file_directory',
  'access_rule_manually_bound',
  'ALTER TABLE `dcc_file_directory` ADD COLUMN `access_rule_manually_bound` tinyint NOT NULL DEFAULT 0 AFTER `remark`'
);
CALL ensure_dcc_column(
  'dcc_file_category',
  'description',
  'ALTER TABLE `dcc_file_category` ADD COLUMN `description` varchar(255) DEFAULT NULL AFTER `remark`'
);
CALL ensure_dcc_column(
  'dcc_file_category',
  'lifecycle_stage',
  'ALTER TABLE `dcc_file_category` ADD COLUMN `lifecycle_stage` varchar(32) DEFAULT NULL COMMENT ''Lifecycle stage: PLAN/INPUT/OUTPUT/VERIFICATION/VALIDATION/TRANSFER'' AFTER `description`'
);
CALL ensure_dcc_column(
  'dcc_file_category',
  'distribution_required',
  'ALTER TABLE `dcc_file_category` ADD COLUMN `distribution_required` tinyint NOT NULL DEFAULT 0 AFTER `lifecycle_stage`'
);
CALL ensure_dcc_column(
  'dcc_file_category',
  'training_required',
  'ALTER TABLE `dcc_file_category` ADD COLUMN `training_required` tinyint NOT NULL DEFAULT 0 AFTER `distribution_required`'
);
CALL ensure_dcc_column(
  'dcc_registration_certificate_version',
  'remark',
  'ALTER TABLE `dcc_registration_certificate_version` ADD COLUMN `remark` varchar(1024) DEFAULT NULL COMMENT ''Draft remark'' AFTER `base_snapshot_id`'
);

UPDATE `dcc_file_category`
SET `lifecycle_stage` = CASE
  WHEN UPPER(`code`) IN ('DCC_FVM_DHF_004', 'DCC_FVM_DHF_005', 'DCC_FVM_DHF_010') THEN 'PLAN'
  WHEN UPPER(`code`) IN ('DCC_FVM_DHF_001', 'DCC_FVM_DHF_002', 'DCC_FVM_DHF_003',
                         'DCC_FVM_DHF_006', 'DCC_FVM_DHF_007', 'DCC_FVM_DHF_008',
                         'DCC_FVM_DHF_009') THEN 'INPUT'
  WHEN UPPER(`code`) LIKE 'DCC_FVM_DMR\_%' OR UPPER(`code`) = 'DCC_FVM_DHF_011' THEN 'OUTPUT'
  WHEN UPPER(`code`) IN ('DCC_FVM_DHF_012', 'DCC_FVM_DHF_013', 'DCC_FVM_DHF_014',
                         'DCC_FVM_DHF_015', 'DCC_FVM_DHF_016') THEN 'VERIFICATION'
  WHEN UPPER(`code`) IN ('DCC_FVM_DHF_017', 'DCC_FVM_DHF_018', 'DCC_FVM_DHF_020',
                         'DCC_FVM_DHF_021', 'DCC_FVM_DHF_022', 'DCC_FVM_DHF_023',
                         'DCC_FVM_DHF_024', 'DCC_FVM_DHF_025', 'DCC_FVM_DHF_026',
                         'DCC_FVM_DHF_027', 'DCC_FVM_DHF_028', 'DCC_FVM_DHF_029') THEN 'VALIDATION'
  WHEN UPPER(`code`) = 'DCC_FVM_DHF_019'
    OR UPPER(`code`) IN ('DCC_FVM_DHF_030', 'DCC_FVM_DHF_031', 'DCC_FVM_DHF_032',
                         'DCC_FVM_DHF_033', 'DCC_FVM_DHF_034', 'DCC_FVM_DHF_035')
    OR UPPER(`code`) LIKE 'DCC_OTHER_TEMPLATE\_%' THEN 'TRANSFER'
  WHEN UPPER(`code`) IN ('QMSFC-0028', 'QMSFC-0029', 'QMSFC-0034') THEN 'PLAN'
  WHEN UPPER(`code`) IN ('INTAUTH-28', 'INTAUTH-29', 'INTAUTH-34') THEN 'PLAN'
  WHEN UPPER(`code`) IN ('QMSFC-0001', 'QMSFC-0002', 'QMSFC-0003', 'QMSFC-0004',
                         'QMSFC-0005', 'QMSFC-0006', 'QMSFC-0007', 'QMSFC-0008',
                         'QMSFC-0009', 'QMSFC-0010', 'QMSFC-0011', 'QMSFC-0012',
                         'QMSFC-0013', 'QMSFC-0014', 'QMSFC-0015', 'QMSFC-0016',
                         'QMSFC-0017', 'QMSFC-0018', 'QMSFC-0019', 'QMSFC-0020',
                         'QMSFC-0021', 'QMSFC-0022', 'QMSFC-0023', 'QMSFC-0024',
                         'QMSFC-0025', 'QMSFC-0026', 'QMSFC-0027', 'QMSFC-0030',
                         'QMSFC-0031', 'QMSFC-0032', 'QMSFC-0033') THEN 'INPUT'
  WHEN UPPER(`code`) IN ('INTAUTH-1', 'INTAUTH-2', 'INTAUTH-3', 'INTAUTH-4',
                         'INTAUTH-5', 'INTAUTH-6', 'INTAUTH-7', 'INTAUTH-8',
                         'INTAUTH-9', 'INTAUTH-10', 'INTAUTH-11', 'INTAUTH-12',
                         'INTAUTH-13', 'INTAUTH-14', 'INTAUTH-15', 'INTAUTH-16',
                         'INTAUTH-17', 'INTAUTH-18', 'INTAUTH-19', 'INTAUTH-20',
                         'INTAUTH-21', 'INTAUTH-22', 'INTAUTH-23', 'INTAUTH-24',
                         'INTAUTH-25', 'INTAUTH-26', 'INTAUTH-27', 'INTAUTH-30',
                         'INTAUTH-31', 'INTAUTH-32', 'INTAUTH-33') THEN 'INPUT'
  WHEN UPPER(`code`) IN ('QMSFC-0035', 'QMSFC-0036', 'QMSFC-0037', 'QMSFC-0038') THEN 'VERIFICATION'
  WHEN UPPER(`code`) IN ('INTAUTH-35', 'INTAUTH-36', 'INTAUTH-37', 'INTAUTH-38') THEN 'VERIFICATION'
  WHEN UPPER(`code`) IN ('QMSFC-0040', 'QMSFC-0041', 'QMSFC-0042', 'QMSFC-0043',
                         'QMSFC-0044', 'QMSFC-0045', 'QMSFC-0046') THEN 'VALIDATION'
  WHEN UPPER(`code`) IN ('INTAUTH-40', 'INTAUTH-41', 'INTAUTH-42', 'INTAUTH-43',
                         'INTAUTH-44', 'INTAUTH-45', 'INTAUTH-46') THEN 'VALIDATION'
  WHEN UPPER(`code`) IN ('QMSFC-0039', 'QMSFC-0047', 'QMSFC-0048')
    OR UPPER(`code`) IN ('INTAUTH-39', 'INTAUTH-47', 'INTAUTH-48')
    OR UPPER(`code`) LIKE 'NASCAT-%'
    OR UPPER(`code`) LIKE 'CODEX_DCC_LOCAL\_%'
    OR UPPER(`code`) LIKE 'CODEX_E2E\_%' THEN 'TRANSFER'
  ELSE `lifecycle_stage`
END
WHERE `deleted` = 0
  AND (`lifecycle_stage` IS NULL OR `lifecycle_stage` = '');

DROP PROCEDURE IF EXISTS ensure_dcc_category_lifecycle_stage_backfilled;
DELIMITER $$
CREATE PROCEDURE ensure_dcc_category_lifecycle_stage_backfilled()
BEGIN
  IF EXISTS (
      SELECT 1
      FROM `dcc_file_category`
      WHERE `deleted` = 0
        AND (`lifecycle_stage` IS NULL OR `lifecycle_stage` = '')
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC category lifecycle_stage backfill incomplete';
  END IF;
END$$
DELIMITER ;
CALL ensure_dcc_category_lifecycle_stage_backfilled();
DROP PROCEDURE IF EXISTS ensure_dcc_category_lifecycle_stage_backfilled;

-- Deleted legacy categories are archived rows; normalize their blank stage before applying NOT NULL.
UPDATE `dcc_file_category`
SET `lifecycle_stage` = 'TRANSFER'
WHERE `deleted` <> 0
  AND (`lifecycle_stage` IS NULL OR `lifecycle_stage` = '');

DROP PROCEDURE IF EXISTS ensure_dcc_category_lifecycle_stage_not_null_ready;
DELIMITER $$
CREATE PROCEDURE ensure_dcc_category_lifecycle_stage_not_null_ready()
BEGIN
  IF EXISTS (
      SELECT 1
      FROM `dcc_file_category`
      WHERE `lifecycle_stage` IS NULL OR `lifecycle_stage` = ''
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC category lifecycle_stage not-null normalization incomplete';
  END IF;
END$$
DELIMITER ;
CALL ensure_dcc_category_lifecycle_stage_not_null_ready();
DROP PROCEDURE IF EXISTS ensure_dcc_category_lifecycle_stage_not_null_ready;

-- Lifecycle backfill contract includes DCC_FVM_DMR_%, DCC_OTHER_TEMPLATE_%, INTAUTH, NASCAT legacy patterns, and deleted legacy normalization; unmatched active categories fail fast above.
ALTER TABLE `dcc_file_category` MODIFY COLUMN `lifecycle_stage` varchar(32) NOT NULL COMMENT 'Lifecycle stage: PLAN/INPUT/OUTPUT/VERIFICATION/VALIDATION/TRANSFER';

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

CREATE TABLE IF NOT EXISTS `dcc_category_view_matrix_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `category_id` bigint NOT NULL,
  `excel_file_name` varchar(255) DEFAULT NULL,
  `excel_row_no` int DEFAULT NULL,
  `excel_column_letter` varchar(16) DEFAULT NULL,
  `subject_label` varchar(255) DEFAULT NULL,
  `subject_top_header` varchar(128) DEFAULT NULL,
  `subject_sub_header` varchar(128) DEFAULT NULL,
  `marker` varchar(8) DEFAULT NULL,
  `scope_type` varchar(32) NOT NULL,
  `subject_type` varchar(32) NOT NULL,
  `subject_id` bigint DEFAULT NULL,
  `active` tinyint NOT NULL DEFAULT 1,
  `remark` varchar(255) DEFAULT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC category view matrix rule';

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

CALL ensure_dcc_column(
  'dcc_file_category_distribution_rule',
  'distribution_medium',
  'ALTER TABLE `dcc_file_category_distribution_rule` ADD COLUMN `distribution_medium` varchar(32) NOT NULL DEFAULT ''PUBLIC_FOLDER'' AFTER `department_id`'
);

UPDATE `dcc_file_category_distribution_rule`
SET `distribution_medium` = 'PUBLIC_FOLDER'
WHERE COALESCE(`distribution_medium`, '') = '';

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

CALL ensure_dcc_column(
  'dcc_category_approval_route_node',
  'stage_code',
  'ALTER TABLE `dcc_category_approval_route_node` ADD COLUMN `stage_code` varchar(64) DEFAULT NULL AFTER `stage_no`'
);
CALL ensure_dcc_column(
  'dcc_category_approval_route_node',
  'stage_order',
  'ALTER TABLE `dcc_category_approval_route_node` ADD COLUMN `stage_order` int DEFAULT NULL AFTER `stage_name`'
);
CALL ensure_dcc_column(
  'dcc_category_approval_route_node',
  'candidate_source_ids',
  'ALTER TABLE `dcc_category_approval_route_node` ADD COLUMN `candidate_source_ids` varchar(1000) DEFAULT NULL AFTER `candidate_source_id`'
);
CALL ensure_dcc_column(
  'dcc_category_approval_route_node',
  'require_all_approvals',
  'ALTER TABLE `dcc_category_approval_route_node` ADD COLUMN `require_all_approvals` tinyint NOT NULL DEFAULT 0 AFTER `approve_ratio`'
);
CALL ensure_dcc_column(
  'dcc_category_approval_route_node',
  'stage_type',
  'ALTER TABLE `dcc_category_approval_route_node` ADD COLUMN `stage_type` varchar(32) DEFAULT NULL COMMENT ''规则阶段类型：DOC_CONTROL/SIGNOFF/APPROVAL'' AFTER `sort`'
);
CALL ensure_dcc_column(
  'dcc_category_approval_route_node',
  'subject_label',
  'ALTER TABLE `dcc_category_approval_route_node` ADD COLUMN `subject_label` varchar(255) DEFAULT NULL COMMENT ''主体标签'' AFTER `stage_type`'
);
CALL ensure_dcc_column(
  'dcc_category_approval_route_node',
  'marker',
  'ALTER TABLE `dcc_category_approval_route_node` ADD COLUMN `marker` varchar(32) DEFAULT NULL COMMENT ''标记'' AFTER `subject_label`'
);
CALL ensure_dcc_column(
  'dcc_category_approval_route_node',
  'subject_type',
  'ALTER TABLE `dcc_category_approval_route_node` ADD COLUMN `subject_type` varchar(32) DEFAULT NULL COMMENT ''主体类型'' AFTER `marker`'
);
CALL ensure_dcc_column(
  'dcc_category_approval_route_node',
  'subject_id',
  'ALTER TABLE `dcc_category_approval_route_node` ADD COLUMN `subject_id` bigint DEFAULT NULL COMMENT ''主体ID'' AFTER `subject_type`'
);
CALL ensure_dcc_column(
  'dcc_category_approval_route_node',
  'subject_name',
  'ALTER TABLE `dcc_category_approval_route_node` ADD COLUMN `subject_name` varchar(255) DEFAULT NULL COMMENT ''主体名称'' AFTER `subject_id`'
);
CALL ensure_dcc_column(
  'dcc_category_approval_route_node',
  'subject_department_path',
  'ALTER TABLE `dcc_category_approval_route_node` ADD COLUMN `subject_department_path` varchar(500) DEFAULT NULL COMMENT ''对应部门路径'' AFTER `subject_name`'
);
CALL ensure_dcc_column(
  'dcc_category_approval_route_node',
  'rule_remark',
  'ALTER TABLE `dcc_category_approval_route_node` ADD COLUMN `rule_remark` varchar(255) DEFAULT NULL COMMENT ''规则备注'' AFTER `subject_department_path`'
);

UPDATE `dcc_category_approval_route_node`
SET `stage_code` = CASE `stage_no`
                       WHEN 1 THEN 'DOC_CONTROL_REVIEW'
                       WHEN 2 THEN 'MATRIX_REVIEW'
                       WHEN 3 THEN 'MATRIX_APPROVAL'
                       WHEN 4 THEN 'DOC_CONTROL_APPROVAL'
                       ELSE `stage_code`
                   END
WHERE (`stage_code` IS NULL OR `stage_code` = '');

UPDATE `dcc_category_approval_route_node`
SET `stage_name` = CASE `stage_no`
                       WHEN 1 THEN 'Document Control Review'
                       WHEN 2 THEN 'Matrix Review'
                       WHEN 3 THEN 'Matrix Approval'
                       WHEN 4 THEN 'Document Control Approval'
                       ELSE `stage_name`
                   END
WHERE (`stage_name` IS NULL OR `stage_name` = '');

UPDATE `dcc_category_approval_route_node`
SET `stage_order` = COALESCE(`stage_order`, `stage_no`)
WHERE `stage_order` IS NULL;

UPDATE `dcc_category_approval_route_node`
SET `require_all_approvals` = CASE
                                  WHEN UPPER(COALESCE(`approve_method`, '')) = 'ALL' THEN 1
                                  WHEN COALESCE(`approve_ratio`, 0) >= 100 THEN 1
                                  ELSE 0
                              END;

UPDATE `dcc_category_approval_route_node`
SET `candidate_source_ids` = CAST(`candidate_source_id` AS CHAR)
WHERE (`candidate_source_ids` IS NULL OR `candidate_source_ids` = '')
  AND `candidate_source_id` IS NOT NULL;

UPDATE `dcc_category_approval_route_node`
SET `approve_method` = 'ANY',
    `approve_ratio` = NULL,
    `require_all_approvals` = 0
WHERE `stage_code` = 'MATRIX_APPROVAL';

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

CALL ensure_dcc_column(
  'dcc_controlled_file_master',
  'directory_id',
  'ALTER TABLE `dcc_controlled_file_master` ADD COLUMN `directory_id` bigint DEFAULT NULL COMMENT ''DCC directory for this logical document chain'' AFTER `category_id`'
);

CALL ensure_dcc_column(
  'dcc_controlled_file',
  'master_id',
  'ALTER TABLE `dcc_controlled_file` ADD COLUMN `master_id` bigint DEFAULT NULL AFTER `id`'
);
CALL ensure_dcc_column(
  'dcc_controlled_file',
  'source_file_id',
  'ALTER TABLE `dcc_controlled_file` ADD COLUMN `source_file_id` bigint DEFAULT NULL AFTER `directory_id`'
);
CALL ensure_dcc_column(
  'dcc_controlled_file',
  'published_file_id',
  'ALTER TABLE `dcc_controlled_file` ADD COLUMN `published_file_id` bigint DEFAULT NULL AFTER `original_file_id`'
);
CALL ensure_dcc_column(
  'dcc_controlled_file',
  'file_name',
  'ALTER TABLE `dcc_controlled_file` ADD COLUMN `file_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL AFTER `stamped_file_id`'
);
CALL ensure_dcc_column(
  'dcc_controlled_file',
  'file_number',
  'ALTER TABLE `dcc_controlled_file` ADD COLUMN `file_number` varchar(64) DEFAULT NULL AFTER `title`'
);
CALL ensure_dcc_column(
  'dcc_controlled_file',
  'product_name',
  'ALTER TABLE `dcc_controlled_file` ADD COLUMN `product_name` varchar(255) DEFAULT NULL AFTER `product_code`'
);
CALL ensure_dcc_column(
  'dcc_controlled_file',
  'submitter_id',
  'ALTER TABLE `dcc_controlled_file` ADD COLUMN `submitter_id` bigint DEFAULT NULL AFTER `status`'
);
CALL ensure_dcc_column(
  'dcc_controlled_file',
  'published_time',
  'ALTER TABLE `dcc_controlled_file` ADD COLUMN `published_time` datetime DEFAULT NULL AFTER `approved_time`'
);
CALL ensure_dcc_column(
  'dcc_controlled_file',
  'obsoleted_by',
  'ALTER TABLE `dcc_controlled_file` ADD COLUMN `obsoleted_by` bigint DEFAULT NULL AFTER `stamped_time`'
);
CALL ensure_dcc_column(
  'dcc_controlled_file',
  'obsoleted_time',
  'ALTER TABLE `dcc_controlled_file` ADD COLUMN `obsoleted_time` datetime DEFAULT NULL AFTER `obsoleted_by`'
);
CALL ensure_dcc_column(
  'dcc_controlled_file',
  'obsolete_reason',
  'ALTER TABLE `dcc_controlled_file` ADD COLUMN `obsolete_reason` varchar(255) DEFAULT NULL AFTER `obsoleted_time`'
);
CALL ensure_dcc_column(
  'dcc_controlled_file',
  'superseded_by_file_id',
  'ALTER TABLE `dcc_controlled_file` ADD COLUMN `superseded_by_file_id` bigint DEFAULT NULL AFTER `obsolete_reason`'
);
CALL ensure_dcc_column(
  'dcc_controlled_file',
  'finalization_error',
  'ALTER TABLE `dcc_controlled_file` ADD COLUMN `finalization_error` varchar(500) DEFAULT NULL AFTER `reject_reason`'
);
CALL ensure_dcc_column(
  'dcc_controlled_file',
  'file_type_level1',
  'ALTER TABLE `dcc_controlled_file` ADD COLUMN `file_type_level1` varchar(64) DEFAULT NULL COMMENT ''File type level 1: QMS or technical document'' AFTER `project_code_recognized_time`'
);
CALL ensure_dcc_column(
  'dcc_controlled_file',
  'file_type_level2',
  'ALTER TABLE `dcc_controlled_file` ADD COLUMN `file_type_level2` varchar(128) DEFAULT NULL COMMENT ''File type level 2: DCC category for technical documents'' AFTER `file_type_level1`'
);
CALL ensure_dcc_column(
  'dcc_controlled_file',
  'file_type_level3',
  'ALTER TABLE `dcc_controlled_file` ADD COLUMN `file_type_level3` varchar(128) DEFAULT NULL COMMENT ''Reserved file type level 3'' AFTER `file_type_level2`'
);
CALL ensure_dcc_column(
  'dcc_controlled_file',
  'file_type_level4',
  'ALTER TABLE `dcc_controlled_file` ADD COLUMN `file_type_level4` varchar(128) DEFAULT NULL COMMENT ''Reserved file type level 4'' AFTER `file_type_level3`'
);
CALL ensure_dcc_column(
  'dcc_controlled_file',
  'file_type_level5',
  'ALTER TABLE `dcc_controlled_file` ADD COLUMN `file_type_level5` varchar(128) DEFAULT NULL COMMENT ''Reserved file type level 5'' AFTER `file_type_level4`'
);

UPDATE `dcc_controlled_file`
SET `master_id` = COALESCE(NULLIF(`master_id`, 0), `id`),
    `source_file_id` = COALESCE(`source_file_id`, `original_file_id`),
    `published_file_id` = COALESCE(`published_file_id`, NULLIF(`stamped_file_id`, 0)),
    `file_name` = COALESCE(NULLIF(`file_name`, ''), NULLIF(`title`, ''), CONCAT('DCC-FILE-', `id`)),
    `file_number` = COALESCE(NULLIF(`file_number`, ''), NULLIF(`title`, ''), CONCAT('DCC-FILE-', `id`)),
    `submitter_id` = COALESCE(`submitter_id`, `requester_id`),
    `published_time` = COALESCE(`published_time`, `stamped_time`, `approved_time`)
WHERE `deleted` = 0;

INSERT IGNORE INTO `dcc_controlled_file_master`
(`id`, `category_id`, `directory_id`, `file_name`, `file_number`, `current_active_controlled_file_id`, `status`,
 `tenant_id`, `create_time`, `update_time`, `creator`, `updater`, `deleted`)
SELECT `master_id`,
       `category_id`,
       `directory_id`,
       `file_name`,
       `file_number`,
       CASE
           WHEN `status` IN ('ACTIVE', 'APPROVED', 'STAMPED', 'SUPERSEDED') THEN `id`
           ELSE NULL
       END,
       CASE
           WHEN `status` = 'OBSOLETE' THEN 'OBSOLETE_CHAIN'
           ELSE 'ACTIVE_CHAIN'
       END,
       `tenant_id`,
       `create_time`,
       `update_time`,
       `creator`,
       `updater`,
       `deleted`
FROM `dcc_controlled_file`
WHERE `master_id` IS NOT NULL;

UPDATE `dcc_controlled_file_master` master_record
LEFT JOIN `dcc_controlled_file` current_file
  ON current_file.`id` = master_record.`current_active_controlled_file_id`
LEFT JOIN (
  SELECT `master_id`,
         MAX(CASE WHEN `deleted` = 0 THEN `directory_id` ELSE NULL END) AS `active_directory_id`,
         MAX(`directory_id`) AS `any_directory_id`
  FROM `dcc_controlled_file`
  WHERE `master_id` IS NOT NULL
  GROUP BY `master_id`
) controlled_file
  ON controlled_file.`master_id` = master_record.`id`
SET master_record.`directory_id` = COALESCE(
  current_file.`directory_id`,
  controlled_file.`active_directory_id`,
  controlled_file.`any_directory_id`
)
WHERE master_record.`directory_id` IS NULL
  AND COALESCE(current_file.`directory_id`, controlled_file.`active_directory_id`,
               controlled_file.`any_directory_id`) IS NOT NULL;

CALL ensure_dcc_column(
  'dcc_controlled_file_route_snapshot',
  'stage_code',
  'ALTER TABLE `dcc_controlled_file_route_snapshot` ADD COLUMN `stage_code` varchar(64) DEFAULT NULL AFTER `stage_no`'
);
CALL ensure_dcc_column(
  'dcc_controlled_file_route_snapshot',
  'stage_name',
  'ALTER TABLE `dcc_controlled_file_route_snapshot` ADD COLUMN `stage_name` varchar(128) DEFAULT NULL AFTER `stage_code`'
);
CALL ensure_dcc_column(
  'dcc_controlled_file_route_snapshot',
  'stage_order',
  'ALTER TABLE `dcc_controlled_file_route_snapshot` ADD COLUMN `stage_order` int DEFAULT NULL AFTER `stage_name`'
);
CALL ensure_dcc_column(
  'dcc_controlled_file_route_snapshot',
  'candidate_source_ids',
  'ALTER TABLE `dcc_controlled_file_route_snapshot` ADD COLUMN `candidate_source_ids` varchar(1000) DEFAULT NULL AFTER `candidate_source_id`'
);
CALL ensure_dcc_column(
  'dcc_controlled_file_route_snapshot',
  'require_all_approvals',
  'ALTER TABLE `dcc_controlled_file_route_snapshot` ADD COLUMN `require_all_approvals` tinyint NOT NULL DEFAULT 0 AFTER `approve_ratio`'
);

UPDATE `dcc_controlled_file_route_snapshot`
SET `stage_code` = CASE `stage_no`
                       WHEN 1 THEN 'DOC_CONTROL_REVIEW'
                       WHEN 2 THEN 'MATRIX_REVIEW'
                       WHEN 3 THEN 'MATRIX_APPROVAL'
                       WHEN 4 THEN 'DOC_CONTROL_APPROVAL'
                       ELSE `stage_code`
                   END
WHERE (`stage_code` IS NULL OR `stage_code` = '');

UPDATE `dcc_controlled_file_route_snapshot`
SET `stage_name` = CASE `stage_no`
                       WHEN 1 THEN 'Document Control Review'
                       WHEN 2 THEN 'Matrix Review'
                       WHEN 3 THEN 'Matrix Approval'
                       WHEN 4 THEN 'Document Control Approval'
                       ELSE `stage_name`
                   END
WHERE (`stage_name` IS NULL OR `stage_name` = '');

UPDATE `dcc_controlled_file_route_snapshot`
SET `stage_order` = COALESCE(`stage_order`, `stage_no`)
WHERE `stage_order` IS NULL;

UPDATE `dcc_controlled_file_route_snapshot`
SET `require_all_approvals` = CASE
                                  WHEN UPPER(COALESCE(`approve_method`, '')) = 'ALL' THEN 1
                                  WHEN COALESCE(`approve_ratio`, 0) >= 100 THEN 1
                                  ELSE 0
                              END;

UPDATE `dcc_controlled_file_route_snapshot`
SET `candidate_source_ids` = CAST(`candidate_source_id` AS CHAR)
WHERE (`candidate_source_ids` IS NULL OR `candidate_source_ids` = '')
  AND `candidate_source_id` IS NOT NULL;

UPDATE `dcc_controlled_file_route_snapshot`
SET `approve_method` = 'ANY',
    `approve_ratio` = NULL,
    `require_all_approvals` = 0
WHERE `stage_code` = 'MATRIX_APPROVAL';

CREATE TABLE IF NOT EXISTS `dcc_controlled_file_signature` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `controlled_file_id` bigint NOT NULL,
  `task_id` varchar(64) NOT NULL,
  `actor_id` bigint NOT NULL,
  `action_type` varchar(32) NOT NULL,
  `signature_mode` varchar(32) NOT NULL,
  `password_verified` tinyint NOT NULL DEFAULT 0,
  `comment` varchar(500) DEFAULT NULL,
  `signed_at` datetime DEFAULT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_dcc_signature_file` (`controlled_file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC controlled file signature evidence';

CREATE TABLE IF NOT EXISTS `dcc_controlled_file_distribution` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `controlled_file_id` bigint NOT NULL,
  `department_id` bigint NOT NULL,
  `distribution_medium` varchar(32) NOT NULL DEFAULT 'PUBLIC_FOLDER',
  `status` varchar(32) NOT NULL,
  `acknowledged_by` bigint DEFAULT NULL,
  `acknowledged_at` datetime DEFAULT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_dcc_distribution_file` (`controlled_file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC controlled file distribution';

CALL ensure_dcc_column(
  'dcc_controlled_file_distribution',
  'distribution_medium',
  'ALTER TABLE `dcc_controlled_file_distribution` ADD COLUMN `distribution_medium` varchar(32) NOT NULL DEFAULT ''PUBLIC_FOLDER'' AFTER `department_id`'
);
CALL ensure_dcc_column(
  'dcc_controlled_file_distribution',
  'acknowledged_by',
  'ALTER TABLE `dcc_controlled_file_distribution` ADD COLUMN `acknowledged_by` bigint DEFAULT NULL AFTER `status`'
);
CALL ensure_dcc_column(
  'dcc_controlled_file_distribution',
  'acknowledged_at',
  'ALTER TABLE `dcc_controlled_file_distribution` ADD COLUMN `acknowledged_at` datetime DEFAULT NULL AFTER `acknowledged_by`'
);

UPDATE `dcc_controlled_file_distribution`
SET `distribution_medium` = 'PUBLIC_FOLDER'
WHERE COALESCE(`distribution_medium`, '') = '';

CREATE TABLE IF NOT EXISTS `dcc_controlled_file_recognition_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `controlled_file_id` bigint NOT NULL,
  `recognition_scope` varchar(32) NOT NULL,
  `recognition_method` varchar(32) NOT NULL,
  `recognition_version` varchar(64) NOT NULL,
  `status` varchar(32) NOT NULL,
  `batch_task_id` bigint DEFAULT NULL,
  `matched_project_code_id` bigint DEFAULT NULL,
  `recognized_product_code` varchar(128) DEFAULT NULL,
  `recognized_product_name` varchar(255) DEFAULT NULL,
  `match_type` varchar(32) DEFAULT NULL,
  `match_text` varchar(255) DEFAULT NULL,
  `failure_stage` varchar(64) DEFAULT NULL,
  `failure_code` varchar(64) DEFAULT NULL,
  `failure_message` varchar(2048) DEFAULT NULL,
  `file_type_level1` varchar(64) DEFAULT NULL,
  `file_type_level2` varchar(128) DEFAULT NULL,
  `file_type_level3` varchar(128) DEFAULT NULL,
  `file_type_level4` varchar(128) DEFAULT NULL,
  `file_type_level5` varchar(128) DEFAULT NULL,
  `recognized_by` bigint DEFAULT NULL,
  `recognized_time` datetime DEFAULT NULL,
  `source_file_id` bigint DEFAULT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_file_recognition_record_biz` (`controlled_file_id`, `recognition_scope`, `recognition_method`, `recognition_version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC controlled file recognition record';

CALL ensure_dcc_column(
  'dcc_controlled_file_recognition_record',
  'batch_task_id',
  'ALTER TABLE `dcc_controlled_file_recognition_record` ADD COLUMN `batch_task_id` bigint DEFAULT NULL AFTER `status`'
);
CALL ensure_dcc_column(
  'dcc_controlled_file_recognition_record',
  'failure_stage',
  'ALTER TABLE `dcc_controlled_file_recognition_record` ADD COLUMN `failure_stage` varchar(64) DEFAULT NULL COMMENT ''Structured recognition failure stage'' AFTER `match_text`'
);
CALL ensure_dcc_column(
  'dcc_controlled_file_recognition_record',
  'failure_code',
  'ALTER TABLE `dcc_controlled_file_recognition_record` ADD COLUMN `failure_code` varchar(64) DEFAULT NULL COMMENT ''Structured recognition failure code'' AFTER `failure_stage`'
);
CALL ensure_dcc_column(
  'dcc_controlled_file_recognition_record',
  'file_type_level1',
  'ALTER TABLE `dcc_controlled_file_recognition_record` ADD COLUMN `file_type_level1` varchar(64) DEFAULT NULL COMMENT ''File type level 1: QMS or technical document'' AFTER `failure_message`'
);
CALL ensure_dcc_column(
  'dcc_controlled_file_recognition_record',
  'file_type_level2',
  'ALTER TABLE `dcc_controlled_file_recognition_record` ADD COLUMN `file_type_level2` varchar(128) DEFAULT NULL COMMENT ''File type level 2: DCC category for technical documents'' AFTER `file_type_level1`'
);
CALL ensure_dcc_column(
  'dcc_controlled_file_recognition_record',
  'file_type_level3',
  'ALTER TABLE `dcc_controlled_file_recognition_record` ADD COLUMN `file_type_level3` varchar(128) DEFAULT NULL COMMENT ''Reserved file type level 3'' AFTER `file_type_level2`'
);
CALL ensure_dcc_column(
  'dcc_controlled_file_recognition_record',
  'file_type_level4',
  'ALTER TABLE `dcc_controlled_file_recognition_record` ADD COLUMN `file_type_level4` varchar(128) DEFAULT NULL COMMENT ''Reserved file type level 4'' AFTER `file_type_level3`'
);
CALL ensure_dcc_column(
  'dcc_controlled_file_recognition_record',
  'file_type_level5',
  'ALTER TABLE `dcc_controlled_file_recognition_record` ADD COLUMN `file_type_level5` varchar(128) DEFAULT NULL COMMENT ''Reserved file type level 5'' AFTER `file_type_level4`'
);

SET @dcc_controlled_file_type_level_index_missing = (
  SELECT COUNT(*) = 0
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'dcc_controlled_file'
    AND INDEX_NAME = 'idx_dcc_controlled_file_type_level'
);
SET @dcc_controlled_file_type_level_index_sql = IF(
  @dcc_controlled_file_type_level_index_missing,
  'CREATE INDEX `idx_dcc_controlled_file_type_level` ON `dcc_controlled_file` (`tenant_id`, `file_type_level1`, `file_type_level2`)',
  'SELECT 1'
);
PREPARE stmt FROM @dcc_controlled_file_type_level_index_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `dcc_controlled_file_distribution_recipient` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `distribution_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `message_job_id` bigint DEFAULT NULL,
  `read_at` datetime DEFAULT NULL,
  `acknowledged_at` datetime DEFAULT NULL,
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
  PRIMARY KEY (`id`)
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
  PRIMARY KEY (`id`)
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

UPDATE `dcc_controlled_file`
SET `deleted` = 0
WHERE `deleted` = 1
  AND `process_definition_key` = 'dcc-controlled-file-approval';

UPDATE `dcc_controlled_file_master` master_record
JOIN `dcc_controlled_file` controlled_file
  ON controlled_file.`master_id` = master_record.`id`
SET master_record.`deleted` = 0
WHERE master_record.`deleted` = 1
  AND controlled_file.`process_definition_key` = 'dcc-controlled-file-approval';

UPDATE `dcc_controlled_file_route_snapshot` route_snapshot
JOIN `dcc_controlled_file` controlled_file
  ON controlled_file.`id` = route_snapshot.`controlled_file_id`
SET route_snapshot.`deleted` = 0
WHERE route_snapshot.`deleted` = 1
  AND controlled_file.`process_definition_key` = 'dcc-controlled-file-approval';

UPDATE `dcc_controlled_file_stamp` stamp_record
JOIN `dcc_controlled_file` controlled_file
  ON controlled_file.`id` = stamp_record.`controlled_file_id`
SET stamp_record.`deleted` = 0
WHERE stamp_record.`deleted` = 1
  AND controlled_file.`process_definition_key` = 'dcc-controlled-file-approval';

UPDATE `dcc_controlled_file_signature` signature_record
JOIN `dcc_controlled_file` controlled_file
  ON controlled_file.`id` = signature_record.`controlled_file_id`
SET signature_record.`deleted` = 0
WHERE signature_record.`deleted` = 1
  AND controlled_file.`process_definition_key` = 'dcc-controlled-file-approval';

UPDATE `dcc_controlled_file_distribution` distribution_record
JOIN `dcc_controlled_file` controlled_file
  ON controlled_file.`id` = distribution_record.`controlled_file_id`
SET distribution_record.`deleted` = 0
WHERE distribution_record.`deleted` = 1
  AND controlled_file.`process_definition_key` = 'dcc-controlled-file-approval';

UPDATE `dcc_controlled_file_distribution_recipient` recipient_record
JOIN `dcc_controlled_file_distribution` distribution_record
  ON distribution_record.`id` = recipient_record.`distribution_id`
JOIN `dcc_controlled_file` controlled_file
  ON controlled_file.`id` = distribution_record.`controlled_file_id`
SET recipient_record.`deleted` = 0,
    distribution_record.`deleted` = 0
WHERE (recipient_record.`deleted` = 1 OR distribution_record.`deleted` = 1)
  AND controlled_file.`process_definition_key` = 'dcc-controlled-file-approval';

UPDATE `dcc_controlled_file_training` training_record
JOIN `dcc_controlled_file` controlled_file
  ON controlled_file.`id` = training_record.`controlled_file_id`
SET training_record.`deleted` = 0
WHERE training_record.`deleted` = 1
  AND controlled_file.`process_definition_key` = 'dcc-controlled-file-approval';

UPDATE `dcc_controlled_file_training_assignment` assignment_record
JOIN `dcc_controlled_file_training` training_record
  ON training_record.`id` = assignment_record.`training_id`
JOIN `dcc_controlled_file` controlled_file
  ON controlled_file.`id` = training_record.`controlled_file_id`
SET assignment_record.`deleted` = 0,
    training_record.`deleted` = 0
WHERE (assignment_record.`deleted` = 1 OR training_record.`deleted` = 1)
  AND controlled_file.`process_definition_key` = 'dcc-controlled-file-approval';

UPDATE `dcc_controlled_file_obsolete_audit` audit_record
JOIN `dcc_controlled_file` controlled_file
  ON controlled_file.`id` = audit_record.`controlled_file_id`
SET audit_record.`deleted` = 0
WHERE audit_record.`deleted` = 1
  AND controlled_file.`process_definition_key` = 'dcc-controlled-file-approval';

DROP PROCEDURE IF EXISTS ensure_dcc_column;
