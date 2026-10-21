-- release-target-preflight: migrationId=20260730_mes_process_readonly_catalog_menu; allowedEnvironments=test,prod,backup
-- Read-only original prerequisite/identity checks; no menu resurrection, grants, or fixture creation.
SELECT CASE WHEN
 NOT COALESCE(((
    SELECT COUNT(*)
    FROM `system_menu`
    WHERE `id` IN (5700, 5710, 5720)
      AND `deleted` = b'0'
  ) <> 3),TRUE)
 AND NOT COALESCE((EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 5718
      AND (`parent_id` <> 5700 OR `path` <> 'mes-process' OR `component` <> 'mes/pro/mes-process/index')
  )),TRUE)
 AND NOT COALESCE((EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 5719
      AND (`parent_id` <> 5718 OR `permission` <> 'mes:pro-mes-process:query')
  )),TRUE)
 AND NOT EXISTS(SELECT 1 FROM information_schema.routines WHERE routine_schema=DATABASE() AND routine_name='ensure_mes_process_readonly_catalog_menu')
 THEN 'TARGET_PREFLIGHT_PASS:20260730_mes_process_readonly_catalog_menu' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260730_mes_process_readonly_catalog_menu:MENU_ROLE_PREREQUISITE_OR_IDENTITY_CONFLICT' END;
