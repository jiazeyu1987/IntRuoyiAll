-- Rollback for the authorized formal MES_QA cleaning repair only.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @target_tenant_id := 1;
SET @target_active_order_id := 49;
SET @target_route_process_id := 980647;
SET @retired_fixture_regulation_id := 41;
SET @retired_fixture_version_id := 53;
SET @target_business_date := DATE('2026-08-09');
SET @actor := 'codex-pqc-formal-standard';

DROP PROCEDURE IF EXISTS codex_rollback_formal_cleaning_qa;
DELIMITER $$
CREATE PROCEDURE codex_rollback_formal_cleaning_qa()
BEGIN
    DECLARE v_count int DEFAULT 0;
    DECLARE v_affected int DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SET @new_regulation_id := (
        SELECT id
        FROM mes_qa_inspection_regulation
        WHERE tenant_id = @target_tenant_id
          AND route_process_id = @target_route_process_id
          AND owner_module = 'MES_QA'
          AND regulation_code = 'PQC-ID-001-RP980647'
          AND creator = @actor
          AND deleted = b'0'
    );
    SET @new_version_id := (
        SELECT current_version_id
        FROM mes_qa_inspection_regulation
        WHERE id = @new_regulation_id
          AND tenant_id = @target_tenant_id
          AND deleted = b'0'
    );

    START TRANSACTION;

    SELECT COUNT(*) INTO v_count
    FROM mes_qa_inspection_regulation
    WHERE id = @new_regulation_id
      AND owner_module = 'MES_QA'
      AND lifecycle_status = 'PUBLISHED'
      AND current_version_id = @new_version_id
      AND creator = @actor
      AND tenant_id = @target_tenant_id
      AND deleted = b'0';
    IF v_count <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Task-created formal regulation is missing or changed';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM mes_qa_inspection_regulation_version
    WHERE id = @new_version_id
      AND regulation_id = @new_regulation_id
      AND version_no = 'G/0'
      AND lifecycle_status = 'PUBLISHED'
      AND creator = @actor
      AND tenant_id = @target_tenant_id
      AND deleted = b'0';
    IF v_count <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Task-created formal version is missing or changed';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM mes_qa_inspection_regulation_item
    WHERE regulation_version_id = @new_version_id
      AND item_code = 'ID-001-WASH-APP'
      AND creator = @actor
      AND tenant_id = @target_tenant_id
      AND deleted = b'0';
    IF v_count <> 3 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Task-created formal items are missing or changed';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM mes_pqc_inspection_task
    WHERE active_order_id = @target_active_order_id
      AND route_process_id = @target_route_process_id
      AND regulation_version_id = @new_version_id
      AND business_date = @target_business_date
      AND task_status = 'PENDING'
      AND actual_inspection_quantity = 0
      AND creator = @actor
      AND tenant_id = @target_tenant_id
      AND deleted = b'0';
    IF v_count <> 4 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Task-created pending tasks are missing or changed';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM mes_pqc_inspection_piece_detail detail
    JOIN mes_pqc_inspection_task task
      ON task.id = detail.task_id
     AND task.tenant_id = detail.tenant_id
    WHERE task.regulation_version_id = @new_version_id
      AND task.creator = @actor
      AND task.tenant_id = @target_tenant_id
      AND task.deleted = b'0'
      AND detail.deleted = b'0';
    IF v_count <> 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Task-created pending tasks contain piece details';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM mes_qa_inspection_regulation
    WHERE id = @retired_fixture_regulation_id
      AND lifecycle_status = 'RETIRED'
      AND current_version_id IS NULL
      AND owner_module = 'CODX_QA'
      AND updater = @actor
      AND tenant_id = @target_tenant_id
      AND deleted = b'1';
    IF v_count <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Soft-deleted retired fixture regulation changed';
    END IF;

    DELETE FROM mes_pqc_inspection_task
    WHERE active_order_id = @target_active_order_id
      AND route_process_id = @target_route_process_id
      AND regulation_version_id = @new_version_id
      AND business_date = @target_business_date
      AND task_status = 'PENDING'
      AND actual_inspection_quantity = 0
      AND creator = 'codex-pqc-formal-standard'
      AND tenant_id = @target_tenant_id
      AND deleted = b'0';
    SET v_affected = ROW_COUNT();
    IF v_affected <> 4 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Task-created pending task delete count changed';
    END IF;

    DELETE FROM mes_qa_inspection_regulation_item
    WHERE regulation_version_id = @new_version_id
      AND creator = @actor
      AND tenant_id = @target_tenant_id;
    SET v_affected = ROW_COUNT();
    IF v_affected <> 3 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Task-created item delete count changed';
    END IF;

    DELETE FROM mes_qa_inspection_regulation_version
    WHERE id = @new_version_id
      AND regulation_id = @new_regulation_id
      AND creator = @actor
      AND tenant_id = @target_tenant_id;
    SET v_affected = ROW_COUNT();
    IF v_affected <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Task-created version delete count changed';
    END IF;

    DELETE FROM mes_qa_inspection_regulation
    WHERE id = @new_regulation_id
      AND owner_module = 'MES_QA'
      AND creator = @actor
      AND tenant_id = @target_tenant_id;
    SET v_affected = ROW_COUNT();
    IF v_affected <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Task-created regulation delete count changed';
    END IF;

    UPDATE mes_qa_inspection_regulation
    SET deleted = b'0',
        updater = '20260809-frontline-pqc-hide-unconfigured-processes',
        update_time = '2026-08-09 02:17:29'
    WHERE id = @retired_fixture_regulation_id
      AND lifecycle_status = 'RETIRED'
      AND current_version_id IS NULL
      AND owner_module = 'CODX_QA'
      AND updater = @actor
      AND tenant_id = @target_tenant_id
      AND deleted = b'1';
    SET v_affected = ROW_COUNT();
    IF v_affected <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Retired fixture regulation restore count changed';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM mes_pqc_inspection_task
    WHERE id IN (296, 297, 298, 299)
      AND task_status = 'CANCELLED'
      AND regulation_version_id = @retired_fixture_version_id
      AND updater = '20260809-frontline-pqc-hide-unconfigured-processes'
      AND tenant_id = @target_tenant_id
      AND deleted = b'0';
    IF v_count <> 4 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Cancelled fixture task audit changed during rollback';
    END IF;

    COMMIT;
END$$
DELIMITER ;

CALL codex_rollback_formal_cleaning_qa();
DROP PROCEDURE codex_rollback_formal_cleaning_qa;
