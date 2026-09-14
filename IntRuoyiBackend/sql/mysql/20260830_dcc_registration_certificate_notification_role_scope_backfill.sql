-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260816_mdm_enterprise_company_scope,20260818_dcc_registration_certificate_reminder,20260830_dcc_registration_certificate_associated_company_backfill; type=data; riskLevel=medium
-- Purpose: Backfill enabled role-company scopes for configured registration-certificate notification roles and existing registration-certificate owner companies without authorizing users.
-- Recovery: The procedure runs inside one transaction for data writes; on failure it rolls back inserted role scopes and leaves existing authorization data untouched.
-- Rollback: Delete only rows created by creator='dcc-reg-cert-notification-role-scope-backfill' after verifying no downstream notification run depends on them.

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_dcc_reg_cert_notification_role_scope_backfill_20260830;
DELIMITER $$
CREATE PROCEDURE ensure_dcc_reg_cert_notification_role_scope_backfill_20260830()
BEGIN
  DECLARE job_count INT DEFAULT 0;
  DECLARE configured_role_count INT DEFAULT 0;
  DECLARE expected_count INT DEFAULT 0;
  DECLARE inserted_count INT DEFAULT 0;

  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    DROP TEMPORARY TABLE IF EXISTS tmp_dcc_reg_cert_notification_role_ids;
    DROP TEMPORARY TABLE IF EXISTS tmp_dcc_reg_cert_notification_roles;
    DROP TEMPORARY TABLE IF EXISTS tmp_dcc_reg_cert_notification_companies;
    DROP TEMPORARY TABLE IF EXISTS tmp_dcc_reg_cert_notification_pending_scopes;
    RESIGNAL;
  END;

  IF NOT EXISTS (
      SELECT 1
        FROM information_schema.TABLES
       WHERE TABLE_SCHEMA = DATABASE()
         AND TABLE_NAME = 'infra_job'
  ) OR NOT EXISTS (
      SELECT 1
        FROM information_schema.TABLES
       WHERE TABLE_SCHEMA = DATABASE()
         AND TABLE_NAME = 'dcc_registration_certificate'
  ) OR NOT EXISTS (
      SELECT 1
        FROM information_schema.TABLES
       WHERE TABLE_SCHEMA = DATABASE()
         AND TABLE_NAME = 'mdm_enterprise'
  ) OR NOT EXISTS (
      SELECT 1
        FROM information_schema.TABLES
       WHERE TABLE_SCHEMA = DATABASE()
         AND TABLE_NAME = 'mdm_role_company_scope'
  ) OR NOT EXISTS (
      SELECT 1
        FROM information_schema.TABLES
       WHERE TABLE_SCHEMA = DATABASE()
         AND TABLE_NAME = 'system_role'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing registration certificate notification role scope source';
  END IF;

  SELECT COUNT(*)
    INTO job_count
    FROM `infra_job`
   WHERE `handler_name` = 'registrationCertificateReminderDailyJob'
     AND `deleted` = b'0';

  IF job_count <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing registration certificate notification role scope source';
  END IF;

  IF EXISTS (
      SELECT 1
        FROM `infra_job` AS `job`
       WHERE `job`.`handler_name` = 'registrationCertificateReminderDailyJob'
         AND `job`.`deleted` = b'0'
         AND (
           `job`.`handler_param` IS NULL
           OR TRIM(`job`.`handler_param`) = ''
           OR JSON_VALID(`job`.`handler_param`) = 0
           OR COALESCE(JSON_TYPE(JSON_EXTRACT(`job`.`handler_param`, '$.roleIds')), '') <> 'ARRAY'
         )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing registration certificate notification role scope source';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_reg_cert_notification_role_ids;
  CREATE TEMPORARY TABLE tmp_dcc_reg_cert_notification_role_ids (
    `role_id` bigint NOT NULL,
    PRIMARY KEY (`role_id`)
  ) ENGINE=MEMORY DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_reg_cert_notification_roles;
  CREATE TEMPORARY TABLE tmp_dcc_reg_cert_notification_roles (
    `tenant_id` bigint NOT NULL,
    `role_id` bigint NOT NULL,
    PRIMARY KEY (`tenant_id`, `role_id`)
  ) ENGINE=MEMORY DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_reg_cert_notification_companies;
  CREATE TEMPORARY TABLE tmp_dcc_reg_cert_notification_companies (
    `tenant_id` bigint NOT NULL,
    `company_id` bigint NOT NULL,
    PRIMARY KEY (`tenant_id`, `company_id`)
  ) ENGINE=MEMORY DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_reg_cert_notification_pending_scopes;
  CREATE TEMPORARY TABLE tmp_dcc_reg_cert_notification_pending_scopes (
    `tenant_id` bigint NOT NULL,
    `role_id` bigint NOT NULL,
    `company_id` bigint NOT NULL,
    PRIMARY KEY (`tenant_id`, `role_id`, `company_id`)
  ) ENGINE=MEMORY DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

  INSERT IGNORE INTO tmp_dcc_reg_cert_notification_role_ids (`role_id`)
  SELECT DISTINCT `role_param`.`role_id`
    FROM `infra_job` AS `job`
    JOIN JSON_TABLE(
           `job`.`handler_param`,
           '$.roleIds[*]' COLUMNS (
             `role_id` bigint PATH '$'
           )
         ) AS `role_param`
   WHERE `job`.`handler_name` = 'registrationCertificateReminderDailyJob'
     AND `job`.`deleted` = b'0';

  SELECT COUNT(*)
    INTO configured_role_count
    FROM tmp_dcc_reg_cert_notification_role_ids;

  IF configured_role_count = 0
      OR EXISTS (
        SELECT 1
          FROM tmp_dcc_reg_cert_notification_role_ids
         WHERE `role_id` IS NULL
            OR `role_id` <= 0
      ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing registration certificate notification role scope source';
  END IF;

  IF EXISTS (
      SELECT 1
        FROM tmp_dcc_reg_cert_notification_role_ids AS `configured`
        LEFT JOIN `system_role` AS `role`
          ON `role`.`id` = `configured`.`role_id`
         AND `role`.`deleted` = b'0'
         AND `role`.`status` = 0
       WHERE `role`.`id` IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing registration certificate notification role scope source';
  END IF;

  INSERT INTO tmp_dcc_reg_cert_notification_roles (`tenant_id`, `role_id`)
  SELECT `role`.`tenant_id`,
         `role`.`id`
    FROM `system_role` AS `role`
    JOIN tmp_dcc_reg_cert_notification_role_ids AS `configured`
      ON `configured`.`role_id` = `role`.`id`
   WHERE `role`.`deleted` = b'0'
     AND `role`.`status` = 0;

  IF EXISTS (
      SELECT 1
        FROM `dcc_registration_certificate` AS `certificate`
        LEFT JOIN `mdm_enterprise` AS `enterprise`
          ON `enterprise`.`id` = `certificate`.`owner_company_id`
         AND `enterprise`.`tenant_id` = `certificate`.`tenant_id`
         AND `enterprise`.`deleted` = b'0'
         AND `enterprise`.`type` = 'OWNED_COMPANY'
         AND `enterprise`.`status` = 'ENABLE'
       WHERE `certificate`.`deleted` = b'0'
         AND `certificate`.`status` = 'ACTIVE'
         AND `certificate`.`owner_company_id` IS NOT NULL
         AND `certificate`.`owner_company_id` > 0
         AND `enterprise`.`id` IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing registration certificate notification role scope source';
  END IF;

  INSERT IGNORE INTO tmp_dcc_reg_cert_notification_companies (`tenant_id`, `company_id`)
  SELECT DISTINCT
         `certificate`.`tenant_id`,
         `certificate`.`owner_company_id`
    FROM `dcc_registration_certificate` AS `certificate`
    JOIN `mdm_enterprise` AS `enterprise`
      ON `enterprise`.`id` = `certificate`.`owner_company_id`
     AND `enterprise`.`tenant_id` = `certificate`.`tenant_id`
     AND `enterprise`.`deleted` = b'0'
     AND `enterprise`.`type` = 'OWNED_COMPANY'
     AND `enterprise`.`status` = 'ENABLE'
   WHERE `certificate`.`deleted` = b'0'
     AND `certificate`.`status` = 'ACTIVE'
     AND `certificate`.`owner_company_id` IS NOT NULL
     AND `certificate`.`owner_company_id` > 0;

  IF EXISTS (
      SELECT 1
        FROM tmp_dcc_reg_cert_notification_companies AS `company`
        JOIN tmp_dcc_reg_cert_notification_roles AS `role`
          ON `role`.`tenant_id` = `company`.`tenant_id`
        JOIN `mdm_role_company_scope` AS `role_scope`
          ON `role_scope`.`tenant_id` = `company`.`tenant_id`
         AND `role_scope`.`role_id` = `role`.`role_id`
         AND `role_scope`.`company_id` = `company`.`company_id`
       WHERE `role_scope`.`deleted` <> b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Registration certificate notification role scope soft-deleted reservation exists';
  END IF;

  INSERT INTO tmp_dcc_reg_cert_notification_pending_scopes
    (`tenant_id`, `role_id`, `company_id`)
  SELECT `company`.`tenant_id`,
         `role`.`role_id`,
         `company`.`company_id`
    FROM tmp_dcc_reg_cert_notification_companies AS `company`
    JOIN tmp_dcc_reg_cert_notification_roles AS `role`
      ON `role`.`tenant_id` = `company`.`tenant_id`
   WHERE NOT EXISTS (
       SELECT 1
         FROM `mdm_role_company_scope` AS `role_scope`
        WHERE `role_scope`.`tenant_id` = `company`.`tenant_id`
          AND `role_scope`.`role_id` = `role`.`role_id`
          AND `role_scope`.`company_id` = `company`.`company_id`
          AND `role_scope`.`deleted` = b'0'
          AND `role_scope`.`status` = 'ENABLE'
   );

  SET expected_count = (
      SELECT COUNT(*)
        FROM tmp_dcc_reg_cert_notification_pending_scopes
  );

  START TRANSACTION;

  INSERT INTO `mdm_role_company_scope`
    (`role_id`, `company_id`, `status`, `revision`, `creator`, `create_time`,
     `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT `pending`.`role_id`,
         `pending`.`company_id`,
         'ENABLE',
         1,
         'dcc-reg-cert-notification-role-scope-backfill',
         NOW(),
         'dcc-reg-cert-notification-role-scope-backfill',
         NOW(),
         b'0',
         `pending`.`tenant_id`
    FROM tmp_dcc_reg_cert_notification_pending_scopes AS `pending`;

  SET inserted_count = ROW_COUNT();

  IF inserted_count <> expected_count THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Registration certificate notification role scope backfill count mismatch';
  END IF;

  IF EXISTS (
      SELECT 1
        FROM tmp_dcc_reg_cert_notification_companies AS `company`
        JOIN tmp_dcc_reg_cert_notification_roles AS `role`
          ON `role`.`tenant_id` = `company`.`tenant_id`
        LEFT JOIN `mdm_role_company_scope` AS `role_scope`
          ON `role_scope`.`tenant_id` = `company`.`tenant_id`
         AND `role_scope`.`role_id` = `role`.`role_id`
         AND `role_scope`.`company_id` = `company`.`company_id`
         AND `role_scope`.`deleted` = b'0'
         AND `role_scope`.`status` = 'ENABLE'
       WHERE `role_scope`.`id` IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Registration certificate notification role scope backfill count mismatch';
  END IF;

  COMMIT;

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_reg_cert_notification_role_ids;
  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_reg_cert_notification_roles;
  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_reg_cert_notification_companies;
  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_reg_cert_notification_pending_scopes;
END$$
DELIMITER ;

CALL ensure_dcc_reg_cert_notification_role_scope_backfill_20260830();
DROP PROCEDURE IF EXISTS ensure_dcc_reg_cert_notification_role_scope_backfill_20260830;
