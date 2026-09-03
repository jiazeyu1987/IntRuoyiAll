-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260718_system_entitlement_management,20260818_dcc_registration_certificate_reminder,20260816_dcc_registration_certificate_menu,20260829_dcc_registration_certificate_upload_approver_role; type=schema; riskLevel=medium
-- Purpose: Persist per-threshold registration-certificate reminder recipients and grant their read entitlement.

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_dcc_reg_cert_threshold_recipients_20260903;
DELIMITER //
CREATE PROCEDURE ensure_dcc_reg_cert_threshold_recipients_20260903()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.TABLES
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'dcc_registration_certificate_reminder_config'
  ) OR NOT EXISTS (
    SELECT 1 FROM information_schema.TABLES
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'system_entitlement_policy'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing registration certificate reminder or entitlement schema';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'dcc_registration_certificate_reminder_config'
       AND COLUMN_NAME = 'threshold_recipient_user_ids_json'
  ) THEN
    ALTER TABLE `dcc_registration_certificate_reminder_config`
      ADD COLUMN `threshold_recipient_user_ids_json` json NOT NULL
        DEFAULT (JSON_OBJECT()) COMMENT 'Recipient user ids keyed by T_30/T_8/T_2/T_1'
        AFTER `threshold_days_json`;
  END IF;

  IF (SELECT COUNT(DISTINCT `permission`) FROM `system_menu`
       WHERE `permission` IN (
         'dcc:registration-certificate:query-current',
         'dcc:registration-certificate:config:query',
         'dcc:registration-certificate:config:update'
       )
       AND `status` = 0
       AND `deleted` = b'0') <> 3 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing registration certificate notification-setting permission menu';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM `system_role`
     WHERE `code` = 'dcc_registration_certificate_approver'
       AND `status` = 0
       AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing enabled registration department manager role';
  END IF;
END//
DELIMITER ;

CALL ensure_dcc_reg_cert_threshold_recipients_20260903();
DROP PROCEDURE IF EXISTS ensure_dcc_reg_cert_threshold_recipients_20260903;

START TRANSACTION;

INSERT INTO `system_entitlement_policy`
(`policy_code`, `policy_name`, `module_code`, `status`, `description`,
 `allowed_permission_codes_json`, `allowed_menu_refs_json`, `forbidden_permission_codes_json`,
 `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT
  'DCC_REGISTRATION_CERTIFICATE_REMINDER_VIEW',
  '注册证到期通知接收人查看权限',
  'dcc',
  0,
  'Source: DCC_REGISTRATION_CERTIFICATE_REMINDER_RECIPIENT',
  JSON_ARRAY('dcc:registration-certificate:query-current'),
  JSON_ARRAY(JSON_OBJECT('permission', 'dcc:registration-certificate:query-current')),
  JSON_ARRAY(),
  'dcc-reg-cert-threshold-recipients', NOW(),
  'dcc-reg-cert-threshold-recipients', NOW(), b'0'
WHERE NOT EXISTS (
  SELECT 1 FROM `system_entitlement_policy`
   WHERE `policy_code` = 'DCC_REGISTRATION_CERTIFICATE_REMINDER_VIEW'
);

UPDATE `system_entitlement_policy`
   SET `policy_name` = '注册证到期通知接收人查看权限',
       `module_code` = 'dcc',
       `status` = 0,
       `description` = 'Source: DCC_REGISTRATION_CERTIFICATE_REMINDER_RECIPIENT',
       `allowed_permission_codes_json` = JSON_ARRAY('dcc:registration-certificate:query-current'),
       `allowed_menu_refs_json` = JSON_ARRAY(
         JSON_OBJECT('permission', 'dcc:registration-certificate:query-current')),
       `forbidden_permission_codes_json` = JSON_ARRAY(),
       `updater` = 'dcc-reg-cert-threshold-recipients',
       `update_time` = NOW(),
       `deleted` = b'0'
 WHERE `policy_code` = 'DCC_REGISTRATION_CERTIFICATE_REMINDER_VIEW';

UPDATE `system_role_menu` AS `role_menu`
JOIN `system_role` AS `role`
  ON `role`.`id` = `role_menu`.`role_id`
 AND `role`.`tenant_id` = `role_menu`.`tenant_id`
 AND `role`.`code` = 'dcc_registration_certificate_approver'
 AND `role`.`status` = 0
 AND `role`.`deleted` = b'0'
JOIN `system_menu` AS `menu`
  ON `menu`.`id` = `role_menu`.`menu_id`
 AND `menu`.`permission` IN (
   'dcc:registration-certificate:query-current',
   'dcc:registration-certificate:config:query',
   'dcc:registration-certificate:config:update'
 )
 AND `menu`.`status` = 0
 AND `menu`.`deleted` = b'0'
   SET `role_menu`.`deleted` = b'0',
       `role_menu`.`updater` = 'dcc-reg-cert-threshold-recipients',
       `role_menu`.`update_time` = NOW();

INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT `role`.`id`, `menu`.`id`,
       'dcc-reg-cert-threshold-recipients', NOW(),
       'dcc-reg-cert-threshold-recipients', NOW(), b'0', `role`.`tenant_id`
  FROM `system_role` AS `role`
  JOIN `system_menu` AS `menu`
    ON `menu`.`permission` IN (
      'dcc:registration-certificate:query-current',
      'dcc:registration-certificate:config:query',
      'dcc:registration-certificate:config:update'
    )
   AND `menu`.`status` = 0
   AND `menu`.`deleted` = b'0'
 WHERE `role`.`code` = 'dcc_registration_certificate_approver'
   AND `role`.`status` = 0
   AND `role`.`deleted` = b'0'
   AND NOT EXISTS (
     SELECT 1 FROM `system_role_menu` AS `existing`
      WHERE `existing`.`tenant_id` = `role`.`tenant_id`
        AND `existing`.`role_id` = `role`.`id`
        AND `existing`.`menu_id` = `menu`.`id`
   );

UPDATE `infra_job`
   SET `handler_param` = JSON_OBJECT('actorId', 1),
       `updater` = 'dcc-reg-cert-threshold-recipients',
       `update_time` = NOW()
 WHERE `handler_name` = 'registrationCertificateReminderDailyJob'
   AND `deleted` = b'0';

COMMIT;
