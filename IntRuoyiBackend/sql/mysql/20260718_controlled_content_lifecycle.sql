-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260718_mes_route_version_single_open_candidate,20260513_dcc_base_schema; type=schema; riskLevel=medium
-- Purpose: Create the minimal controlled content lifecycle reference and transition audit tables.

DROP PROCEDURE IF EXISTS ensure_controlled_content_lifecycle_preflight;
DELIMITER $$
CREATE PROCEDURE ensure_controlled_content_lifecycle_preflight()
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.TABLES
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'mes_pro_route_version'
  ) THEN
    IF EXISTS (
      SELECT 1
        FROM `mes_pro_route_version`
       WHERE `deleted` = b'0'
         AND `active` = b'1'
       GROUP BY `tenant_id`, `route_id`
      HAVING COUNT(*) > 1
    ) THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'duplicate MES route active versions must be resolved before controlled content lifecycle migration';
    END IF;

    IF EXISTS (
      SELECT 1
        FROM `mes_pro_route_version`
       WHERE `deleted` = b'0'
         AND `active` = b'0'
         AND `lifecycle_status` IN ('DRAFT', 'PENDING_APPROVAL', 'READY_TO_PUBLISH', 'FINALIZATION_FAILED')
       GROUP BY `tenant_id`, `route_id`
      HAVING COUNT(*) > 1
    ) THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'duplicate MES route open candidate versions must be resolved before controlled content lifecycle migration';
    END IF;
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.TABLES
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'dcc_controlled_file'
  ) THEN
    IF EXISTS (
      SELECT 1
        FROM `dcc_controlled_file`
       WHERE `deleted` = 0
         AND `status` = 'ACTIVE'
       GROUP BY `tenant_id`, `master_id`
      HAVING COUNT(*) > 1
    ) THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'duplicate DCC active revisions must be resolved before controlled content lifecycle migration';
    END IF;

    IF EXISTS (
      SELECT 1
        FROM `dcc_controlled_file`
       WHERE `deleted` = 0
         AND `status` IN (
           'DRAFT',
           'PENDING_DOC_CONTROL_REVIEW',
           'PENDING_MATRIX_REVIEW',
           'PENDING_MATRIX_APPROVAL',
           'PENDING_DOC_CONTROL_APPROVAL',
           'PENDING_APPLICANT_REWORK',
           'PENDING_APPLICANT_TRAINING_RECORD',
           'FINALIZING',
           'TRAINING_IN_PROGRESS',
           'PENDING_MANUAL_DISTRIBUTION',
           'FINALIZATION_FAILED'
         )
       GROUP BY `tenant_id`, `master_id`
      HAVING COUNT(*) > 1
    ) THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'duplicate DCC open candidate revisions must be resolved before controlled content lifecycle migration';
    END IF;
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.TABLES
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'dcc_controlled_file_master'
  ) AND EXISTS (
    SELECT 1 FROM information_schema.TABLES
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'dcc_controlled_file'
  ) THEN
    IF EXISTS (
      SELECT 1
        FROM `dcc_controlled_file_master` master
        JOIN `dcc_controlled_file` file
          ON file.`master_id` = master.`id`
         AND file.`tenant_id` = master.`tenant_id`
       WHERE master.`deleted` = 0
         AND master.`status` = 'OBSOLETE_CHAIN'
         AND file.`deleted` = 0
         AND file.`status` = 'ACTIVE'
    ) THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'dcc obsolete chain has active revision; repair chain status before controlled content lifecycle migration';
    END IF;

    IF EXISTS (
      SELECT 1
        FROM `dcc_controlled_file_master` master
        JOIN `dcc_controlled_file` file
          ON file.`id` = master.`current_active_controlled_file_id`
       WHERE master.`deleted` = 0
         AND master.`status` <> 'OBSOLETE_CHAIN'
         AND file.`deleted` = 0
         AND file.`status` = 'OBSOLETE'
    ) THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'dcc master points to obsolete revision; repair current active before controlled content lifecycle migration';
    END IF;

    IF EXISTS (
      SELECT 1
        FROM `dcc_controlled_file_master` master
        LEFT JOIN `dcc_controlled_file` file
          ON file.`id` = master.`current_active_controlled_file_id`
         AND file.`tenant_id` = master.`tenant_id`
       WHERE master.`deleted` = 0
         AND master.`status` <> 'OBSOLETE_CHAIN'
         AND master.`current_active_controlled_file_id` IS NOT NULL
         AND (
           file.`id` IS NULL
           OR file.`deleted` <> 0
           OR file.`status` <> 'ACTIVE'
         )
    ) THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'dcc master current active revision must be ACTIVE before controlled content lifecycle migration';
    END IF;
  END IF;
END$$
DELIMITER ;

CALL ensure_controlled_content_lifecycle_preflight();

DROP PROCEDURE IF EXISTS ensure_controlled_content_lifecycle_schema;
DELIMITER $$
CREATE PROCEDURE ensure_controlled_content_lifecycle_schema()
BEGIN
  CREATE TABLE IF NOT EXISTS `controlled_content_version_ref` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Lifecycle ref id',
    `tenant_id` bigint NOT NULL COMMENT 'Tenant id',
    `content_type` varchar(64) NOT NULL COMMENT 'Controlled content type',
    `content_key` varchar(128) NOT NULL COMMENT 'Stable native content key',
    `native_master_id` bigint NOT NULL COMMENT 'Native master id',
    `native_version_id` bigint NOT NULL COMMENT 'Native version id',
    `version_no` varchar(64) NOT NULL COMMENT 'Business version number',
    `canonical_status` varchar(64) NOT NULL COMMENT 'Platform lifecycle status',
    `domain_status` varchar(128) DEFAULT NULL COMMENT 'Native domain status',
    `source_version_ref_id` bigint DEFAULT NULL COMMENT 'Source platform version ref id',
    `source_native_version_id` bigint DEFAULT NULL COMMENT 'Source native version id',
    `successor_version_ref_id` bigint DEFAULT NULL COMMENT 'Successor platform version ref id',
    `successor_native_version_id` bigint DEFAULT NULL COMMENT 'Successor native version id',
    `active_unique_flag` tinyint DEFAULT NULL COMMENT '1 only for active version, null otherwise',
    `open_candidate_unique_flag` tinyint DEFAULT NULL COMMENT '1 only for unfinished candidate, null otherwise',
    `approval_process_instance_id` varchar(128) DEFAULT NULL COMMENT 'Approval process instance id',
    `last_transition_time` datetime DEFAULT NULL COMMENT 'Last transition time',
    `creator` varchar(64) DEFAULT NULL COMMENT 'Creator',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    `updater` varchar(64) DEFAULT NULL COMMENT 'Updater',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'Deleted flag',
    PRIMARY KEY (`id`),
    KEY `idx_controlled_content_ref_native` (`tenant_id`, `content_type`, `native_version_id`),
    KEY `idx_controlled_content_ref_content` (`tenant_id`, `content_type`, `content_key`),
    KEY `idx_controlled_content_ref_source` (`source_version_ref_id`),
    KEY `idx_controlled_content_ref_successor` (`successor_version_ref_id`),
    UNIQUE KEY `uk_controlled_content_active` (`tenant_id`, `content_type`, `content_key`, `active_unique_flag`),
    UNIQUE KEY `uk_controlled_content_open_candidate` (`tenant_id`, `content_type`, `content_key`, `open_candidate_unique_flag`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Controlled content lifecycle version reference';

  CREATE TABLE IF NOT EXISTS `controlled_content_transition_audit` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Transition audit id',
    `tenant_id` bigint NOT NULL COMMENT 'Tenant id',
    `version_ref_id` bigint NOT NULL COMMENT 'Lifecycle ref id',
    `content_type` varchar(64) NOT NULL COMMENT 'Controlled content type',
    `content_key` varchar(128) NOT NULL COMMENT 'Stable native content key',
    `from_status` varchar(64) DEFAULT NULL COMMENT 'Previous platform status',
    `to_status` varchar(64) NOT NULL COMMENT 'Next platform status',
    `domain_from_status` varchar(128) DEFAULT NULL COMMENT 'Previous native status',
    `domain_to_status` varchar(128) DEFAULT NULL COMMENT 'Next native status',
    `action` varchar(64) NOT NULL COMMENT 'Transition action',
    `event_key` varchar(256) DEFAULT NULL COMMENT 'Idempotent domain event key',
    `actor_id` bigint DEFAULT NULL COMMENT 'Actor user id',
    `reason` varchar(1024) DEFAULT NULL COMMENT 'Transition reason',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    PRIMARY KEY (`id`),
    KEY `idx_controlled_content_transition_ref` (`version_ref_id`, `create_time`),
    KEY `idx_controlled_content_transition_content` (`tenant_id`, `content_type`, `content_key`, `create_time`),
    UNIQUE KEY `uk_controlled_content_transition_event` (`version_ref_id`, `action`, `event_key`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Controlled content lifecycle transition audit';

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'controlled_content_version_ref'
       AND INDEX_NAME = 'uk_controlled_content_active'
  ) THEN
    ALTER TABLE `controlled_content_version_ref`
      ADD UNIQUE INDEX `uk_controlled_content_active`
        (`tenant_id`, `content_type`, `content_key`, `active_unique_flag`) USING BTREE;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'controlled_content_version_ref'
       AND INDEX_NAME = 'uk_controlled_content_open_candidate'
  ) THEN
    ALTER TABLE `controlled_content_version_ref`
      ADD UNIQUE INDEX `uk_controlled_content_open_candidate`
        (`tenant_id`, `content_type`, `content_key`, `open_candidate_unique_flag`) USING BTREE;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'controlled_content_transition_audit'
       AND COLUMN_NAME = 'event_key'
  ) THEN
    ALTER TABLE `controlled_content_transition_audit`
      ADD COLUMN `event_key` varchar(256) DEFAULT NULL COMMENT 'Idempotent domain event key'
      AFTER `action`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'controlled_content_transition_audit'
       AND INDEX_NAME = 'uk_controlled_content_transition_event'
  ) THEN
    ALTER TABLE `controlled_content_transition_audit`
      ADD UNIQUE INDEX `uk_controlled_content_transition_event`
        (`version_ref_id`, `action`, `event_key`) USING BTREE;
  END IF;
END$$
DELIMITER ;

CALL ensure_controlled_content_lifecycle_schema();

DROP PROCEDURE IF EXISTS ensure_controlled_content_lifecycle_adoption;
DELIMITER $$
CREATE PROCEDURE ensure_controlled_content_lifecycle_adoption()
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.TABLES
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'mes_pro_route_version'
  ) THEN
    IF EXISTS (
      SELECT 1
        FROM `mes_pro_route_version` route_version
        JOIN `controlled_content_version_ref` ref
          ON ref.`tenant_id` = route_version.`tenant_id`
         AND ref.`content_type` = 'MES_ROUTE'
         AND ref.`content_key` = CAST(route_version.`route_id` AS CHAR) COLLATE utf8mb4_unicode_ci
         AND ref.`active_unique_flag` = 1
         AND ref.`deleted` = b'0'
       WHERE route_version.`deleted` = b'0'
         AND route_version.`active` = b'1'
         AND ref.`native_version_id` <> route_version.`id`
    ) THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'controlled content MES active refs drift from native active versions; repair before adoption';
    END IF;

    INSERT INTO `controlled_content_version_ref` (
      `tenant_id`,
      `content_type`,
      `content_key`,
      `native_master_id`,
      `native_version_id`,
      `version_no`,
      `canonical_status`,
      `domain_status`,
      `active_unique_flag`,
      `open_candidate_unique_flag`,
      `approval_process_instance_id`,
      `last_transition_time`,
      `creator`,
      `create_time`,
      `updater`,
      `update_time`,
      `deleted`
    )
    SELECT route_version.`tenant_id`,
           'MES_ROUTE',
           CAST(route_version.`route_id` AS CHAR) COLLATE utf8mb4_unicode_ci,
           route_version.`route_id`,
           route_version.`id`,
           route_version.`version_no`,
           'ACTIVE',
           route_version.`lifecycle_status`,
           1,
           NULL,
           route_version.`approval_process_instance_id`,
           COALESCE(route_version.`published_time`, route_version.`update_time`, route_version.`create_time`, NOW()),
           route_version.`creator`,
           COALESCE(route_version.`create_time`, NOW()),
           route_version.`updater`,
           COALESCE(route_version.`update_time`, route_version.`create_time`, NOW()),
           b'0'
      FROM `mes_pro_route_version` route_version
     WHERE route_version.`deleted` = b'0'
       AND route_version.`active` = b'1'
       AND route_version.`lifecycle_status` = 'ACTIVE'
       AND NOT EXISTS (
         SELECT 1
           FROM `controlled_content_version_ref` existing_ref
          WHERE existing_ref.`tenant_id` = route_version.`tenant_id`
            AND existing_ref.`content_type` = 'MES_ROUTE'
            AND existing_ref.`content_key` = CAST(route_version.`route_id` AS CHAR) COLLATE utf8mb4_unicode_ci
            AND existing_ref.`native_version_id` = route_version.`id`
            AND existing_ref.`deleted` = b'0'
       );
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.TABLES
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'dcc_controlled_file_master'
  ) AND EXISTS (
    SELECT 1 FROM information_schema.TABLES
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'dcc_controlled_file'
  ) THEN
    IF EXISTS (
      SELECT 1
        FROM `dcc_controlled_file_master` master
        LEFT JOIN `dcc_controlled_file` file
          ON file.`id` = master.`current_active_controlled_file_id`
         AND file.`tenant_id` = master.`tenant_id`
       WHERE master.`deleted` = 0
         AND master.`status` <> 'OBSOLETE_CHAIN'
         AND master.`current_active_controlled_file_id` IS NOT NULL
         AND (
           file.`id` IS NULL
           OR file.`deleted` <> 0
           OR file.`status` <> 'ACTIVE'
         )
    ) THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'dcc master current active revision must be ACTIVE before controlled content lifecycle adoption';
    END IF;

    IF EXISTS (
      SELECT 1
        FROM `dcc_controlled_file_master` master
        JOIN `dcc_controlled_file` file
          ON file.`id` = master.`current_active_controlled_file_id`
         AND file.`tenant_id` = master.`tenant_id`
        JOIN `controlled_content_version_ref` ref
          ON ref.`tenant_id` = master.`tenant_id`
         AND ref.`content_type` = 'DCC_CONTROLLED_FILE'
         AND ref.`content_key` = CAST(master.`id` AS CHAR) COLLATE utf8mb4_unicode_ci
         AND ref.`active_unique_flag` = 1
         AND ref.`deleted` = b'0'
       WHERE master.`deleted` = 0
         AND master.`status` <> 'OBSOLETE_CHAIN'
         AND file.`deleted` = 0
         AND file.`status` = 'ACTIVE'
         AND ref.`native_version_id` <> file.`id`
    ) THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'controlled content DCC active refs drift from native current active revisions; repair before adoption';
    END IF;

    INSERT INTO `controlled_content_version_ref` (
      `tenant_id`,
      `content_type`,
      `content_key`,
      `native_master_id`,
      `native_version_id`,
      `version_no`,
      `canonical_status`,
      `domain_status`,
      `active_unique_flag`,
      `open_candidate_unique_flag`,
      `approval_process_instance_id`,
      `last_transition_time`,
      `creator`,
      `create_time`,
      `updater`,
      `update_time`,
      `deleted`
    )
    SELECT master.`tenant_id`,
           'DCC_CONTROLLED_FILE',
           CAST(master.`id` AS CHAR) COLLATE utf8mb4_unicode_ci,
           master.`id`,
           file.`id`,
           file.`version_no`,
           'ACTIVE',
           file.`status`,
           1,
           NULL,
           file.`process_instance_id`,
           COALESCE(file.`published_time`, file.`approved_time`, file.`update_time`, file.`create_time`, NOW()),
           file.`creator`,
           COALESCE(file.`create_time`, NOW()),
           file.`updater`,
           COALESCE(file.`update_time`, file.`create_time`, NOW()),
           b'0'
      FROM `dcc_controlled_file_master` master
      JOIN `dcc_controlled_file` file
        ON file.`id` = master.`current_active_controlled_file_id`
       AND file.`tenant_id` = master.`tenant_id`
     WHERE master.`deleted` = 0
       AND master.`status` <> 'OBSOLETE_CHAIN'
       AND file.`deleted` = 0
       AND file.`status` = 'ACTIVE'
       AND NOT EXISTS (
         SELECT 1
           FROM `controlled_content_version_ref` existing_ref
          WHERE existing_ref.`tenant_id` = master.`tenant_id`
            AND existing_ref.`content_type` = 'DCC_CONTROLLED_FILE'
            AND existing_ref.`content_key` = CAST(master.`id` AS CHAR) COLLATE utf8mb4_unicode_ci
            AND existing_ref.`native_version_id` = file.`id`
            AND existing_ref.`deleted` = b'0'
       );
  END IF;
END$$
DELIMITER ;

CALL ensure_controlled_content_lifecycle_adoption();

DROP PROCEDURE IF EXISTS ensure_controlled_content_lifecycle_preflight;
DROP PROCEDURE IF EXISTS ensure_controlled_content_lifecycle_schema;
DROP PROCEDURE IF EXISTS ensure_controlled_content_lifecycle_adoption;
