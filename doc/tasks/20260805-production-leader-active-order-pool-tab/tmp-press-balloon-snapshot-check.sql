SELECT JSON_TYPE(JSON_EXTRACT(route_snapshot_json, '$.configSnapshots.products')) AS products_type,
       JSON_LENGTH(JSON_EXTRACT(route_snapshot_json, '$.configSnapshots.products')) AS products_len,
       LEFT(JSON_UNQUOTE(JSON_EXTRACT(route_snapshot_json, '$.configSnapshots.products')), 1200) AS products_preview
FROM mes_pro_route_version
WHERE id = 448;

SELECT JSON_TYPE(JSON_EXTRACT(route_snapshot_json, '$.configSnapshots.productBoms')) AS product_boms_type,
       JSON_LENGTH(JSON_EXTRACT(route_snapshot_json, '$.configSnapshots.productBoms')) AS product_boms_len,
       LEFT(JSON_UNQUOTE(JSON_EXTRACT(route_snapshot_json, '$.configSnapshots.productBoms')), 1200) AS product_boms_preview
FROM mes_pro_route_version
WHERE id = 448;
