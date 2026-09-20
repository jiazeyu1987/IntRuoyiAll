-- release-target-preflight: migrationId=20260818_mes_pressure_pump_same_name_item_convergence; allowedEnvironments=test,backup,prod
WITH
required_tables AS (
  SELECT COUNT(*) AS required_table_count
    FROM information_schema.tables
   WHERE table_schema = DATABASE()
     AND LOWER(table_name) IN ('dcc_project_code','mes_md_item','mes_pro_route','mes_pro_route_dcc_project_binding','mes_pro_route_product')
),
target_item_count AS (
  SELECT COUNT(1) AS v_target_item_count
    FROM `mes_md_item`
   WHERE `tenant_id` = 1
     AND `id` = 902101
     AND `code` COLLATE utf8mb4_unicode_ci = 'AW.107.02.01.1009'
     AND (`product_master_id` = 11 OR `product_master_id` IS NULL)
     AND `deleted` = b'0'
),
target_route_count AS (
  SELECT COUNT(1) AS v_target_route_count
    FROM `mes_pro_route`
   WHERE `tenant_id` = 1
     AND `id` = 922119
     AND `code` COLLATE utf8mb4_unicode_ci = 'RT000028'
     AND `deleted` = b'0'
),
existing_route_product_count AS (
  SELECT COUNT(1) AS v_existing_route_product_count
    FROM `mes_pro_route_product`
   WHERE `tenant_id` = 1
     AND `route_id` = 922119
     AND `item_id` = 902101
     AND `deleted` = b'0'
),
route_dcc_count AS (
  SELECT COUNT(1) AS v_route_dcc_count
    FROM `mes_pro_route_dcc_project_binding` `binding`
    JOIN `dcc_project_code` `project`
      ON `project`.`tenant_id` = `binding`.`tenant_id`
     AND `project`.`id` = `binding`.`dcc_project_code_id`
     AND `project`.`deleted` = b'0'
   WHERE `binding`.`tenant_id` = 1
     AND `binding`.`route_id` = 922119
     AND `binding`.`dcc_project_code_id` = 147
     AND `binding`.`deleted` = b'0'
     AND `project`.`status` COLLATE utf8mb4_unicode_ci = 'ENABLE'
     AND `project`.`product_master_id` = 11
),
route_product_master_drift_count AS (
  SELECT COUNT(1) AS v_route_product_master_drift_count
    FROM `mes_pro_route_product` `route_product`
    LEFT JOIN `mes_md_item` `item`
      ON `item`.`tenant_id` = `route_product`.`tenant_id`
     AND `item`.`id` = `route_product`.`item_id`
     AND `item`.`deleted` = b'0'
   WHERE `route_product`.`tenant_id` = 1
     AND `route_product`.`route_id` = 922119
     AND `route_product`.`deleted` = b'0'
     AND (`item`.`id` IS NULL OR `item`.`product_master_id` <> 11)
)
SELECT CASE WHEN
  required_table_count = 5
  AND v_target_item_count = 1
  AND v_target_route_count = 1
  AND v_existing_route_product_count <= 1
  AND (
    v_existing_route_product_count = 1
    OR (
      v_existing_route_product_count = 0
      AND v_route_dcc_count = 1
      AND v_route_product_master_drift_count = 0
    )
  )
THEN 'TARGET_PREFLIGHT_PASS:20260818_mes_pressure_pump_same_name_item_convergence' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260818_mes_pressure_pump_same_name_item_convergence' END
FROM required_tables, target_item_count, target_route_count, existing_route_product_count, route_dcc_count, route_product_master_drift_count;
