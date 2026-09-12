-- release-target-preflight: migrationId=20260811_mes_process_pool_cleaning_wash_parameter_data; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND LOWER(table_name) IN ('mes_pro_process','mes_pro_process_pool_device_parameter_rule','mes_pro_process_pool_team_device')) = 3
THEN 'TARGET_PREFLIGHT_PASS:20260811_mes_process_pool_cleaning_wash_parameter_data' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260811_mes_process_pool_cleaning_wash_parameter_data' END;
