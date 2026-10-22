-- release-target-preflight: migrationId=20260818_dcc_registration_certificate_reminder; allowedEnvironments=test,backup,prod
SELECT CASE
  WHEN (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME IN (
    'dcc_registration_certificate_reminder_config',
    'dcc_registration_certificate_daily_run',
    'dcc_registration_certificate_reminder_occurrence',
    'dcc_registration_certificate_reminder_delivery'
  )) IN (0, 4)
   AND EXISTS (
     SELECT 1 FROM information_schema.TABLES
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME IN (
          'dcc_registration_certificate_lifecycle_event',
          'dcc_registration_certificate_activation_replay',
          'dcc_registration_certificate_supporting_document',
          'dcc_registration_certificate_change',
          'dcc_registration_certificate_change_item',
          'infra_job'
        )
     GROUP BY TABLE_SCHEMA
     HAVING COUNT(*) = 6
   )
   AND NOT EXISTS (
     SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'dcc_registration_certificate_reminder_config'
        AND COLUMN_NAME = 'threshold_recipient_user_ids_json'
   )
  THEN 'TARGET_PREFLIGHT_PASS:20260818_dcc_registration_certificate_reminder'
  WHEN (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME IN (
    'dcc_registration_certificate_reminder_config',
    'dcc_registration_certificate_daily_run',
    'dcc_registration_certificate_reminder_occurrence',
    'dcc_registration_certificate_reminder_delivery'
  )) = 4
   AND EXISTS (
     SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'dcc_registration_certificate_reminder_config'
        AND COLUMN_NAME = 'threshold_recipient_user_ids_json'
   )
  THEN 'TARGET_PREFLIGHT_BLOCKED:20260818_dcc_registration_certificate_reminder_ALREADY_EXTENDED'
  ELSE 'TARGET_PREFLIGHT_BLOCKED:20260818_dcc_registration_certificate_reminder'
END;
