SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS codex_bind_press_balloon_project_mdm;
DELIMITER //
CREATE PROCEDURE codex_bind_press_balloon_project_mdm()
BEGIN
  DECLARE v_target_name VARCHAR(255) DEFAULT CONVERT(UNHEX('E68C89E58E8BE5BC8FE79083E59B8AE689A9E58585E58E8BE58A9BE6B3B5') USING utf8mb4);
  DECLARE v_source_name VARCHAR(255) DEFAULT CONVERT(UNHEX('E79083E59B8AE689A9E5BCA0E58E8BE58A9BE6B3B5') USING utf8mb4);
  DECLARE v_target_route_id BIGINT DEFAULT 980091;
  DECLARE v_source_route_id BIGINT DEFAULT 922119;
  DECLARE v_target_route_version_id BIGINT DEFAULT 622;
  DECLARE v_project_code VARCHAR(64) DEFAULT 'IDI';
  DECLARE v_expected_mdm_product_id BIGINT DEFAULT 14;
  DECLARE v_project_code_count INT DEFAULT 0;
  DECLARE v_target_route_count INT DEFAULT 0;
  DECLARE v_target_version_count INT DEFAULT 0;
  DECLARE v_mdm_product_count INT DEFAULT 0;
  DECLARE v_source_project_mdm BIGINT DEFAULT NULL;
  DECLARE v_unexpected_active_count INT DEFAULT 0;
  DECLARE v_active_source_count INT DEFAULT 0;
  DECLARE v_active_target_count INT DEFAULT 0;
  DECLARE v_target_route_product_id BIGINT DEFAULT NULL;
  DECLARE v_snapshot_products_type VARCHAR(32) DEFAULT NULL;
  DECLARE v_final_target_count INT DEFAULT 0;
  DECLARE v_final_non_target_count INT DEFAULT 0;
  DECLARE v_final_snapshot_contains INT DEFAULT 0;

  SET v_target_name = v_target_name COLLATE utf8mb4_unicode_ci;
  SET v_source_name = v_source_name COLLATE utf8mb4_unicode_ci;

  START TRANSACTION;

  SELECT COUNT(*)
    INTO v_project_code_count
    FROM dcc_project_code
   WHERE tenant_id = 1
     AND deleted = 0
     AND status = 'ENABLE'
     AND project_name = v_target_name
     AND project_code = v_project_code
     AND product_master_id = v_expected_mdm_product_id;
  IF v_project_code_count <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'target DCC project code / MDM binding is not unique or missing';
  END IF;

  SELECT COUNT(*)
    INTO v_mdm_product_count
    FROM mdm_product
   WHERE tenant_id = 1
     AND deleted = b'0'
     AND id = v_expected_mdm_product_id
     AND status = 'ENABLE'
     AND name_cn = v_target_name;
  IF v_mdm_product_count <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'target MDM product is not enabled or does not match project name';
  END IF;

  SELECT product_master_id
    INTO v_source_project_mdm
    FROM dcc_project_code
   WHERE tenant_id = 1
     AND deleted = 0
     AND status = 'ENABLE'
     AND project_name = v_source_name
     AND project_code = 'ID'
   LIMIT 1;
  IF v_source_project_mdm IS NULL OR v_source_project_mdm = v_expected_mdm_product_id THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'source route project-code MDM identity is unsafe for rebinding';
  END IF;

  SELECT COUNT(*)
    INTO v_target_route_count
    FROM mes_pro_route
   WHERE tenant_id = 1
     AND deleted = b'0'
     AND id = v_target_route_id
     AND code = 'RT000028-IDI'
     AND name = v_target_name;
  IF v_target_route_count <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'target route identity mismatch';
  END IF;

  SELECT COUNT(*),
         MAX(JSON_TYPE(JSON_EXTRACT(route_snapshot_json, '$.configSnapshots.products')))
    INTO v_target_version_count,
         v_snapshot_products_type
    FROM mes_pro_route_version
   WHERE tenant_id = 1
     AND deleted = b'0'
     AND id = v_target_route_version_id
     AND route_id = v_target_route_id
     AND active = b'1'
     AND lifecycle_status = 'ACTIVE'
     AND JSON_VALID(route_snapshot_json);
  IF v_target_version_count <> 1 OR v_snapshot_products_type <> 'ARRAY' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'target active route version snapshot is missing or not an array';
  END IF;

  SELECT COUNT(*)
    INTO v_unexpected_active_count
    FROM mes_pro_route_product
   WHERE tenant_id = 1
     AND deleted = b'0'
     AND item_id = v_expected_mdm_product_id
     AND route_id NOT IN (v_source_route_id, v_target_route_id);
  IF v_unexpected_active_count <> 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'unexpected active MDM route-product binding exists';
  END IF;

  SELECT COUNT(*)
    INTO v_active_source_count
    FROM mes_pro_route_product
   WHERE tenant_id = 1
     AND deleted = b'0'
     AND item_id = v_expected_mdm_product_id
     AND route_id = v_source_route_id;
  IF v_active_source_count > 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'source MDM route-product binding is duplicated';
  END IF;

  SELECT COUNT(*)
    INTO v_active_target_count
    FROM mes_pro_route_product
   WHERE tenant_id = 1
     AND deleted = b'0'
     AND item_id = v_expected_mdm_product_id
     AND route_id = v_target_route_id;
  IF v_active_target_count > 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'target MDM route-product binding is duplicated';
  END IF;

  UPDATE mes_pro_route_product
     SET deleted = b'1',
         updater = 'codex',
         update_time = NOW()
   WHERE tenant_id = 1
     AND deleted = b'0'
     AND item_id = v_expected_mdm_product_id
     AND route_id = v_source_route_id;

  SELECT MAX(id)
    INTO v_target_route_product_id
    FROM mes_pro_route_product
   WHERE tenant_id = 1
     AND route_id = v_target_route_id
     AND item_id = v_expected_mdm_product_id;

  IF v_target_route_product_id IS NULL THEN
    INSERT INTO mes_pro_route_product
      (route_id, item_id, quantity, production_time, time_unit_type, remark,
       creator, create_time, updater, update_time, deleted, tenant_id)
    VALUES
      (v_target_route_id, v_expected_mdm_product_id, 1, 1.000000, 'MINUTE', 'QA 规程手动绑定',
       'codex', NOW(), 'codex', NOW(), b'0', 1);
    SET v_target_route_product_id = LAST_INSERT_ID();
  ELSE
    UPDATE mes_pro_route_product
       SET quantity = 1,
           production_time = 1.000000,
           time_unit_type = 'MINUTE',
           remark = 'QA 规程手动绑定',
           deleted = b'0',
           updater = 'codex',
           update_time = NOW()
     WHERE id = v_target_route_product_id
       AND tenant_id = 1;
  END IF;

  UPDATE mes_pro_route_version
     SET route_snapshot_json = JSON_ARRAY_APPEND(
           route_snapshot_json,
           '$.configSnapshots.products',
           JSON_OBJECT(
             'id', v_target_route_product_id,
             'routeId', v_target_route_id,
             'itemId', v_expected_mdm_product_id,
             'quantity', 1,
             'productionTime', 1.0,
             'timeUnitType', 'MINUTE',
             'remark', 'QA 规程手动绑定',
             'creator', 'codex',
             'updater', 'codex',
             'deleted', false
           )
         ),
         updater = 'codex',
         update_time = NOW()
   WHERE tenant_id = 1
     AND id = v_target_route_version_id
     AND route_id = v_target_route_id
     AND JSON_CONTAINS(JSON_EXTRACT(route_snapshot_json, '$.configSnapshots.products'), JSON_OBJECT('itemId', v_expected_mdm_product_id)) = 0;

  SELECT COUNT(*)
    INTO v_final_target_count
    FROM mes_pro_route_product
   WHERE tenant_id = 1
     AND deleted = b'0'
     AND route_id = v_target_route_id
     AND item_id = v_expected_mdm_product_id;
  SELECT COUNT(*)
    INTO v_final_non_target_count
    FROM mes_pro_route_product
   WHERE tenant_id = 1
     AND deleted = b'0'
     AND route_id <> v_target_route_id
     AND item_id = v_expected_mdm_product_id;
  SELECT JSON_CONTAINS(JSON_EXTRACT(route_snapshot_json, '$.configSnapshots.products'), JSON_OBJECT('itemId', v_expected_mdm_product_id))
    INTO v_final_snapshot_contains
    FROM mes_pro_route_version
   WHERE tenant_id = 1
     AND id = v_target_route_version_id
     AND route_id = v_target_route_id;

  IF v_final_target_count <> 1 OR v_final_non_target_count <> 0 OR v_final_snapshot_contains <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'final MDM route binding verification failed';
  END IF;

  COMMIT;

  SELECT
    v_target_route_id AS target_route_id,
    v_target_route_version_id AS target_route_version_id,
    v_project_code AS project_code,
    v_expected_mdm_product_id AS mdm_product_id,
    v_target_route_product_id AS target_route_product_id,
    v_final_target_count AS final_target_mdm_bindings,
    v_final_non_target_count AS final_non_target_mdm_bindings,
    v_final_snapshot_contains AS final_snapshot_contains_mdm;
END//
DELIMITER ;

CALL codex_bind_press_balloon_project_mdm();
DROP PROCEDURE IF EXISTS codex_bind_press_balloon_project_mdm;
