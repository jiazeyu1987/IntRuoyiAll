-- release-target-preflight: migrationId=20260717_mes_balloon_excel_device_workstation_binding; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND LOWER(table_name) IN ('mes_md_workshop','mes_md_production_line','mes_md_workstation','mes_dv_machinery','mes_dv_machinery_process','mes_md_workstation_machine','mes_pro_route','mes_pro_route_process','mes_pro_process')) = 9
  AND (SELECT COUNT(*) FROM mes_md_workshop WHERE deleted = b'0') >= 1
  AND (SELECT COUNT(*) FROM mes_md_production_line WHERE deleted = b'0') >= 1
  AND (
    SELECT COUNT(*)
    FROM mes_pro_route route
    JOIN mes_pro_route_process route_process
      ON route_process.route_id = route.id
     AND route_process.tenant_id = route.tenant_id
     AND route_process.deleted = b'0'
    JOIN mes_pro_process process
      ON process.id = route_process.process_id
     AND process.tenant_id = route_process.tenant_id
     AND process.deleted = b'0'
    WHERE route.tenant_id = 1
      AND route.deleted = b'0'
      AND route.code IN ('ROUTE-XLSX-00001', 'ROUTE-XLSX-00002')
  ) = 49
  AND (
    SELECT COUNT(DISTINCT CONCAT(route_process.`id`, ':', mp.`machinery_id`))
    FROM mes_pro_route route
    JOIN mes_pro_route_process route_process
      ON route_process.route_id = route.id
     AND route_process.tenant_id = route.tenant_id
     AND route_process.deleted = b'0'
    JOIN mes_pro_process process
      ON process.id = route_process.process_id
     AND process.tenant_id = route_process.tenant_id
     AND process.deleted = b'0'
    JOIN mes_dv_machinery_process mp
      ON mp.tenant_id = route_process.tenant_id
     AND mp.deleted = b'0'
     AND mp.line_name = route.name
     AND mp.process_id = route_process.process_id
     AND mp.process_code = process.code
    JOIN mes_dv_machinery machinery
      ON machinery.id = mp.machinery_id
     AND machinery.tenant_id = mp.tenant_id
     AND machinery.deleted = b'0'
    WHERE route.tenant_id = 1
      AND route.deleted = b'0'
      AND route.code IN ('ROUTE-XLSX-00001', 'ROUTE-XLSX-00002')
  ) > 0
THEN 'TARGET_PREFLIGHT_PASS:20260717_mes_balloon_excel_device_workstation_binding' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260717_mes_balloon_excel_device_workstation_binding' END;
