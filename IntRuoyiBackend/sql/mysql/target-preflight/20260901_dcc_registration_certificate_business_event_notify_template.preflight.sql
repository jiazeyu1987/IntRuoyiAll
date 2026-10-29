-- release-target-preflight: migrationId=20260901_dcc_registration_certificate_business_event_notify_template; allowedEnvironments=test,backup,prod
-- Reject ambiguous active template selection; customized existing text is preserved by this seed.
SELECT CASE WHEN
  (SELECT COUNT(*) FROM system_notify_template WHERE code='DCC_REGISTRATION_CERTIFICATE_BUSINESS_EVENT' AND deleted=b'0')<=1

THEN 'TARGET_PREFLIGHT_PASS:20260901_dcc_registration_certificate_business_event_notify_template' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260901_dcc_registration_certificate_business_event_notify_template' END;
