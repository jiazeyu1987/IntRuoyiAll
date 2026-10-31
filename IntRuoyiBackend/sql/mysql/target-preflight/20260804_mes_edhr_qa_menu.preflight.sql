-- release-target-preflight: migrationId=20260804_mes_edhr_qa_menu; allowedEnvironments=test,prod,backup
-- Read-only original prerequisite/identity checks; no menu resurrection, grants, or fixture creation.
SELECT CASE WHEN
 NOT COALESCE((EXISTS (
    SELECT 1
    FROM `system_tenant_package`
    WHERE `deleted` = b'0'
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
        AND `id` IN (900365, 900033, 900025, 900432)) <> 4),TRUE)
 AND NOT COALESCE((EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 900434
      AND `deleted` = b'0'
      AND NOT (
        `name` = 'QA'
        AND `parent_id` = 900220
        AND `path` = '/mes/pro/process-pool/qa-regulation'
        AND `component` = 'mes/pro/processpool/QaRegulationPage'
        AND `component_name` = 'MesProProcessPoolQaRegulation'
      )
  )),TRUE)
 AND NOT COALESCE((EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 900436
      AND `deleted` = b'0'
      AND NOT (
        `name` = '生产组长'
        AND `parent_id` = 900220
        AND `path` = '/mes/pro/process-pool/production-leader'
        AND `component` = 'mes/pro/processpool/ProductionLeaderWorkbenchPage'
        AND `component_name` = 'MesProProcessPoolProductionLeaderWorkbench'
      )
  )),TRUE)
 AND NOT COALESCE((EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 900435
      AND `deleted` = b'0'
      AND NOT (
        `name` = 'PQC组长'
        AND `parent_id` = 900220
        AND `path` = '/mes/pro/process-pool/pqc-leader'
        AND `component` = 'mes/pro/processpool/PqcLeaderWorkbenchPage'
        AND `component_name` = 'MesProProcessPoolPqcLeaderWorkbench'
      )
  )),TRUE)
 AND NOT COALESCE((EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 900437
      AND `deleted` = b'0'
      AND NOT (
        `name` = '一线生产'
        AND `parent_id` = 900220
        AND `path` = '/mes/pro/feedback/edhr-batch-production-fill'
        AND `component` = 'mes/pro/edhr-batch/BatchProductionFillPage'
        AND `component_name` = 'MesProEdhrBatchProductionFill'
      )
  )),TRUE)
 AND NOT COALESCE((EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `path` = '/mes/pro/process-pool/qa-regulation'
      AND `id` <> 900434
      AND `deleted` = b'0'
  )),TRUE)
 AND NOT COALESCE((EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `path` = '/mes/pro/process-pool/production-leader'
      AND `id` <> 900436
      AND `deleted` = b'0'
  )),TRUE)
 AND NOT COALESCE((EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `path` = '/mes/pro/process-pool/pqc-leader'
      AND `id` <> 900435
      AND `deleted` = b'0'
  )),TRUE)
 AND NOT COALESCE((EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `path` = '/mes/pro/feedback/edhr-batch-production-fill'
      AND `id` <> 900437
      AND `deleted` = b'0'
  )),TRUE)
 AND NOT EXISTS(SELECT 1 FROM information_schema.routines WHERE routine_schema=DATABASE() AND routine_name='ensure_mes_edhr_qa_menu')
 THEN 'TARGET_PREFLIGHT_PASS:20260804_mes_edhr_qa_menu' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260804_mes_edhr_qa_menu:MENU_ROLE_PREREQUISITE_OR_IDENTITY_CONFLICT' END;
