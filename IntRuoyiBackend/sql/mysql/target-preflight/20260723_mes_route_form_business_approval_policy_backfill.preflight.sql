-- release-target-preflight: migrationId=20260723_mes_route_form_business_approval_policy_backfill; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND LOWER(table_name) IN ('bpm_form_action_policy','bpm_business_approval_policy')) = 2
THEN 'TARGET_PREFLIGHT_PASS:20260723_mes_route_form_business_approval_policy_backfill' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260723_mes_route_form_business_approval_policy_backfill' END;
