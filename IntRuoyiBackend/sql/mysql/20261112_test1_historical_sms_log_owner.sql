-- release-migration: allowedEnvironments=test; dependsOn=; type=data; riskLevel=medium
-- User-confirmed attribution on 2026-11-12: the 396 historical SMS logs for missing user 113 belong to 测试1.
-- Run before retrying 20261112_tenant_notification_resources.sql. Never change the source logs.
-- Recovery: before copying notification resources, remove only the owner rows inserted by this execution.
-- After copying resources, retain the attribution and review any ownership correction with the copied logs.
SET NAMES utf8mb4;
CREATE TABLE IF NOT EXISTS system_tenant_message_log_owner (
  log_kind varchar(8) NOT NULL, log_id bigint NOT NULL, tenant_id bigint NOT NULL,
  PRIMARY KEY (log_kind,log_id)
) ENGINE=InnoDB;
DROP PROCEDURE IF EXISTS assign_test1_historical_sms_owner;
DELIMITER $$
CREATE PROCEDURE assign_test1_historical_sms_owner()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
  START TRANSACTION;
  IF NOT EXISTS (SELECT 1 FROM system_tenant WHERE id=186 AND name='测试1'
      AND status=0 AND deleted=0)
      OR EXISTS (SELECT 1 FROM system_users WHERE id=113)
      OR (SELECT COUNT(*) FROM system_sms_log
          WHERE user_id=113 AND user_type=2 AND id BETWEEN 1659 AND 2280)<>396 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='TEST1_HISTORICAL_SMS_CONFIRMED_SCOPE_CHANGED';
  END IF;
  IF EXISTS (SELECT 1 FROM system_sms_log l JOIN system_tenant_message_log_owner o
      ON o.log_kind='sms' AND o.log_id=l.id
      WHERE l.user_id=113 AND l.user_type=2 AND l.id BETWEEN 1659 AND 2280 AND o.tenant_id<>186) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='TEST1_HISTORICAL_SMS_OWNER_CONFLICT';
  END IF;
  INSERT INTO system_tenant_message_log_owner (log_kind,log_id,tenant_id)
    SELECT 'sms',l.id,186 FROM system_sms_log l
    WHERE l.user_id=113 AND l.user_type=2 AND l.id BETWEEN 1659 AND 2280
      AND NOT EXISTS (SELECT 1 FROM system_tenant_message_log_owner o
        WHERE o.log_kind='sms' AND o.log_id=l.id);
  IF (SELECT COUNT(*) FROM system_sms_log l JOIN system_tenant_message_log_owner o
      ON o.log_kind='sms' AND o.log_id=l.id AND o.tenant_id=186
      WHERE l.user_id=113 AND l.user_type=2 AND l.id BETWEEN 1659 AND 2280)<>396 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='TEST1_HISTORICAL_SMS_OWNER_VERIFICATION_FAILED';
  END IF;
  COMMIT;
END$$
DELIMITER ;
CALL assign_test1_historical_sms_owner();
DROP PROCEDURE assign_test1_historical_sms_owner;
