-- release-target-preflight: migrationId=20260812_mes_pqc_dcc_qa_c00_backfill; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND LOWER(table_name) IN ('mes_pro_route_dcc_project_binding','mes_pro_process_pool_active_order','mes_pro_process_pool_event','mes_pqc_inspection_task')) = 4
THEN 'TARGET_PREFLIGHT_PASS:20260812_mes_pqc_dcc_qa_c00_backfill' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260812_mes_pqc_dcc_qa_c00_backfill' END;
