-- release-target-preflight: migrationId=20260715_mes_schedule_capacity_mode_unification; allowedEnvironments=test,backup,prod
-- Accept the explicit legacy mode only when its conversion has a positive capacity.
SELECT CASE WHEN
  NOT EXISTS (SELECT 1 FROM mes_pro_route_schedule_config WHERE deleted=b'0' AND (
    capacity_mode IS NULL OR capacity_mode NOT IN ('FINITE_HOURLY','RESOURCE_CALCULATED','MANUAL_OVERRIDE','INFINITE_FORMULA')
    OR (capacity_mode IN ('FINITE_HOURLY','MANUAL_OVERRIDE') AND (hourly_capacity IS NULL OR hourly_capacity<=0))
  ))
  AND NOT EXISTS (
    SELECT machinery_id,process_id,tenant_id FROM mes_dv_machinery_process
    WHERE deleted=b'0' GROUP BY machinery_id,process_id,tenant_id HAVING COUNT(*)>1
  )
  THEN 'TARGET_PREFLIGHT_PASS:20260715_mes_schedule_capacity_mode_unification'
  ELSE 'TARGET_PREFLIGHT_BLOCKED:capacity-mode-or-machinery-process-conflict' END;
