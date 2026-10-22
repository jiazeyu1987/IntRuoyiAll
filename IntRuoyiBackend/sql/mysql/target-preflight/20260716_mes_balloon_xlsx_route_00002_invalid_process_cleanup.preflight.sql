-- release-target-preflight: migrationId=20260716_mes_balloon_xlsx_route_00002_invalid_process_cleanup; allowedEnvironments=test
WITH route_00002_contract AS (
  SELECT
    (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND LOWER(table_name) IN ('mes_pro_route','mes_pro_route_process','mes_pro_process','mes_pro_route_flow_process_config','mes_pro_route_schedule_config','mes_pro_schedule_order_process')) AS required_table_count,
    (SELECT COUNT(*)
       FROM mes_pro_route_process route_process
       JOIN mes_pro_route route ON route.id = route_process.route_id AND route.tenant_id = route_process.tenant_id AND route.deleted = b'0'
      WHERE route_process.tenant_id = 1
        AND route_process.deleted = b'0'
        AND route.code IN ('ROUTE-XLSX-00001', 'ROUTE-XLSX-00002')) AS total_active_process_count,
    (SELECT COUNT(*)
       FROM mes_pro_route_process route_process
       JOIN mes_pro_route route ON route.id = route_process.route_id AND route.tenant_id = route_process.tenant_id AND route.deleted = b'0'
       JOIN mes_pro_process process ON process.id = route_process.process_id AND process.tenant_id = route_process.tenant_id AND process.deleted = b'0'
      WHERE route_process.tenant_id = 1
        AND route_process.deleted = b'0'
        AND route.code = 'ROUTE-XLSX-00002'
        AND route_process.sort = 26
        AND process.code = 'B320') AS legacy_cleanup_target_count,
    (SELECT COUNT(*)
       FROM mes_pro_route_process route_process
       JOIN mes_pro_route route ON route.id = route_process.route_id AND route.tenant_id = route_process.tenant_id AND route.deleted = b'0'
       JOIN mes_pro_process process ON process.id = route_process.process_id AND process.tenant_id = route_process.tenant_id AND process.deleted = b'0'
      WHERE route_process.tenant_id = 1
        AND route_process.deleted = b'0'
        AND route.code = 'ROUTE-XLSX-00002'
        AND route_process.sort = 26
        AND process.code = 'Z2620') AS already_normalized_target_count,
    (SELECT COUNT(*)
       FROM mes_pro_schedule_order_process schedule_process
       JOIN mes_pro_route_process route_process ON route_process.id = schedule_process.route_process_id AND route_process.tenant_id = schedule_process.tenant_id AND route_process.deleted = b'0'
       JOIN mes_pro_route route ON route.id = route_process.route_id AND route.tenant_id = route_process.tenant_id AND route.deleted = b'0'
       JOIN mes_pro_process process ON process.id = route_process.process_id AND process.tenant_id = route_process.tenant_id AND process.deleted = b'0'
      WHERE schedule_process.tenant_id = 1
        AND schedule_process.deleted = b'0'
        AND route.code = 'ROUTE-XLSX-00002'
        AND route_process.sort = 26
        AND process.code = 'B320'
        AND schedule_process.reported_quantity <> 0) AS legacy_reported_schedule_count
)
SELECT CASE WHEN required_table_count = 6
  AND (
    (legacy_cleanup_target_count = 1 AND legacy_reported_schedule_count = 0)
    OR (total_active_process_count = 49 AND legacy_cleanup_target_count = 0 AND already_normalized_target_count = 1)
  )
THEN 'TARGET_PREFLIGHT_PASS:20260716_mes_balloon_xlsx_route_00002_invalid_process_cleanup' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260716_mes_balloon_xlsx_route_00002_invalid_process_cleanup' END
FROM route_00002_contract;
