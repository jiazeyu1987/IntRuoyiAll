-- release-target-preflight: migrationId=20260818_mes_pressure_pump_same_name_item_convergence; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND LOWER(table_name) IN ('dcc_project_code','mes_md_item','mes_pro_route','mes_pro_route_dcc_project_binding','mes_pro_route_product')) = 5
THEN 'TARGET_PREFLIGHT_PASS:20260818_mes_pressure_pump_same_name_item_convergence' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260818_mes_pressure_pump_same_name_item_convergence' END;
