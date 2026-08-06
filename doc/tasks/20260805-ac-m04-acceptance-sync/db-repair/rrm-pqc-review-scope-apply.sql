-- AC-M04 / RRM local PQC review-scope prerequisite repair.
-- Scope: local Docker MySQL ruoyi-vue-pro only.
-- Inserts exactly one task-owned PQC EMPLOYEE scope after strict source checks.

DROP PROCEDURE IF EXISTS codex_acm04_rrm_pqc_review_scope_apply;

DELIMITER //
CREATE PROCEDURE codex_acm04_rrm_pqc_review_scope_apply()
BEGIN
    DECLARE v_count INT DEFAULT 0;

    SELECT COUNT(*)
      INTO v_count
      FROM system_users
     WHERE id = 512
       AND username = 'huzonggang'
       AND status = 0
       AND deleted = b'0'
       AND tenant_id = 1;
    IF v_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RRM PQC leader source user is missing or disabled';
    END IF;

    SELECT COUNT(*)
      INTO v_count
      FROM system_users
     WHERE id = 914524
       AND username = 'pqce2efullscreen'
       AND status = 0
       AND deleted = b'0'
       AND tenant_id = 1;
    IF v_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RRM PQC actual employee source user is missing or disabled';
    END IF;

    SELECT COUNT(*)
      INTO v_count
      FROM mes_pro_process_pool_team_leader_scope
     WHERE id = 980013
       AND leader_user_id = 512
       AND leader_type = 'PQC'
       AND scope_type = 'EMPLOYEE'
       AND employee_user_id = 659
       AND enabled = b'1'
       AND deleted = b'0'
       AND tenant_id = 1;
    IF v_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Existing RRM PQC leader scope source is missing or changed';
    END IF;

    SELECT COUNT(*)
      INTO v_count
      FROM mes_pro_process_pool_team_leader_scope
     WHERE id = 980024
       AND leader_user_id = 914524
       AND leader_type = 'PQC'
       AND scope_type = 'EMPLOYEE'
       AND employee_user_id = 914524
       AND enabled = b'1'
       AND deleted = b'0'
       AND tenant_id = 1;
    IF v_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'PQC actual employee formal source scope is missing or changed';
    END IF;

    SELECT COUNT(*)
      INTO v_count
      FROM mes_pro_process_pool_event event_row
      JOIN mes_pro_process_pool_pqc_record pqc_record
        ON pqc_record.event_id = event_row.id
       AND pqc_record.tenant_id = event_row.tenant_id
       AND pqc_record.deleted = b'0'
     WHERE event_row.id = 133
       AND event_row.event_type = 'PQC_INSPECTION'
       AND event_row.work_order_id = 980008
       AND event_row.route_id = 922119
       AND event_row.route_process_id = 928610
       AND event_row.process_id = 922986
       AND event_row.actual_employee_id = 914524
       AND event_row.signature_id = 99009104
       AND event_row.signature_user_id = 914524
       AND event_row.pqc_task_id = 93
       AND event_row.deleted = b'0'
       AND event_row.tenant_id = 1
       AND pqc_record.id = 90
       AND pqc_record.production_submit_event_id = 132
       AND pqc_record.actual_employee_id = 914524
       AND pqc_record.signature_id = 99009104
       AND pqc_record.signature_user_id = 914524;
    IF v_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RRM PQC submitted event source is missing or changed';
    END IF;

    SELECT COUNT(*)
      INTO v_count
      FROM mes_pro_process_pool_team_leader_scope
     WHERE id = 980041
        OR (
             leader_user_id = 512
         AND leader_type = 'PQC'
         AND scope_type = 'EMPLOYEE'
         AND employee_user_id = 914524
         AND deleted = b'0'
         AND tenant_id = 1
        );
    IF v_count <> 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RRM PQC review scope id or semantic-key collision';
    END IF;

    INSERT INTO mes_pro_process_pool_team_leader_scope (
        id, leader_user_id, leader_type, scope_type, employee_user_id,
        process_id, workstation_id, production_line_id, equipment_id, work_order_id,
        enabled, remark, creator, create_time, updater, update_time, deleted, tenant_id
    ) VALUES (
        980041,
        512,
        'PQC',
        'EMPLOYEE',
        914524,
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        b'1',
        'rrm-ac-m04-pqc-review-scope',
        'rrm-acm04',
        NOW(),
        'rrm-acm04',
        NOW(),
        b'0',
        1
    );
    IF ROW_COUNT() <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RRM PQC review scope insert did not affect exactly one row';
    END IF;

    SELECT COUNT(*)
      INTO v_count
      FROM mes_pro_process_pool_team_leader_scope
     WHERE id = 980041
       AND leader_user_id = 512
       AND leader_type = 'PQC'
       AND scope_type = 'EMPLOYEE'
       AND employee_user_id = 914524
       AND process_id IS NULL
       AND workstation_id IS NULL
       AND production_line_id IS NULL
       AND equipment_id IS NULL
       AND work_order_id IS NULL
       AND enabled = b'1'
       AND remark = 'rrm-ac-m04-pqc-review-scope'
       AND creator = 'rrm-acm04'
       AND updater = 'rrm-acm04'
       AND deleted = b'0'
       AND tenant_id = 1;
    IF v_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RRM PQC review scope post-verification failed';
    END IF;
END//
DELIMITER ;

-- Keep procedure DDL outside the transaction because MySQL implicitly commits around DDL.
START TRANSACTION;

CALL codex_acm04_rrm_pqc_review_scope_apply();

COMMIT;

DROP PROCEDURE IF EXISTS codex_acm04_rrm_pqc_review_scope_apply;
