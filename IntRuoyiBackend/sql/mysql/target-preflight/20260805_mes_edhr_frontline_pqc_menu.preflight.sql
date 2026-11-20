-- release-target-preflight: migrationId=20260805_mes_edhr_frontline_pqc_menu; allowedEnvironments=test,prod,backup
-- Read-only original prerequisite/identity checks; no menu resurrection, grants, or fixture creation.
SELECT CASE WHEN
 NOT COALESCE((EXISTS (
    SELECT 1
    FROM `system_tenant_package`
    WHERE `menu_ids` IS NOT NULL
      AND `menu_ids` <> ''
      AND NOT JSON_VALID(`menu_ids`)
  )),TRUE)
 AND NOT COALESCE((NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 900220
      AND `deleted` = b'0'
  )),TRUE)
 AND NOT COALESCE(((SELECT COUNT(*)
      FROM `system_menu`
      WHERE `deleted` = b'0'
        AND `id` IN (900033, 900025, 900432)) <> 3),TRUE)
 AND NOT COALESCE((EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 900438
      AND `deleted` = b'0'
      AND `path` <> '/mes/pro/feedback/edhr-batch-pqc-fill'
  )),TRUE)
 AND NOT COALESCE((EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `path` = '/mes/pro/feedback/edhr-batch-pqc-fill'
      AND `id` <> 900438
      AND `deleted` = b'0'
  )),TRUE)
 AND NOT EXISTS(SELECT 1 FROM information_schema.routines WHERE routine_schema=DATABASE() AND routine_name='ensure_mes_edhr_frontline_pqc_menu')
 THEN 'TARGET_PREFLIGHT_PASS:20260805_mes_edhr_frontline_pqc_menu' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260805_mes_edhr_frontline_pqc_menu:MENU_ROLE_PREREQUISITE_OR_IDENTITY_CONFLICT' END;
