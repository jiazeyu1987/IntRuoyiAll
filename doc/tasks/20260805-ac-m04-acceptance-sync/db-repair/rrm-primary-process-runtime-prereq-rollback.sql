-- Roll back only the exact AC-M04 / RRM local primary-process fixture rows.

DROP PROCEDURE IF EXISTS codex_acm04_rrm_primary_process_runtime_prereq_rollback;

DELIMITER //
CREATE PROCEDURE codex_acm04_rrm_primary_process_runtime_prereq_rollback()
BEGIN
    DECLARE v_count INT DEFAULT 0;

    SELECT COUNT(*)
      INTO v_count
      FROM mes_pro_process_pool_team_leader_scope
     WHERE id IN (980039, 980040)
       AND leader_user_id = 1520
       AND leader_type = 'PRODUCTION'
       AND enabled = b'1'
       AND deleted = b'0'
       AND tenant_id = 1
       AND creator = 'rrm-acm04'
       AND updater = 'rrm-acm04'
       AND remark = 'rrm-ac-m04-runtime-prereq'
       AND (
            (id = 980039 AND scope_type = 'PROCESS' AND process_id = 922985
             AND employee_user_id IS NULL AND workstation_id IS NULL
             AND production_line_id IS NULL AND equipment_id IS NULL AND work_order_id IS NULL)
         OR (id = 980040 AND scope_type = 'WORKSTATION' AND workstation_id = 980010
             AND employee_user_id IS NULL AND process_id IS NULL
             AND production_line_id IS NULL AND equipment_id IS NULL AND work_order_id IS NULL)
       );
    IF v_count <> 2 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Primary RRM scope rollback precondition failed';
    END IF;

    SELECT COUNT(*)
      INTO v_count
      FROM mes_pro_process_pool_team_process_device
     WHERE id = 15
       AND leader_user_id = 1520
       AND process_id = 922985
       AND device_id = 41
       AND enabled = b'1'
       AND disabled_at IS NULL
       AND deleted = b'0'
       AND tenant_id = 1
       AND creator = 'rrm-acm04'
       AND updater = 'rrm-acm04'
       AND remark = 'rrm-ac-m04-runtime-prereq';
    IF v_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Primary RRM process-device rollback precondition failed';
    END IF;

    SELECT COUNT(*)
      INTO v_count
      FROM mes_pro_process_pool_team_employee_binding b
      JOIN mes_pro_process_pool_team_employee_profile p
        ON p.id = b.employee_profile_id
       AND p.deleted = b'0'
     WHERE b.id = 21
       AND b.leader_user_id = 1520
       AND b.process_id = 922985
       AND b.employee_profile_id = 980022
       AND b.employee_user_id = 964
       AND b.display_name_snapshot = COALESCE(NULLIF(TRIM(p.display_name), ''), NULLIF(TRIM(p.employee_name), ''))
       AND b.enabled = b'1'
       AND b.disabled_at IS NULL
       AND b.deleted = b'0'
       AND b.tenant_id = 1
       AND b.creator = 'rrm-acm04'
       AND b.updater = 'rrm-acm04'
       AND b.remark = 'rrm-ac-m04-runtime-prereq';
    IF v_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Primary RRM process-employee rollback precondition failed';
    END IF;

    DELETE FROM mes_pro_process_pool_team_employee_binding
     WHERE id = 21
       AND creator = 'rrm-acm04'
       AND updater = 'rrm-acm04'
       AND remark = 'rrm-ac-m04-runtime-prereq';
    IF ROW_COUNT() <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Primary RRM process-employee rollback delete failed';
    END IF;

    DELETE FROM mes_pro_process_pool_team_process_device
     WHERE id = 15
       AND creator = 'rrm-acm04'
       AND updater = 'rrm-acm04'
       AND remark = 'rrm-ac-m04-runtime-prereq';
    IF ROW_COUNT() <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Primary RRM process-device rollback delete failed';
    END IF;

    DELETE FROM mes_pro_process_pool_team_leader_scope
     WHERE id IN (980039, 980040)
       AND creator = 'rrm-acm04'
       AND updater = 'rrm-acm04'
       AND remark = 'rrm-ac-m04-runtime-prereq';
    IF ROW_COUNT() <> 2 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Primary RRM scope rollback delete failed';
    END IF;

    SELECT
        (SELECT COUNT(*) FROM mes_pro_process_pool_team_leader_scope WHERE id IN (980039, 980040))
      + (SELECT COUNT(*) FROM mes_pro_process_pool_team_process_device WHERE id = 15)
      + (SELECT COUNT(*) FROM mes_pro_process_pool_team_employee_binding WHERE id = 21)
      INTO v_count;
    IF v_count <> 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Primary RRM rollback post-verification failed';
    END IF;
END//
DELIMITER ;

-- Keep procedure DDL outside the transaction because MySQL implicitly commits around DDL.
START TRANSACTION;

CALL codex_acm04_rrm_primary_process_runtime_prereq_rollback();

COMMIT;

DROP PROCEDURE IF EXISTS codex_acm04_rrm_primary_process_runtime_prereq_rollback;
