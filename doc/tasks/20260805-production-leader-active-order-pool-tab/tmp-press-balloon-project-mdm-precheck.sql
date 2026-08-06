SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DESCRIBE dcc_project_code;
DESCRIBE mes_md_item;
DESCRIBE mes_pro_route;
DESCRIBE mes_pro_route_product;

SELECT
  id,
  code,
  name,
  specification,
  status,
  tenant_id,
  deleted
FROM mes_md_item
WHERE tenant_id = 1
  AND deleted = 0
  AND name = CONVERT(UNHEX('E68C89E58E8BE5BC8FE79083E59B8AE689A9E58585E58E8BE58A9BE6B3B5') USING utf8mb4) COLLATE utf8mb4_unicode_ci
ORDER BY id;

SELECT
  pc.id,
  pc.product_master_id,
  pc.project_name,
  pc.project_code,
  pc.category,
  pc.status,
  pc.tenant_id,
  pc.deleted
FROM dcc_project_code pc
WHERE pc.tenant_id = 1
  AND pc.deleted = 0
  AND pc.project_name = CONVERT(UNHEX('E68C89E58E8BE5BC8FE79083E59B8AE689A9E58585E58E8BE58A9BE6B3B5') USING utf8mb4) COLLATE utf8mb4_unicode_ci
ORDER BY pc.id;

SELECT
  rp.id AS route_product_id,
  rp.route_id,
  r.code AS route_code,
  r.name AS route_name,
  rp.item_id,
  mi.code AS item_code,
  mi.name AS item_name,
  mi.specification,
  mi.status AS item_status,
  rp.tenant_id,
  rp.deleted
FROM mes_pro_route_product rp
JOIN mes_pro_route r ON r.id = rp.route_id AND r.tenant_id = rp.tenant_id AND r.deleted = 0
JOIN mes_md_item mi ON mi.id = rp.item_id AND mi.tenant_id = rp.tenant_id AND mi.deleted = 0
WHERE rp.tenant_id = 1
  AND rp.deleted = 0
  AND rp.route_id = 980091
ORDER BY rp.id;

SELECT
  pc.id AS project_code_id,
  pc.project_code,
  pc.product_master_id,
  mi.id AS mdm_item_id,
  mi.code AS mdm_code,
  mi.name AS mdm_name,
  mi.specification AS mdm_specification,
  mi.status AS mdm_status
FROM dcc_project_code pc
LEFT JOIN mes_md_item mi
  ON mi.tenant_id = pc.tenant_id
 AND mi.id = pc.product_master_id
 AND mi.deleted = 0
WHERE pc.tenant_id = 1
  AND pc.deleted = 0
  AND pc.project_name = CONVERT(UNHEX('E68C89E58E8BE5BC8FE79083E59B8AE689A9E58585E58E8BE58A9BE6B3B5') USING utf8mb4) COLLATE utf8mb4_unicode_ci
ORDER BY pc.id;
