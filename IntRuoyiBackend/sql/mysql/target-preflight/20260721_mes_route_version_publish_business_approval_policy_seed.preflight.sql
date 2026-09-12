-- release-target-preflight: migrationId=20260721_mes_route_version_publish_business_approval_policy_seed; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND LOWER(table_name) IN ('bpm_business_approval_policy','system_tenant')) = 2
THEN 'TARGET_PREFLIGHT_PASS:20260721_mes_route_version_publish_business_approval_policy_seed' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260721_mes_route_version_publish_business_approval_policy_seed' END;
