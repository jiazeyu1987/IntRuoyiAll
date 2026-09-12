-- release-target-preflight: migrationId=20260629_mes_smart_scheduling_role_scope; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND LOWER(table_name) IN ('system_menu','system_role','system_role_menu','system_tenant','system_tenant_package')) = 5
  AND (SELECT COUNT(*) FROM system_role WHERE code IN ('mes_scheduler','mes_team_leader','mes_workshop_director') AND deleted = b'0') >= 3
  AND NOT EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted = b'0' AND menu_ids IS NOT NULL AND NOT JSON_VALID(menu_ids))
THEN 'TARGET_PREFLIGHT_PASS:20260629_mes_smart_scheduling_role_scope' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260629_mes_smart_scheduling_role_scope' END;
