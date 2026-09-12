-- release-target-preflight: migrationId=20260830_dcc_registration_certificate_associated_company_backfill; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND LOWER(table_name) IN ('dcc_registration_certificate','mdm_enterprise')) = 2
THEN 'TARGET_PREFLIGHT_PASS:20260830_dcc_registration_certificate_associated_company_backfill' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260830_dcc_registration_certificate_associated_company_backfill' END;
