-- release-target-preflight: migrationId=20260812_mes_qa_pressure_pump_idi_seed; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND LOWER(table_name) IN ('dcc_project_code','mes_qa_inspection_regulation','mes_qa_inspection_regulation_item','mes_qa_inspection_regulation_process','mes_qa_inspection_regulation_version')) = 5
THEN 'TARGET_PREFLIGHT_PASS:20260812_mes_qa_pressure_pump_idi_seed' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260812_mes_qa_pressure_pump_idi_seed' END;
