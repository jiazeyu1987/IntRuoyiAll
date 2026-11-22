-- release-target-preflight: migrationId=20261122_dcc_registration_certificate_event_template_params; allowedEnvironments=test,backup,prod
SELECT CASE
  WHEN (SELECT COUNT(*) FROM system_notify_template
    WHERE code='DCC_REGISTRATION_CERTIFICATE_BUSINESS_EVENT' AND deleted=b'0') <> 1
  THEN 'TARGET_PREFLIGHT_BLOCKED:20261122_dcc_registration_certificate_event_template_params:SOURCE_TEMPLATE_IDENTITY_CONFLICT'
  WHEN NOT EXISTS (SELECT 1 FROM system_tenant_notify_template
    WHERE code='DCC_REGISTRATION_CERTIFICATE_BUSINESS_EVENT' AND deleted=b'0')
  THEN 'TARGET_PREFLIGHT_BLOCKED:20261122_dcc_registration_certificate_event_template_params:TENANT_TEMPLATES_MISSING'
  WHEN EXISTS (SELECT tenant_id FROM system_tenant_notify_template
    WHERE code='DCC_REGISTRATION_CERTIFICATE_BUSINESS_EVENT' AND deleted=b'0'
    GROUP BY tenant_id HAVING COUNT(*) > 1)
  THEN 'TARGET_PREFLIGHT_BLOCKED:20261122_dcc_registration_certificate_event_template_params:TENANT_TEMPLATE_IDENTITY_CONFLICT'
  ELSE 'TARGET_PREFLIGHT_PASS:20261122_dcc_registration_certificate_event_template_params'
END;
