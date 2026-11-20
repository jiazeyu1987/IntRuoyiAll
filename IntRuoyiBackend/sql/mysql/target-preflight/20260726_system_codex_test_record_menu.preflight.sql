-- release-target-preflight: migrationId=20260726_system_codex_test_record_menu; allowedEnvironments=test,prod,backup
-- Read-only original prerequisite/identity checks; no menu resurrection, grants, or fixture creation.
SELECT CASE WHEN
 NOT COALESCE((NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 1
      AND `deleted` = b'0'
  )),TRUE)
 AND NOT COALESCE((EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 901090
      AND (
        `deleted` <> b'0'
        OR `path` <> 'codex-test-record'
        OR `component` <> 'system/codex-test-record/index'
      )
  )),TRUE)
 AND NOT COALESCE((EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `id` <> 901090
      AND (`path` = 'codex-test-record' OR `component` = 'system/codex-test-record/index')
  )),TRUE)
 AND NOT EXISTS(SELECT 1 FROM information_schema.routines WHERE routine_schema=DATABASE() AND routine_name='ensure_system_codex_test_record_menu')
 THEN 'TARGET_PREFLIGHT_PASS:20260726_system_codex_test_record_menu' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260726_system_codex_test_record_menu:MENU_ROLE_PREREQUISITE_OR_IDENTITY_CONFLICT' END;
