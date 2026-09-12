-- release-target-preflight: migrationId=20260716_mes_balloon_xlsx_route_00002_invalid_process_cleanup; allowedEnvironments=test
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND LOWER(table_name) IN ('mes_pro_route','mes_pro_route_process','mes_pro_process','mes_pro_route_flow_process_config','mes_pro_route_schedule_config','mes_pro_schedule_order_process')) = 6
  AND (SELECT COUNT(*) FROM mes_pro_route WHERE code = '00002' AND deleted = b'0') <= 1
THEN 'TARGET_PREFLIGHT_PASS:20260716_mes_balloon_xlsx_route_00002_invalid_process_cleanup' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260716_mes_balloon_xlsx_route_00002_invalid_process_cleanup' END;
