-- release-target-preflight: migrationId=20260718_mes_puhui_schedule_admin_role_visibility; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND LOWER(table_name) IN ('system_menu','system_role','system_role_menu','system_tenant','system_tenant_package','system_user_role','system_users')) = 7
  AND EXISTS (SELECT 1 FROM system_menu WHERE id = 5100 AND path = '/mes' AND status = 0 AND deleted = b'0')
  AND EXISTS (SELECT 1 FROM system_menu WHERE id = 900120 AND name = '智能排产' AND parent_id IN (0, 5100) AND status = 0 AND deleted = b'0')
  AND EXISTS (SELECT 1 FROM system_menu WHERE id = 900104 AND parent_id = 900120 AND name = '璞慧排产' AND path = '/mes/pro/puhui-schedule' AND component_name = 'MesProPuhuiSchedule' AND permission = 'mes:pro-puhui-schedule:query' AND status = 0 AND deleted = b'0')
  AND NOT EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted = b'0' AND menu_ids IS NOT NULL AND NOT JSON_VALID(menu_ids))
THEN 'TARGET_PREFLIGHT_PASS:20260718_mes_puhui_schedule_admin_role_visibility' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260718_mes_puhui_schedule_admin_role_visibility' END;
