-- Repair source tenant route BOM process bindings for export/import consistency
-- Generated from source_bom_repair_mapping.tsv
START TRANSACTION;
CREATE TEMPORARY TABLE tmp_route_bom_repair (
    bom_id BIGINT PRIMARY KEY,
    route_code VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    item_code VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    target_process_id BIGINT,
    target_process_code VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    action VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
);
INSERT INTO tmp_route_bom_repair VALUES (2910,'ROUTE-XLSX-00001','YXN.037.011.1011',922894,'Z2630','UPDATE');
INSERT INTO tmp_route_bom_repair VALUES (2911,'ROUTE-XLSX-00001','YXN.041.011.1003',922895,'Z3710','UPDATE');
INSERT INTO tmp_route_bom_repair VALUES (2912,'ROUTE-XLSX-00001','YXN.041.011.1008',922896,'Z2775','UPDATE');
INSERT INTO tmp_route_bom_repair VALUES (2913,'ROUTE-XLSX-00001','YXN.041.011.1003',922897,'Z2772','UPDATE');
INSERT INTO tmp_route_bom_repair VALUES (2914,'ROUTE-XLSX-00001','YXN.037.011.1007',922898,'Z2510','UPDATE');
INSERT INTO tmp_route_bom_repair VALUES (2915,'ROUTE-XLSX-00001','YXN.041.011.1003',922899,'Z3810','UPDATE');
INSERT INTO tmp_route_bom_repair VALUES (2916,'ROUTE-XLSX-00001','YXN.037.011.1007',922900,'Z3720','UPDATE');
INSERT INTO tmp_route_bom_repair VALUES (2917,'ROUTE-XLSX-00001','YXN.037.011.1011',922901,'Z5200','UPDATE');
INSERT INTO tmp_route_bom_repair VALUES (2918,'ROUTE-XLSX-00001','YXN.037.011.1009',922902,'Z2520','UPDATE');
INSERT INTO tmp_route_bom_repair VALUES (2919,'ROUTE-XLSX-00001','YXN.037.011.1009',922903,'Z2530','UPDATE');
INSERT INTO tmp_route_bom_repair VALUES (2920,'ROUTE-XLSX-00001','YXN.037.011.1007',922904,'Z2550','UPDATE');
INSERT INTO tmp_route_bom_repair VALUES (2921,'ROUTE-XLSX-00001','YXN.037.011.1009',922905,'Z3850','UPDATE');
INSERT INTO tmp_route_bom_repair VALUES (2922,'ROUTE-XLSX-00001','YXN.041.011.1003',922906,'Z2560','UPDATE');
INSERT INTO tmp_route_bom_repair VALUES (2923,'ROUTE-XLSX-00001','YXN.041.011.1008',922907,'Z2570','UPDATE');
INSERT INTO tmp_route_bom_repair VALUES (2924,'ROUTE-XLSX-00001','YXN.037.011.1009',922908,'Z2600','UPDATE');
INSERT INTO tmp_route_bom_repair VALUES (2925,'ROUTE-XLSX-00001','YXN.037.011.1007',922909,'Z2580','UPDATE');
INSERT INTO tmp_route_bom_repair VALUES (2926,'ROUTE-XLSX-00001','YXN.037.011.1011',922910,'Z2480','UPDATE');
INSERT INTO tmp_route_bom_repair VALUES (2927,'ROUTE-XLSX-00001','YXN.041.011.1008',922911,'Z2590','UPDATE');
INSERT INTO tmp_route_bom_repair VALUES (2928,'ROUTE-XLSX-00001','YXN.037.011.1011',922912,'Z2490','UPDATE');
INSERT INTO tmp_route_bom_repair VALUES (2929,'ROUTE-XLSX-00001','YXN.041.011.1003',922913,'Z5600','UPDATE');
INSERT INTO tmp_route_bom_repair VALUES (2930,'ROUTE-XLSX-00001','YXN.037.011.1007',922914,'Z2620','UPDATE');
INSERT INTO tmp_route_bom_repair VALUES (2931,'ROUTE-XLSX-00001','YXN.041.011.1003',922915,'Z760','UPDATE');
INSERT INTO tmp_route_bom_repair VALUES (2932,'ROUTE-XLSX-00001','YXN.037.011.1007',922916,'Z830','UPDATE');
INSERT INTO tmp_route_bom_repair VALUES (2756,'ROUTE-XLSX-00002','YXN.069.001.1006',922917,'Z2630','UPDATE');
INSERT INTO tmp_route_bom_repair VALUES (2757,'ROUTE-XLSX-00002','YXN.069.001.1014',922918,'Z3710','UPDATE');
INSERT INTO tmp_route_bom_repair VALUES (2758,'ROUTE-XLSX-00002','YXN.069.001.1014',922919,'Z2775','UPDATE');
INSERT INTO tmp_route_bom_repair VALUES (2759,'ROUTE-XLSX-00002','YXN.069.001.1003',922920,'Z2772','UPDATE');
INSERT INTO tmp_route_bom_repair VALUES (2760,'ROUTE-XLSX-00002','YXN.069.001.1003',922921,'Z2510','UPDATE');
INSERT INTO tmp_route_bom_repair VALUES (2761,'ROUTE-XLSX-00002','YXN.069.001.1014',922922,'Z3810','UPDATE');
INSERT INTO tmp_route_bom_repair VALUES (2762,'ROUTE-XLSX-00002','YXN.069.001.1006',922923,'Z5200','UPDATE');
INSERT INTO tmp_route_bom_repair VALUES (2763,'ROUTE-XLSX-00002','YXN.069.001.1013',922924,'Z2530','UPDATE');
INSERT INTO tmp_route_bom_repair VALUES (2764,'ROUTE-XLSX-00002','YXN.069.001.1009',922925,'Z2971','UPDATE');
INSERT INTO tmp_route_bom_repair VALUES (2765,'ROUTE-XLSX-00002','YXN.069.001.1006',922926,'Z2976','UPDATE');
INSERT INTO tmp_route_bom_repair VALUES (2766,'ROUTE-XLSX-00002','YXN.069.001.1013',922927,'Z2972','UPDATE');
INSERT INTO tmp_route_bom_repair VALUES (2767,'ROUTE-XLSX-00002','YXN.069.001.1014',922928,'Z2973','UPDATE');
INSERT INTO tmp_route_bom_repair VALUES (2768,'ROUTE-XLSX-00002','YXN.069.001.1009',922929,'Z2974','UPDATE');
INSERT INTO tmp_route_bom_repair VALUES (2770,'ROUTE-XLSX-00002','YXN.069.001.1003',922931,'Z2550','UPDATE');
INSERT INTO tmp_route_bom_repair VALUES (2771,'ROUTE-XLSX-00002','YXN.069.001.1003',922932,'Z2570','UPDATE');
INSERT INTO tmp_route_bom_repair VALUES (2772,'ROUTE-XLSX-00002','YXN.069.001.1009',922933,'Z2600','UPDATE');
INSERT INTO tmp_route_bom_repair VALUES (2773,'ROUTE-XLSX-00002','YXN.069.001.1006',922934,'Z2580','UPDATE');
INSERT INTO tmp_route_bom_repair VALUES (2774,'ROUTE-XLSX-00002','YXN.069.001.1006',922935,'Z2490','UPDATE');
INSERT INTO tmp_route_bom_repair VALUES (2777,'ROUTE-XLSX-00002','YXN.069.001.1013',922938,'Z3850','UPDATE');
INSERT INTO tmp_route_bom_repair VALUES (2778,'ROUTE-XLSX-00002','YXN.069.001.1014',922939,'Z2560','UPDATE');
INSERT INTO tmp_route_bom_repair VALUES (2780,'ROUTE-XLSX-00002','YXN.069.001.1014',922941,'Z5600','UPDATE');
INSERT INTO tmp_route_bom_repair VALUES (2781,'ROUTE-XLSX-00002','YXN.069.001.1003',922942,'Z2620','UPDATE');
INSERT INTO tmp_route_bom_repair VALUES (2933,'ROUTE-XLSX-00001','YXN.037.011.1009',NULL,NULL,'SOFT_DELETE');
SELECT 'mapping_rows', COUNT(*) FROM tmp_route_bom_repair;
SELECT 'precheck_unmatched_rows', COUNT(*) FROM tmp_route_bom_repair m LEFT JOIN mes_pro_route_product_bom b ON b.id=m.bom_id AND b.tenant_id=1 AND b.deleted=b'0' AND b.process_id=0 LEFT JOIN mes_pro_route r ON r.id=b.route_id AND r.tenant_id=b.tenant_id AND r.code=m.route_code AND r.deleted=b'0' LEFT JOIN mes_md_item item ON item.id=b.item_id AND item.tenant_id=b.tenant_id AND item.code=m.item_code AND item.deleted=b'0' WHERE b.id IS NULL OR r.id IS NULL OR item.id IS NULL;
SELECT 'precheck_missing_target_process', COUNT(*) FROM tmp_route_bom_repair m LEFT JOIN mes_pro_route r ON r.tenant_id=1 AND r.code=m.route_code AND r.deleted=b'0' LEFT JOIN mes_pro_process p ON p.tenant_id=1 AND p.id=m.target_process_id AND p.code=m.target_process_code AND p.deleted=b'0' LEFT JOIN mes_pro_route_process rp ON rp.tenant_id=1 AND rp.route_id=r.id AND rp.process_id=p.id AND rp.deleted=b'0' WHERE m.action='UPDATE' AND rp.id IS NULL;
UPDATE mes_pro_route_product_bom b JOIN tmp_route_bom_repair m ON m.bom_id=b.id AND m.action='UPDATE' JOIN mes_pro_route r ON r.id=b.route_id AND r.tenant_id=b.tenant_id AND r.code=m.route_code AND r.deleted=b'0' JOIN mes_md_item item ON item.id=b.item_id AND item.tenant_id=b.tenant_id AND item.code=m.item_code AND item.deleted=b'0' JOIN mes_pro_process p ON p.tenant_id=b.tenant_id AND p.id=m.target_process_id AND p.code=m.target_process_code AND p.deleted=b'0' JOIN mes_pro_route_process rp ON rp.tenant_id=b.tenant_id AND rp.route_id=r.id AND rp.process_id=p.id AND rp.deleted=b'0' SET b.process_id=m.target_process_id, b.updater='codex', b.update_time=NOW() WHERE b.tenant_id=1 AND b.deleted=b'0' AND b.process_id=0;
SELECT 'updated_rows', ROW_COUNT();
UPDATE mes_pro_route_product_bom b JOIN tmp_route_bom_repair m ON m.bom_id=b.id AND m.action='SOFT_DELETE' JOIN mes_pro_route r ON r.id=b.route_id AND r.tenant_id=b.tenant_id AND r.code=m.route_code AND r.deleted=b'0' JOIN mes_md_item item ON item.id=b.item_id AND item.tenant_id=b.tenant_id AND item.code=m.item_code AND item.deleted=b'0' SET b.deleted=b'1', b.updater='codex', b.update_time=NOW() WHERE b.tenant_id=1 AND b.deleted=b'0' AND b.process_id=0;
SELECT 'soft_deleted_rows', ROW_COUNT();
SELECT 'post_missing_process_rows', COUNT(*) FROM mes_pro_route_product_bom b JOIN mes_pro_route r ON r.id=b.route_id AND r.tenant_id=b.tenant_id WHERE b.tenant_id=1 AND b.deleted=b'0' AND r.code IN ('ROUTE-XLSX-00001','ROUTE-XLSX-00002') AND (b.process_id IS NULL OR b.process_id=0);
COMMIT;
