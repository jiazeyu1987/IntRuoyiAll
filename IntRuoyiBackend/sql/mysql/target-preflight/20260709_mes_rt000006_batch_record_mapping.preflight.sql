-- release-target-preflight: migrationId=20260709_mes_rt000006_batch_record_mapping; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND LOWER(table_name) IN ('mes_pro_route','mes_pro_route_process','mes_pro_process','mes_pro_batch_record_report','mes_pro_route_flow_process_batch_record','system_role')) = 6
  AND ((SELECT COUNT(*) FROM mes_pro_route WHERE code = 'RT000006' AND deleted = b'0') = 0 OR (SELECT COUNT(*) FROM mes_pro_route WHERE code = 'RT000006' AND deleted = b'0') = 1)
THEN 'TARGET_PREFLIGHT_PASS:20260709_mes_rt000006_batch_record_mapping' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260709_mes_rt000006_batch_record_mapping' END;
