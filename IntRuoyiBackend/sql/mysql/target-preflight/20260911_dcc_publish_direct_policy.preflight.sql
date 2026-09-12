-- release-target-preflight: migrationId=20260911_dcc_publish_direct_policy; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND LOWER(table_name) IN ('bpm_business_approval_policy')) = 1
THEN 'TARGET_PREFLIGHT_PASS:20260911_dcc_publish_direct_policy' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260911_dcc_publish_direct_policy' END;
