SELECT 'TARGET_ROUTE' AS section;
SELECT r.id, r.code, r.name, r.status, rv.id AS active_version_id, rv.version_no, BIN(rv.active) AS active, rv.lifecycle_status
FROM mes_pro_route r
LEFT JOIN mes_pro_route_version rv
       ON rv.deleted = 0
      AND rv.tenant_id = r.tenant_id
      AND rv.route_id = r.id
      AND rv.active = b'1'
      AND rv.lifecycle_status = 'ACTIVE'
WHERE r.deleted = 0
  AND r.tenant_id = 1
  AND r.id = 980091;

SELECT 'TARGET_PRODUCTS' AS section;
SELECT rp.id AS route_product_id,
       rp.route_id,
       rp.item_id,
       mi.code AS item_code,
       mi.name AS item_name,
       mi.specification,
       rp.quantity,
       rp.production_time,
       rp.time_unit_type,
       rp.remark
FROM mes_pro_route_product rp
JOIN mes_md_item mi
  ON mi.deleted = 0
 AND mi.tenant_id = rp.tenant_id
 AND mi.id = rp.item_id
WHERE rp.deleted = 0
  AND rp.tenant_id = 1
  AND rp.route_id = 980091
ORDER BY rp.item_id, rp.id;

SELECT 'COUNTS' AS section;
SELECT
  (SELECT COUNT(*) FROM mes_pro_route_process WHERE deleted = 0 AND tenant_id = 1 AND route_id = 922119) AS source_processes,
  (SELECT COUNT(*) FROM mes_pro_route_process WHERE deleted = 0 AND tenant_id = 1 AND route_id = 980091) AS target_processes,
  (SELECT COUNT(*) FROM mes_pro_route_flow_config WHERE deleted = 0 AND tenant_id = 1 AND route_id = 922119) AS source_flow_configs,
  (SELECT COUNT(*) FROM mes_pro_route_flow_config WHERE deleted = 0 AND tenant_id = 1 AND route_id = 980091) AS target_flow_configs,
  (SELECT COUNT(*) FROM mes_pro_route_schedule_config rsc JOIN mes_pro_route_version rv ON rv.deleted = 0 AND rv.tenant_id = rsc.tenant_id AND rv.id = rsc.route_version_id WHERE rsc.deleted = 0 AND rsc.tenant_id = 1 AND rv.route_id = 922119 AND rv.id = 448) AS source_active_schedule_configs,
  (SELECT COUNT(*) FROM mes_pro_route_schedule_config WHERE deleted = 0 AND tenant_id = 1 AND route_version_id = 622) AS target_active_schedule_configs,
  (SELECT COUNT(*) FROM mes_pro_route_product WHERE deleted = 0 AND tenant_id = 1 AND route_id = 980091) AS target_active_products,
  (SELECT COUNT(*) FROM mes_pro_route_product rp LEFT JOIN mes_md_item mi ON mi.deleted = 0 AND mi.tenant_id = rp.tenant_id AND mi.id = rp.item_id WHERE rp.deleted = 0 AND rp.tenant_id = 1 AND rp.route_id = 980091 AND (mi.id IS NULL OR mi.name <> CONVERT(UNHEX('E68C89E58E8BE5BC8FE79083E59B8AE689A9E58585E58E8BE58A9BE6B3B5') USING utf8mb4) COLLATE utf8mb4_unicode_ci)) AS target_old_products,
  (SELECT COUNT(*) FROM mes_pro_route_product_bom WHERE deleted = 0 AND tenant_id = 1 AND route_id = 980091) AS target_product_boms;

SELECT 'TARGET_ITEM_LOOKUP' AS section;
SELECT mi.id AS item_id,
       mi.code AS item_code,
       mi.specification,
       rp.route_id,
       r.code AS route_code,
       r.name AS route_name
FROM mes_md_item mi
LEFT JOIN mes_pro_route_product rp
       ON rp.deleted = 0
      AND rp.tenant_id = mi.tenant_id
      AND rp.item_id = mi.id
LEFT JOIN mes_pro_route r
       ON r.deleted = 0
      AND r.tenant_id = rp.tenant_id
      AND r.id = rp.route_id
WHERE mi.deleted = 0
  AND mi.tenant_id = 1
  AND mi.name = CONVERT(UNHEX('E68C89E58E8BE5BC8FE79083E59B8AE689A9E58585E58E8BE58A9BE6B3B5') USING utf8mb4) COLLATE utf8mb4_unicode_ci
ORDER BY mi.id;

SELECT 'SNAPSHOT' AS section;
SELECT JSON_LENGTH(JSON_EXTRACT(route_snapshot_json, '$.configSnapshots.products')) AS snapshot_products,
       JSON_EXTRACT(route_snapshot_json, '$.configSnapshots.products[*].itemId') AS snapshot_item_ids,
       JSON_LENGTH(JSON_EXTRACT(route_snapshot_json, '$.configSnapshots.productBoms')) AS snapshot_product_boms
FROM mes_pro_route_version
WHERE id = 622;