-- AC-M04 / RRM local PQC-selected-process production-task prerequisite repair.
-- Scope: local Docker MySQL ruoyi-vue-pro only.
-- Inserts exactly one task-owned mes_pro_task row after strict source and collision checks.

DROP PROCEDURE IF EXISTS codex_acm04_rrm_pqc_selected_process_task_apply;

DELIMITER //
CREATE PROCEDURE codex_acm04_rrm_pqc_selected_process_task_apply()
BEGIN
    DECLARE v_count INT DEFAULT 0;

    SELECT COUNT(*)
      INTO v_count
      FROM mes_pro_work_order
     WHERE id = 980008
       AND code = 'RRM-20260801-PP-MO-001'
       AND product_id = 902149
       AND quantity = 300.00
       AND status = 1
       AND deleted = b'0'
       AND tenant_id = 1;
    IF v_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RRM source work order is missing or ambiguous';
    END IF;

    SELECT COUNT(*)
      INTO v_count
      FROM mes_pqc_inspection_task
     WHERE id = 68
       AND active_order_id = 12
       AND work_order_id = 980008
       AND route_id = 922119
       AND route_version_id = 448
       AND route_process_id = 928611
       AND process_id = 922987
       AND regulation_version_id = 18
       AND inspection_type = 'PATROL'
       AND task_status = 'PENDING'
       AND deleted = b'0'
       AND tenant_id = 1;
    IF v_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RRM selected PQC task source is missing or changed';
    END IF;

    SELECT COUNT(*)
      INTO v_count
      FROM mes_pro_route_process
     WHERE id = 928611
       AND route_id = 922119
       AND process_id = 922987
       AND workstation_id = 980009
       AND sort = 3
       AND deleted = b'0'
       AND tenant_id = 1;
    IF v_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RRM selected route process is missing or ambiguous';
    END IF;

    SELECT COUNT(*)
      INTO v_count
      FROM mes_pro_process
     WHERE id = 922987
       AND code = 'ER1A05996F5AA2'
       AND status = 0
       AND deleted = b'0'
       AND tenant_id = 1;
    IF v_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RRM selected process is missing or disabled';
    END IF;

    SELECT COUNT(*)
      INTO v_count
      FROM mes_md_workstation
     WHERE id = 980009
       AND code = 'RRM-20260801-PP-WS-928611'
       AND process_id = 922987
       AND status = 0
       AND deleted = b'0'
       AND tenant_id = 1;
    IF v_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RRM selected workstation is missing or disabled';
    END IF;

    SELECT COUNT(*)
      INTO v_count
      FROM mes_md_item
     WHERE id = 902149
       AND code = 'AW.107.02.01.2010'
       AND deleted = b'0'
       AND tenant_id = 1;
    IF v_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RRM source item is missing or ambiguous';
    END IF;

    SELECT COUNT(*)
      INTO v_count
      FROM mes_pro_task
     WHERE id = 981939
       AND code = 'PT-52097'
       AND work_order_id = 980008
       AND workstation_id = 980008
       AND route_id = 922119
       AND process_id = 922986
       AND item_id = 902149
       AND quantity = 10.000000
       AND start_time = '2026-08-05 08:00:00'
       AND duration = 1
       AND end_time = '2026-08-05 09:00:00'
       AND status = 0
       AND deleted = b'0'
       AND tenant_id = 1;
    IF v_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Adjacent RRM production task source is missing or changed';
    END IF;

    SELECT COUNT(*)
      INTO v_count
      FROM mes_pro_task
     WHERE id = 981940
       AND code = 'RRM-20260805-PRIMARY-922985'
       AND work_order_id = 980008
       AND workstation_id = 980010
       AND route_id = 922119
       AND process_id = 922985
       AND item_id = 902149
       AND quantity = 10.000000
       AND status = 0
       AND deleted = b'0'
       AND tenant_id = 1;
    IF v_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RRM primary production task source is missing or changed';
    END IF;

    SELECT COUNT(*)
      INTO v_count
      FROM mes_pro_task
     WHERE id = 981941
        OR code = 'RRM-20260805-PQC-922987'
        OR (
             work_order_id = 980008
         AND route_id = 922119
         AND process_id = 922987
        );
    IF v_count <> 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RRM selected production task id, code, or semantic-key collision';
    END IF;

    INSERT INTO mes_pro_task (
        id, code, name, work_order_id, workstation_id, route_id, process_id, item_id,
        quantity, produced_quantity, qualify_quantity, unqualify_quantity, changed_quantity,
        client_id, start_time, duration, end_time, color_code, finish_date, cancel_date,
        status, remark, creator, create_time, updater, update_time, deleted, tenant_id
    ) VALUES (
        981941,
        'RRM-20260805-PQC-922987',
        'RRM PQC selected process source task 922987',
        980008,
        980009,
        922119,
        922987,
        902149,
        10.000000,
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        '2026-08-05 08:00:00',
        1,
        '2026-08-05 09:00:00',
        '#00AEF3',
        NULL,
        NULL,
        0,
        'rrm-ac-m04-pqc-source-task-922987',
        'rrm-acm04',
        NOW(),
        'rrm-acm04',
        NOW(),
        b'0',
        1
    );
    IF ROW_COUNT() <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RRM selected production task insert did not affect exactly one row';
    END IF;

    SELECT COUNT(*)
      INTO v_count
      FROM mes_pro_task
     WHERE id = 981941
       AND code = 'RRM-20260805-PQC-922987'
       AND name = 'RRM PQC selected process source task 922987'
       AND work_order_id = 980008
       AND workstation_id = 980009
       AND route_id = 922119
       AND process_id = 922987
       AND item_id = 902149
       AND quantity = 10.000000
       AND produced_quantity IS NULL
       AND qualify_quantity IS NULL
       AND unqualify_quantity IS NULL
       AND changed_quantity IS NULL
       AND client_id IS NULL
       AND start_time = '2026-08-05 08:00:00'
       AND duration = 1
       AND end_time = '2026-08-05 09:00:00'
       AND color_code = '#00AEF3'
       AND finish_date IS NULL
       AND cancel_date IS NULL
       AND status = 0
       AND remark = 'rrm-ac-m04-pqc-source-task-922987'
       AND creator = 'rrm-acm04'
       AND updater = 'rrm-acm04'
       AND deleted = b'0'
       AND tenant_id = 1;
    IF v_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RRM selected production task post-verification failed';
    END IF;
END//
DELIMITER ;

-- Keep procedure DDL outside the transaction because MySQL implicitly commits around DDL.
START TRANSACTION;

CALL codex_acm04_rrm_pqc_selected_process_task_apply();

COMMIT;

DROP PROCEDURE IF EXISTS codex_acm04_rrm_pqc_selected_process_task_apply;
