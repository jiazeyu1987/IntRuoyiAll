-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261122_dcc_registration_certificate_event_template_params; type=seed; riskLevel=low
-- Render the actual domestic/CE/FDA certificate type supplied by the application.
-- Deploy together with the notifier providing certificateTypeName; old application instances cannot send this template.
-- Preserve status, sender, category and delivered message snapshots.
-- Recovery: restore name/content/params from the exact pre-migration template rows in both tables.
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_dcc_reg_cert_event_type_20261126;
DELIMITER $$
CREATE PROCEDURE ensure_dcc_reg_cert_event_type_20261126()
BEGIN
  DECLARE active_template_count INT DEFAULT 0;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;
  IF NOT EXISTS (SELECT 1 FROM information_schema.TABLES
                  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'system_notify_template') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing system_notify_template for registration certificate notification';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.TABLES
                  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'system_tenant_notify_template') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing tenant registration certificate notification templates';
  END IF;
  SELECT COUNT(*) INTO active_template_count FROM system_notify_template
    WHERE code = 'DCC_REGISTRATION_CERTIFICATE_BUSINESS_EVENT' AND deleted = b'0';
  IF active_template_count <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Expected one active registration certificate notification template';
  END IF;
  SELECT COUNT(*) INTO active_template_count FROM system_tenant_notify_template
    WHERE code = 'DCC_REGISTRATION_CERTIFICATE_BUSINESS_EVENT' AND deleted = b'0';
  IF active_template_count = 0 OR EXISTS (
      SELECT tenant_id FROM system_tenant_notify_template
       WHERE code = 'DCC_REGISTRATION_CERTIFICATE_BUSINESS_EVENT' AND deleted = b'0'
       GROUP BY tenant_id HAVING COUNT(*) > 1) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing or ambiguous tenant registration certificate notification templates';
  END IF;
  START TRANSACTION;
  UPDATE system_notify_template
     SET name = '注册证业务通知',
         content = '产品《{productName}》的{certificateTypeName}{eventTitle}。注册证号：{certificateNo}，生效日期：{effectiveDate}，有效期至：{expiryDate}。',
         params = JSON_ARRAY('productName', 'certificateTypeName', 'eventTitle', 'certificateNo', 'effectiveDate', 'expiryDate')
   WHERE code = 'DCC_REGISTRATION_CERTIFICATE_BUSINESS_EVENT' AND deleted = b'0';
  UPDATE system_tenant_notify_template
     SET name = '注册证业务通知',
         content = '产品《{productName}》的{certificateTypeName}{eventTitle}。注册证号：{certificateNo}，生效日期：{effectiveDate}，有效期至：{expiryDate}。',
         params = JSON_ARRAY('productName', 'certificateTypeName', 'eventTitle', 'certificateNo', 'effectiveDate', 'expiryDate')
   WHERE code = 'DCC_REGISTRATION_CERTIFICATE_BUSINESS_EVENT' AND deleted = b'0';
  IF NOT EXISTS (SELECT 1 FROM system_notify_template
                  WHERE code = 'DCC_REGISTRATION_CERTIFICATE_BUSINESS_EVENT' AND deleted = b'0'
                    AND name = '注册证业务通知'
                    AND content = '产品《{productName}》的{certificateTypeName}{eventTitle}。注册证号：{certificateNo}，生效日期：{effectiveDate}，有效期至：{expiryDate}。'
                    AND JSON_VALID(params)
                    AND CAST(params AS JSON) = JSON_ARRAY('productName', 'certificateTypeName', 'eventTitle', 'certificateNo', 'effectiveDate', 'expiryDate')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Registration certificate template content/parameter verification failed';
  END IF;
  IF EXISTS (SELECT 1 FROM system_tenant_notify_template
              WHERE code = 'DCC_REGISTRATION_CERTIFICATE_BUSINESS_EVENT' AND deleted = b'0'
                AND NOT (name = '注册证业务通知'
                  AND content = '产品《{productName}》的{certificateTypeName}{eventTitle}。注册证号：{certificateNo}，生效日期：{effectiveDate}，有效期至：{expiryDate}。'
                  AND JSON_VALID(params)
                  AND CAST(params AS JSON) = JSON_ARRAY('productName', 'certificateTypeName', 'eventTitle', 'certificateNo', 'effectiveDate', 'expiryDate'))) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Tenant registration certificate template verification failed';
  END IF;
  COMMIT;
END $$
DELIMITER ;
CALL ensure_dcc_reg_cert_event_type_20261126();
DROP PROCEDURE IF EXISTS ensure_dcc_reg_cert_event_type_20261126;
