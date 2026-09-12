-- release-target-preflight: migrationId=20260811_mes_process_pool_uv1_metering_valid_parameter; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND LOWER(table_name) IN ('mes_pro_process','mes_pro_process_pool_device_parameter_rule','mes_pro_process_pool_team_device','mes_pro_process_pool_team_process_device')) = 4
THEN 'TARGET_PREFLIGHT_PASS:20260811_mes_process_pool_uv1_metering_valid_parameter' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260811_mes_process_pool_uv1_metering_valid_parameter' END;
