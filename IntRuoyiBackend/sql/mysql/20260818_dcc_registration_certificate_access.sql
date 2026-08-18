-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260818_dcc_registration_certificate_reminder; type=schema; riskLevel=high
-- Purpose: Add shared request, BPM binding, grant, download-consumption and audit constraints for domestic registration certificate access.

DROP PROCEDURE IF EXISTS assert_dcc_registration_certificate_access_contract;

DELIMITER $$
CREATE PROCEDURE assert_dcc_registration_certificate_access_contract()
BEGIN
  DECLARE reminder_table_count int DEFAULT 0;
  DECLARE access_table_count int DEFAULT 0;

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
      SET MESSAGE_TEXT = 'DCC registration certificate access requires reminder schema';
  END IF;

  SELECT COUNT(*)
    INTO access_table_count
    FROM information_schema.TABLES
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME IN (
       'dcc_registration_certificate_access_request',
       'dcc_registration_certificate_access_request_file',
       'dcc_registration_certificate_bpm_binding',
       'dcc_registration_certificate_grant',
       'dcc_registration_certificate_download_consumption',
       'dcc_registration_certificate_access_audit'
     );

  IF access_table_count NOT IN (0, 6) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC registration certificate access partial schema detected';
  END IF;

  IF access_table_count = 6 THEN
    DROP TEMPORARY TABLE IF EXISTS tmp_dcc_reg_cert_access_expected_column;
    CREATE TEMPORARY TABLE tmp_dcc_reg_cert_access_expected_column (
      table_name varchar(128) NOT NULL,
      column_name varchar(128) NOT NULL,
      column_type varchar(64) NOT NULL,
      is_nullable char(3) NOT NULL,
      PRIMARY KEY (table_name, column_name)
    );
    INSERT INTO tmp_dcc_reg_cert_access_expected_column
      (table_name, column_name, column_type, is_nullable)
    VALUES
      ('dcc_registration_certificate_access_request', 'id', 'bigint', 'NO'),
      ('dcc_registration_certificate_access_request', 'tenant_id', 'bigint', 'NO'),
      ('dcc_registration_certificate_access_request', 'owner_company_id', 'bigint', 'NO'),
      ('dcc_registration_certificate_access_request', 'certificate_id', 'bigint', 'NO'),
      ('dcc_registration_certificate_access_request', 'requester_user_id', 'bigint', 'NO'),
      ('dcc_registration_certificate_access_request', 'request_type', 'varchar(32)', 'NO'),
      ('dcc_registration_certificate_access_request', 'request_key', 'varchar(256)', 'NO'),
      ('dcc_registration_certificate_access_request', 'bpm_process_instance_id', 'varchar(128)', 'YES'),
      ('dcc_registration_certificate_access_request', 'purpose', 'varchar(512)', 'NO'),
      ('dcc_registration_certificate_access_request', 'project_code_id', 'bigint', 'YES'),
      ('dcc_registration_certificate_access_request', 'status', 'varchar(32)', 'NO'),
      ('dcc_registration_certificate_access_request', 'requested_at', 'datetime', 'NO'),
      ('dcc_registration_certificate_access_request', 'completed_at', 'datetime', 'YES'),
      ('dcc_registration_certificate_access_request', 'withdrawn_at', 'datetime', 'YES'),
      ('dcc_registration_certificate_access_request', 'withdraw_reason', 'varchar(512)', 'YES'),
      ('dcc_registration_certificate_access_request', 'reject_reason', 'varchar(512)', 'YES'),
      ('dcc_registration_certificate_access_request', 'detail_json', 'json', 'NO'),
      ('dcc_registration_certificate_access_request', 'creator', 'varchar(64)', 'YES'),
      ('dcc_registration_certificate_access_request', 'create_time', 'datetime', 'NO'),
      ('dcc_registration_certificate_access_request', 'updater', 'varchar(64)', 'YES'),
      ('dcc_registration_certificate_access_request', 'update_time', 'datetime', 'NO'),
      ('dcc_registration_certificate_access_request', 'deleted', 'bit(1)', 'NO'),

      ('dcc_registration_certificate_access_request_file', 'id', 'bigint', 'NO'),
      ('dcc_registration_certificate_access_request_file', 'tenant_id', 'bigint', 'NO'),
      ('dcc_registration_certificate_access_request_file', 'request_id', 'bigint', 'NO'),
      ('dcc_registration_certificate_access_request_file', 'business_file_id', 'bigint', 'NO'),
      ('dcc_registration_certificate_access_request_file', 'file_kind', 'varchar(64)', 'NO'),
      ('dcc_registration_certificate_access_request_file', 'download_requested', 'bit(1)', 'NO'),
      ('dcc_registration_certificate_access_request_file', 'status', 'varchar(32)', 'NO'),
      ('dcc_registration_certificate_access_request_file', 'detail_json', 'json', 'NO'),
      ('dcc_registration_certificate_access_request_file', 'creator', 'varchar(64)', 'YES'),
      ('dcc_registration_certificate_access_request_file', 'create_time', 'datetime', 'NO'),
      ('dcc_registration_certificate_access_request_file', 'updater', 'varchar(64)', 'YES'),
      ('dcc_registration_certificate_access_request_file', 'update_time', 'datetime', 'NO'),
      ('dcc_registration_certificate_access_request_file', 'deleted', 'bit(1)', 'NO'),

      ('dcc_registration_certificate_bpm_binding', 'id', 'bigint', 'NO'),
      ('dcc_registration_certificate_bpm_binding', 'tenant_id', 'bigint', 'NO'),
      ('dcc_registration_certificate_bpm_binding', 'request_id', 'bigint', 'NO'),
      ('dcc_registration_certificate_bpm_binding', 'business_key', 'varchar(256)', 'NO'),
      ('dcc_registration_certificate_bpm_binding', 'bpm_process_instance_id', 'varchar(128)', 'NO'),
      ('dcc_registration_certificate_bpm_binding', 'status', 'varchar(32)', 'NO'),
      ('dcc_registration_certificate_bpm_binding', 'created_at', 'datetime', 'NO'),
      ('dcc_registration_certificate_bpm_binding', 'completed_at', 'datetime', 'YES'),
      ('dcc_registration_certificate_bpm_binding', 'detail_json', 'json', 'NO'),
      ('dcc_registration_certificate_bpm_binding', 'creator', 'varchar(64)', 'YES'),
      ('dcc_registration_certificate_bpm_binding', 'create_time', 'datetime', 'NO'),
      ('dcc_registration_certificate_bpm_binding', 'updater', 'varchar(64)', 'YES'),
      ('dcc_registration_certificate_bpm_binding', 'update_time', 'datetime', 'NO'),

      ('dcc_registration_certificate_grant', 'id', 'bigint', 'NO'),
      ('dcc_registration_certificate_grant', 'tenant_id', 'bigint', 'NO'),
      ('dcc_registration_certificate_grant', 'request_id', 'bigint', 'NO'),
      ('dcc_registration_certificate_grant', 'request_file_id', 'bigint', 'YES'),
      ('dcc_registration_certificate_grant', 'owner_company_id', 'bigint', 'NO'),
      ('dcc_registration_certificate_grant', 'certificate_id', 'bigint', 'NO'),
      ('dcc_registration_certificate_grant', 'business_file_id', 'bigint', 'YES'),
      ('dcc_registration_certificate_grant', 'grantee_user_id', 'bigint', 'NO'),
      ('dcc_registration_certificate_grant', 'grant_type', 'varchar(32)', 'NO'),
      ('dcc_registration_certificate_grant', 'grant_key', 'varchar(256)', 'NO'),
      ('dcc_registration_certificate_grant', 'status', 'varchar(32)', 'NO'),
      ('dcc_registration_certificate_grant', 'granted_at', 'datetime', 'NO'),
      ('dcc_registration_certificate_grant', 'expires_at', 'datetime', 'NO'),
      ('dcc_registration_certificate_grant', 'revoked_at', 'datetime', 'YES'),
      ('dcc_registration_certificate_grant', 'revoked_by', 'bigint', 'YES'),
      ('dcc_registration_certificate_grant', 'revoke_reason', 'varchar(512)', 'YES'),
      ('dcc_registration_certificate_grant', 'detail_json', 'json', 'NO'),
      ('dcc_registration_certificate_grant', 'creator', 'varchar(64)', 'YES'),
      ('dcc_registration_certificate_grant', 'create_time', 'datetime', 'NO'),
      ('dcc_registration_certificate_grant', 'updater', 'varchar(64)', 'YES'),
      ('dcc_registration_certificate_grant', 'update_time', 'datetime', 'NO'),
      ('dcc_registration_certificate_grant', 'deleted', 'bit(1)', 'NO'),

      ('dcc_registration_certificate_download_consumption', 'id', 'bigint', 'NO'),
      ('dcc_registration_certificate_download_consumption', 'tenant_id', 'bigint', 'NO'),
      ('dcc_registration_certificate_download_consumption', 'grant_id', 'bigint', 'NO'),
      ('dcc_registration_certificate_download_consumption', 'business_file_id', 'bigint', 'NO'),
      ('dcc_registration_certificate_download_consumption', 'attempt_key', 'varchar(256)', 'NO'),
      ('dcc_registration_certificate_download_consumption', 'result', 'varchar(32)', 'NO'),
      ('dcc_registration_certificate_download_consumption', 'success_unique_flag', 'bigint', 'YES'),
      ('dcc_registration_certificate_download_consumption', 'started_at', 'datetime', 'NO'),
      ('dcc_registration_certificate_download_consumption', 'completed_at', 'datetime', 'YES'),
      ('dcc_registration_certificate_download_consumption', 'failure_reason', 'varchar(1024)', 'YES'),
      ('dcc_registration_certificate_download_consumption', 'detail_json', 'json', 'NO'),
      ('dcc_registration_certificate_download_consumption', 'creator', 'varchar(64)', 'YES'),
      ('dcc_registration_certificate_download_consumption', 'create_time', 'datetime', 'NO'),
      ('dcc_registration_certificate_download_consumption', 'updater', 'varchar(64)', 'YES'),
      ('dcc_registration_certificate_download_consumption', 'update_time', 'datetime', 'NO'),
      ('dcc_registration_certificate_download_consumption', 'deleted', 'bit(1)', 'NO'),

      ('dcc_registration_certificate_access_audit', 'id', 'bigint', 'NO'),
      ('dcc_registration_certificate_access_audit', 'tenant_id', 'bigint', 'NO'),
      ('dcc_registration_certificate_access_audit', 'request_id', 'bigint', 'YES'),
      ('dcc_registration_certificate_access_audit', 'grant_id', 'bigint', 'YES'),
      ('dcc_registration_certificate_access_audit', 'business_file_id', 'bigint', 'YES'),
      ('dcc_registration_certificate_access_audit', 'actor_user_id', 'bigint', 'NO'),
      ('dcc_registration_certificate_access_audit', 'event_type', 'varchar(64)', 'NO'),
      ('dcc_registration_certificate_access_audit', 'event_key', 'varchar(256)', 'NO'),
      ('dcc_registration_certificate_access_audit', 'result', 'varchar(32)', 'NO'),
      ('dcc_registration_certificate_access_audit', 'occurred_at', 'datetime', 'NO'),
      ('dcc_registration_certificate_access_audit', 'detail_json', 'json', 'NO'),
      ('dcc_registration_certificate_access_audit', 'creator', 'varchar(64)', 'YES'),
      ('dcc_registration_certificate_access_audit', 'create_time', 'datetime', 'NO'),
      ('dcc_registration_certificate_access_audit', 'updater', 'varchar(64)', 'YES'),
      ('dcc_registration_certificate_access_audit', 'update_time', 'datetime', 'NO'),
      ('dcc_registration_certificate_access_audit', 'deleted', 'bit(1)', 'NO');

    IF EXISTS (
      SELECT 1
        FROM tmp_dcc_reg_cert_access_expected_column AS expected_column
        LEFT JOIN information_schema.COLUMNS AS actual_column
          ON actual_column.TABLE_SCHEMA = DATABASE()
         AND actual_column.TABLE_NAME = expected_column.table_name
         AND actual_column.COLUMN_NAME = expected_column.column_name
       WHERE actual_column.COLUMN_NAME IS NULL
          OR LOWER(actual_column.COLUMN_TYPE) <> expected_column.column_type
          OR actual_column.IS_NULLABLE <> expected_column.is_nullable
    ) THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'DCC registration certificate access column contract mismatch';
    END IF;

    IF EXISTS (
      SELECT 1
        FROM (
          SELECT table_name, COUNT(*) AS column_count
            FROM tmp_dcc_reg_cert_access_expected_column
           GROUP BY table_name
        ) AS expected_table
        LEFT JOIN (
          SELECT TABLE_NAME AS table_name, COUNT(*) AS column_count
            FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME IN (
               'dcc_registration_certificate_access_request',
               'dcc_registration_certificate_access_request_file',
               'dcc_registration_certificate_bpm_binding',
               'dcc_registration_certificate_grant',
               'dcc_registration_certificate_download_consumption',
               'dcc_registration_certificate_access_audit'
             )
           GROUP BY TABLE_NAME
        ) AS actual_table ON actual_table.table_name = expected_table.table_name
       WHERE actual_table.column_count <> expected_table.column_count
    ) THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'DCC registration certificate access column contract mismatch';
    END IF;
    DROP TEMPORARY TABLE IF EXISTS tmp_dcc_reg_cert_access_expected_column;

    DROP TEMPORARY TABLE IF EXISTS tmp_dcc_reg_cert_access_expected_index;
    CREATE TEMPORARY TABLE tmp_dcc_reg_cert_access_expected_index (
      table_name varchar(128) NOT NULL,
      index_name varchar(128) NOT NULL,
      column_list varchar(512) NOT NULL,
      non_unique int NOT NULL,
      PRIMARY KEY (table_name, index_name)
    );
    INSERT INTO tmp_dcc_reg_cert_access_expected_index
      (table_name, index_name, column_list, non_unique)
    VALUES
      ('dcc_registration_certificate_access_request', 'PRIMARY', 'id', 0),
      ('dcc_registration_certificate_access_request', 'uk_dcc_reg_cert_access_request_key', 'tenant_id,request_key', 0),
      ('dcc_registration_certificate_access_request', 'uk_dcc_reg_cert_access_request_bpm', 'tenant_id,bpm_process_instance_id', 0),
      ('dcc_registration_certificate_access_request_file', 'PRIMARY', 'id', 0),
      ('dcc_registration_certificate_access_request_file', 'uk_dcc_reg_cert_request_file_scope', 'tenant_id,request_id,business_file_id', 0),
      ('dcc_registration_certificate_bpm_binding', 'PRIMARY', 'id', 0),
      ('dcc_registration_certificate_bpm_binding', 'uk_dcc_reg_cert_bpm_binding_business', 'tenant_id,business_key', 0),
      ('dcc_registration_certificate_bpm_binding', 'uk_dcc_reg_cert_bpm_binding_process', 'tenant_id,bpm_process_instance_id', 0),
      ('dcc_registration_certificate_grant', 'PRIMARY', 'id', 0),
      ('dcc_registration_certificate_grant', 'uk_dcc_reg_cert_grant_key', 'tenant_id,grant_key', 0),
      ('dcc_registration_certificate_grant', 'uk_dcc_reg_cert_grant_request_file', 'tenant_id,request_file_id,grant_type', 0),
      ('dcc_registration_certificate_download_consumption', 'PRIMARY', 'id', 0),
      ('dcc_registration_certificate_download_consumption', 'uk_dcc_reg_cert_download_once', 'tenant_id,grant_id,business_file_id,success_unique_flag', 0),
      ('dcc_registration_certificate_download_consumption', 'uk_dcc_reg_cert_download_attempt', 'tenant_id,attempt_key', 0),
      ('dcc_registration_certificate_access_audit', 'PRIMARY', 'id', 0),
      ('dcc_registration_certificate_access_audit', 'uk_dcc_reg_cert_access_audit_key', 'tenant_id,event_key', 0);

    IF EXISTS (
      SELECT 1
        FROM tmp_dcc_reg_cert_access_expected_index AS expected_index
        LEFT JOIN (
          SELECT TABLE_NAME AS table_name,
                 INDEX_NAME AS index_name,
                 GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') AS column_list,
                 MAX(NON_UNIQUE) AS non_unique
            FROM information_schema.STATISTICS
           WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME IN (
               'dcc_registration_certificate_access_request',
               'dcc_registration_certificate_access_request_file',
               'dcc_registration_certificate_bpm_binding',
               'dcc_registration_certificate_grant',
               'dcc_registration_certificate_download_consumption',
               'dcc_registration_certificate_access_audit'
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
        SET MESSAGE_TEXT = 'DCC registration certificate access index contract mismatch';
    END IF;
    DROP TEMPORARY TABLE IF EXISTS tmp_dcc_reg_cert_access_expected_index;
  END IF;
END$$
DELIMITER ;

CALL assert_dcc_registration_certificate_access_contract();

CREATE TABLE IF NOT EXISTS `dcc_registration_certificate_access_request` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Access request id',
  `tenant_id` bigint NOT NULL COMMENT 'Tenant id',
  `owner_company_id` bigint NOT NULL COMMENT 'Owning company enterprise id',
  `certificate_id` bigint NOT NULL COMMENT 'Registration certificate aggregate id',
  `requester_user_id` bigint NOT NULL COMMENT 'Requester user id',
  `request_type` varchar(32) NOT NULL COMMENT 'Access request type',
  `request_key` varchar(256) NOT NULL COMMENT 'Tenant-scoped idempotency key',
  `bpm_process_instance_id` varchar(128) DEFAULT NULL COMMENT 'Native BPM process id after binding',
  `purpose` varchar(512) NOT NULL COMMENT 'Access purpose',
  `project_code_id` bigint DEFAULT NULL COMMENT 'Required for download requests',
  `status` varchar(32) NOT NULL COMMENT 'Request status',
  `requested_at` datetime NOT NULL COMMENT 'Submission time',
  `completed_at` datetime DEFAULT NULL COMMENT 'Approval completion time',
  `withdrawn_at` datetime DEFAULT NULL COMMENT 'Requester withdrawal time',
  `withdraw_reason` varchar(512) DEFAULT NULL COMMENT 'Withdrawal reason',
  `reject_reason` varchar(512) DEFAULT NULL COMMENT 'Reject reason',
  `detail_json` json NOT NULL COMMENT 'Request evidence without secrets',
  `creator` varchar(64) DEFAULT '' COMMENT 'Creator',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `updater` varchar(64) DEFAULT '' COMMENT 'Updater',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'Deleted flag',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_reg_cert_access_request_key` (`tenant_id`, `request_key`),
  UNIQUE KEY `uk_dcc_reg_cert_access_request_bpm` (`tenant_id`, `bpm_process_instance_id`),
  CONSTRAINT `chk_dcc_reg_cert_access_request_type` CHECK (`request_type` IN
    ('VIEW_OLD_CERTIFICATE', 'DOWNLOAD_FILE')),
  CONSTRAINT `chk_dcc_reg_cert_access_request_status` CHECK (`status` IN
    ('SUBMITTED', 'BPM_BOUND', 'APPROVED', 'REJECTED', 'WITHDRAWN', 'REVOKED')),
  CONSTRAINT `chk_dcc_reg_cert_access_request_key` CHECK (TRIM(`request_key`) <> ''),
  CONSTRAINT `chk_dcc_reg_cert_access_request_purpose` CHECK (TRIM(`purpose`) <> ''),
  CONSTRAINT `chk_dcc_reg_cert_access_request_project` CHECK (
    `request_type` <> 'DOWNLOAD_FILE' OR `project_code_id` IS NOT NULL
  ),
  CONSTRAINT `chk_dcc_reg_cert_access_request_reject` CHECK (
    `status` <> 'REJECTED' OR (`reject_reason` IS NOT NULL AND TRIM(`reject_reason`) <> '')
  ),
  CONSTRAINT `chk_dcc_reg_cert_access_request_withdraw` CHECK (
    `status` <> 'WITHDRAWN' OR `withdrawn_at` IS NOT NULL
  ),
  CONSTRAINT `chk_dcc_reg_cert_access_request_detail` CHECK (JSON_VALID(`detail_json`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC registration certificate access request';

CREATE TABLE IF NOT EXISTS `dcc_registration_certificate_access_request_file` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Request file id',
  `tenant_id` bigint NOT NULL COMMENT 'Tenant id',
  `request_id` bigint NOT NULL COMMENT 'Access request id',
  `business_file_id` bigint NOT NULL COMMENT 'Registration certificate business file id',
  `file_kind` varchar(64) NOT NULL COMMENT 'Registration certificate file kind',
  `download_requested` bit(1) NOT NULL DEFAULT b'0' COMMENT 'Whether this file requires download grant',
  `status` varchar(32) NOT NULL COMMENT 'Requested file status',
  `detail_json` json NOT NULL COMMENT 'Requested file evidence without secrets',
  `creator` varchar(64) DEFAULT '' COMMENT 'Creator',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `updater` varchar(64) DEFAULT '' COMMENT 'Updater',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'Deleted flag',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_reg_cert_request_file_scope` (`tenant_id`, `request_id`, `business_file_id`),
  CONSTRAINT `chk_dcc_reg_cert_request_file_kind` CHECK (TRIM(`file_kind`) <> ''),
  CONSTRAINT `chk_dcc_reg_cert_request_file_download` CHECK (
    (`download_requested` = b'1' AND `status` IN ('REQUESTED', 'APPROVED', 'REJECTED', 'GRANTED'))
    OR (`download_requested` = b'0' AND `status` IN ('REQUESTED', 'APPROVED', 'REJECTED'))
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC registration certificate access requested file';

CREATE TABLE IF NOT EXISTS `dcc_registration_certificate_bpm_binding` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'BPM binding id',
  `tenant_id` bigint NOT NULL COMMENT 'Tenant id',
  `request_id` bigint NOT NULL COMMENT 'Access request id',
  `business_key` varchar(256) NOT NULL COMMENT 'BPM business key',
  `bpm_process_instance_id` varchar(128) NOT NULL COMMENT 'Native BPM process id',
  `status` varchar(32) NOT NULL COMMENT 'BPM binding status',
  `created_at` datetime NOT NULL COMMENT 'BPM binding create time',
  `completed_at` datetime DEFAULT NULL COMMENT 'BPM completion time',
  `detail_json` json NOT NULL COMMENT 'BPM binding evidence without secrets',
  `creator` varchar(64) DEFAULT '' COMMENT 'Creator',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `updater` varchar(64) DEFAULT '' COMMENT 'Updater',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'Deleted flag',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_reg_cert_bpm_binding_business` (`tenant_id`, `business_key`),
  UNIQUE KEY `uk_dcc_reg_cert_bpm_binding_process` (`tenant_id`, `bpm_process_instance_id`),
  CONSTRAINT `chk_dcc_reg_cert_bpm_binding_key` CHECK (TRIM(`business_key`) <> ''),
  CONSTRAINT `chk_dcc_reg_cert_bpm_binding_instance` CHECK (TRIM(`bpm_process_instance_id`) <> ''),
  CONSTRAINT `chk_dcc_reg_cert_bpm_binding_status` CHECK (`status` IN
    ('RUNNING', 'APPROVED', 'REJECTED', 'WITHDRAWN', 'CANCELLED')),
  CONSTRAINT `chk_dcc_reg_cert_bpm_binding_detail` CHECK (JSON_VALID(`detail_json`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC registration certificate Native BPM binding';

CREATE TABLE IF NOT EXISTS `dcc_registration_certificate_grant` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Grant id',
  `tenant_id` bigint NOT NULL COMMENT 'Tenant id',
  `request_id` bigint NOT NULL COMMENT 'Access request id',
  `request_file_id` bigint DEFAULT NULL COMMENT 'Requested file id for download grants',
  `owner_company_id` bigint NOT NULL COMMENT 'Owning company enterprise id',
  `certificate_id` bigint NOT NULL COMMENT 'Registration certificate aggregate id',
  `business_file_id` bigint DEFAULT NULL COMMENT 'Business file id for file-scoped grants',
  `grantee_user_id` bigint NOT NULL COMMENT 'Granted user id',
  `grant_type` varchar(32) NOT NULL COMMENT 'Grant type',
  `grant_key` varchar(256) NOT NULL COMMENT 'Tenant-scoped idempotency key',
  `status` varchar(32) NOT NULL COMMENT 'Grant status',
  `granted_at` datetime NOT NULL COMMENT 'Grant start time',
  `expires_at` datetime NOT NULL COMMENT 'Grant expiry time',
  `revoked_at` datetime DEFAULT NULL COMMENT 'Revoked time',
  `revoked_by` bigint DEFAULT NULL COMMENT 'Revoker user id',
  `revoke_reason` varchar(512) DEFAULT NULL COMMENT 'Required revoke reason',
  `detail_json` json NOT NULL COMMENT 'Grant evidence without secrets',
  `creator` varchar(64) DEFAULT '' COMMENT 'Creator',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `updater` varchar(64) DEFAULT '' COMMENT 'Updater',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'Deleted flag',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_reg_cert_grant_key` (`tenant_id`, `grant_key`),
  UNIQUE KEY `uk_dcc_reg_cert_grant_request_file` (`tenant_id`, `request_file_id`, `grant_type`),
  CONSTRAINT `chk_dcc_reg_cert_grant_key` CHECK (TRIM(`grant_key`) <> ''),
  CONSTRAINT `chk_dcc_reg_cert_grant_type` CHECK (`grant_type` IN
    ('VIEW_OLD_CERTIFICATE', 'DOWNLOAD')),
  CONSTRAINT `chk_dcc_reg_cert_grant_status` CHECK (`status` IN
    ('ACTIVE', 'EXPIRED', 'REVOKED', 'CONSUMED')),
  CONSTRAINT `chk_dcc_reg_cert_grant_file_scope` CHECK (
    (`grant_type` = 'DOWNLOAD' AND `request_file_id` IS NOT NULL AND `business_file_id` IS NOT NULL)
    OR (`grant_type` = 'VIEW_OLD_CERTIFICATE' AND `request_file_id` IS NULL)
  ),
  CONSTRAINT `chk_dcc_reg_cert_grant_window` CHECK (`expires_at` > `granted_at`),
  CONSTRAINT `chk_dcc_reg_cert_grant_revoke` CHECK (
    `status` <> 'REVOKED'
    OR (`revoked_at` IS NOT NULL AND `revoked_by` IS NOT NULL
      AND `revoke_reason` IS NOT NULL AND TRIM(`revoke_reason`) <> '')
  ),
  CONSTRAINT `chk_dcc_reg_cert_grant_detail` CHECK (JSON_VALID(`detail_json`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC registration certificate access grant';

CREATE TABLE IF NOT EXISTS `dcc_registration_certificate_download_consumption` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Download consumption id',
  `tenant_id` bigint NOT NULL COMMENT 'Tenant id',
  `grant_id` bigint NOT NULL COMMENT 'Grant id',
  `business_file_id` bigint NOT NULL COMMENT 'Business file id',
  `attempt_key` varchar(256) NOT NULL COMMENT 'Tenant-scoped download attempt key',
  `result` varchar(32) NOT NULL COMMENT 'Download attempt result',
  `success_unique_flag` bigint GENERATED ALWAYS AS (
    CASE WHEN `result` = 'SUCCESS' THEN 1 ELSE NULL END
  ) STORED COMMENT 'One successful download per grant and file',
  `started_at` datetime NOT NULL COMMENT 'Attempt start time',
  `completed_at` datetime DEFAULT NULL COMMENT 'Attempt completion time',
  `failure_reason` varchar(1024) DEFAULT NULL COMMENT 'Failure reason',
  `detail_json` json NOT NULL COMMENT 'Download evidence without secrets',
  `creator` varchar(64) DEFAULT '' COMMENT 'Creator',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `updater` varchar(64) DEFAULT '' COMMENT 'Updater',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'Deleted flag',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_reg_cert_download_once` (`tenant_id`, `grant_id`, `business_file_id`, `success_unique_flag`),
  UNIQUE KEY `uk_dcc_reg_cert_download_attempt` (`tenant_id`, `attempt_key`),
  CONSTRAINT `chk_dcc_reg_cert_download_key` CHECK (TRIM(`attempt_key`) <> ''),
  CONSTRAINT `chk_dcc_reg_cert_download_result` CHECK (`result` IN
    ('SUCCESS', 'FAILED_BEFORE_START', 'FAILED_AFTER_START')),
  CONSTRAINT `chk_dcc_reg_cert_download_completion` CHECK (
    (`result` = 'SUCCESS' AND `completed_at` IS NOT NULL)
    OR (`result` <> 'SUCCESS')
  ),
  CONSTRAINT `chk_dcc_reg_cert_download_failure` CHECK (
    `result` = 'SUCCESS'
    OR (`failure_reason` IS NOT NULL AND TRIM(`failure_reason`) <> '')
  ),
  CONSTRAINT `chk_dcc_reg_cert_download_detail` CHECK (JSON_VALID(`detail_json`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC registration certificate download consumption';

CREATE TABLE IF NOT EXISTS `dcc_registration_certificate_access_audit` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Access audit id',
  `tenant_id` bigint NOT NULL COMMENT 'Tenant id',
  `request_id` bigint DEFAULT NULL COMMENT 'Access request id',
  `grant_id` bigint DEFAULT NULL COMMENT 'Grant id',
  `business_file_id` bigint DEFAULT NULL COMMENT 'Business file id',
  `actor_user_id` bigint NOT NULL COMMENT 'Actor user id',
  `event_type` varchar(64) NOT NULL COMMENT 'Access event type',
  `event_key` varchar(256) NOT NULL COMMENT 'Tenant-scoped idempotency key',
  `result` varchar(32) NOT NULL COMMENT 'Access event result',
  `occurred_at` datetime NOT NULL COMMENT 'Event occurrence time',
  `detail_json` json NOT NULL COMMENT 'Audit evidence without secrets',
  `creator` varchar(64) DEFAULT '' COMMENT 'Creator',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `updater` varchar(64) DEFAULT '' COMMENT 'Updater',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_reg_cert_access_audit_key` (`tenant_id`, `event_key`),
  CONSTRAINT `chk_dcc_reg_cert_access_audit_key` CHECK (TRIM(`event_key`) <> ''),
  CONSTRAINT `chk_dcc_reg_cert_access_audit_type` CHECK (TRIM(`event_type`) <> ''),
  CONSTRAINT `chk_dcc_reg_cert_access_audit_result` CHECK (`result` IN ('SUCCESS', 'FAILURE')),
  CONSTRAINT `chk_dcc_reg_cert_access_audit_detail` CHECK (JSON_VALID(`detail_json`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC registration certificate access audit';

CALL assert_dcc_registration_certificate_access_contract();
DROP PROCEDURE IF EXISTS assert_dcc_registration_certificate_access_contract;
