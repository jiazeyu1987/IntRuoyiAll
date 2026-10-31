-- release-target-preflight: migrationId=20260803_mes_frontline_pressure_pump_all_process_permission; allowedEnvironments=test,prod,backup
-- Read-only original prerequisite/identity checks; no menu resurrection, grants, or fixture creation.
SELECT CASE WHEN
 NOT COALESCE((NOT EXISTS (
      SELECT 1
      FROM `system_menu`
      WHERE `id` = 5550
        AND `deleted` = b'0'
  )),TRUE)
 AND NOT COALESCE((EXISTS (
      SELECT 1
      FROM `system_menu`
      WHERE `id` = 900450
        AND `deleted` = b'0'
        AND NOT (
          `parent_id` = 5550
          AND `permission` = 'mes:pro-feedback:frontline-pressure-pump:all-processes'
        )
  )),TRUE)
 AND NOT COALESCE((EXISTS (
      SELECT 1
      FROM `system_menu`
      WHERE `parent_id` = 5550
        AND `permission` = 'mes:pro-feedback:frontline-pressure-pump:all-processes'
        AND `deleted` = b'0'
        AND `id` <> 900450
  )),TRUE)
 AND NOT COALESCE((EXISTS (
      SELECT 1
      FROM `system_tenant_package`
      WHERE `deleted` = b'0'
        AND NOT JSON_VALID(`menu_ids`)
  )),TRUE)
 AND NOT EXISTS(SELECT 1 FROM information_schema.routines WHERE routine_schema=DATABASE() AND routine_name='ensure_mes_frontline_pressure_pump_permission')
 THEN 'TARGET_PREFLIGHT_PASS:20260803_mes_frontline_pressure_pump_all_process_permission' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260803_mes_frontline_pressure_pump_all_process_permission:MENU_ROLE_PREREQUISITE_OR_IDENTITY_CONFLICT' END;
