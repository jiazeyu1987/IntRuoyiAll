DROP PROCEDURE IF EXISTS codex_bind_press_balloon_products;
DELIMITER //
CREATE PROCEDURE codex_bind_press_balloon_products()
BEGIN
  DECLARE v_target_route_id BIGINT DEFAULT 980091;
  DECLARE v_target_version_id BIGINT DEFAULT NULL;
  DECLARE v_target_route_count INT DEFAULT 0;
  DECLARE v_target_item_count INT DEFAULT 0;
  DECLARE v_final_target_count INT DEFAULT 0;
  DECLARE v_final_old_count INT DEFAULT 0;
  DECLARE v_snapshot_valid INT DEFAULT 0;
  DECLARE v_products_json JSON DEFAULT NULL;
  DECLARE v_product_boms_json JSON DEFAULT NULL;

  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  SELECT COUNT(*) INTO v_target_route_count
  FROM mes_pro_route
  WHERE deleted = 0
    AND tenant_id = 1
    AND id = v_target_route_id
    AND code = 'RT000028-IDI'
    AND name = CONVERT(UNHEX('E68C89E58E8BE5BC8FE79083E59B8AE689A9E58585E58E8BE58A9BE6B3B5') USING utf8mb4) COLLATE utf8mb4_unicode_ci;

  IF v_target_route_count <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'target route precondition failed';
  END IF;

  SELECT COUNT(*) INTO v_target_item_count
  FROM mes_md_item
  WHERE deleted = 0
    AND tenant_id = 1
    AND name = CONVERT(UNHEX('E68C89E58E8BE5BC8FE79083E59B8AE689A9E58585E58E8BE58A9BE6B3B5') USING utf8mb4) COLLATE utf8mb4_unicode_ci;

  IF v_target_item_count <> 3 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'target product count precondition failed';
  END IF;

  SELECT id INTO v_target_version_id
  FROM mes_pro_route_version
  WHERE deleted = 0
    AND tenant_id = 1
    AND route_id = v_target_route_id
    AND active = b'1'
    AND lifecycle_status = 'ACTIVE'
  ORDER BY id DESC
  LIMIT 1;

  IF v_target_version_id IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'target active route version precondition failed';
  END IF;

  SELECT JSON_VALID(route_snapshot_json) INTO v_snapshot_valid
  FROM mes_pro_route_version
  WHERE id = v_target_version_id;

  IF v_snapshot_valid <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'target route version snapshot invalid';
  END IF;

  START TRANSACTION;

  UPDATE mes_pro_route_product rp
  SET rp.deleted = b'1',
      rp.updater = 'codex',
      rp.update_time = NOW()
  WHERE rp.deleted = 0
    AND rp.tenant_id = 1
    AND rp.route_id = v_target_route_id
    AND rp.item_id NOT IN (
      SELECT mi.id
      FROM mes_md_item mi
      WHERE mi.deleted = 0
        AND mi.tenant_id = 1
        AND mi.name = CONVERT(UNHEX('E68C89E58E8BE5BC8FE79083E59B8AE689A9E58585E58E8BE58A9BE6B3B5') USING utf8mb4) COLLATE utf8mb4_unicode_ci
    );

  UPDATE mes_pro_route_product_bom rb
  SET rb.deleted = b'1',
      rb.updater = 'codex',
      rb.update_time = NOW()
  WHERE rb.deleted = 0
    AND rb.tenant_id = 1
    AND rb.route_id = v_target_route_id
    AND rb.product_id NOT IN (
      SELECT mi.id
      FROM mes_md_item mi
      WHERE mi.deleted = 0
        AND mi.tenant_id = 1
        AND mi.name = CONVERT(UNHEX('E68C89E58E8BE5BC8FE79083E59B8AE689A9E58585E58E8BE58A9BE6B3B5') USING utf8mb4) COLLATE utf8mb4_unicode_ci
    );

  INSERT INTO mes_pro_route_product (
      route_id, item_id, quantity, production_time, time_unit_type, remark,
      creator, create_time, updater, update_time, deleted, tenant_id
  )
  SELECT v_target_route_id,
         mi.id,
         1,
         1.000000,
         'MINUTE',
         'Copied from RT000028 and associated with press balloon products',
         'codex',
         NOW(),
         'codex',
         NOW(),
         b'0',
         1
  FROM mes_md_item mi
  WHERE mi.deleted = 0
    AND mi.tenant_id = 1
    AND mi.name = CONVERT(UNHEX('E68C89E58E8BE5BC8FE79083E59B8AE689A9E58585E58E8BE58A9BE6B3B5') USING utf8mb4) COLLATE utf8mb4_unicode_ci
    AND NOT EXISTS (
      SELECT 1
      FROM mes_pro_route_product existing
      WHERE existing.deleted = 0
        AND existing.tenant_id = 1
        AND existing.route_id = v_target_route_id
        AND existing.item_id = mi.id
    )
  ORDER BY mi.id;

  SELECT COUNT(*) INTO v_final_target_count
  FROM mes_pro_route_product rp
  JOIN mes_md_item mi
    ON mi.deleted = 0
   AND mi.tenant_id = rp.tenant_id
   AND mi.id = rp.item_id
  WHERE rp.deleted = 0
    AND rp.tenant_id = 1
    AND rp.route_id = v_target_route_id
    AND mi.name = CONVERT(UNHEX('E68C89E58E8BE5BC8FE79083E59B8AE689A9E58585E58E8BE58A9BE6B3B5') USING utf8mb4) COLLATE utf8mb4_unicode_ci;

  SELECT COUNT(*) INTO v_final_old_count
  FROM mes_pro_route_product rp
  LEFT JOIN mes_md_item mi
    ON mi.deleted = 0
   AND mi.tenant_id = rp.tenant_id
   AND mi.id = rp.item_id
  WHERE rp.deleted = 0
    AND rp.tenant_id = 1
    AND rp.route_id = v_target_route_id
    AND (mi.id IS NULL OR mi.name <> CONVERT(UNHEX('E68C89E58E8BE5BC8FE79083E59B8AE689A9E58585E58E8BE58A9BE6B3B5') USING utf8mb4) COLLATE utf8mb4_unicode_ci);

  IF v_final_target_count <> 3 OR v_final_old_count <> 0 THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'final route product convergence failed';
  END IF;

  SELECT COALESCE(JSON_ARRAYAGG(JSON_OBJECT(
           'id', rp.id,
           'itemId', rp.item_id,
           'remark', rp.remark,
           'creator', rp.creator,
           'deleted', CAST(0 AS UNSIGNED),
           'routeId', rp.route_id,
           'updater', rp.updater,
           'quantity', rp.quantity,
           'createTime', DATE_FORMAT(rp.create_time, '%Y-%m-%d %H:%i:%s'),
           'updateTime', DATE_FORMAT(rp.update_time, '%Y-%m-%d %H:%i:%s'),
           'timeUnitType', rp.time_unit_type,
           'productionTime', rp.production_time
         )), JSON_ARRAY()) INTO v_products_json
  FROM mes_pro_route_product rp
  WHERE rp.deleted = 0
    AND rp.tenant_id = 1
    AND rp.route_id = v_target_route_id;

  SELECT COALESCE(JSON_ARRAYAGG(JSON_OBJECT(
           'id', rb.id,
           'routeId', rb.route_id,
           'processId', rb.process_id,
           'productId', rb.product_id,
           'itemId', rb.item_id,
           'quantity', rb.quantity,
           'remark', rb.remark,
           'creator', rb.creator,
           'deleted', CAST(0 AS UNSIGNED),
           'updater', rb.updater,
           'createTime', DATE_FORMAT(rb.create_time, '%Y-%m-%d %H:%i:%s'),
           'updateTime', DATE_FORMAT(rb.update_time, '%Y-%m-%d %H:%i:%s')
         )), JSON_ARRAY()) INTO v_product_boms_json
  FROM mes_pro_route_product_bom rb
  WHERE rb.deleted = 0
    AND rb.tenant_id = 1
    AND rb.route_id = v_target_route_id;

  UPDATE mes_pro_route_version rv
  SET rv.route_snapshot_json = JSON_SET(
        CAST(rv.route_snapshot_json AS JSON),
        '$.configSnapshots.products', v_products_json,
        '$.configSnapshots.productBoms', v_product_boms_json
      ),
      rv.updater = 'codex',
      rv.update_time = NOW()
  WHERE rv.id = v_target_version_id;

  COMMIT;

  SELECT v_target_route_id AS target_route_id,
         v_target_version_id AS target_version_id,
         v_target_item_count AS target_item_count,
         v_final_target_count AS final_target_product_bindings,
         v_final_old_count AS final_old_product_bindings;
END//
DELIMITER ;
CALL codex_bind_press_balloon_products();
DROP PROCEDURE codex_bind_press_balloon_products;