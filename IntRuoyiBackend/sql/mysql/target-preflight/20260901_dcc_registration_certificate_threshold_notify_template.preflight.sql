-- release-target-preflight: migrationId=20260901_dcc_registration_certificate_threshold_notify_template; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND LOWER(table_name) IN ('system_notify_template')) = 1
  AND (SELECT COUNT(*) FROM system_notify_template WHERE code = 'DCC_REGISTRATION_CERTIFICATE_THRESHOLD_REMINDER' AND deleted = b'0') <= 1
THEN 'TARGET_PREFLIGHT_PASS:20260901_dcc_registration_certificate_threshold_notify_template' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260901_dcc_registration_certificate_threshold_notify_template' END;
