-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- DCC electronic signature hardening migration.
-- Adds immutable signature evidence fields, fail-closed authorization state, audits, failure lock policy, and indexes.

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

DROP PROCEDURE IF EXISTS ensure_dcc_index;
DELIMITER $$
CREATE PROCEDURE ensure_dcc_index(IN target_table VARCHAR(64), IN target_index VARCHAR(64), IN ddl_statement TEXT)
BEGIN
  IF NOT EXISTS (
      SELECT 1
      FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = target_table
        AND INDEX_NAME = target_index
  ) THEN
    SET @ddl_statement = ddl_statement;
    PREPARE stmt FROM @ddl_statement;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;

CALL ensure_dcc_column('dcc_controlled_file_signature', 'revision_id',
  'ALTER TABLE `dcc_controlled_file_signature` ADD COLUMN `revision_id` bigint DEFAULT NULL AFTER `controlled_file_id`');
CALL ensure_dcc_column('dcc_controlled_file_signature', 'version_no',
  'ALTER TABLE `dcc_controlled_file_signature` ADD COLUMN `version_no` varchar(64) DEFAULT NULL AFTER `revision_id`');
CALL ensure_dcc_column('dcc_controlled_file_signature', 'actor_username_snapshot',
  'ALTER TABLE `dcc_controlled_file_signature` ADD COLUMN `actor_username_snapshot` varchar(64) DEFAULT NULL AFTER `actor_id`');
CALL ensure_dcc_column('dcc_controlled_file_signature', 'actor_nickname_snapshot',
  'ALTER TABLE `dcc_controlled_file_signature` ADD COLUMN `actor_nickname_snapshot` varchar(64) DEFAULT NULL AFTER `actor_username_snapshot`');
CALL ensure_dcc_column('dcc_controlled_file_signature', 'actor_dept_id_snapshot',
  'ALTER TABLE `dcc_controlled_file_signature` ADD COLUMN `actor_dept_id_snapshot` bigint DEFAULT NULL AFTER `actor_nickname_snapshot`');
CALL ensure_dcc_column('dcc_controlled_file_signature', 'actor_dept_name_snapshot',
  'ALTER TABLE `dcc_controlled_file_signature` ADD COLUMN `actor_dept_name_snapshot` varchar(128) DEFAULT NULL AFTER `actor_dept_id_snapshot`');
CALL ensure_dcc_column('dcc_controlled_file_signature', 'actor_post_names_snapshot',
  'ALTER TABLE `dcc_controlled_file_signature` ADD COLUMN `actor_post_names_snapshot` varchar(512) DEFAULT NULL AFTER `actor_dept_name_snapshot`');
CALL ensure_dcc_column('dcc_controlled_file_signature', 'actor_role_names_snapshot',
  'ALTER TABLE `dcc_controlled_file_signature` ADD COLUMN `actor_role_names_snapshot` varchar(512) DEFAULT NULL AFTER `actor_post_names_snapshot`');
CALL ensure_dcc_column('dcc_controlled_file_signature', 'signature_purpose',
  'ALTER TABLE `dcc_controlled_file_signature` ADD COLUMN `signature_purpose` varchar(128) DEFAULT NULL AFTER `actor_role_names_snapshot`');
CALL ensure_dcc_column('dcc_controlled_file_signature', 'authorization_basis',
  'ALTER TABLE `dcc_controlled_file_signature` ADD COLUMN `authorization_basis` varchar(500) DEFAULT NULL AFTER `signature_purpose`');
CALL ensure_dcc_column('dcc_controlled_file_signature', 'authentication_method',
  'ALTER TABLE `dcc_controlled_file_signature` ADD COLUMN `authentication_method` varchar(64) DEFAULT NULL AFTER `authorization_basis`');
CALL ensure_dcc_column('dcc_controlled_file_signature', 'record_version_snapshot',
  'ALTER TABLE `dcc_controlled_file_signature` ADD COLUMN `record_version_snapshot` varchar(64) DEFAULT NULL AFTER `authentication_method`');
CALL ensure_dcc_column('dcc_controlled_file_signature', 'record_hash_snapshot',
  'ALTER TABLE `dcc_controlled_file_signature` ADD COLUMN `record_hash_snapshot` varchar(128) DEFAULT NULL AFTER `record_version_snapshot`');
CALL ensure_dcc_column('dcc_controlled_file_signature', 'client_ip_snapshot',
  'ALTER TABLE `dcc_controlled_file_signature` ADD COLUMN `client_ip_snapshot` varchar(64) DEFAULT NULL AFTER `record_hash_snapshot`');
CALL ensure_dcc_column('dcc_controlled_file_signature', 'user_agent_snapshot',
  'ALTER TABLE `dcc_controlled_file_signature` ADD COLUMN `user_agent_snapshot` varchar(512) DEFAULT NULL AFTER `client_ip_snapshot`');
CALL ensure_dcc_column('dcc_controlled_file_signature', 'snapshot_status',
  'ALTER TABLE `dcc_controlled_file_signature` ADD COLUMN `snapshot_status` varchar(32) DEFAULT NULL AFTER `user_agent_snapshot`');
CALL ensure_dcc_column('dcc_controlled_file_signature', 'meaning_code',
  'ALTER TABLE `dcc_controlled_file_signature` ADD COLUMN `meaning_code` varchar(64) DEFAULT NULL AFTER `action_type`');
CALL ensure_dcc_column('dcc_controlled_file_signature', 'meaning_label',
  'ALTER TABLE `dcc_controlled_file_signature` ADD COLUMN `meaning_label` varchar(128) DEFAULT NULL AFTER `meaning_code`');
CALL ensure_dcc_column('dcc_controlled_file_signature', 'source_file_id',
  'ALTER TABLE `dcc_controlled_file_signature` ADD COLUMN `source_file_id` bigint DEFAULT NULL AFTER `signed_at`');
CALL ensure_dcc_column('dcc_controlled_file_signature', 'source_file_hash',
  'ALTER TABLE `dcc_controlled_file_signature` ADD COLUMN `source_file_hash` varchar(128) DEFAULT NULL AFTER `source_file_id`');
CALL ensure_dcc_column('dcc_controlled_file_signature', 'source_file_hash_algorithm',
  'ALTER TABLE `dcc_controlled_file_signature` ADD COLUMN `source_file_hash_algorithm` varchar(32) DEFAULT NULL AFTER `source_file_hash`');
CALL ensure_dcc_column('dcc_controlled_file_signature', 'source_file_hash_status',
  'ALTER TABLE `dcc_controlled_file_signature` ADD COLUMN `source_file_hash_status` varchar(32) NOT NULL DEFAULT ''HISTORICAL_UNBOUND'' AFTER `source_file_hash_algorithm`');
CALL ensure_dcc_column('dcc_controlled_file_signature', 'controlled_copy_file_id',
  'ALTER TABLE `dcc_controlled_file_signature` ADD COLUMN `controlled_copy_file_id` bigint DEFAULT NULL AFTER `source_file_hash_status`');
CALL ensure_dcc_column('dcc_controlled_file_signature', 'controlled_copy_hash',
  'ALTER TABLE `dcc_controlled_file_signature` ADD COLUMN `controlled_copy_hash` varchar(128) DEFAULT NULL AFTER `controlled_copy_file_id`');
CALL ensure_dcc_column('dcc_controlled_file_signature', 'controlled_copy_hash_algorithm',
  'ALTER TABLE `dcc_controlled_file_signature` ADD COLUMN `controlled_copy_hash_algorithm` varchar(32) DEFAULT NULL AFTER `controlled_copy_hash`');
CALL ensure_dcc_column('dcc_controlled_file_signature', 'controlled_copy_hash_status',
  'ALTER TABLE `dcc_controlled_file_signature` ADD COLUMN `controlled_copy_hash_status` varchar(32) NOT NULL DEFAULT ''NOT_APPLICABLE'' AFTER `controlled_copy_hash_algorithm`');
CALL ensure_dcc_column('dcc_controlled_file_signature', 'evidence_payload_version',
  'ALTER TABLE `dcc_controlled_file_signature` ADD COLUMN `evidence_payload_version` varchar(32) DEFAULT NULL AFTER `controlled_copy_hash_status`');
CALL ensure_dcc_column('dcc_controlled_file_signature', 'evidence_key_version',
  'ALTER TABLE `dcc_controlled_file_signature` ADD COLUMN `evidence_key_version` varchar(64) DEFAULT NULL AFTER `evidence_payload_version`');
CALL ensure_dcc_column('dcc_controlled_file_signature', 'evidence_hash',
  'ALTER TABLE `dcc_controlled_file_signature` ADD COLUMN `evidence_hash` varchar(128) DEFAULT NULL AFTER `evidence_key_version`');
CALL ensure_dcc_column('dcc_controlled_file_signature', 'evidence_hash_algorithm',
  'ALTER TABLE `dcc_controlled_file_signature` ADD COLUMN `evidence_hash_algorithm` varchar(32) DEFAULT NULL AFTER `evidence_hash`');
CALL ensure_dcc_column('dcc_controlled_file_signature', 'evidence_status',
  'ALTER TABLE `dcc_controlled_file_signature` ADD COLUMN `evidence_status` varchar(32) NOT NULL DEFAULT ''HISTORICAL_UNBOUND'' AFTER `evidence_hash_algorithm`');

CALL ensure_dcc_column('dcc_electronic_signature_authorization', 'authorization_state',
  'ALTER TABLE `dcc_electronic_signature_authorization` ADD COLUMN `authorization_state` varchar(32) NOT NULL DEFAULT ''UNAUTHORIZED'' AFTER `electronic_signature_enabled`');
CALL ensure_dcc_column('dcc_electronic_signature_authorization', 'locked_until',
  'ALTER TABLE `dcc_electronic_signature_authorization` ADD COLUMN `locked_until` datetime DEFAULT NULL AFTER `authorization_state`');
CALL ensure_dcc_column('dcc_electronic_signature_authorization', 'lock_reason',
  'ALTER TABLE `dcc_electronic_signature_authorization` ADD COLUMN `lock_reason` varchar(255) DEFAULT NULL AFTER `locked_until`');
CALL ensure_dcc_column('dcc_electronic_signature_authorization', 'last_failure_at',
  'ALTER TABLE `dcc_electronic_signature_authorization` ADD COLUMN `last_failure_at` datetime DEFAULT NULL AFTER `lock_reason`');
CALL ensure_dcc_column('dcc_electronic_signature_authorization', 'failure_count',
  'ALTER TABLE `dcc_electronic_signature_authorization` ADD COLUMN `failure_count` int NOT NULL DEFAULT 0 AFTER `last_failure_at`');

CREATE TABLE IF NOT EXISTS `dcc_electronic_signature_authorization_audit` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `target_user_id` bigint NOT NULL,
  `operator_id` bigint DEFAULT NULL,
  `before_state` varchar(32) DEFAULT NULL,
  `after_state` varchar(32) NOT NULL,
  `before_enabled` tinyint DEFAULT NULL,
  `after_enabled` tinyint NOT NULL,
  `reason` varchar(255) NOT NULL,
  `operated_at` datetime NOT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_dcc_signature_auth_audit_user` (`target_user_id`, `operated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC electronic signature authorization audit';

CREATE TABLE IF NOT EXISTS `dcc_electronic_signature_failure_audit` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `target_user_id` bigint NOT NULL,
  `controlled_file_id` bigint DEFAULT NULL,
  `revision_id` bigint DEFAULT NULL,
  `task_id` varchar(64) DEFAULT NULL,
  `action_type` varchar(32) DEFAULT NULL,
  `meaning_code` varchar(64) DEFAULT NULL,
  `failure_type` varchar(32) NOT NULL,
  `failure_message` varchar(255) DEFAULT NULL,
  `failed_at` datetime NOT NULL,
  `remote_ip` varchar(64) DEFAULT NULL,
  `user_agent` varchar(255) DEFAULT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_dcc_signature_failure_user_time` (`target_user_id`, `failed_at`),
  KEY `idx_dcc_signature_failure_file` (`controlled_file_id`, `revision_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC electronic signature failure audit';

CREATE TABLE IF NOT EXISTS `dcc_electronic_signature_policy` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `password_failure_window_minutes` int NOT NULL,
  `password_failure_threshold` int NOT NULL,
  `lock_minutes` int NOT NULL,
  `evidence_payload_version` varchar(32) NOT NULL,
  `hash_algorithm` varchar(32) NOT NULL,
  `status` tinyint NOT NULL DEFAULT 0,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_dcc_signature_policy_status` (`status`),
  KEY `idx_dcc_signature_policy_tenant_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC electronic signature policy';

UPDATE `dcc_controlled_file_signature`
SET `source_file_hash_status` = 'HISTORICAL_UNBOUND',
    `controlled_copy_hash_status` = 'HISTORICAL_UNBOUND',
    `evidence_status` = 'HISTORICAL_UNBOUND'
WHERE `evidence_hash` IS NULL
  AND `revision_id` IS NULL;

DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_signature_authorization_init_users`;
CREATE TEMPORARY TABLE `tmp_dcc_signature_authorization_init_users` (
  `user_id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  PRIMARY KEY (`user_id`)
) ENGINE=Memory;

INSERT IGNORE INTO `tmp_dcc_signature_authorization_init_users` (`user_id`, `tenant_id`)
SELECT authorization.`user_id`, authorization.`tenant_id`
FROM `dcc_electronic_signature_authorization` authorization
JOIN `system_users` active_user
  ON active_user.`id` = authorization.`user_id`
 AND active_user.`status` = 0
 AND active_user.`deleted` = 0
WHERE authorization.`electronic_signature_enabled` = 1
  AND authorization.`deleted` = 0;

INSERT IGNORE INTO `tmp_dcc_signature_authorization_init_users` (`user_id`, `tenant_id`)
SELECT active_user.`id`, route_node.`tenant_id`
FROM `dcc_category_approval_route` route
JOIN `dcc_category_approval_route_node` route_node
  ON route_node.`route_id` = route.`id`
 AND route_node.`deleted` = 0
JOIN `system_users` active_user
  ON active_user.`status` = 0
 AND active_user.`deleted` = 0
 AND FIND_IN_SET(
     CONVERT(CAST(active_user.`id` AS CHAR) USING utf8mb4) COLLATE utf8mb4_unicode_ci,
     REPLACE(COALESCE(
         CONVERT(route_node.`candidate_source_ids` USING utf8mb4) COLLATE utf8mb4_unicode_ci,
         CONVERT(CAST(route_node.`candidate_source_id` AS CHAR) USING utf8mb4) COLLATE utf8mb4_unicode_ci
       ), _utf8mb4' ' COLLATE utf8mb4_unicode_ci, _utf8mb4'' COLLATE utf8mb4_unicode_ci)
       COLLATE utf8mb4_unicode_ci
   ) > 0
WHERE route.`active` = 1
  AND route.`deleted` = 0
  AND (route.`effective_time` IS NULL OR route.`effective_time` <= NOW())
  AND route_node.`candidate_source_type` IN ('USER', 'user');

INSERT IGNORE INTO `tmp_dcc_signature_authorization_init_users` (`user_id`, `tenant_id`)
SELECT active_user.`id`, route_node.`tenant_id`
FROM `dcc_category_approval_route` route
JOIN `dcc_category_approval_route_node` route_node
  ON route_node.`route_id` = route.`id`
 AND route_node.`deleted` = 0
JOIN `dcc_position_assignment` assignment
  ON assignment.`active` = 1
 AND assignment.`deleted` = 0
 AND FIND_IN_SET(
     CONVERT(CAST(assignment.`position_id` AS CHAR) USING utf8mb4) COLLATE utf8mb4_unicode_ci,
     REPLACE(COALESCE(
         CONVERT(route_node.`candidate_source_ids` USING utf8mb4) COLLATE utf8mb4_unicode_ci,
         CONVERT(CAST(route_node.`candidate_source_id` AS CHAR) USING utf8mb4) COLLATE utf8mb4_unicode_ci
       ), _utf8mb4' ' COLLATE utf8mb4_unicode_ci, _utf8mb4'' COLLATE utf8mb4_unicode_ci)
       COLLATE utf8mb4_unicode_ci
   ) > 0
JOIN `system_users` active_user
  ON active_user.`status` = 0
 AND active_user.`deleted` = 0
 AND (
      (assignment.`user_id` IS NOT NULL AND active_user.`id` = assignment.`user_id`)
      OR (assignment.`system_post_id` IS NOT NULL
          AND JSON_CONTAINS(COALESCE(active_user.`post_ids`, '[]'), CAST(assignment.`system_post_id` AS JSON)))
 )
WHERE route.`active` = 1
  AND route.`deleted` = 0
  AND (route.`effective_time` IS NULL OR route.`effective_time` <= NOW())
  AND route_node.`candidate_source_type` IN ('POSITION', 'position');

INSERT IGNORE INTO `tmp_dcc_signature_authorization_init_users` (`user_id`, `tenant_id`)
SELECT active_user.`id`, route_snapshot.`tenant_id`
FROM `dcc_controlled_file_route_snapshot` route_snapshot
JOIN `dcc_controlled_file` controlled_file
  ON controlled_file.`id` = route_snapshot.`controlled_file_id`
 AND controlled_file.`deleted` = 0
 AND controlled_file.`process_definition_key` = 'dcc-controlled-file-approval'
 AND controlled_file.`status` IN (
    'PENDING_DOC_CONTROL_REVIEW', 'PENDING_MATRIX_REVIEW',
    'PENDING_MATRIX_APPROVAL', 'PENDING_DOC_CONTROL_APPROVAL'
 )
JOIN `system_users` active_user
  ON active_user.`status` = 0
 AND active_user.`deleted` = 0
 AND FIND_IN_SET(
     CONVERT(CAST(active_user.`id` AS CHAR) USING utf8mb4) COLLATE utf8mb4_unicode_ci,
     REPLACE(
       CONVERT(route_snapshot.`resolved_user_ids` USING utf8mb4) COLLATE utf8mb4_unicode_ci,
       _utf8mb4' ' COLLATE utf8mb4_unicode_ci,
       _utf8mb4'' COLLATE utf8mb4_unicode_ci
     ) COLLATE utf8mb4_unicode_ci
   ) > 0
WHERE route_snapshot.`deleted` = 0
  AND route_snapshot.`resolved_user_ids` IS NOT NULL
  AND route_snapshot.`resolved_user_ids` <> '';

INSERT IGNORE INTO `tmp_dcc_signature_authorization_init_users` (`user_id`, `tenant_id`)
SELECT active_user.`id`, controlled_file.`tenant_id`
FROM `ACT_RU_TASK` task
JOIN `dcc_controlled_file` controlled_file
  ON controlled_file.`process_instance_id` = task.`PROC_INST_ID_`
 AND controlled_file.`deleted` = 0
 AND controlled_file.`process_definition_key` = 'dcc-controlled-file-approval'
 AND controlled_file.`status` IN (
    'PENDING_DOC_CONTROL_REVIEW', 'PENDING_MATRIX_REVIEW',
    'PENDING_MATRIX_APPROVAL', 'PENDING_DOC_CONTROL_APPROVAL'
 )
JOIN `system_users` active_user
  ON active_user.`status` = 0
 AND active_user.`deleted` = 0
 AND active_user.`id` IN (
      CAST(task.`ASSIGNEE_` AS UNSIGNED),
      CAST(task.`OWNER_` AS UNSIGNED)
 )
WHERE (task.`ASSIGNEE_` REGEXP '^[0-9]+$' OR task.`OWNER_` REGEXP '^[0-9]+$');

INSERT INTO `dcc_electronic_signature_authorization`
(`user_id`, `electronic_signature_enabled`, `authorization_state`, `locked_until`, `lock_reason`, `last_failure_at`,
 `failure_count`, `tenant_id`, `create_time`, `update_time`, `creator`, `updater`, `deleted`)
SELECT init_user.`user_id`, 1, 'ENABLED', NULL, NULL, NULL, 0, init_user.`tenant_id`,
       NOW(), NOW(), 'migration', 'migration', 0
FROM `tmp_dcc_signature_authorization_init_users` init_user
LEFT JOIN `dcc_electronic_signature_authorization` authorization_existing
  ON authorization_existing.`user_id` = init_user.`user_id`
WHERE authorization_existing.`user_id` IS NULL;

UPDATE `dcc_electronic_signature_authorization`
SET `authorization_state` = CASE
    WHEN `electronic_signature_enabled` = 1 THEN 'ENABLED'
    ELSE 'DISABLED'
  END,
  `failure_count` = COALESCE(`failure_count`, 0)
WHERE `authorization_state` = 'UNAUTHORIZED';

INSERT INTO `dcc_electronic_signature_authorization_audit`
(`target_user_id`, `operator_id`, `before_state`, `after_state`, `before_enabled`, `after_enabled`, `reason`,
 `operated_at`, `tenant_id`, `create_time`, `update_time`, `creator`, `updater`, `deleted`)
SELECT `user_id`, NULL, 'UNAUTHORIZED', `authorization_state`, 0, `electronic_signature_enabled`,
       'PHASE1_FAIL_CLOSED_INITIALIZATION', NOW(), `tenant_id`, NOW(), NOW(), 'migration', 'migration', 0
FROM `dcc_electronic_signature_authorization` authorization
WHERE NOT EXISTS (
  SELECT 1
  FROM `dcc_electronic_signature_authorization_audit` audit
  WHERE audit.`target_user_id` = authorization.`user_id`
    AND audit.`reason` = 'PHASE1_FAIL_CLOSED_INITIALIZATION'
);

INSERT INTO `dcc_electronic_signature_policy`
(`password_failure_window_minutes`, `password_failure_threshold`, `lock_minutes`, `evidence_payload_version`,
 `hash_algorithm`, `status`, `tenant_id`, `create_time`, `update_time`, `creator`, `updater`, `deleted`)
SELECT 15, 5, 30, 'v1', 'HMAC_SHA256', 0, tenant.`id`, NOW(), NOW(), 'migration', 'migration', 0
FROM `system_tenant` tenant
LEFT JOIN `dcc_electronic_signature_policy` policy_existing
  ON policy_existing.`tenant_id` = tenant.`id`
 AND policy_existing.`status` = 0
 AND policy_existing.`deleted` = 0
WHERE tenant.`status` = 0
  AND tenant.`deleted` = 0
  AND policy_existing.`id` IS NULL;

CALL ensure_dcc_index('dcc_controlled_file_signature', 'idx_dcc_signature_revision',
  'ALTER TABLE `dcc_controlled_file_signature` ADD INDEX `idx_dcc_signature_revision` (`revision_id`)');
CALL ensure_dcc_index('dcc_controlled_file_signature', 'idx_dcc_signature_actor',
  'ALTER TABLE `dcc_controlled_file_signature` ADD INDEX `idx_dcc_signature_actor` (`actor_id`)');
CALL ensure_dcc_index('dcc_controlled_file_signature', 'idx_dcc_signature_meaning',
  'ALTER TABLE `dcc_controlled_file_signature` ADD INDEX `idx_dcc_signature_meaning` (`meaning_code`)');
CALL ensure_dcc_index('dcc_controlled_file_signature', 'idx_dcc_signature_signed_at',
  'ALTER TABLE `dcc_controlled_file_signature` ADD INDEX `idx_dcc_signature_signed_at` (`signed_at`)');
CALL ensure_dcc_index('dcc_controlled_file_signature', 'idx_dcc_signature_evidence_status',
  'ALTER TABLE `dcc_controlled_file_signature` ADD INDEX `idx_dcc_signature_evidence_status` (`evidence_status`)');
CALL ensure_dcc_index('dcc_controlled_file_signature', 'idx_dcc_signature_source_file',
  'ALTER TABLE `dcc_controlled_file_signature` ADD INDEX `idx_dcc_signature_source_file` (`source_file_id`)');
CALL ensure_dcc_index('dcc_controlled_file_signature', 'idx_dcc_signature_copy_file',
  'ALTER TABLE `dcc_controlled_file_signature` ADD INDEX `idx_dcc_signature_copy_file` (`controlled_copy_file_id`)');
CALL ensure_dcc_index('dcc_electronic_signature_policy', 'idx_dcc_signature_policy_tenant_status',
  'ALTER TABLE `dcc_electronic_signature_policy` ADD INDEX `idx_dcc_signature_policy_tenant_status` (`tenant_id`, `status`)');

DROP PROCEDURE IF EXISTS ensure_dcc_index;
DROP PROCEDURE IF EXISTS ensure_dcc_column;
