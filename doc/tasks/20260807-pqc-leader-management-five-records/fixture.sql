-- Task-owned pending PQC rounds only. Formal PQC submissions must still be created through the real frontend.
DROP PROCEDURE IF EXISTS codx_pqc_20260807_apply;
DELIMITER $$
CREATE PROCEDURE codx_pqc_20260807_apply()
BEGIN
    DECLARE v_count INT DEFAULT 0;

    SELECT COUNT(*) INTO v_count
      FROM mes_pqc_inspection_task
     WHERE tenant_id = 1
       AND deleted = b'0'
       AND creator = 'CODX-PQC-20260807';
    IF v_count <> 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'CODX-PQC-20260807 task fixture already exists';
    END IF;

    SELECT COUNT(*) INTO v_count
      FROM mes_pro_process_pool_event
     WHERE tenant_id = 1
       AND deleted = b'0'
       AND event_idempotency_key LIKE 'CODX-PQC-20260807%';
    IF v_count <> 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'CODX-PQC-20260807 formal submissions already exist';
    END IF;

    SELECT COUNT(*) INTO v_count
      FROM mes_pro_process_pool_active_order
     WHERE id = 12
       AND tenant_id = 1
       AND deleted = b'0'
       AND leader_user_id = 1520
       AND work_order_id = 980008
       AND route_id = 922119
       AND route_version_id = 448
       AND active_status IN ('ACTIVE', 'REMOVED');
    IF v_count <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'formal active order 12 prerequisite is missing';
    END IF;

    SELECT COUNT(*) INTO v_count
      FROM mes_pro_process_pool_event
     WHERE id = 131
       AND tenant_id = 1
       AND deleted = b'0'
       AND event_type = 'PRODUCTION_SUBMIT'
       AND work_order_id = 980008
       AND route_id = 922119
       AND route_process_id = 928609
       AND process_id = 922985
       AND device_account_id = 964
       AND device_id = 41
       AND workstation_id = 980010;
    IF v_count <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'formal production source event 131 prerequisite is missing';
    END IF;

    SELECT COUNT(*) INTO v_count
      FROM mes_qa_inspection_regulation regulation
      JOIN mes_qa_inspection_regulation_version version
        ON version.id = regulation.current_version_id
       AND version.tenant_id = regulation.tenant_id
       AND version.deleted = b'0'
     WHERE regulation.id = 16
       AND regulation.tenant_id = 1
       AND regulation.deleted = b'0'
       AND regulation.product_id = 902149
       AND regulation.route_id = 922119
       AND regulation.route_version_id = 448
       AND regulation.route_process_id = 928609
       AND regulation.process_id = 922985
       AND regulation.current_version_id = 16
       AND regulation.lifecycle_status = 'PUBLISHED'
       AND version.lifecycle_status = 'PUBLISHED';
    IF v_count <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'published QA regulation 16 prerequisite is missing';
    END IF;

    SELECT COUNT(*) INTO v_count
      FROM mes_qa_inspection_regulation_item
     WHERE tenant_id = 1
       AND deleted = b'0'
       AND regulation_version_id = 16
       AND inspection_type = 'PATROL'
       AND item_code IS NOT NULL
       AND item_name IS NOT NULL
       AND inspection_method IS NOT NULL
       AND standard_text IS NOT NULL
       AND result_type IS NOT NULL
       AND equipment_required = b'1';
    IF v_count < 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'structured PATROL QA items are missing';
    END IF;

    SELECT COUNT(*) INTO v_count
      FROM mes_pqc_inspection_task
     WHERE tenant_id = 1
       AND active_order_id = 12
       AND route_process_id = 928609
       AND inspection_type = 'PATROL'
       AND business_date = '2026-08-07'
       AND shift_code = 'CODX5'
       AND round_no BETWEEN 80701 AND 80705;
    IF v_count <> 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'task identity collision for CODX-PQC-20260807';
    END IF;

    START TRANSACTION;
    INSERT INTO mes_pqc_inspection_task (
        active_order_id, work_order_id, route_id, route_version_id, route_process_id, process_id,
        regulation_version_id, inspection_type, business_date, shift_code, round_no,
        planned_inspection_quantity, actual_inspection_quantity, task_status,
        creator, create_time, updater, update_time, deleted, tenant_id
    ) VALUES
        (12, 980008, 922119, 448, 928609, 922985, 16, 'PATROL', '2026-08-07', 'CODX5', 80701, 15, 0, 'PENDING', 'CODX-PQC-20260807', NOW(), 'CODX-PQC-20260807', NOW(), b'0', 1),
        (12, 980008, 922119, 448, 928609, 922985, 16, 'PATROL', '2026-08-07', 'CODX5', 80702, 15, 0, 'PENDING', 'CODX-PQC-20260807', NOW(), 'CODX-PQC-20260807', NOW(), b'0', 1),
        (12, 980008, 922119, 448, 928609, 922985, 16, 'PATROL', '2026-08-07', 'CODX5', 80703, 15, 0, 'PENDING', 'CODX-PQC-20260807', NOW(), 'CODX-PQC-20260807', NOW(), b'0', 1),
        (12, 980008, 922119, 448, 928609, 922985, 16, 'PATROL', '2026-08-07', 'CODX5', 80704, 15, 0, 'PENDING', 'CODX-PQC-20260807', NOW(), 'CODX-PQC-20260807', NOW(), b'0', 1),
        (12, 980008, 922119, 448, 928609, 922985, 16, 'PATROL', '2026-08-07', 'CODX5', 80705, 15, 0, 'PENDING', 'CODX-PQC-20260807', NOW(), 'CODX-PQC-20260807', NOW(), b'0', 1);

    IF ROW_COUNT() <> 5 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'expected exactly five PQC task rows';
    END IF;
    COMMIT;
END$$
DELIMITER ;
CALL codx_pqc_20260807_apply();
DROP PROCEDURE codx_pqc_20260807_apply;

SELECT id, work_order_id, route_process_id, process_id, regulation_version_id,
       inspection_type, business_date, shift_code, round_no, planned_inspection_quantity, task_status
  FROM mes_pqc_inspection_task
 WHERE tenant_id = 1
   AND deleted = b'0'
   AND creator = 'CODX-PQC-20260807'
 ORDER BY round_no;
