-- release-target-preflight: migrationId=20261120_dcc_registration_certificate_neutral_event_notify_template; allowedEnvironments=test,backup,prod
SELECT CASE WHEN (SELECT COUNT(*) FROM system_notify_template
  WHERE code='DCC_REGISTRATION_CERTIFICATE_BUSINESS_EVENT' AND deleted=b'0')=1
THEN 'TARGET_PREFLIGHT_PASS:20261120_dcc_registration_certificate_neutral_event_notify_template'
ELSE 'TARGET_PREFLIGHT_BLOCKED:20261120_dcc_registration_certificate_neutral_event_notify_template:TEMPLATE_IDENTITY_CONFLICT' END;
