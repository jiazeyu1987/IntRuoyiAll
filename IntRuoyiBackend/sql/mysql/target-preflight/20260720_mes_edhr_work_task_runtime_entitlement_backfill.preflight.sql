-- release-target-preflight: migrationId=20260720_mes_edhr_work_task_runtime_entitlement_backfill; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND LOWER(table_name) IN ('mes_pro_edhr_work_task','system_entitlement_claim','system_entitlement_policy','system_entitlement_grant','system_entitlement_audit_event','system_menu','system_users')) = 7
THEN 'TARGET_PREFLIGHT_PASS:20260720_mes_edhr_work_task_runtime_entitlement_backfill' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260720_mes_edhr_work_task_runtime_entitlement_backfill' END;
