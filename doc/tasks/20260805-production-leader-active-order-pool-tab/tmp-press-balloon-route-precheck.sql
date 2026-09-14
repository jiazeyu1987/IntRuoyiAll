SELECT 'ROUTES' AS section;
SELECT id, code, name, status, remark
FROM mes_pro_route
WHERE deleted = 0
  AND tenant_id = 1
  AND (
    code IN ('RT000028', 'RT000028-IDI')
    OR name IN (
      CONVERT(UNHEX('E79083E59B8AE689A9E5BCA0E58E8BE58A9BE6B3B5') USING utf8mb4) COLLATE utf8mb4_unicode_ci,
      CONVERT(UNHEX('E68C89E58E8BE5BC8FE79083E59B8AE689A9E58585E58E8BE58A9BE6B3B5') USING utf8mb4) COLLATE utf8mb4_unicode_ci
    )
  )
ORDER BY id;

SELECT 'TARGET_PRODUCTS' AS section;
SELECT mi.id,
       mi.code,
       mi.name,
       mi.specification,
       COUNT(rp.id) AS route_bindings,
       GROUP_CONCAT(CONCAT(rp.route_id, ':', r.name) ORDER BY rp.route_id SEPARATOR ',') AS routes
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
GROUP BY mi.id, mi.code, mi.name, mi.specification
ORDER BY mi.id;

SELECT 'SOURCE_VERSIONS' AS section;
SELECT id, route_id, version_no, BIN(active) AS active, lifecycle_status
FROM mes_pro_route_version
WHERE deleted = 0
  AND tenant_id = 1
  AND route_id = 922119
ORDER BY id;

SELECT 'SOURCE_COUNTS' AS section;
SELECT
  (SELECT COUNT(*) FROM mes_pro_route_product WHERE deleted = 0 AND tenant_id = 1 AND route_id = 922119) AS route_products,
  (SELECT COUNT(*) FROM mes_pro_route_product_bom WHERE deleted = 0 AND tenant_id = 1 AND route_id = 922119) AS route_product_boms,
  (SELECT COUNT(*) FROM mes_pro_route_process WHERE deleted = 0 AND tenant_id = 1 AND route_id = 922119) AS route_processes,
  (SELECT COUNT(*) FROM mes_pro_route_flow_config WHERE deleted = 0 AND tenant_id = 1 AND route_id = 922119) AS route_flow_configs,
  (SELECT COUNT(*)
   FROM mes_pro_route_schedule_config rsc
   JOIN mes_pro_route_version rv
     ON rv.deleted = 0
    AND rv.tenant_id = rsc.tenant_id
    AND rv.id = rsc.route_version_id
   WHERE rsc.deleted = 0
     AND rsc.tenant_id = 1
     AND rv.route_id = 922119) AS route_schedule_configs;

SELECT 'ROUTE_RELATED_TABLES' AS section;
SHOW TABLES LIKE 'mes_pro_route%';
