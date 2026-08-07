DROP PROCEDURE IF EXISTS codx_seed_active_order_five;

DELIMITER //
CREATE PROCEDURE codx_seed_active_order_five()
BEGIN
    DECLARE v_existing_count INT DEFAULT 0;
    DECLARE v_route_count INT DEFAULT 0;
    DECLARE v_version_count INT DEFAULT 0;
    DECLARE v_process_count INT DEFAULT 0;
    DECLARE v_product_count INT DEFAULT 0;
    DECLARE v_leader_count INT DEFAULT 0;
    DECLARE v_regulation_id BIGINT;
    DECLARE v_regulation_version_id BIGINT;
    DECLARE v_work_order_id BIGINT;
    DECLARE v_schedule_order_id BIGINT;
    DECLARE v_route_snapshot MEDIUMTEXT;
    DECLARE v_product_code VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    DECLARE v_product_name VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    DECLARE v_route_code VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    DECLARE v_route_name VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    DECLARE v_process_code VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    DECLARE v_process_name VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    DECLARE v_workstation_id BIGINT;
    DECLARE v_i INT DEFAULT 1;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    SELECT COUNT(*) INTO v_existing_count
    FROM mes_pro_work_order
    WHERE tenant_id = 1
      AND deleted = b'0'
      AND code LIKE 'CODX-AO5-20260807-%' COLLATE utf8mb4_unicode_ci;
    IF v_existing_count <> 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'AO5 fixture work orders already exist';
    END IF;

    SELECT COUNT(*) INTO v_existing_count
    FROM mes_pro_schedule_order
    WHERE tenant_id = 1
      AND deleted = b'0'
      AND code LIKE 'CODX-AO5-SCH-20260807-%' COLLATE utf8mb4_unicode_ci;
    IF v_existing_count <> 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'AO5 fixture schedules already exist';
    END IF;

    SELECT COUNT(*) INTO v_existing_count
    FROM mes_qa_inspection_regulation
    WHERE tenant_id = 1
      AND deleted = b'0'
      AND regulation_code = 'CODX-AO5-QA-20260807' COLLATE utf8mb4_unicode_ci;
    IF v_existing_count <> 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'AO5 fixture regulation already exists';
    END IF;

    SELECT COUNT(*) INTO v_existing_count
    FROM mes_qa_inspection_regulation
    WHERE tenant_id = 1
      AND product_id = 924008
      AND route_id = 980091
      AND route_version_id = 622
      AND route_process_id = 980631
      AND deleted = b'0';
    IF v_existing_count <> 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Target route process already has a QA regulation';
    END IF;

    SELECT COUNT(*) INTO v_route_count
    FROM mes_pro_route
    WHERE id = 980091 AND tenant_id = 1 AND status = 0 AND deleted = b'0';
    SELECT COUNT(*) INTO v_version_count
    FROM mes_pro_route_version
    WHERE id = 622
      AND route_id = 980091
      AND tenant_id = 1
      AND active = b'1'
      AND lifecycle_status = 'ACTIVE'
      AND deleted = b'0';
    SELECT COUNT(*), MAX(p.code), MAX(p.name), MAX(rp.workstation_id)
      INTO v_process_count, v_process_code, v_process_name, v_workstation_id
    FROM mes_pro_route_process rp
    JOIN mes_pro_process p
      ON p.id = rp.process_id AND p.tenant_id = rp.tenant_id AND p.deleted = b'0'
    WHERE rp.id = 980631
      AND rp.route_id = 980091
      AND rp.process_id = 922985
      AND rp.tenant_id = 1
      AND rp.deleted = b'0';
    SELECT COUNT(*), MAX(i.code), MAX(i.name)
      INTO v_product_count, v_product_code, v_product_name
    FROM mes_pro_route_product rp
    JOIN mes_md_item i
      ON i.id = rp.item_id AND i.tenant_id = rp.tenant_id AND i.deleted = b'0' AND i.status = 0
    WHERE rp.route_id = 980091
      AND rp.item_id = 924008
      AND rp.tenant_id = 1
      AND rp.deleted = b'0';
    SELECT COUNT(*), MAX(code), MAX(name)
      INTO v_route_count, v_route_code, v_route_name
    FROM mes_pro_route
    WHERE id = 980091 AND tenant_id = 1 AND status = 0 AND deleted = b'0';
    SELECT COUNT(*) INTO v_leader_count
    FROM system_users
    WHERE id = 1 AND tenant_id = 1 AND username = 'admin' AND status = 0 AND deleted = b'0';

    IF v_route_count <> 1 OR v_version_count <> 1 OR v_process_count <> 1
       OR v_product_count <> 1 OR v_leader_count <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'AO5 formal source precondition is missing';
    END IF;
    IF v_workstation_id IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'AO5 workstation is missing';
    END IF;

    SET v_route_snapshot = JSON_OBJECT(
        'fixture', 'CODX-AO5-20260807',
        'routeId', 980091,
        'routeCode', v_route_code,
        'routeName', v_route_name,
        'routeVersionId', 622,
        'routeVersion', 'V1',
        'processes', JSON_ARRAY(JSON_OBJECT(
            'routeProcessId', 980631,
            'processId', 922985,
            'processCode', v_process_code,
            'processName', v_process_name,
            'workstationId', v_workstation_id,
            'sort', 1,
            'predecessorRouteProcessId', NULL,
            'rootProcessFlag', TRUE,
            'enabled', TRUE,
            'productionQuantityFactor', 1.000000
        ))
    );

    INSERT INTO mes_qa_inspection_regulation (
        product_id, route_id, route_version_id, route_process_id, process_id,
        owner_module, regulation_code, regulation_name, lifecycle_status,
        current_version_id, creator, create_time, updater, update_time, deleted, tenant_id
    ) VALUES (
        924008, 980091, 622, 980631, 922985,
        'QA', 'CODX-AO5-QA-20260807', 'CODX AO5 QA regulation', 'PUBLISHED',
        NULL, 'codx-ao5-20260807', NOW(), 'codx-ao5-20260807', NOW(), b'0', 1
    );
    SET v_regulation_id = LAST_INSERT_ID();

    INSERT INTO mes_qa_inspection_regulation_version (
        regulation_id, version_no, lifecycle_status, published_at, retired_at,
        final_inspection_applicable, final_inspection_not_applicable_reason,
        snapshot_json, creator, create_time, updater, update_time, deleted, tenant_id
    ) VALUES (
        v_regulation_id, 'V1', 'PUBLISHED', NOW(), NULL,
        b'1', NULL,
        JSON_OBJECT(
            'fixture', 'CODX-AO5-20260807',
            'productId', 924008,
            'routeId', 980091,
            'routeVersionId', 622,
            'routeProcessId', 980631,
            'processId', 922985,
            'finalInspectionApplicable', TRUE
        ),
        'codx-ao5-20260807', NOW(), 'codx-ao5-20260807', NOW(), b'0', 1
    );
    SET v_regulation_version_id = LAST_INSERT_ID();

    UPDATE mes_qa_inspection_regulation
    SET current_version_id = v_regulation_version_id,
        updater = 'codx-ao5-20260807',
        update_time = NOW()
    WHERE id = v_regulation_id AND tenant_id = 1 AND deleted = b'0';

    INSERT INTO mes_qa_inspection_regulation_item (
        regulation_version_id, inspection_type, item_code, item_name,
        inspection_method, standard_text, standard_lower_limit, standard_upper_limit,
        standard_unit, standard_precision, equipment_required, result_type,
        first_inspection_quantity, patrol_inspection_ratio,
        creator, create_time, updater, update_time, deleted, tenant_id
    ) VALUES
        (v_regulation_version_id, 'FIRST', 'CODX-AO5-QA-FIRST', 'AO5 first inspection',
         'Visual inspection', 'Pass', NULL, NULL, NULL, NULL, b'0', 'CHOICE',
         2, NULL, 'codx-ao5-20260807', NOW(), 'codx-ao5-20260807', NOW(), b'0', 1),
        (v_regulation_version_id, 'PATROL', 'CODX-AO5-QA-PATROL', 'AO5 patrol inspection',
         'Visual inspection', 'Pass', NULL, NULL, NULL, NULL, b'0', 'CHOICE',
         NULL, 0.100000, 'codx-ao5-20260807', NOW(), 'codx-ao5-20260807', NOW(), b'0', 1),
        (v_regulation_version_id, 'FINAL', 'CODX-AO5-QA-FINAL', 'AO5 final inspection',
         'Visual inspection', 'Pass', NULL, NULL, NULL, NULL, b'0', 'CHOICE',
         3, NULL, 'codx-ao5-20260807', NOW(), 'codx-ao5-20260807', NOW(), b'0', 1);

    WHILE v_i <= 5 DO
        INSERT INTO mes_pro_work_order (
            code, name, type, order_source_type, order_source_code, product_id,
            quantity, quantity_produced, quantity_changed, quantity_scheduled,
            business_status, schedule_status, planned_start_time, planned_end_time,
            request_date, parent_id, status, temporary_frozen, remark,
            creator, create_time, updater, update_time, deleted, tenant_id
        ) VALUES (
            CONCAT('CODX-AO5-20260807-', LPAD(v_i, 2, '0')),
            CONCAT('CODX active order fixture ', LPAD(v_i, 2, '0')),
            1, 1, CONCAT('CODX-AO5-SOURCE-', LPAD(v_i, 2, '0')), 924008,
            10.00, 0.00, 0.00, 10.00,
            'CONFIRMED', 'SCHEDULED',
            TIMESTAMP('2026-08-07 08:00:00') + INTERVAL (v_i - 1) DAY,
            TIMESTAMP('2026-08-07 18:00:00') + INTERVAL (v_i - 1) DAY,
            TIMESTAMP('2026-08-07 18:00:00') + INTERVAL (v_i - 1) DAY,
            0, 1, b'0', 'CODX-AO5-20260807 task-owned data',
            'codx-ao5-20260807', NOW(), 'codx-ao5-20260807', NOW(), b'0', 1
        );
        SET v_work_order_id = LAST_INSERT_ID();

        INSERT INTO mes_pro_schedule_order (
            code, source_work_order_id, source_work_order_code, source_order_code,
            work_order_id, erp_work_order_code, product_id, quantity, promise_date,
            priority_no, status, diff_status, risk_status, route_status, auto_schedulable,
            route_id, route_version_id, route_version, schedule_config_version,
            planned_start_time, planned_end_time, total_quantity, completed_quantity,
            uncompleted_quantity, progress_percent, frozen, manual_finished,
            source_snapshot_json, route_snapshot_json, remark,
            creator, create_time, updater, update_time, deleted, tenant_id,
            planned_quantity, promised_delivery_date, priority, active_flag,
            scheduled_quantity, reported_quantity, product_code, product_name,
            route_code, route_name
        ) VALUES (
            CONCAT('CODX-AO5-SCH-20260807-', LPAD(v_i, 2, '0')),
            v_work_order_id, CONCAT('CODX-AO5-20260807-', LPAD(v_i, 2, '0')),
            CONCAT('CODX-AO5-SOURCE-', LPAD(v_i, 2, '0')),
            v_work_order_id, CONCAT('CODX-AO5-20260807-', LPAD(v_i, 2, '0')),
            924008, 10.000000,
            DATE('2026-08-07') + INTERVAL (v_i - 1) DAY,
            100, 1, 0, 0, 1, b'1',
            980091, 622, 'V1', 'CODX-AO5-V1',
            TIMESTAMP('2026-08-07 08:00:00') + INTERVAL (v_i - 1) DAY,
            TIMESTAMP('2026-08-07 18:00:00') + INTERVAL (v_i - 1) DAY,
            10.000000, 0.000000, 10.000000, 0.000000,
            b'0', b'0',
            JSON_OBJECT('fixture', 'CODX-AO5-20260807', 'workOrderId', v_work_order_id),
            v_route_snapshot, 'CODX-AO5-20260807 task-owned schedule',
            'codx-ao5-20260807', NOW(), 'codx-ao5-20260807', NOW(), b'0', 1,
            10.000000,
            TIMESTAMP('2026-08-07 18:00:00') + INTERVAL (v_i - 1) DAY,
            5, 1, 10.000000, 0.000000,
            v_product_code, v_product_name, v_route_code, v_route_name
        );
        SET v_schedule_order_id = LAST_INSERT_ID();

        INSERT INTO mes_pro_schedule_order_process (
            schedule_order_id, route_process_id, predecessor_route_process_id,
            root_process_flag, route_version_id, route_schedule_config_id,
            process_id, sort, enabled, capacity_source, capacity_mode,
            hourly_capacity_total, infinite_duration_quantity_factor,
            infinite_duration_base_minutes, planned_quantity, reported_quantity,
            remaining_quantity, progress_percent, night_shift_enabled,
            calendar_rule_id, key_process_flag, plan_date, planned_start_time,
            planned_end_time, bottleneck_flag, remark,
            creator, create_time, updater, update_time, deleted, tenant_id,
            source_work_order_id, route_id, process_code, process_name,
            workstation_id, scheduling_enabled, status, production_quantity_factor,
            resource_snapshot_json
        ) VALUES (
            v_schedule_order_id, 980631, NULL,
            b'1', 622, NULL,
            922985, 1, b'1', 'FORMAL_ROUTE_VERSION', 'FINITE',
            0.000000, NULL, NULL,
            10.000000, 0.000000, 10.000000, 0.000000, b'0',
            NULL, b'0', DATE('2026-08-07') + INTERVAL (v_i - 1) DAY,
            TIMESTAMP('2026-08-07 08:00:00') + INTERVAL (v_i - 1) DAY,
            TIMESTAMP('2026-08-07 18:00:00') + INTERVAL (v_i - 1) DAY,
            b'0', 'CODX-AO5-20260807 task-owned process',
            'codx-ao5-20260807', NOW(), 'codx-ao5-20260807', NOW(), b'0', 1,
            v_work_order_id, 980091, v_process_code, v_process_name,
            v_workstation_id, b'1', 0, 1.000000,
            JSON_OBJECT('fixture', 'CODX-AO5-20260807', 'routeProcessId', 980631)
        );

        SET v_i = v_i + 1;
    END WHILE;

    SELECT COUNT(*) INTO v_existing_count
    FROM mes_pro_work_order wo
    JOIN mes_pro_schedule_order so
      ON so.work_order_id = wo.id AND so.tenant_id = wo.tenant_id AND so.deleted = b'0'
    JOIN mes_pro_schedule_order_process sp
      ON sp.schedule_order_id = so.id AND sp.tenant_id = so.tenant_id AND sp.deleted = b'0'
    WHERE wo.tenant_id = 1
      AND wo.deleted = b'0'
      AND wo.code LIKE 'CODX-AO5-20260807-%' COLLATE utf8mb4_unicode_ci;
    IF v_existing_count <> 5 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'AO5 fixture final row count is not five';
    END IF;

    COMMIT;

    SELECT v_regulation_id AS regulation_id,
           v_regulation_version_id AS regulation_version_id,
           v_existing_count AS fixture_count;
END//
DELIMITER ;

CALL codx_seed_active_order_five();
DROP PROCEDURE IF EXISTS codx_seed_active_order_five;
