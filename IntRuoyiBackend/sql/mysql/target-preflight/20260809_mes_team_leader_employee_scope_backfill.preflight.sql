-- release-target-preflight: migrationId=20260809_mes_team_leader_employee_scope_backfill; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND LOWER(table_name) IN ('mes_pro_process_pool_team_employee_profile','mes_pro_process_pool_team_leader_scope')) = 2
THEN 'TARGET_PREFLIGHT_PASS:20260809_mes_team_leader_employee_scope_backfill' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260809_mes_team_leader_employee_scope_backfill' END;
