-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260818_dcc_registration_certificate_reminder; type=seed; riskLevel=low
-- Purpose: Make domestic registration certificate business event inbox notifications readable with product name and certificate validity dates.
-- Recovery: Re-run this idempotent migration after restoring the prior template row if the update is interrupted.
-- Rollback: Restore the previous system_notify_template row for code DCC_REGISTRATION_CERTIFICATE_BUSINESS_EVENT from backup if business users require the prior wording.

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_dcc_registration_certificate_business_event_notify_template_20260901;
DELIMITER $$
CREATE PROCEDURE ensure_dcc_registration_certificate_business_event_notify_template_20260901()
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

  IF active_template_count > 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Duplicate active registration certificate notification template';
  END IF;

  IF active_template_count = 0 THEN
    INSERT INTO `system_notify_template`
      (`name`, `code`, `type`, `nickname`, `content`, `params`, `status`, `remark`,
       `creator`, `create_time`, `updater`, `update_time`, `deleted`)
    VALUES
      ('国内注册证业务通知', 'DCC_REGISTRATION_CERTIFICATE_BUSINESS_EVENT', 2, 'DCC系统',
       '产品《{productName}》的国内注册证{eventTitle}。注册证号：{certificateNo}，生效日期：{effectiveDate}，有效期至：{expiryDate}。',
       '["eventTitle","productName","certificateNo","effectiveDate","expiryDate"]',
       0, '国内注册证业务通知可读化', '1', NOW(), '1', NOW(), b'0');
  ELSE
    UPDATE `system_notify_template`
       SET `name` = '国内注册证业务通知',
           `type` = 2,
           `nickname` = 'DCC系统',
           `content` = '产品《{productName}》的国内注册证{eventTitle}。注册证号：{certificateNo}，生效日期：{effectiveDate}，有效期至：{expiryDate}。',
           `params` = '["eventTitle","productName","certificateNo","effectiveDate","expiryDate"]',
           `status` = 0,
           `remark` = '国内注册证业务通知可读化',
           `updater` = '1',
           `update_time` = NOW(),
           `deleted` = b'0'
     WHERE `code` = 'DCC_REGISTRATION_CERTIFICATE_BUSINESS_EVENT'
       AND `deleted` = b'0';
  END IF;

  IF NOT EXISTS (
      SELECT 1
        FROM `system_notify_template`
       WHERE `code` = 'DCC_REGISTRATION_CERTIFICATE_BUSINESS_EVENT'
         AND `deleted` = b'0'
         AND `content` = '产品《{productName}》的国内注册证{eventTitle}。注册证号：{certificateNo}，生效日期：{effectiveDate}，有效期至：{expiryDate}。'
         AND `params` = '["eventTitle","productName","certificateNo","effectiveDate","expiryDate"]'
         AND `status` = 0
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Registration certificate notification template update failed';
  END IF;
END $$
DELIMITER ;

CALL ensure_dcc_registration_certificate_business_event_notify_template_20260901();

DROP PROCEDURE IF EXISTS ensure_dcc_registration_certificate_business_event_notify_template_20260901;
