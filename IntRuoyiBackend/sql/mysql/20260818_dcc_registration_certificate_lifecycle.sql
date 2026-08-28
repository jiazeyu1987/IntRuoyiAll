-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260817_dcc_registration_certificate_core; type=schema; riskLevel=high
-- Purpose: Add the shared lifecycle schema for domestic registration-certificate renewal, activation, supporting-document and change tasks.

DROP PROCEDURE IF EXISTS assert_dcc_registration_certificate_lifecycle_contract;
DROP TRIGGER IF EXISTS `trg_dcc_reg_cert_lifecycle_event_immutable_bu`;
DROP TRIGGER IF EXISTS `trg_dcc_reg_cert_lifecycle_event_immutable_bd`;
DROP TRIGGER IF EXISTS `trg_dcc_reg_cert_activation_replay_immutable_bu`;
DROP TRIGGER IF EXISTS `trg_dcc_reg_cert_activation_replay_immutable_bd`;
DROP TRIGGER IF EXISTS `trg_dcc_reg_cert_change_item_immutable_bu`;
DROP TRIGGER IF EXISTS `trg_dcc_reg_cert_change_item_immutable_bd`;

DELIMITER $$
CREATE PROCEDURE assert_dcc_registration_certificate_lifecycle_contract()
BEGIN
  DECLARE core_table_count int DEFAULT 0;
  DECLARE lifecycle_table_count int DEFAULT 0;

  SELECT COUNT(*)
    INTO core_table_count
    FROM information_schema.TABLES
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME IN (
       'dcc_registration_certificate',
       'dcc_registration_certificate_version',
       'dcc_registration_certificate_snapshot',
       'dcc_registration_certificate_snapshot_entrusted',
       'dcc_registration_certificate_file',
       'dcc_registration_certificate_audit'
     );

  IF core_table_count <> 6 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC registration certificate lifecycle requires core schema';
  END IF;

  SELECT COUNT(*)
    INTO lifecycle_table_count
    FROM information_schema.TABLES
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME IN (
       'dcc_registration_certificate_lifecycle_event',
       'dcc_registration_certificate_activation_replay',
       'dcc_registration_certificate_supporting_document',
       'dcc_registration_certificate_change',
       'dcc_registration_certificate_change_item'
     );

  IF lifecycle_table_count <> 5 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC registration certificate lifecycle partial schema detected';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_reg_cert_lifecycle_expected_column;
  CREATE TEMPORARY TABLE tmp_dcc_reg_cert_lifecycle_expected_column (
    table_name varchar(128) NOT NULL,
    column_name varchar(128) NOT NULL,
    column_type varchar(64) NOT NULL,
    is_nullable char(3) NOT NULL,
    generated_column boolean NOT NULL,
    PRIMARY KEY (table_name, column_name)
  ) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
  INSERT INTO tmp_dcc_reg_cert_lifecycle_expected_column
    (table_name, column_name, column_type, is_nullable, generated_column)
  VALUES
    ('dcc_registration_certificate_lifecycle_event', 'id', 'bigint', 'NO', FALSE),
    ('dcc_registration_certificate_lifecycle_event', 'tenant_id', 'bigint', 'NO', FALSE),
    ('dcc_registration_certificate_lifecycle_event', 'owner_company_id', 'bigint', 'NO', FALSE),
    ('dcc_registration_certificate_lifecycle_event', 'certificate_id', 'bigint', 'NO', FALSE),
    ('dcc_registration_certificate_lifecycle_event', 'source_version_id', 'bigint', 'YES', FALSE),
    ('dcc_registration_certificate_lifecycle_event', 'target_version_id', 'bigint', 'YES', FALSE),
    ('dcc_registration_certificate_lifecycle_event', 'source_snapshot_id', 'bigint', 'YES', FALSE),
    ('dcc_registration_certificate_lifecycle_event', 'target_snapshot_id', 'bigint', 'YES', FALSE),
    ('dcc_registration_certificate_lifecycle_event', 'event_key', 'varchar(256)', 'NO', FALSE),
    ('dcc_registration_certificate_lifecycle_event', 'event_type', 'varchar(64)', 'NO', FALSE),
    ('dcc_registration_certificate_lifecycle_event', 'event_sequence', 'int', 'NO', FALSE),
    ('dcc_registration_certificate_lifecycle_event', 'baseline_row_version', 'int', 'YES', FALSE),
    ('dcc_registration_certificate_lifecycle_event', 'baseline_snapshot_revision', 'int', 'YES', FALSE),
    ('dcc_registration_certificate_lifecycle_event', 'actor_id', 'bigint', 'YES', FALSE),
    ('dcc_registration_certificate_lifecycle_event', 'detail_json', 'json', 'NO', FALSE),
    ('dcc_registration_certificate_lifecycle_event', 'occurred_at', 'datetime', 'NO', FALSE),
    ('dcc_registration_certificate_lifecycle_event', 'creator', 'varchar(64)', 'YES', FALSE),
    ('dcc_registration_certificate_lifecycle_event', 'create_time', 'datetime', 'NO', FALSE),

    ('dcc_registration_certificate_activation_replay', 'id', 'bigint', 'NO', FALSE),
    ('dcc_registration_certificate_activation_replay', 'tenant_id', 'bigint', 'NO', FALSE),
    ('dcc_registration_certificate_activation_replay', 'activation_event_id', 'bigint', 'NO', FALSE),
    ('dcc_registration_certificate_activation_replay', 'source_event_id', 'bigint', 'NO', FALSE),
    ('dcc_registration_certificate_activation_replay', 'certificate_id', 'bigint', 'NO', FALSE),
    ('dcc_registration_certificate_activation_replay', 'source_sequence', 'int', 'NO', FALSE),
    ('dcc_registration_certificate_activation_replay', 'applied_sequence', 'int', 'NO', FALSE),
    ('dcc_registration_certificate_activation_replay', 'replay_result', 'varchar(32)', 'NO', FALSE),
    ('dcc_registration_certificate_activation_replay', 'detail_json', 'json', 'NO', FALSE),
    ('dcc_registration_certificate_activation_replay', 'creator', 'varchar(64)', 'YES', FALSE),
    ('dcc_registration_certificate_activation_replay', 'create_time', 'datetime', 'NO', FALSE),

    ('dcc_registration_certificate_supporting_document', 'id', 'bigint', 'NO', FALSE),
    ('dcc_registration_certificate_supporting_document', 'tenant_id', 'bigint', 'NO', FALSE),
    ('dcc_registration_certificate_supporting_document', 'owner_company_id', 'bigint', 'NO', FALSE),
    ('dcc_registration_certificate_supporting_document', 'certificate_id', 'bigint', 'NO', FALSE),
    ('dcc_registration_certificate_supporting_document', 'version_id', 'bigint', 'NO', FALSE),
    ('dcc_registration_certificate_supporting_document', 'business_file_id', 'bigint', 'YES', FALSE),
    ('dcc_registration_certificate_supporting_document', 'document_type', 'varchar(64)', 'NO', FALSE),
    ('dcc_registration_certificate_supporting_document', 'status', 'varchar(32)', 'NO', FALSE),
    ('dcc_registration_certificate_supporting_document', 'open_unique_flag', 'bigint', 'YES', TRUE),
    ('dcc_registration_certificate_supporting_document', 'row_version', 'int', 'NO', FALSE),
    ('dcc_registration_certificate_supporting_document', 'uploaded_at', 'datetime', 'NO', FALSE),
    ('dcc_registration_certificate_supporting_document', 'uploaded_by', 'bigint', 'NO', FALSE),
    ('dcc_registration_certificate_supporting_document', 'confirmed_at', 'datetime', 'YES', FALSE),
    ('dcc_registration_certificate_supporting_document', 'confirmed_by', 'bigint', 'YES', FALSE),
    ('dcc_registration_certificate_supporting_document', 'rejected_at', 'datetime', 'YES', FALSE),
    ('dcc_registration_certificate_supporting_document', 'rejected_by', 'bigint', 'YES', FALSE),
    ('dcc_registration_certificate_supporting_document', 'reject_reason', 'varchar(1024)', 'YES', FALSE),
    ('dcc_registration_certificate_supporting_document', 'voided_at', 'datetime', 'YES', FALSE),
    ('dcc_registration_certificate_supporting_document', 'voided_by', 'bigint', 'YES', FALSE),
    ('dcc_registration_certificate_supporting_document', 'void_reason', 'varchar(1024)', 'YES', FALSE),
    ('dcc_registration_certificate_supporting_document', 'creator', 'varchar(64)', 'YES', FALSE),
    ('dcc_registration_certificate_supporting_document', 'create_time', 'datetime', 'NO', FALSE),
    ('dcc_registration_certificate_supporting_document', 'updater', 'varchar(64)', 'YES', FALSE),
    ('dcc_registration_certificate_supporting_document', 'update_time', 'datetime', 'NO', FALSE),
    ('dcc_registration_certificate_supporting_document', 'deleted', 'bit(1)', 'NO', FALSE),

    ('dcc_registration_certificate_change', 'id', 'bigint', 'NO', FALSE),
    ('dcc_registration_certificate_change', 'tenant_id', 'bigint', 'NO', FALSE),
    ('dcc_registration_certificate_change', 'owner_company_id', 'bigint', 'NO', FALSE),
    ('dcc_registration_certificate_change', 'certificate_id', 'bigint', 'NO', FALSE),
    ('dcc_registration_certificate_change', 'source_version_id', 'bigint', 'NO', FALSE),
    ('dcc_registration_certificate_change', 'source_snapshot_id', 'bigint', 'NO', FALSE),
    ('dcc_registration_certificate_change', 'resulting_snapshot_id', 'bigint', 'YES', FALSE),
    ('dcc_registration_certificate_change', 'event_id', 'bigint', 'NO', FALSE),
    ('dcc_registration_certificate_change', 'approval_date', 'date', 'NO', FALSE),
    ('dcc_registration_certificate_change', 'selected_change_types_json', 'json', 'NO', FALSE),
    ('dcc_registration_certificate_change', 'selected_item_count', 'int', 'YES', TRUE),
    ('dcc_registration_certificate_change', 'status', 'varchar(32)', 'NO', FALSE),
    ('dcc_registration_certificate_change', 'row_version', 'int', 'NO', FALSE),
    ('dcc_registration_certificate_change', 'actor_id', 'bigint', 'NO', FALSE),
    ('dcc_registration_certificate_change', 'applied_at', 'datetime', 'NO', FALSE),
    ('dcc_registration_certificate_change', 'voided_at', 'datetime', 'YES', FALSE),
    ('dcc_registration_certificate_change', 'voided_by', 'bigint', 'YES', FALSE),
    ('dcc_registration_certificate_change', 'void_reason', 'varchar(1024)', 'YES', FALSE),
    ('dcc_registration_certificate_change', 'creator', 'varchar(64)', 'YES', FALSE),
    ('dcc_registration_certificate_change', 'create_time', 'datetime', 'NO', FALSE),
    ('dcc_registration_certificate_change', 'updater', 'varchar(64)', 'YES', FALSE),
    ('dcc_registration_certificate_change', 'update_time', 'datetime', 'NO', FALSE),
    ('dcc_registration_certificate_change', 'deleted', 'bit(1)', 'NO', FALSE),

    ('dcc_registration_certificate_change_item', 'id', 'bigint', 'NO', FALSE),
    ('dcc_registration_certificate_change_item', 'tenant_id', 'bigint', 'NO', FALSE),
    ('dcc_registration_certificate_change_item', 'change_id', 'bigint', 'NO', FALSE),
    ('dcc_registration_certificate_change_item', 'item_type', 'varchar(64)', 'NO', FALSE),
    ('dcc_registration_certificate_change_item', 'before_value_json', 'json', 'NO', FALSE),
    ('dcc_registration_certificate_change_item', 'after_value_json', 'json', 'NO', FALSE),
    ('dcc_registration_certificate_change_item', 'sort_order', 'int', 'NO', FALSE),
    ('dcc_registration_certificate_change_item', 'creator', 'varchar(64)', 'YES', FALSE),
    ('dcc_registration_certificate_change_item', 'create_time', 'datetime', 'NO', FALSE);

  IF EXISTS (
    SELECT 1
      FROM tmp_dcc_reg_cert_lifecycle_expected_column AS expected_column
      LEFT JOIN information_schema.COLUMNS AS actual_column
        ON actual_column.TABLE_SCHEMA = DATABASE()
       AND actual_column.TABLE_NAME = expected_column.table_name
       AND actual_column.COLUMN_NAME = expected_column.column_name
     WHERE actual_column.COLUMN_NAME IS NULL
        OR LOWER(actual_column.COLUMN_TYPE) <> expected_column.column_type
        OR actual_column.IS_NULLABLE <> expected_column.is_nullable
        OR (expected_column.generated_column
            AND actual_column.EXTRA NOT LIKE '%STORED GENERATED%')
        OR (NOT expected_column.generated_column
            AND (actual_column.EXTRA LIKE '%STORED GENERATED%'
              OR actual_column.EXTRA LIKE '%VIRTUAL GENERATED%'))
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC registration certificate lifecycle column contract mismatch';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM (
        SELECT table_name, COUNT(*) AS column_count
          FROM tmp_dcc_reg_cert_lifecycle_expected_column
         GROUP BY table_name
      ) AS expected_table
      LEFT JOIN (
        SELECT TABLE_NAME AS table_name, COUNT(*) AS column_count
          FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME IN (
             'dcc_registration_certificate_lifecycle_event',
             'dcc_registration_certificate_activation_replay',
             'dcc_registration_certificate_supporting_document',
             'dcc_registration_certificate_change',
             'dcc_registration_certificate_change_item'
           )
         GROUP BY TABLE_NAME
      ) AS actual_table ON actual_table.table_name = expected_table.table_name
     WHERE actual_table.column_count <> expected_table.column_count
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC registration certificate lifecycle column contract mismatch';
  END IF;
  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_reg_cert_lifecycle_expected_column;

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_reg_cert_lifecycle_expected_index;
  CREATE TEMPORARY TABLE tmp_dcc_reg_cert_lifecycle_expected_index (
    table_name varchar(128) NOT NULL,
    index_name varchar(128) NOT NULL,
    column_list varchar(512) NOT NULL,
    non_unique int NOT NULL,
    PRIMARY KEY (table_name, index_name)
  ) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
  INSERT INTO tmp_dcc_reg_cert_lifecycle_expected_index
    (table_name, index_name, column_list, non_unique)
  VALUES
    ('dcc_registration_certificate_lifecycle_event', 'PRIMARY', 'id', 0),
    ('dcc_registration_certificate_lifecycle_event', 'uk_dcc_reg_cert_lifecycle_event_key', 'tenant_id,event_key', 0),
    ('dcc_registration_certificate_lifecycle_event', 'uk_dcc_reg_cert_lifecycle_sequence', 'tenant_id,certificate_id,event_sequence', 0),
    ('dcc_registration_certificate_activation_replay', 'PRIMARY', 'id', 0),
    ('dcc_registration_certificate_activation_replay', 'uk_dcc_reg_cert_activation_source', 'tenant_id,activation_event_id,source_event_id', 0),
    ('dcc_registration_certificate_supporting_document', 'PRIMARY', 'id', 0),
    ('dcc_registration_certificate_supporting_document', 'uk_dcc_reg_cert_support_open', 'tenant_id,certificate_id,document_type,open_unique_flag', 0),
    ('dcc_registration_certificate_change', 'PRIMARY', 'id', 0),
    ('dcc_registration_certificate_change', 'uk_dcc_reg_cert_change_event', 'tenant_id,event_id', 0),
    ('dcc_registration_certificate_change_item', 'PRIMARY', 'id', 0),
    ('dcc_registration_certificate_change_item', 'uk_dcc_reg_cert_change_item_type', 'tenant_id,change_id,item_type', 0);

  IF EXISTS (
    SELECT 1
      FROM tmp_dcc_reg_cert_lifecycle_expected_index AS expected_index
      LEFT JOIN (
        SELECT TABLE_NAME AS table_name,
               INDEX_NAME AS index_name,
               GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') AS column_list,
               MAX(NON_UNIQUE) AS non_unique
          FROM information_schema.STATISTICS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME IN (
             'dcc_registration_certificate_lifecycle_event',
             'dcc_registration_certificate_activation_replay',
             'dcc_registration_certificate_supporting_document',
             'dcc_registration_certificate_change',
             'dcc_registration_certificate_change_item'
           )
         GROUP BY TABLE_NAME, INDEX_NAME
      ) AS actual_index
        ON actual_index.table_name = expected_index.table_name
       AND actual_index.index_name = expected_index.index_name
     WHERE actual_index.index_name IS NULL
        OR actual_index.column_list <> expected_index.column_list
        OR actual_index.non_unique <> expected_index.non_unique
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC registration certificate lifecycle index contract mismatch';
  END IF;
  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_reg_cert_lifecycle_expected_index;
END$$
DELIMITER ;

CREATE TABLE IF NOT EXISTS `dcc_registration_certificate_lifecycle_event` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Lifecycle event id',
  `tenant_id` bigint NOT NULL COMMENT 'Tenant id',
  `owner_company_id` bigint NOT NULL COMMENT 'Owning company enterprise id',
  `certificate_id` bigint NOT NULL COMMENT 'Registration certificate aggregate id',
  `source_version_id` bigint DEFAULT NULL COMMENT 'Source version id',
  `target_version_id` bigint DEFAULT NULL COMMENT 'Target version id',
  `source_snapshot_id` bigint DEFAULT NULL COMMENT 'Source snapshot id',
  `target_snapshot_id` bigint DEFAULT NULL COMMENT 'Target snapshot id',
  `event_key` varchar(256) NOT NULL COMMENT 'Tenant-scoped deterministic event key',
  `event_type` varchar(64) NOT NULL COMMENT 'Lifecycle event type',
  `event_sequence` int NOT NULL COMMENT 'Certificate-local ordered event sequence',
  `baseline_row_version` int DEFAULT NULL COMMENT 'Expected aggregate row version before event',
  `baseline_snapshot_revision` int DEFAULT NULL COMMENT 'Expected source snapshot revision before event',
  `actor_id` bigint DEFAULT NULL COMMENT 'Actor user id',
  `detail_json` json NOT NULL COMMENT 'Structured lifecycle evidence without secrets',
  `occurred_at` datetime NOT NULL COMMENT 'Business occurrence time',
  `creator` varchar(64) DEFAULT '' COMMENT 'Creator',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_reg_cert_lifecycle_event_key` (`tenant_id`, `event_key`),
  UNIQUE KEY `uk_dcc_reg_cert_lifecycle_sequence` (`tenant_id`, `certificate_id`, `event_sequence`),
  CONSTRAINT `chk_dcc_reg_cert_lifecycle_event_key` CHECK (TRIM(`event_key`) <> ''),
  CONSTRAINT `chk_dcc_reg_cert_lifecycle_event_type` CHECK (`event_type` IN
    ('RENEWAL_UPLOADED', 'ACTIVATION_APPLIED', 'SUPPORTING_DOCUMENT_UPLOADED',
     'SUPPORTING_DOCUMENT_CONFIRMED', 'SUPPORTING_DOCUMENT_REJECTED',
     'CHANGE_APPLIED', 'CANDIDATE_VOIDED', 'CERTIFICATE_VOIDED')),
  CONSTRAINT `chk_dcc_reg_cert_lifecycle_sequence` CHECK (`event_sequence` > 0),
  CONSTRAINT `chk_dcc_reg_cert_lifecycle_detail` CHECK (JSON_VALID(`detail_json`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC registration certificate lifecycle event';

CREATE TABLE IF NOT EXISTS `dcc_registration_certificate_activation_replay` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Activation replay id',
  `tenant_id` bigint NOT NULL COMMENT 'Tenant id',
  `activation_event_id` bigint NOT NULL COMMENT 'Activation lifecycle event id',
  `source_event_id` bigint NOT NULL COMMENT 'Waiting-period source lifecycle event id',
  `certificate_id` bigint NOT NULL COMMENT 'Registration certificate aggregate id',
  `source_sequence` int NOT NULL COMMENT 'Source event sequence',
  `applied_sequence` int NOT NULL COMMENT 'Replay application sequence',
  `replay_result` varchar(32) NOT NULL COMMENT 'Replay result',
  `detail_json` json NOT NULL COMMENT 'Replay evidence without secrets',
  `creator` varchar(64) DEFAULT '' COMMENT 'Creator',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_reg_cert_activation_source` (`tenant_id`, `activation_event_id`, `source_event_id`),
  CONSTRAINT `chk_dcc_reg_cert_activation_replay_order` CHECK (`source_sequence` > 0 AND `applied_sequence` > 0),
  CONSTRAINT `chk_dcc_reg_cert_activation_replay_result` CHECK (`replay_result` IN ('APPLIED', 'SKIPPED')),
  CONSTRAINT `chk_dcc_reg_cert_activation_replay_detail` CHECK (JSON_VALID(`detail_json`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC registration certificate activation replay evidence';

CREATE TABLE IF NOT EXISTS `dcc_registration_certificate_supporting_document` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Supporting document id',
  `tenant_id` bigint NOT NULL COMMENT 'Tenant id',
  `owner_company_id` bigint NOT NULL COMMENT 'Owning company enterprise id',
  `certificate_id` bigint NOT NULL COMMENT 'Registration certificate aggregate id',
  `version_id` bigint NOT NULL COMMENT 'Related certificate version id',
  `business_file_id` bigint DEFAULT NULL COMMENT 'Registration certificate business file id',
  `document_type` varchar(64) NOT NULL COMMENT 'Supporting document type',
  `status` varchar(32) NOT NULL COMMENT 'Supporting document status',
  `open_unique_flag` bigint GENERATED ALWAYS AS (
    CASE WHEN `deleted` = b'0' AND `status` = 'PENDING_CONFIRMATION' THEN `certificate_id` ELSE NULL END
  ) STORED COMMENT 'Unique flag for open confirmation per certificate and type',
  `row_version` int NOT NULL DEFAULT 1 COMMENT 'Optimistic row version',
  `uploaded_at` datetime NOT NULL COMMENT 'Upload time',
  `uploaded_by` bigint NOT NULL COMMENT 'Uploader user id',
  `confirmed_at` datetime DEFAULT NULL COMMENT 'Confirmation time',
  `confirmed_by` bigint DEFAULT NULL COMMENT 'Confirmation user id',
  `rejected_at` datetime DEFAULT NULL COMMENT 'Rejection time',
  `rejected_by` bigint DEFAULT NULL COMMENT 'Rejection user id',
  `reject_reason` varchar(1024) DEFAULT NULL COMMENT 'Required rejection reason',
  `voided_at` datetime DEFAULT NULL COMMENT 'Void time',
  `voided_by` bigint DEFAULT NULL COMMENT 'Void user id',
  `void_reason` varchar(1024) DEFAULT NULL COMMENT 'Void reason',
  `creator` varchar(64) DEFAULT '' COMMENT 'Creator',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `updater` varchar(64) DEFAULT '' COMMENT 'Updater',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'Deleted flag',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_reg_cert_support_open` (`tenant_id`, `certificate_id`, `document_type`, `open_unique_flag`),
  CONSTRAINT `chk_dcc_reg_cert_support_type` CHECK (`document_type` IN
    ('RENEWAL_ACCEPTANCE_RECEIPT', 'RENEWAL_SUPPLEMENT_NOTICE')),
  CONSTRAINT `chk_dcc_reg_cert_support_status` CHECK (`status` IN
    ('PENDING_CONFIRMATION', 'CONFIRMED', 'REJECTED', 'VOIDED')),
  CONSTRAINT `chk_dcc_reg_cert_support_reject_reason` CHECK (
    `status` <> 'REJECTED' OR (`reject_reason` IS NOT NULL AND TRIM(`reject_reason`) <> '')
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC registration certificate supporting document confirmation';

CREATE TABLE IF NOT EXISTS `dcc_registration_certificate_change` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Change id',
  `tenant_id` bigint NOT NULL COMMENT 'Tenant id',
  `owner_company_id` bigint NOT NULL COMMENT 'Owning company enterprise id',
  `certificate_id` bigint NOT NULL COMMENT 'Registration certificate aggregate id',
  `source_version_id` bigint NOT NULL COMMENT 'Source current version id',
  `source_snapshot_id` bigint NOT NULL COMMENT 'Source current snapshot id',
  `resulting_snapshot_id` bigint DEFAULT NULL COMMENT 'Resulting snapshot id',
  `event_id` bigint NOT NULL COMMENT 'Lifecycle event id',
  `approval_date` date NOT NULL COMMENT 'Change approval date',
  `selected_change_types_json` json NOT NULL COMMENT 'Selected structured change types',
  `selected_item_count` int GENERATED ALWAYS AS (JSON_LENGTH(`selected_change_types_json`)) STORED COMMENT 'Selected change type count',
  `status` varchar(32) NOT NULL COMMENT 'Change status',
  `row_version` int NOT NULL DEFAULT 1 COMMENT 'Optimistic row version',
  `actor_id` bigint NOT NULL COMMENT 'Actor user id',
  `applied_at` datetime NOT NULL COMMENT 'Applied time',
  `voided_at` datetime DEFAULT NULL COMMENT 'Void time',
  `voided_by` bigint DEFAULT NULL COMMENT 'Void user id',
  `void_reason` varchar(1024) DEFAULT NULL COMMENT 'Void reason',
  `creator` varchar(64) DEFAULT '' COMMENT 'Creator',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `updater` varchar(64) DEFAULT '' COMMENT 'Updater',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'Deleted flag',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_reg_cert_change_event` (`tenant_id`, `event_id`),
  CONSTRAINT `chk_dcc_reg_cert_change_status` CHECK (`status` IN ('APPLIED', 'VOIDED')),
  CONSTRAINT `chk_dcc_reg_cert_change_selected_count` CHECK (`selected_item_count` > 0),
  CONSTRAINT `chk_dcc_reg_cert_change_type_json` CHECK (JSON_VALID(`selected_change_types_json`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC registration certificate approved change';

CREATE TABLE IF NOT EXISTS `dcc_registration_certificate_change_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Change item id',
  `tenant_id` bigint NOT NULL COMMENT 'Tenant id',
  `change_id` bigint NOT NULL COMMENT 'Change id',
  `item_type` varchar(64) NOT NULL COMMENT 'Changed item type',
  `before_value_json` json NOT NULL COMMENT 'Structured before value',
  `after_value_json` json NOT NULL COMMENT 'Structured after value',
  `sort_order` int NOT NULL COMMENT 'Display order',
  `creator` varchar(64) DEFAULT '' COMMENT 'Creator',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_reg_cert_change_item_type` (`tenant_id`, `change_id`, `item_type`),
  CONSTRAINT `chk_dcc_reg_cert_change_item_type` CHECK (`item_type` IN
    ('PRODUCT_NAME', 'REGISTRANT_NAME', 'MODEL_SPECIFICATION', 'STRUCTURE_COMPOSITION',
     'INTENDED_USE', 'TECHNICAL_REQUIREMENTS', 'RESIDENCE_ADDRESS', 'PRODUCTION_ADDRESS',
     'OTHER_CONTENT')),
  CONSTRAINT `chk_dcc_reg_cert_change_item_value` CHECK (
    JSON_VALID(`before_value_json`) AND JSON_VALID(`after_value_json`) AND `sort_order` > 0
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC registration certificate change item history';

DELIMITER $$
CREATE TRIGGER `trg_dcc_reg_cert_lifecycle_event_immutable_bu`
BEFORE UPDATE ON `dcc_registration_certificate_lifecycle_event`
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000'
    SET MESSAGE_TEXT = 'Registration certificate lifecycle event is append-only';
END$$

CREATE TRIGGER `trg_dcc_reg_cert_lifecycle_event_immutable_bd`
BEFORE DELETE ON `dcc_registration_certificate_lifecycle_event`
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000'
    SET MESSAGE_TEXT = 'Registration certificate lifecycle event cannot be deleted';
END$$

CREATE TRIGGER `trg_dcc_reg_cert_activation_replay_immutable_bu`
BEFORE UPDATE ON `dcc_registration_certificate_activation_replay`
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000'
    SET MESSAGE_TEXT = 'Registration certificate activation replay is append-only';
END$$

CREATE TRIGGER `trg_dcc_reg_cert_activation_replay_immutable_bd`
BEFORE DELETE ON `dcc_registration_certificate_activation_replay`
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000'
    SET MESSAGE_TEXT = 'Registration certificate activation replay cannot be deleted';
END$$

CREATE TRIGGER `trg_dcc_reg_cert_change_item_immutable_bu`
BEFORE UPDATE ON `dcc_registration_certificate_change_item`
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000'
    SET MESSAGE_TEXT = 'Registration certificate change item history is append-only';
END$$

CREATE TRIGGER `trg_dcc_reg_cert_change_item_immutable_bd`
BEFORE DELETE ON `dcc_registration_certificate_change_item`
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000'
    SET MESSAGE_TEXT = 'Registration certificate change item history cannot be deleted';
END$$
DELIMITER ;

CALL assert_dcc_registration_certificate_lifecycle_contract();
DROP PROCEDURE IF EXISTS assert_dcc_registration_certificate_lifecycle_contract;
