SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SELECT
  pc.id AS project_code_id,
  pc.project_name,
  pc.project_code,
  pc.product_master_id,
  mp.product_code,
  mp.name_cn,
  mp.status AS mdm_status
FROM dcc_project_code pc
JOIN mdm_product mp
  ON mp.tenant_id = pc.tenant_id
 AND mp.id = pc.product_master_id
 AND mp.deleted = b'0'
WHERE pc.tenant_id = 1
  AND pc.deleted = 0
  AND pc.project_name = CONVERT(UNHEX('E68C89E58E8BE5BC8FE79083E59B8AE689A9E58585E58E8BE58A9BE6B3B5') USING utf8mb4) COLLATE utf8mb4_unicode_ci
ORDER BY pc.id;

SELECT
  rp.id,
  rp.route_id,
  r.code AS route_code,
  r.name AS route_name,
  rp.item_id,
  rp.quantity,
  rp.production_time,
  rp.time_unit_type,
  rp.remark,
  HEX(rp.deleted) AS deleted_hex
FROM mes_pro_route_product rp
JOIN mes_pro_route r
  ON r.tenant_id = rp.tenant_id
 AND r.id = rp.route_id
WHERE rp.tenant_id = 1
  AND rp.item_id = 14
ORDER BY rp.id;

SELECT
  rv.id,
  rv.route_id,
  r.code AS route_code,
  r.name AS route_name,
  rv.version_no,
  rv.lifecycle_status,
  rv.active,
  JSON_CONTAINS(
    JSON_EXTRACT(rv.route_snapshot_json, '$.configSnapshots.products'),
    JSON_OBJECT('itemId', 14)
  ) AS contains_mdm_product,
  JSON_LENGTH(JSON_EXTRACT(rv.route_snapshot_json, '$.configSnapshots.products')) AS product_snapshot_count
FROM mes_pro_route_version rv
JOIN mes_pro_route r
  ON r.tenant_id = rv.tenant_id
 AND r.id = rv.route_id
WHERE rv.tenant_id = 1
  AND rv.deleted = b'0'
  AND rv.id IN (490, 622)
ORDER BY rv.id;

SELECT
  (SELECT COUNT(*)
     FROM mes_pro_route_product
    WHERE tenant_id = 1
      AND deleted = b'0'
      AND route_id = 980091
      AND item_id = 14) AS active_target_mdm_binding_count,
  (SELECT COUNT(*)
     FROM mes_pro_route_product
    WHERE tenant_id = 1
      AND deleted = b'0'
      AND route_id <> 980091
      AND item_id = 14) AS active_non_target_mdm_binding_count,
  (SELECT COUNT(*)
     FROM mes_pro_route_product
    WHERE tenant_id = 1
      AND deleted = b'0'
      AND route_id = 980091) AS active_press_route_all_product_count;
