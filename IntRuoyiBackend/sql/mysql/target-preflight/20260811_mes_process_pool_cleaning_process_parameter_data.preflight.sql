-- release-target-preflight: migrationId=20260811_mes_process_pool_cleaning_process_parameter_data; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND LOWER(table_name) IN ('mes_pro_process','mes_pro_route_process','mes_pro_process_pool_device_parameter_rule','mes_pro_process_pool_team_device')) = 4
  AND (SELECT COUNT(*) FROM mes_pro_process WHERE name = '清洗工序' AND deleted = b'0') >= 1
THEN 'TARGET_PREFLIGHT_PASS:20260811_mes_process_pool_cleaning_process_parameter_data' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260811_mes_process_pool_cleaning_process_parameter_data' END;
