SELECT 'WORK_ORDERS' AS section;
SELECT mi.id AS product_id,
       mi.code AS product_code,
       mi.specification,
       COUNT(wo.id) AS work_order_count,
       GROUP_CONCAT(CONCAT(wo.id, ':', wo.code, ':status=', wo.status) ORDER BY wo.id SEPARATOR ',') AS work_orders
FROM mes_md_item mi
LEFT JOIN mes_pro_work_order wo
       ON wo.deleted = 0
      AND wo.tenant_id = mi.tenant_id
      AND wo.product_id = mi.id
WHERE mi.deleted = 0
  AND mi.tenant_id = 1
  AND mi.name = CONVERT(UNHEX('E68C89E58E8BE5BC8FE79083E59B8AE689A9E58585E58E8BE58A9BE6B3B5') USING utf8mb4) COLLATE utf8mb4_unicode_ci
GROUP BY mi.id, mi.code, mi.specification
ORDER BY mi.id;

SELECT 'SCHEDULE_ORDERS' AS section;
SELECT mi.id AS product_id,
       mi.code AS product_code,
       mi.specification,
       COUNT(so.id) AS schedule_order_count,
       GROUP_CONCAT(CONCAT(so.id, ':', so.code, ':status=', so.status, ':route=', so.route_id) ORDER BY so.id SEPARATOR ',') AS schedule_orders
FROM mes_md_item mi
LEFT JOIN mes_pro_schedule_order so
       ON so.deleted = 0
      AND so.tenant_id = mi.tenant_id
      AND so.product_id = mi.id
WHERE mi.deleted = 0
  AND mi.tenant_id = 1
  AND mi.name = CONVERT(UNHEX('E68C89E58E8BE5BC8FE79083E59B8AE689A9E58585E58E8BE58A9BE6B3B5') USING utf8mb4) COLLATE utf8mb4_unicode_ci
GROUP BY mi.id, mi.code, mi.specification
ORDER BY mi.id;