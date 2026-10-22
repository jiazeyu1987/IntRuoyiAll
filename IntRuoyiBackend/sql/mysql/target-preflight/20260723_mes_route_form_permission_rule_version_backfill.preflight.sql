-- release-target-preflight: migrationId=20260723_mes_route_form_permission_rule_version_backfill; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND LOWER(table_name) IN ('mes_pro_edhr_process_form_permission_rule','mes_pro_route_flow_process_batch_record','mes_pro_route_version')) = 3
THEN 'TARGET_PREFLIGHT_PASS:20260723_mes_route_form_permission_rule_version_backfill' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260723_mes_route_form_permission_rule_version_backfill' END;
