-- release-target-preflight: migrationId=20261022_mes_pqc_c00_empty_history_activation; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE()
    AND table_name IN ('mes_pro_process_pool_active_order','mes_pqc_inspection_task','mes_pro_process_pool_event'))=3
  AND NOT EXISTS (SELECT 1 FROM mes_pro_process_pool_active_order)
  AND NOT EXISTS (SELECT 1 FROM mes_pqc_inspection_task)
  AND NOT EXISTS (SELECT 1 FROM mes_pro_process_pool_event WHERE deleted=b'0'
    AND event_type='PQC_INSPECTION' AND feedback_source_type='MES_PQC_INSPECTION_TASK')
THEN 'TARGET_PREFLIGHT_PASS:20261022_mes_pqc_c00_empty_history_activation' ELSE 'TARGET_PREFLIGHT_BLOCKED:20261022_mes_pqc_c00_empty_history_activation' END;
