-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260818_dcc_registration_certificate_reminder; type=data; riskLevel=medium
-- Registration reminder dispatch checks each tenant's configured Beijing daily_run_time.
-- Keep existing enabled/paused status and all other jobs unchanged. Quartz startup sync loads this cron.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS migrate_reg_reminder_minute_tick_20261123;
DELIMITER $$
CREATE PROCEDURE migrate_reg_reminder_minute_tick_20261123()
BEGIN
  IF (SELECT COUNT(*) FROM infra_job WHERE handler_name='registrationCertificateReminderDailyJob' AND deleted=b'0') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Expected exactly one registrationCertificateReminderDailyJob';
  END IF;
  UPDATE infra_job
     SET cron_expression='0 * * * * ?', update_time=CURRENT_TIMESTAMP
   WHERE handler_name='registrationCertificateReminderDailyJob' AND deleted=b'0';
END$$
DELIMITER ;
CALL migrate_reg_reminder_minute_tick_20261123();
DROP PROCEDURE migrate_reg_reminder_minute_tick_20261123;
