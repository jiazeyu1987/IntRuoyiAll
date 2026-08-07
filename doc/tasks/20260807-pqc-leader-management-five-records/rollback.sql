-- This rollback is valid only before any task-owned PQC submission has been created.
DROP PROCEDURE IF EXISTS codx_pqc_20260807_rollback;
DELIMITER $$
CREATE PROCEDURE codx_pqc_20260807_rollback()
BEGIN
    DECLARE v_task_count INT DEFAULT 0;
    DECLARE v_submission_count INT DEFAULT 0;

    SELECT COUNT(*) INTO v_task_count
      FROM mes_pqc_inspection_task
     WHERE tenant_id = 1
       AND deleted = b'0'
       AND creator = 'CODX-PQC-20260807';
    IF v_task_count <> 5 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'rollback requires exactly five task-owned pending rows';
    END IF;

    SELECT COUNT(*) INTO v_submission_count
      FROM mes_pro_process_pool_event event
      JOIN mes_pqc_inspection_task task ON task.id = event.pqc_task_id
     WHERE event.tenant_id = 1
       AND event.deleted = b'0'
       AND task.tenant_id = 1
       AND task.deleted = b'0'
       AND task.creator = 'CODX-PQC-20260807';
    IF v_submission_count <> 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'formal submissions exist; automatic rollback is forbidden';
    END IF;

    DELETE FROM mes_pqc_inspection_task
     WHERE tenant_id = 1
       AND deleted = b'0'
       AND creator = 'CODX-PQC-20260807'
       AND task_status = 'PENDING';
    IF ROW_COUNT() <> 5 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'rollback did not delete exactly five pending rows';
    END IF;
END$$
DELIMITER ;
CALL codx_pqc_20260807_rollback();
DROP PROCEDURE codx_pqc_20260807_rollback;
