-- release-target-preflight: migrationId=20260717_mes_balloon_excel_device_workstation_binding; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND LOWER(table_name) IN ('mes_md_workshop','mes_md_production_line','mes_md_workstation','mes_dv_machinery','mes_dv_machinery_process','mes_md_workstation_machine','mes_pro_route','mes_pro_route_process','mes_pro_process')) = 9
  AND (SELECT COUNT(*) FROM mes_md_workshop WHERE deleted = b'0') >= 1
  AND (SELECT COUNT(*) FROM mes_md_production_line WHERE deleted = b'0') >= 1
THEN 'TARGET_PREFLIGHT_PASS:20260717_mes_balloon_excel_device_workstation_binding' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260717_mes_balloon_excel_device_workstation_binding' END;
