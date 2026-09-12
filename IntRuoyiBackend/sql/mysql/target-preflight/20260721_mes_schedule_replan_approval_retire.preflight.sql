-- release-target-preflight: migrationId=20260721_mes_schedule_replan_approval_retire; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND LOWER(table_name) IN ('bpm_business_approval_policy')) = 1
THEN 'TARGET_PREFLIGHT_PASS:20260721_mes_schedule_replan_approval_retire' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260721_mes_schedule_replan_approval_retire' END;
