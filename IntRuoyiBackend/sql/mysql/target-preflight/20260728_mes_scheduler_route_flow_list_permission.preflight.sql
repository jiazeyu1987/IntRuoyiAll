-- release-target-preflight: migrationId=20260728_mes_scheduler_route_flow_list_permission; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND LOWER(table_name) IN ('system_menu','system_role','system_role_menu','system_tenant','system_tenant_package')) = 5
  AND (SELECT COUNT(*) FROM system_menu WHERE id = 5723 AND deleted = b'0') = 1
  AND (SELECT COUNT(*) FROM system_role WHERE code = 'mes_scheduler' AND deleted = b'0') >= 1
  AND NOT EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted = b'0' AND menu_ids IS NOT NULL AND NOT JSON_VALID(menu_ids))
THEN 'TARGET_PREFLIGHT_PASS:20260728_mes_scheduler_route_flow_list_permission' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260728_mes_scheduler_route_flow_list_permission' END;
