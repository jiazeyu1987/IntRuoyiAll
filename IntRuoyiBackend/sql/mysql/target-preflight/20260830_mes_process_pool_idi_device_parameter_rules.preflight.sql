-- release-target-preflight: migrationId=20260830_mes_process_pool_idi_device_parameter_rules; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND LOWER(table_name) IN ('dcc_project_code','mes_pro_process_pool_device_parameter_rule','mes_pro_process_pool_team_device','mes_pro_process_pool_team_process_device','mes_pro_route','mes_pro_route_dcc_project_binding','mes_pro_route_process','system_users')) = 8
THEN 'TARGET_PREFLIGHT_PASS:20260830_mes_process_pool_idi_device_parameter_rules' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260830_mes_process_pool_idi_device_parameter_rules' END;
