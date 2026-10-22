-- release-target-preflight: migrationId=20260722_mes_route_creator_route_edit_permission_backfill; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND LOWER(table_name) IN ('mes_pro_edhr_permission_rule','mes_pro_edhr_permission_scope','mes_pro_route','system_users')) = 4
THEN 'TARGET_PREFLIGHT_PASS:20260722_mes_route_creator_route_edit_permission_backfill' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260722_mes_route_creator_route_edit_permission_backfill' END;
