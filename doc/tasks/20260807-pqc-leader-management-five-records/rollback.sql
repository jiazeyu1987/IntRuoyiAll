-- Valid only before the task-owned production or PQC frontend submission has been created.
DROP PROCEDURE IF EXISTS codx_pqc_20260807_rollback;
DELIMITER $$
CREATE PROCEDURE codx_pqc_20260807_rollback()
BEGIN
    DECLARE v_count INT DEFAULT 0;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
    SELECT COUNT(*) INTO v_count FROM mes_pro_process_pool_event
     WHERE tenant_id=1 AND deleted=b'0' AND event_idempotency_key LIKE 'CODX-PQC-20260807%';
    IF v_count <> 0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='formal events exist; automatic rollback is forbidden'; END IF;
    START TRANSACTION;
    DELETE FROM mes_pqc_inspection_task WHERE tenant_id=1 AND deleted=b'0' AND creator='CODX-PQC-20260807' AND task_status='PENDING';
    DELETE FROM mes_pro_edhr_recordbook WHERE tenant_id=1 AND deleted=b'0' AND creator='CODX-PQC-20260807' AND entry_count=0;
    DELETE FROM mes_pro_task WHERE tenant_id=1 AND deleted=b'0' AND creator='CODX-PQC-20260807';
    DELETE FROM mes_pro_process_pool_team_leader_scope WHERE tenant_id=1 AND deleted=b'0' AND creator='CODX-PQC-20260807';
    DELETE FROM mes_pro_process_pool_active_order_process_snapshot WHERE tenant_id=1 AND deleted=b'0' AND creator='CODX-PQC-20260807';
    SELECT
      (SELECT COUNT(*) FROM mes_pqc_inspection_task WHERE tenant_id=1 AND deleted=b'0' AND creator='CODX-PQC-20260807')
      + (SELECT COUNT(*) FROM mes_pro_edhr_recordbook WHERE tenant_id=1 AND deleted=b'0' AND creator='CODX-PQC-20260807')
      + (SELECT COUNT(*) FROM mes_pro_task WHERE tenant_id=1 AND deleted=b'0' AND creator='CODX-PQC-20260807')
      + (SELECT COUNT(*) FROM mes_pro_process_pool_team_leader_scope WHERE tenant_id=1 AND deleted=b'0' AND creator='CODX-PQC-20260807')
      + (SELECT COUNT(*) FROM mes_pro_process_pool_active_order_process_snapshot WHERE tenant_id=1 AND deleted=b'0' AND creator='CODX-PQC-20260807')
      INTO v_count;
    IF v_count <> 0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='rollback left task-owned prerequisites'; END IF;
    COMMIT;
END$$
DELIMITER ;
CALL codx_pqc_20260807_rollback();
DROP PROCEDURE codx_pqc_20260807_rollback;
