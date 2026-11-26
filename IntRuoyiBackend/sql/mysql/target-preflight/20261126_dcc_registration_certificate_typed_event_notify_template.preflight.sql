-- release-target-preflight: migrationId=20261126_dcc_registration_certificate_typed_event_notify_template; allowedEnvironments=test,backup,prod
-- Match the immutable migration's shared and per-tenant identity assertions before changing template content.
SELECT CASE
  WHEN (SELECT COUNT(*) FROM system_notify_template
      WHERE code='DCC_REGISTRATION_CERTIFICATE_BUSINESS_EVENT' AND deleted=b'0')<>1
    THEN 'TARGET_PREFLIGHT_BLOCKED:20261126_dcc_registration_certificate_typed_event_notify_template:SYSTEM_TEMPLATE_IDENTITY'
  WHEN NOT EXISTS (SELECT 1 FROM system_tenant_notify_template
      WHERE code='DCC_REGISTRATION_CERTIFICATE_BUSINESS_EVENT' AND deleted=b'0')
    THEN 'TARGET_PREFLIGHT_BLOCKED:20261126_dcc_registration_certificate_typed_event_notify_template:TENANT_TEMPLATES_MISSING'
  WHEN EXISTS (SELECT tenant_id FROM system_tenant_notify_template
      WHERE code='DCC_REGISTRATION_CERTIFICATE_BUSINESS_EVENT' AND deleted=b'0'
      GROUP BY tenant_id HAVING COUNT(*)>1)
    THEN 'TARGET_PREFLIGHT_BLOCKED:20261126_dcc_registration_certificate_typed_event_notify_template:TENANT_TEMPLATE_IDENTITY'
  ELSE 'TARGET_PREFLIGHT_PASS:20261126_dcc_registration_certificate_typed_event_notify_template'
END;
