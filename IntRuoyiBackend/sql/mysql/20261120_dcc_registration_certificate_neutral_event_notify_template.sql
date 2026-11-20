-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260901_dcc_registration_certificate_business_event_notify_template; type=seed; riskLevel=low
-- Purpose: Update the existing shared business-event template to certificate-type-neutral wording for domestic, CE and FDA.
-- Existing rows are updated by code; this is not limited to fresh initialization.
-- Apply before starting application instances so their notify-template caches load the new content.
-- Previously delivered inbox messages retain their historical content.
-- Recovery: Re-run this idempotent migration after restoring the prior template row if the update is interrupted.
-- Rollback: Restore the previous system_notify_template row for code DCC_REGISTRATION_CERTIFICATE_BUSINESS_EVENT from backup if business users require the prior wording.

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_dcc_reg_cert_event_notice_20261120;
DELIMITER $$
CREATE PROCEDURE ensure_dcc_reg_cert_event_notice_20261120()
BEGIN
  DECLARE active_template_count INT DEFAULT 0;

  IF NOT EXISTS (
      SELECT 1
        FROM information_schema.TABLES
       WHERE TABLE_SCHEMA = DATABASE()
         AND TABLE_NAME = 'system_notify_template'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing system_notify_template for registration certificate notification template';
  END IF;

  SELECT COUNT(*)
    INTO active_template_count
    FROM `system_notify_template`
   WHERE `code` = 'DCC_REGISTRATION_CERTIFICATE_BUSINESS_EVENT'
     AND `deleted` = b'0';

  IF active_template_count <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Expected one existing active registration certificate notification template';
  END IF;

  UPDATE `system_notify_template`
     SET `name` = '注册证业务通知',
         `content` = '产品《{productName}》的注册证{eventTitle}。注册证号：{certificateNo}，生效日期：{effectiveDate}，有效期至：{expiryDate}。'
   WHERE `code` = 'DCC_REGISTRATION_CERTIFICATE_BUSINESS_EVENT'
     AND `deleted` = b'0';

  IF NOT EXISTS (
      SELECT 1
        FROM `system_notify_template`
       WHERE `code` = 'DCC_REGISTRATION_CERTIFICATE_BUSINESS_EVENT'
         AND `deleted` = b'0'
         AND `content` = '产品《{productName}》的注册证{eventTitle}。注册证号：{certificateNo}，生效日期：{effectiveDate}，有效期至：{expiryDate}。'
         AND `name` = '注册证业务通知'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Registration certificate notification template update failed';
  END IF;
END $$
DELIMITER ;

CALL ensure_dcc_reg_cert_event_notice_20261120();

DROP PROCEDURE IF EXISTS ensure_dcc_reg_cert_event_notice_20261120;
