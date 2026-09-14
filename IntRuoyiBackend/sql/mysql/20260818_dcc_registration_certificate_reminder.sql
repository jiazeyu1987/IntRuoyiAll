-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260818_dcc_registration_certificate_lifecycle; type=schema; riskLevel=high
-- Purpose: Add shared reminder configuration, daily run, occurrence, delivery and scheduler seed contracts for domestic registration certificates.

DROP PROCEDURE IF EXISTS assert_dcc_registration_certificate_reminder_contract;

DELIMITER $$
CREATE PROCEDURE assert_dcc_registration_certificate_reminder_contract()
BEGIN
  DECLARE lifecycle_table_count int DEFAULT 0;
  DECLARE infra_job_table_count int DEFAULT 0;
  DECLARE reminder_table_count int DEFAULT 0;

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
      SET MESSAGE_TEXT = 'DCC registration certificate reminder requires lifecycle schema';
  END IF;

  SELECT COUNT(*)
    INTO infra_job_table_count
    FROM information_schema.TABLES
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'infra_job';

  IF infra_job_table_count <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC registration certificate reminder requires Infra job schema';
  END IF;

  SELECT COUNT(*)
    INTO reminder_table_count
    FROM information_schema.TABLES
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME IN (
       'dcc_registration_certificate_reminder_config',
       'dcc_registration_certificate_daily_run',
       'dcc_registration_certificate_reminder_occurrence',
       'dcc_registration_certificate_reminder_delivery'
     );

  IF reminder_table_count <> 4 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC registration certificate reminder partial schema detected';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_reg_cert_reminder_expected_column;
  CREATE TEMPORARY TABLE tmp_dcc_reg_cert_reminder_expected_column (
    table_name varchar(128) NOT NULL,
    column_name varchar(128) NOT NULL,
    column_type varchar(64) NOT NULL,
    is_nullable char(3) NOT NULL,
    generated_column boolean NOT NULL,
    PRIMARY KEY (table_name, column_name)
  ) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
  INSERT INTO tmp_dcc_reg_cert_reminder_expected_column
    (table_name, column_name, column_type, is_nullable, generated_column)
  VALUES
    ('dcc_registration_certificate_reminder_config', 'id', 'bigint', 'NO', FALSE),
    ('dcc_registration_certificate_reminder_config', 'tenant_id', 'bigint', 'NO', FALSE),
    ('dcc_registration_certificate_reminder_config', 'active_unique_flag', 'bigint', 'YES', TRUE),
    ('dcc_registration_certificate_reminder_config', 'enabled', 'bit(1)', 'NO', FALSE),
    ('dcc_registration_certificate_reminder_config', 'daily_run_time', 'varchar(5)', 'NO', FALSE),
    ('dcc_registration_certificate_reminder_config', 'timezone', 'varchar(64)', 'NO', FALSE),
    ('dcc_registration_certificate_reminder_config', 'threshold_days_json', 'json', 'NO', FALSE),
    ('dcc_registration_certificate_reminder_config', 'row_version', 'int', 'NO', FALSE),
    ('dcc_registration_certificate_reminder_config', 'creator', 'varchar(64)', 'YES', FALSE),
    ('dcc_registration_certificate_reminder_config', 'create_time', 'datetime', 'NO', FALSE),
    ('dcc_registration_certificate_reminder_config', 'updater', 'varchar(64)', 'YES', FALSE),
    ('dcc_registration_certificate_reminder_config', 'update_time', 'datetime', 'NO', FALSE),
    ('dcc_registration_certificate_reminder_config', 'deleted', 'bit(1)', 'NO', FALSE),

    ('dcc_registration_certificate_daily_run', 'id', 'bigint', 'NO', FALSE),
    ('dcc_registration_certificate_daily_run', 'tenant_id', 'bigint', 'NO', FALSE),
    ('dcc_registration_certificate_daily_run', 'business_date', 'date', 'NO', FALSE),
    ('dcc_registration_certificate_daily_run', 'run_key', 'varchar(128)', 'NO', FALSE),
    ('dcc_registration_certificate_daily_run', 'status', 'varchar(32)', 'NO', FALSE),
    ('dcc_registration_certificate_daily_run', 'retry_count', 'int', 'NO', FALSE),
    ('dcc_registration_certificate_daily_run', 'failure_reason', 'varchar(1024)', 'YES', FALSE),
    ('dcc_registration_certificate_daily_run', 'started_at', 'datetime', 'NO', FALSE),
    ('dcc_registration_certificate_daily_run', 'finished_at', 'datetime', 'YES', FALSE),
    ('dcc_registration_certificate_daily_run', 'detail_json', 'json', 'NO', FALSE),
    ('dcc_registration_certificate_daily_run', 'creator', 'varchar(64)', 'YES', FALSE),
    ('dcc_registration_certificate_daily_run', 'create_time', 'datetime', 'NO', FALSE),
    ('dcc_registration_certificate_daily_run', 'updater', 'varchar(64)', 'YES', FALSE),
    ('dcc_registration_certificate_daily_run', 'update_time', 'datetime', 'NO', FALSE),

    ('dcc_registration_certificate_reminder_occurrence', 'id', 'bigint', 'NO', FALSE),
    ('dcc_registration_certificate_reminder_occurrence', 'tenant_id', 'bigint', 'NO', FALSE),
    ('dcc_registration_certificate_reminder_occurrence', 'run_id', 'bigint', 'NO', FALSE),
    ('dcc_registration_certificate_reminder_occurrence', 'owner_company_id', 'bigint', 'NO', FALSE),
    ('dcc_registration_certificate_reminder_occurrence', 'certificate_id', 'bigint', 'NO', FALSE),
    ('dcc_registration_certificate_reminder_occurrence', 'version_id', 'bigint', 'NO', FALSE),
    ('dcc_registration_certificate_reminder_occurrence', 'supporting_document_id', 'bigint', 'YES', FALSE),
    ('dcc_registration_certificate_reminder_occurrence', 'reminder_type', 'varchar(64)', 'NO', FALSE),
    ('dcc_registration_certificate_reminder_occurrence', 'threshold_level', 'varchar(16)', 'NO', FALSE),
    ('dcc_registration_certificate_reminder_occurrence', 'business_date', 'date', 'NO', FALSE),
    ('dcc_registration_certificate_reminder_occurrence', 'due_date', 'date', 'NO', FALSE),
    ('dcc_registration_certificate_reminder_occurrence', 'event_key', 'varchar(256)', 'NO', FALSE),
    ('dcc_registration_certificate_reminder_occurrence', 'status', 'varchar(32)', 'NO', FALSE),
    ('dcc_registration_certificate_reminder_occurrence', 'suppressed_by_occurrence_id', 'bigint', 'YES', FALSE),
    ('dcc_registration_certificate_reminder_occurrence', 'suppress_reason', 'varchar(512)', 'YES', FALSE),
    ('dcc_registration_certificate_reminder_occurrence', 'detail_json', 'json', 'NO', FALSE),
    ('dcc_registration_certificate_reminder_occurrence', 'creator', 'varchar(64)', 'YES', FALSE),
    ('dcc_registration_certificate_reminder_occurrence', 'create_time', 'datetime', 'NO', FALSE),
    ('dcc_registration_certificate_reminder_occurrence', 'updater', 'varchar(64)', 'YES', FALSE),
    ('dcc_registration_certificate_reminder_occurrence', 'update_time', 'datetime', 'NO', FALSE),

    ('dcc_registration_certificate_reminder_delivery', 'id', 'bigint', 'NO', FALSE),
    ('dcc_registration_certificate_reminder_delivery', 'tenant_id', 'bigint', 'NO', FALSE),
    ('dcc_registration_certificate_reminder_delivery', 'occurrence_id', 'bigint', 'NO', FALSE),
    ('dcc_registration_certificate_reminder_delivery', 'recipient_user_id', 'bigint', 'NO', FALSE),
    ('dcc_registration_certificate_reminder_delivery', 'recipient_company_id', 'bigint', 'YES', FALSE),
    ('dcc_registration_certificate_reminder_delivery', 'delivery_key', 'varchar(256)', 'NO', FALSE),
    ('dcc_registration_certificate_reminder_delivery', 'status', 'varchar(32)', 'NO', FALSE),
    ('dcc_registration_certificate_reminder_delivery', 'notify_message_id', 'bigint', 'YES', FALSE),
    ('dcc_registration_certificate_reminder_delivery', 'attempt_count', 'int', 'NO', FALSE),
    ('dcc_registration_certificate_reminder_delivery', 'last_failure_code', 'varchar(64)', 'YES', FALSE),
    ('dcc_registration_certificate_reminder_delivery', 'last_failure_reason', 'varchar(1024)', 'YES', FALSE),
    ('dcc_registration_certificate_reminder_delivery', 'sent_at', 'datetime', 'YES', FALSE),
    ('dcc_registration_certificate_reminder_delivery', 'next_retry_at', 'datetime', 'YES', FALSE),
    ('dcc_registration_certificate_reminder_delivery', 'detail_json', 'json', 'NO', FALSE),
    ('dcc_registration_certificate_reminder_delivery', 'creator', 'varchar(64)', 'YES', FALSE),
    ('dcc_registration_certificate_reminder_delivery', 'create_time', 'datetime', 'NO', FALSE),
    ('dcc_registration_certificate_reminder_delivery', 'updater', 'varchar(64)', 'YES', FALSE),
    ('dcc_registration_certificate_reminder_delivery', 'update_time', 'datetime', 'NO', FALSE);

  IF EXISTS (
    SELECT 1
      FROM tmp_dcc_reg_cert_reminder_expected_column AS expected_column
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
      SET MESSAGE_TEXT = 'DCC registration certificate reminder column contract mismatch';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM (
        SELECT table_name, COUNT(*) AS column_count
          FROM tmp_dcc_reg_cert_reminder_expected_column
         GROUP BY table_name
      ) AS expected_table
      LEFT JOIN (
        SELECT TABLE_NAME AS table_name, COUNT(*) AS column_count
          FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME IN (
             'dcc_registration_certificate_reminder_config',
             'dcc_registration_certificate_daily_run',
             'dcc_registration_certificate_reminder_occurrence',
             'dcc_registration_certificate_reminder_delivery'
           )
         GROUP BY TABLE_NAME
      ) AS actual_table ON actual_table.table_name = expected_table.table_name
     WHERE actual_table.column_count <> expected_table.column_count
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC registration certificate reminder column contract mismatch';
  END IF;
  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_reg_cert_reminder_expected_column;

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_reg_cert_reminder_expected_index;
  CREATE TEMPORARY TABLE tmp_dcc_reg_cert_reminder_expected_index (
    table_name varchar(128) NOT NULL,
    index_name varchar(128) NOT NULL,
    column_list varchar(512) NOT NULL,
    non_unique int NOT NULL,
    PRIMARY KEY (table_name, index_name)
  ) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
  INSERT INTO tmp_dcc_reg_cert_reminder_expected_index
    (table_name, index_name, column_list, non_unique)
  VALUES
    ('dcc_registration_certificate_reminder_config', 'PRIMARY', 'id', 0),
    ('dcc_registration_certificate_reminder_config', 'uk_dcc_reg_cert_reminder_config_active', 'tenant_id,active_unique_flag', 0),
    ('dcc_registration_certificate_daily_run', 'PRIMARY', 'id', 0),
    ('dcc_registration_certificate_daily_run', 'uk_dcc_reg_cert_daily_run_date', 'tenant_id,business_date', 0),
    ('dcc_registration_certificate_daily_run', 'uk_dcc_reg_cert_daily_run_key', 'tenant_id,run_key', 0),
    ('dcc_registration_certificate_reminder_occurrence', 'PRIMARY', 'id', 0),
    ('dcc_registration_certificate_reminder_occurrence', 'uk_dcc_reg_cert_reminder_occurrence_key', 'tenant_id,event_key', 0),
    ('dcc_registration_certificate_reminder_occurrence', 'uk_dcc_reg_cert_reminder_occurrence_run', 'tenant_id,run_id,certificate_id,reminder_type,threshold_level', 0),
    ('dcc_registration_certificate_reminder_delivery', 'PRIMARY', 'id', 0),
    ('dcc_registration_certificate_reminder_delivery', 'uk_dcc_reg_cert_reminder_delivery_key', 'tenant_id,delivery_key', 0),
    ('dcc_registration_certificate_reminder_delivery', 'uk_dcc_reg_cert_reminder_delivery_recipient', 'tenant_id,occurrence_id,recipient_user_id', 0);

  IF EXISTS (
    SELECT 1
      FROM tmp_dcc_reg_cert_reminder_expected_index AS expected_index
      LEFT JOIN (
        SELECT TABLE_NAME AS table_name,
               INDEX_NAME AS index_name,
               GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') AS column_list,
               MAX(NON_UNIQUE) AS non_unique
          FROM information_schema.STATISTICS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME IN (
             'dcc_registration_certificate_reminder_config',
             'dcc_registration_certificate_daily_run',
             'dcc_registration_certificate_reminder_occurrence',
             'dcc_registration_certificate_reminder_delivery'
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
      SET MESSAGE_TEXT = 'DCC registration certificate reminder index contract mismatch';
  END IF;
  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_reg_cert_reminder_expected_index;
END$$
DELIMITER ;

CREATE TABLE IF NOT EXISTS `dcc_registration_certificate_reminder_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Reminder config id',
  `tenant_id` bigint NOT NULL COMMENT 'Tenant id',
  `active_unique_flag` bigint GENERATED ALWAYS AS (
    CASE WHEN `deleted` = b'0' THEN `tenant_id` ELSE NULL END
  ) STORED COMMENT 'One active reminder configuration per tenant',
  `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT 'Reminder enabled flag',
  `daily_run_time` varchar(5) NOT NULL DEFAULT '09:00' COMMENT 'Daily business run time HH:mm',
  `timezone` varchar(64) NOT NULL DEFAULT 'Asia/Shanghai' COMMENT 'Business timezone',
  `threshold_days_json` json NOT NULL COMMENT 'Threshold days, fixed by later behavior task',
  `row_version` int NOT NULL DEFAULT 1 COMMENT 'Optimistic row version',
  `creator` varchar(64) DEFAULT '' COMMENT 'Creator',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `updater` varchar(64) DEFAULT '' COMMENT 'Updater',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'Deleted flag',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_reg_cert_reminder_config_active` (`tenant_id`, `active_unique_flag`),
  CONSTRAINT `chk_dcc_reg_cert_reminder_config_time` CHECK (
    `daily_run_time` REGEXP '^[0-2][0-9]:[0-5][0-9]$' AND `daily_run_time` <= '23:59'
  ),
  CONSTRAINT `chk_dcc_reg_cert_reminder_config_timezone` CHECK (`timezone` = 'Asia/Shanghai'),
  CONSTRAINT `chk_dcc_reg_cert_reminder_config_thresholds` CHECK (JSON_VALID(`threshold_days_json`)),
  CONSTRAINT `chk_dcc_reg_cert_reminder_config_revision` CHECK (`row_version` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC registration certificate reminder tenant configuration';

CREATE TABLE IF NOT EXISTS `dcc_registration_certificate_daily_run` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Daily run id',
  `tenant_id` bigint NOT NULL COMMENT 'Tenant id',
  `business_date` date NOT NULL COMMENT 'Asia/Shanghai business date',
  `run_key` varchar(128) NOT NULL COMMENT 'Tenant-scoped deterministic run key',
  `status` varchar(32) NOT NULL COMMENT 'Run status',
  `retry_count` int NOT NULL DEFAULT 0 COMMENT 'Visible same-day retry count',
  `failure_reason` varchar(1024) DEFAULT NULL COMMENT 'Required failure reason when failed',
  `started_at` datetime NOT NULL COMMENT 'Run start time',
  `finished_at` datetime DEFAULT NULL COMMENT 'Run finish time',
  `detail_json` json NOT NULL COMMENT 'Run evidence without secrets',
  `creator` varchar(64) DEFAULT '' COMMENT 'Creator',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `updater` varchar(64) DEFAULT '' COMMENT 'Updater',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_reg_cert_daily_run_date` (`tenant_id`, `business_date`),
  UNIQUE KEY `uk_dcc_reg_cert_daily_run_key` (`tenant_id`, `run_key`),
  CONSTRAINT `chk_dcc_reg_cert_daily_run_key` CHECK (TRIM(`run_key`) <> ''),
  CONSTRAINT `chk_dcc_reg_cert_daily_run_status` CHECK (`status` IN ('RUNNING', 'SUCCESS', 'FAILED')),
  CONSTRAINT `chk_dcc_reg_cert_daily_run_failure` CHECK (
    `status` <> 'FAILED' OR (`failure_reason` IS NOT NULL AND TRIM(`failure_reason`) <> '')
  ),
  CONSTRAINT `chk_dcc_reg_cert_daily_run_success` CHECK (`status` <> 'SUCCESS' OR `finished_at` IS NOT NULL),
  CONSTRAINT `chk_dcc_reg_cert_daily_run_retry` CHECK (`retry_count` >= 0),
  CONSTRAINT `chk_dcc_reg_cert_daily_run_detail` CHECK (JSON_VALID(`detail_json`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC registration certificate reminder daily run';

CREATE TABLE IF NOT EXISTS `dcc_registration_certificate_reminder_occurrence` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Reminder occurrence id',
  `tenant_id` bigint NOT NULL COMMENT 'Tenant id',
  `run_id` bigint NOT NULL COMMENT 'Daily run id',
  `owner_company_id` bigint NOT NULL COMMENT 'Owning company enterprise id',
  `certificate_id` bigint NOT NULL COMMENT 'Registration certificate aggregate id',
  `version_id` bigint NOT NULL COMMENT 'Registration certificate version id',
  `supporting_document_id` bigint DEFAULT NULL COMMENT 'Optional supporting document id',
  `reminder_type` varchar(64) NOT NULL COMMENT 'Reminder type',
  `threshold_level` varchar(16) NOT NULL COMMENT 'Threshold level',
  `business_date` date NOT NULL COMMENT 'Run business date',
  `due_date` date NOT NULL COMMENT 'Certificate or support due date',
  `event_key` varchar(256) NOT NULL COMMENT 'Tenant-scoped deterministic occurrence key',
  `status` varchar(32) NOT NULL COMMENT 'Occurrence status',
  `suppressed_by_occurrence_id` bigint DEFAULT NULL COMMENT 'Higher-priority occurrence that suppresses this row',
  `suppress_reason` varchar(512) DEFAULT NULL COMMENT 'Required suppression reason',
  `detail_json` json NOT NULL COMMENT 'Occurrence evidence without secrets',
  `creator` varchar(64) DEFAULT '' COMMENT 'Creator',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `updater` varchar(64) DEFAULT '' COMMENT 'Updater',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_reg_cert_reminder_occurrence_key` (`tenant_id`, `event_key`),
  UNIQUE KEY `uk_dcc_reg_cert_reminder_occurrence_run` (`tenant_id`, `run_id`, `certificate_id`, `reminder_type`, `threshold_level`),
  CONSTRAINT `chk_dcc_reg_cert_reminder_occurrence_key` CHECK (TRIM(`event_key`) <> ''),
  CONSTRAINT `chk_dcc_reg_cert_reminder_occurrence_type` CHECK (`reminder_type` IN
    ('CERTIFICATE_EXPIRY', 'RENEWAL_UPLOAD', 'SUPPORTING_DOCUMENT')),
  CONSTRAINT `chk_dcc_reg_cert_reminder_occurrence_threshold` CHECK (`threshold_level` IN
    ('T_30', 'T_8', 'T_2', 'T_1')),
  CONSTRAINT `chk_dcc_reg_cert_reminder_occurrence_status` CHECK (`status` IN
    ('PENDING_DELIVERY', 'SUPPRESSED', 'DELIVERED', 'CLEARED', 'FAILED')),
  CONSTRAINT `chk_dcc_reg_cert_reminder_occurrence_suppression` CHECK (
    `status` <> 'SUPPRESSED'
    OR (`suppressed_by_occurrence_id` IS NOT NULL
      AND `suppress_reason` IS NOT NULL
      AND TRIM(`suppress_reason`) <> '')
  ),
  CONSTRAINT `chk_dcc_reg_cert_reminder_occurrence_detail` CHECK (JSON_VALID(`detail_json`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC registration certificate reminder occurrence';

CREATE TABLE IF NOT EXISTS `dcc_registration_certificate_reminder_delivery` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Reminder delivery id',
  `tenant_id` bigint NOT NULL COMMENT 'Tenant id',
  `occurrence_id` bigint NOT NULL COMMENT 'Reminder occurrence id',
  `recipient_user_id` bigint NOT NULL COMMENT 'Recipient user id',
  `recipient_company_id` bigint DEFAULT NULL COMMENT 'Recipient company id used for traceability',
  `delivery_key` varchar(256) NOT NULL COMMENT 'Tenant-scoped deterministic delivery key',
  `status` varchar(32) NOT NULL COMMENT 'Delivery status',
  `notify_message_id` bigint DEFAULT NULL COMMENT 'System notify message id',
  `attempt_count` int NOT NULL DEFAULT 0 COMMENT 'Delivery attempt count',
  `last_failure_code` varchar(64) DEFAULT NULL COMMENT 'Last failure code',
  `last_failure_reason` varchar(1024) DEFAULT NULL COMMENT 'Last failure reason',
  `sent_at` datetime DEFAULT NULL COMMENT 'Sent time',
  `next_retry_at` datetime DEFAULT NULL COMMENT 'Next retry time',
  `detail_json` json NOT NULL COMMENT 'Delivery evidence without secrets',
  `creator` varchar(64) DEFAULT '' COMMENT 'Creator',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `updater` varchar(64) DEFAULT '' COMMENT 'Updater',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_reg_cert_reminder_delivery_key` (`tenant_id`, `delivery_key`),
  UNIQUE KEY `uk_dcc_reg_cert_reminder_delivery_recipient` (`tenant_id`, `occurrence_id`, `recipient_user_id`),
  CONSTRAINT `chk_dcc_reg_cert_reminder_delivery_key` CHECK (TRIM(`delivery_key`) <> ''),
  CONSTRAINT `chk_dcc_reg_cert_reminder_delivery_recipient` CHECK (`recipient_user_id` > 0),
  CONSTRAINT `chk_dcc_reg_cert_reminder_delivery_status` CHECK (`status` IN
    ('PENDING', 'SENDING', 'SENT', 'FAILED')),
  CONSTRAINT `chk_dcc_reg_cert_reminder_delivery_message` CHECK (
    `status` <> 'SENT' OR (`notify_message_id` IS NOT NULL AND `notify_message_id` > 0 AND `sent_at` IS NOT NULL)
  ),
  CONSTRAINT `chk_dcc_reg_cert_reminder_delivery_failure` CHECK (
    `status` <> 'FAILED'
    OR (`last_failure_reason` IS NOT NULL AND TRIM(`last_failure_reason`) <> '')
  ),
  CONSTRAINT `chk_dcc_reg_cert_reminder_delivery_attempt` CHECK (`attempt_count` >= 0),
  CONSTRAINT `chk_dcc_reg_cert_reminder_delivery_detail` CHECK (JSON_VALID(`detail_json`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC registration certificate reminder delivery';

CALL assert_dcc_registration_certificate_reminder_contract();
DROP PROCEDURE IF EXISTS assert_dcc_registration_certificate_reminder_contract;

INSERT INTO `infra_job`
  (`id`, `name`, `status`, `handler_name`, `handler_param`, `cron_expression`,
   `retry_count`, `retry_interval`, `monitor_timeout`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 5620, '国内注册证每日提醒扫描', 1, 'registrationCertificateReminderDailyJob',
       '{"actorId":1,"roleIds":[910218,910231],"permission":"dcc:registration-certificate:query-current"}',
       '0 0 9 * * ?',
       3, 60, 0, 'registration-certificate-reminder', NOW(), 'registration-certificate-reminder', NOW(), b'0'
WHERE NOT EXISTS (
  SELECT 1
    FROM `infra_job`
   WHERE (`id` = 5620 OR `handler_name` = 'registrationCertificateReminderDailyJob')
     AND `deleted` = b'0'
);

UPDATE `infra_job`
   SET `name` = '国内注册证每日提醒扫描',
       `status` = 1,
       `handler_name` = 'registrationCertificateReminderDailyJob',
       `handler_param` = '{"actorId":1,"roleIds":[910218,910231],"permission":"dcc:registration-certificate:query-current"}',
       `cron_expression` = '0 0 9 * * ?',
       `retry_count` = 3,
       `retry_interval` = 60,
       `monitor_timeout` = 0,
       `updater` = 'registration-certificate-reminder',
       `update_time` = NOW(),
       `deleted` = b'0'
 WHERE `id` = 5620
    OR `handler_name` = 'registrationCertificateReminderDailyJob';
