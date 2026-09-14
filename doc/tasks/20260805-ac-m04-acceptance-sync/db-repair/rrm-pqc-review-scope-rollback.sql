-- Roll back only the exact AC-M04 / RRM local PQC review scope.

DROP PROCEDURE IF EXISTS codex_acm04_rrm_pqc_review_scope_rollback;

DELIMITER //
CREATE PROCEDURE codex_acm04_rrm_pqc_review_scope_rollback()
BEGIN
    DECLARE v_count INT DEFAULT 0;

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
            SET MESSAGE_TEXT = 'RRM PQC review scope rollback precondition failed';
    END IF;

    DELETE FROM mes_pro_process_pool_team_leader_scope
     WHERE id = 980041
       AND leader_user_id = 512
       AND leader_type = 'PQC'
       AND scope_type = 'EMPLOYEE'
       AND employee_user_id = 914524
       AND remark = 'rrm-ac-m04-pqc-review-scope'
       AND creator = 'rrm-acm04'
       AND updater = 'rrm-acm04'
       AND deleted = b'0'
       AND tenant_id = 1;
    IF ROW_COUNT() <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RRM PQC review scope rollback delete failed';
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
         AND creator = 'rrm-acm04'
         AND remark = 'rrm-ac-m04-pqc-review-scope'
         AND tenant_id = 1
        );
    IF v_count <> 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RRM PQC review scope rollback post-verification failed';
    END IF;
END//
DELIMITER ;

-- Keep procedure DDL outside the transaction because MySQL implicitly commits around DDL.
START TRANSACTION;

CALL codex_acm04_rrm_pqc_review_scope_rollback();

COMMIT;

DROP PROCEDURE IF EXISTS codex_acm04_rrm_pqc_review_scope_rollback;
