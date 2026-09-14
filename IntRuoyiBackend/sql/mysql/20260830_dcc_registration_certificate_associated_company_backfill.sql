-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260817_dcc_registration_certificate_core; type=data; riskLevel=medium
-- Purpose: Backfill active owned-company master rows for registration-certificate owner companies without authorizing users or roles.

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_dcc_reg_cert_associated_company_backfill;
DELIMITER $$
CREATE PROCEDURE ensure_dcc_reg_cert_associated_company_backfill()
BEGIN
  DECLARE expected_count INT DEFAULT 0;
  DECLARE inserted_count INT DEFAULT 0;

  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    DROP TEMPORARY TABLE IF EXISTS tmp_dcc_reg_cert_owner_company_backfill;
    RESIGNAL;
  END;

  IF NOT EXISTS (
      SELECT 1
        FROM information_schema.TABLES
       WHERE TABLE_SCHEMA = DATABASE()
         AND TABLE_NAME = 'dcc_registration_certificate'
  ) OR NOT EXISTS (
      SELECT 1
        FROM information_schema.TABLES
       WHERE TABLE_SCHEMA = DATABASE()
         AND TABLE_NAME = 'mdm_enterprise'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing registration certificate owner company master data source';
  END IF;

  IF EXISTS (
      SELECT 1
        FROM `dcc_registration_certificate` AS `certificate`
        JOIN `mdm_enterprise` AS `enterprise`
          ON `enterprise`.`id` = `certificate`.`owner_company_id`
       WHERE `certificate`.`deleted` = b'0'
         AND `certificate`.`status` = 'ACTIVE'
         AND `certificate`.`owner_company_id` IS NOT NULL
         AND `certificate`.`owner_company_id` > 0
         AND (
           `enterprise`.`tenant_id` <> `certificate`.`tenant_id`
           OR `enterprise`.`deleted` <> b'0'
           OR `enterprise`.`type` <> 'OWNED_COMPANY'
           OR `enterprise`.`status` <> 'ENABLE'
           OR TRIM(`enterprise`.`name`) = ''
         )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing registration certificate owner company master data source';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_reg_cert_owner_company_backfill;
  CREATE TEMPORARY TABLE tmp_dcc_reg_cert_owner_company_backfill (
    `tenant_id` bigint NOT NULL,
    `owner_company_id` bigint NOT NULL,
    PRIMARY KEY (`tenant_id`, `owner_company_id`)
  ) ENGINE=MEMORY DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

  START TRANSACTION;

  INSERT INTO tmp_dcc_reg_cert_owner_company_backfill
    (`tenant_id`, `owner_company_id`)
  SELECT DISTINCT
         `certificate`.`tenant_id`,
         `certificate`.`owner_company_id`
    FROM `dcc_registration_certificate` AS `certificate`
   WHERE `certificate`.`deleted` = b'0'
     AND `certificate`.`status` = 'ACTIVE'
     AND `certificate`.`owner_company_id` IS NOT NULL
     AND `certificate`.`owner_company_id` > 0
     AND NOT EXISTS (
       SELECT 1
         FROM `mdm_enterprise` AS `enterprise`
        WHERE `enterprise`.`id` = `certificate`.`owner_company_id`
          AND `enterprise`.`tenant_id` = `certificate`.`tenant_id`
          AND `enterprise`.`deleted` = b'0'
          AND `enterprise`.`type` = 'OWNED_COMPANY'
          AND `enterprise`.`status` = 'ENABLE'
     );

  SET expected_count = (
      SELECT COUNT(1)
        FROM tmp_dcc_reg_cert_owner_company_backfill
  );

  IF EXISTS (
      SELECT 1
        FROM tmp_dcc_reg_cert_owner_company_backfill AS pending_company
        JOIN `mdm_enterprise` AS `enterprise`
          ON `enterprise`.`tenant_id` = pending_company.`tenant_id`
         AND `enterprise`.`enterprise_code` =
             CONCAT('HIST-REG-OWN-', pending_company.`tenant_id`, '-', pending_company.`owner_company_id`)
         AND `enterprise`.`id` <> pending_company.`owner_company_id`
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Registration certificate owner company backfill count mismatch';
  END IF;

  INSERT INTO `mdm_enterprise`
    (`id`, `enterprise_code`, `name`, `type`, `status`, `revision`, `creator`, `create_time`,
     `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT pending_company.`owner_company_id`,
         CONCAT('HIST-REG-OWN-', pending_company.`tenant_id`, '-', pending_company.`owner_company_id`),
         CONCAT('历史注册证公司-', pending_company.`owner_company_id`),
         'OWNED_COMPANY',
         'ENABLE',
         1,
         'dcc-reg-cert-company-backfill',
         NOW(),
         'dcc-reg-cert-company-backfill',
         NOW(),
         b'0',
         pending_company.`tenant_id`
    FROM tmp_dcc_reg_cert_owner_company_backfill AS pending_company;

  SET inserted_count = ROW_COUNT();

  IF inserted_count <> expected_count THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Registration certificate owner company backfill count mismatch';
  END IF;

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
      SET MESSAGE_TEXT = 'Registration certificate owner company backfill count mismatch';
  END IF;

  COMMIT;

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_reg_cert_owner_company_backfill;
END$$
DELIMITER ;

CALL ensure_dcc_reg_cert_associated_company_backfill();
DROP PROCEDURE IF EXISTS ensure_dcc_reg_cert_associated_company_backfill;
