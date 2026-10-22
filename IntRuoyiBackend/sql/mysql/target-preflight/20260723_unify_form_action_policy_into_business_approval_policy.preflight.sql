-- release-target-preflight: migrationId=20260723_unify_form_action_policy_into_business_approval_policy; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND LOWER(table_name) IN ('bpm_form_action_policy','bpm_business_approval_policy','bpm_form_action_instance')) = 3
THEN 'TARGET_PREFLIGHT_PASS:20260723_unify_form_action_policy_into_business_approval_policy' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260723_unify_form_action_policy_into_business_approval_policy' END;
