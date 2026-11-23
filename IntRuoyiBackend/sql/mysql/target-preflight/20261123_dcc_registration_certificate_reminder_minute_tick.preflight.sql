-- release-target-preflight: migrationId=20261123_dcc_registration_certificate_reminder_minute_tick; allowedEnvironments=test,backup,prod
-- Match the immutable migration's exact target-row assertion; preserve enabled/paused status.
SELECT CASE
  WHEN (SELECT COUNT(*) FROM infra_job
         WHERE handler_name='registrationCertificateReminderDailyJob' AND deleted=b'0') = 1
  THEN 'TARGET_PREFLIGHT_PASS:20261123_dcc_registration_certificate_reminder_minute_tick'
  ELSE 'TARGET_PREFLIGHT_BLOCKED:20261123_dcc_registration_certificate_reminder_minute_tick'
END;
