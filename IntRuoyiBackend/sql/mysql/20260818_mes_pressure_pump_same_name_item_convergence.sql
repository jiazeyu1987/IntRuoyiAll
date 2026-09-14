-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260814_mes_c015_route_dcc_qa_reconciliation_schema; type=data; riskLevel=medium
-- Converges the AW.107.02.01.1009 pressure-pump material into the formal RT000028 route-product set.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS converge_mes_pressure_pump_same_name_item;
DELIMITER $$
CREATE PROCEDURE converge_mes_pressure_pump_same_name_item()
BEGIN
  DECLARE v_target_tenant_id bigint DEFAULT 1;
  DECLARE v_target_route_id bigint DEFAULT 922119;
  DECLARE v_target_item_id bigint DEFAULT 902101;
  DECLARE v_target_item_code varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
    DEFAULT 'AW.107.02.01.1009';
  DECLARE v_target_route_code varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
    DEFAULT 'RT000028';
  DECLARE v_enabled_status varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'ENABLE';
  DECLARE v_expected_product_master_id bigint DEFAULT 11;
  DECLARE v_expected_dcc_project_code_id bigint DEFAULT 147;
  DECLARE v_required_table_count int DEFAULT 0;
  DECLARE v_target_item_count bigint DEFAULT 0;
  DECLARE v_target_route_count bigint DEFAULT 0;
  DECLARE v_route_dcc_count bigint DEFAULT 0;
  DECLARE v_route_product_master_drift_count bigint DEFAULT 0;
  DECLARE v_existing_route_product_count bigint DEFAULT 0;
  DECLARE v_inserted_count bigint DEFAULT 0;
  DECLARE v_final_route_product_count bigint DEFAULT 0;

  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  START TRANSACTION;

  SELECT COUNT(1)
    INTO v_required_table_count
    FROM information_schema.tables
   WHERE table_schema = DATABASE()
     AND table_name IN ('mes_md_item', 'mes_pro_route', 'mes_pro_route_product',
                        'dcc_project_code', 'mes_pro_route_dcc_project_binding');
  IF v_required_table_count <> 5 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Pressure pump convergence failed: required tables are missing';
  END IF;

  SELECT COUNT(1)
    INTO v_target_item_count
    FROM `mes_md_item`
   WHERE `tenant_id` = v_target_tenant_id
     AND `id` = v_target_item_id
     AND `code` COLLATE utf8mb4_unicode_ci = v_target_item_code
     AND `product_master_id` = v_expected_product_master_id
     AND `deleted` = b'0';
  IF v_target_item_count <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Pressure pump convergence failed: target item identity drifted';
  END IF;

  SELECT COUNT(1)
    INTO v_target_route_count
    FROM `mes_pro_route`
   WHERE `tenant_id` = v_target_tenant_id
     AND `id` = v_target_route_id
     AND `code` COLLATE utf8mb4_unicode_ci = v_target_route_code
     AND `deleted` = b'0';
  IF v_target_route_count <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Pressure pump convergence failed: target route identity drifted';
  END IF;

  SELECT COUNT(1)
    INTO v_route_dcc_count
    FROM `mes_pro_route_dcc_project_binding` `binding`
    JOIN `dcc_project_code` `project`
      ON `project`.`tenant_id` = `binding`.`tenant_id`
     AND `project`.`id` = `binding`.`dcc_project_code_id`
     AND `project`.`deleted` = b'0'
   WHERE `binding`.`tenant_id` = v_target_tenant_id
     AND `binding`.`route_id` = v_target_route_id
     AND `binding`.`dcc_project_code_id` = v_expected_dcc_project_code_id
     AND `binding`.`deleted` = b'0'
     AND `project`.`status` COLLATE utf8mb4_unicode_ci = v_enabled_status
     AND `project`.`product_master_id` = v_expected_product_master_id;
  IF v_route_dcc_count <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Pressure pump convergence failed: route DCC product master drifted';
  END IF;

  SELECT COUNT(1)
    INTO v_route_product_master_drift_count
    FROM `mes_pro_route_product` `route_product`
    LEFT JOIN `mes_md_item` `item`
      ON `item`.`tenant_id` = `route_product`.`tenant_id`
     AND `item`.`id` = `route_product`.`item_id`
     AND `item`.`deleted` = b'0'
   WHERE `route_product`.`tenant_id` = v_target_tenant_id
     AND `route_product`.`route_id` = v_target_route_id
     AND `route_product`.`deleted` = b'0'
     AND (`item`.`id` IS NULL OR `item`.`product_master_id` <> v_expected_product_master_id);
  IF v_route_product_master_drift_count <> 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Pressure pump convergence failed: route DCC product master drifted';
  END IF;

  SELECT COUNT(1)
    INTO v_existing_route_product_count
    FROM `mes_pro_route_product`
   WHERE `tenant_id` = v_target_tenant_id
     AND `route_id` = v_target_route_id
     AND `item_id` = v_target_item_id
     AND `deleted` = b'0';
  IF v_existing_route_product_count > 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Pressure pump convergence failed: duplicate route product binding';
  END IF;

  IF v_existing_route_product_count = 0 THEN
    INSERT INTO `mes_pro_route_product`
      (`route_id`, `item_id`, `quantity`, `production_time`, `time_unit_type`,
       `remark`, `creator`, `updater`, `tenant_id`, `deleted`)
    VALUES
      (v_target_route_id, v_target_item_id, 1, 1.000000, 'MINUTE',
       'pressure-pump-same-name-item-convergence-20260818', '1', '1', v_target_tenant_id, b'0');
    SET v_inserted_count = ROW_COUNT();
    IF v_inserted_count <> 1 THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Pressure pump convergence failed: insert incomplete';
    END IF;
  END IF;

  SELECT COUNT(1)
    INTO v_final_route_product_count
    FROM `mes_pro_route_product`
   WHERE `tenant_id` = v_target_tenant_id
     AND `route_id` = v_target_route_id
     AND `item_id` = v_target_item_id
     AND `deleted` = b'0';
  IF v_final_route_product_count <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Pressure pump convergence failed: insert incomplete';
  END IF;

  COMMIT;
END$$
DELIMITER ;

CALL converge_mes_pressure_pump_same_name_item();
DROP PROCEDURE IF EXISTS converge_mes_pressure_pump_same_name_item;
