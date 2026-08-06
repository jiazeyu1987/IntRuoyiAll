-- Roll back only the exact AC-M04 / RRM local primary-process production task.

DROP PROCEDURE IF EXISTS codex_acm04_rrm_primary_process_task_rollback;

DELIMITER //
CREATE PROCEDURE codex_acm04_rrm_primary_process_task_rollback()
BEGIN
    DECLARE v_count INT DEFAULT 0;

    SELECT COUNT(*)
      INTO v_count
      FROM mes_pro_task
     WHERE id = 981940
       AND code = 'RRM-20260805-PRIMARY-922985'
       AND name = 'RRM primary process source task 922985'
       AND work_order_id = 980008
       AND workstation_id = 980010
       AND route_id = 922119
       AND process_id = 922985
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
       AND remark = 'rrm-ac-m04-pqc-source-task'
       AND creator = 'rrm-acm04'
       AND updater = 'rrm-acm04'
       AND deleted = b'0'
       AND tenant_id = 1;
    IF v_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RRM primary production task rollback precondition failed';
    END IF;

    SELECT COUNT(*)
      INTO v_count
      FROM mes_pro_task_schedule_ext
     WHERE task_id = 981940;
    IF v_count <> 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RRM primary production task has schedule extension dependencies';
    END IF;

    SELECT
        (SELECT COUNT(*) FROM mes_pro_feedback WHERE task_id = 981940)
      + (SELECT COUNT(*) FROM mes_pro_task_issue WHERE task_id = 981940)
      INTO v_count;
    IF v_count <> 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RRM primary production task has downstream feedback or issue dependencies';
    END IF;

    DELETE FROM mes_pro_task
     WHERE id = 981940
       AND code = 'RRM-20260805-PRIMARY-922985'
       AND creator = 'rrm-acm04'
       AND updater = 'rrm-acm04'
       AND remark = 'rrm-ac-m04-pqc-source-task';
    IF ROW_COUNT() <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RRM primary production task rollback delete failed';
    END IF;

    SELECT COUNT(*)
      INTO v_count
      FROM mes_pro_task
     WHERE id = 981940
        OR code = 'RRM-20260805-PRIMARY-922985'
        OR (
             work_order_id = 980008
         AND route_id = 922119
         AND process_id = 922985
         AND creator = 'rrm-acm04'
         AND remark = 'rrm-ac-m04-pqc-source-task'
        );
    IF v_count <> 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RRM primary production task rollback post-verification failed';
    END IF;
END//
DELIMITER ;

-- Keep procedure DDL outside the transaction because MySQL implicitly commits around DDL.
START TRANSACTION;

CALL codex_acm04_rrm_primary_process_task_rollback();

COMMIT;

DROP PROCEDURE IF EXISTS codex_acm04_rrm_primary_process_task_rollback;
