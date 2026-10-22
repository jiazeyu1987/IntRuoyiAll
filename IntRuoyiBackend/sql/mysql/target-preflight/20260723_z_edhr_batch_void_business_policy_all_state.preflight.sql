-- release-target-preflight: migrationId=20260723_z_edhr_batch_void_business_policy_all_state; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND LOWER(table_name) IN ('bpm_business_approval_policy','act_re_procdef')) = 2
THEN 'TARGET_PREFLIGHT_PASS:20260723_z_edhr_batch_void_business_policy_all_state' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260723_z_edhr_batch_void_business_policy_all_state' END;
