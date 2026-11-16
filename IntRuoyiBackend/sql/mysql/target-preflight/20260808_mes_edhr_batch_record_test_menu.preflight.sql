-- release-target-preflight: migrationId=20260808_mes_edhr_batch_record_test_menu; allowedEnvironments=test,prod,backup
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
 AND NOT COALESCE((NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 900033
      AND `deleted` = b'0'
      AND `path` = '/mes/pro/feedback/edhr-batch-execution'
  )),TRUE)
 AND NOT COALESCE((EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 900440
      AND `deleted` = b'0'
      AND NOT (
        `name` = '批记录测试'
        AND `parent_id` = 900220
        AND `path` = '/mes/pro/feedback/edhr-batch-test'
        AND `component` = 'mes/pro/edhr-batch/BatchRecordTestPage'
        AND `component_name` = 'MesProEdhrBatchRecordTest'
      )
  )),TRUE)
 AND NOT COALESCE((EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `path` = '/mes/pro/feedback/edhr-batch-test'
      AND `id` <> 900440
      AND `deleted` = b'0'
  )),TRUE)
 AND NOT EXISTS(SELECT 1 FROM information_schema.routines WHERE routine_schema=DATABASE() AND routine_name='ensure_mes_edhr_batch_record_test_menu')
 THEN 'TARGET_PREFLIGHT_PASS:20260808_mes_edhr_batch_record_test_menu' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260808_mes_edhr_batch_record_test_menu:MENU_ROLE_PREREQUISITE_OR_IDENTITY_CONFLICT' END;
