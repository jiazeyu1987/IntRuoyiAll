-- release-target-preflight: migrationId=20260830_dcc_registration_certificate_notification_role_scope_backfill; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND LOWER(table_name) IN ('dcc_registration_certificate','infra_job','mdm_enterprise','mdm_role_company_scope','system_role')) = 5
THEN 'TARGET_PREFLIGHT_PASS:20260830_dcc_registration_certificate_notification_role_scope_backfill' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260830_dcc_registration_certificate_notification_role_scope_backfill' END;
