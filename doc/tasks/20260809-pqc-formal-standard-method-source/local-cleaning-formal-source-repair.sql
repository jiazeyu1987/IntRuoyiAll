-- Authorized local-only repair for tenant 1, work order 881MO090889, active order 49.
-- Keeps the retired CODX_QA fixture history and creates a separate formal MES_QA regulation.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @target_tenant_id := 1;
SET @target_active_order_id := 49;
SET @target_work_order_id := 923889;
SET @target_product_id := 902149;
SET @target_route_id := 922119;
SET @target_route_version_id := 627;
SET @target_route_process_id := 980647;
SET @target_process_id := 922987;
SET @retired_fixture_regulation_id := 41;
SET @retired_fixture_version_id := 53;
SET @target_business_date := DATE('2026-08-09');
SET @target_version_no := 'G/0';
SET @actor := 'codex-pqc-formal-standard';

DROP PROCEDURE IF EXISTS codex_create_formal_cleaning_qa;
DELIMITER $$
CREATE PROCEDURE codex_create_formal_cleaning_qa()
BEGIN
    DECLARE v_count int DEFAULT 0;
    DECLARE v_affected int DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    SELECT COUNT(*) INTO v_count
    FROM mes_pro_work_order wo
    JOIN mes_pro_process_pool_active_order ao
      ON ao.work_order_id = wo.id
     AND ao.tenant_id = wo.tenant_id
     AND ao.deleted = b'0'
    WHERE wo.id = @target_work_order_id
      AND wo.code = '881MO090889'
      AND wo.product_id = @target_product_id
      AND wo.tenant_id = @target_tenant_id
      AND wo.deleted = b'0'
      AND ao.id = @target_active_order_id
      AND ao.route_id = @target_route_id
      AND ao.route_version_id = @target_route_version_id
      AND ao.active_status = 'ACTIVE';
    IF v_count <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Target local active order identity changed';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM mes_qa_inspection_regulation
    WHERE id = @retired_fixture_regulation_id
      AND product_id = @target_product_id
      AND route_id = @target_route_id
      AND route_version_id = @target_route_version_id
      AND route_process_id = @target_route_process_id
      AND process_id = @target_process_id
      AND owner_module = 'CODX_QA'
      AND regulation_code = 'PQC-ID-001-RP980647'
      AND regulation_name = '（椎体）球囊扩张压力泵组装过程检验规程-清洗工序'
      AND lifecycle_status = 'RETIRED'
      AND current_version_id IS NULL
      AND creator = '20260808-pressure-pump-active-orders'
      AND updater = '20260809-frontline-pqc-hide-unconfigured-processes'
      AND tenant_id = @target_tenant_id
      AND deleted = b'0';
    IF v_count <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Retired cleaning fixture regulation changed';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM mes_qa_inspection_regulation_version
    WHERE id = @retired_fixture_version_id
      AND regulation_id = @retired_fixture_regulation_id
      AND version_no = 'G/0'
      AND lifecycle_status = 'RETIRED'
      AND creator = @actor
      AND updater = '20260809-frontline-pqc-hide-unconfigured-processes'
      AND tenant_id = @target_tenant_id
      AND deleted = b'0';
    IF v_count <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Retired fixture G/0 version changed';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM mes_qa_inspection_regulation_item
    WHERE regulation_version_id = @retired_fixture_version_id
      AND item_code = 'ID-001-WASH-APP'
      AND inspection_method = '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。'
      AND standard_text = '弹簧、胶塞、套筒、手柄、齿条、芯杆、螺盖清洗干燥后表面及内部应无液珠；表面应清洁，无黑点、无异物等。'
      AND tenant_id = @target_tenant_id
      AND deleted = b'0';
    IF v_count <> 3 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Retired fixture formal item snapshot changed';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM mes_pqc_inspection_task
    WHERE id IN (296, 297, 298, 299)
      AND active_order_id = @target_active_order_id
      AND work_order_id = @target_work_order_id
      AND route_id = @target_route_id
      AND route_version_id = @target_route_version_id
      AND route_process_id = @target_route_process_id
      AND process_id = @target_process_id
      AND regulation_version_id = @retired_fixture_version_id
      AND task_status = 'CANCELLED'
      AND actual_inspection_quantity = 0
      AND updater = '20260809-frontline-pqc-hide-unconfigured-processes'
      AND tenant_id = @target_tenant_id
      AND deleted = b'0';
    IF v_count <> 4 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Cancelled cleaning fixture tasks changed';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM mes_pqc_inspection_piece_detail detail
    JOIN mes_pqc_inspection_task task
      ON task.id = detail.task_id
     AND task.tenant_id = detail.tenant_id
    WHERE task.id IN (296, 297, 298, 299)
      AND task.tenant_id = @target_tenant_id
      AND task.deleted = b'0'
      AND detail.deleted = b'0';
    IF v_count <> 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Cancelled fixture tasks contain piece details';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM mes_qa_inspection_regulation
    WHERE tenant_id = @target_tenant_id
      AND product_id = @target_product_id
      AND route_id = @target_route_id
      AND route_version_id = @target_route_version_id
      AND route_process_id = @target_route_process_id
      AND id <> @retired_fixture_regulation_id
      AND deleted = b'0';
    IF v_count <> 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Another cleaning regulation already occupies the formal identity';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM mes_pqc_inspection_task
    WHERE tenant_id = @target_tenant_id
      AND active_order_id = @target_active_order_id
      AND route_process_id = @target_route_process_id
      AND business_date = @target_business_date
      AND deleted = b'0';
    IF v_count <> 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Target business date already has cleaning PQC tasks';
    END IF;

    UPDATE mes_qa_inspection_regulation
    SET deleted = b'1',
        updater = @actor
    WHERE id = @retired_fixture_regulation_id
      AND lifecycle_status = 'RETIRED'
      AND current_version_id IS NULL
      AND owner_module = 'CODX_QA'
      AND tenant_id = @target_tenant_id
      AND deleted = b'0';
    SET v_affected = ROW_COUNT();
    IF v_affected <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Retired fixture regulation soft-delete count changed';
    END IF;

    INSERT INTO mes_qa_inspection_regulation (
        product_id, route_id, route_version_id, route_process_id, process_id,
        owner_module, regulation_code, regulation_name, lifecycle_status,
        current_version_id, creator, updater, tenant_id
    ) VALUES (
        @target_product_id, @target_route_id, @target_route_version_id,
        @target_route_process_id, @target_process_id,
        'MES_QA', 'PQC-ID-001-RP980647',
        '（椎体）球囊扩张压力泵组装过程检验规程-清洗工序',
        'DRAFT', NULL, @actor, @actor, @target_tenant_id
    );
    SET @new_regulation_id := LAST_INSERT_ID();

    SET @published_at := NOW();
    INSERT INTO mes_qa_inspection_regulation_version (
        regulation_id, version_no, lifecycle_status, published_at, retired_at,
        final_inspection_applicable, final_inspection_not_applicable_reason, snapshot_json,
        creator, updater, tenant_id
    ) VALUES (
        @new_regulation_id, @target_version_no, 'PUBLISHED', @published_at, NULL,
        b'1', NULL,
        JSON_OBJECT(
            'sourceType', 'FORMAL_DCC_CONTROLLED_DOCUMENT',
            'sourceDocumentCode', 'PQC-ID-001',
            'sourceDocumentVersion', 'G/0',
            'effectiveDate', '2025-09-30',
            'productName', '球囊扩张压力泵',
            'routeId', @target_route_id,
            'routeName', '球囊扩张压力泵',
            'routeVersionId', @target_route_version_id,
            'routeVersionNo', 'V27',
            'routeProcessId', @target_route_process_id,
            'processId', @target_process_id,
            'routeProcessName', '清洗工序',
            'batchRecordReports', JSON_ARRAY(JSON_OBJECT(
                'routeProcessId', @target_route_process_id,
                'batchRecordReportId', '9d78beff251548538f73cb6f98c624fc',
                'batchRecordReportName', '清洗工序生产记录',
                'sourceTable', 'mes_pro_route_process.batch_record_report_id'
            ))
        ),
        @actor, @actor, @target_tenant_id
    );
    SET @new_version_id := LAST_INSERT_ID();

    INSERT INTO mes_qa_inspection_regulation_item (
        regulation_version_id, inspection_type, item_code, item_name,
        inspection_method, standard_text,
        standard_lower_limit, standard_upper_limit, standard_unit, standard_precision,
        equipment_required, result_type, first_inspection_quantity, patrol_inspection_ratio,
        creator, updater, tenant_id
    ) VALUES
        (
            @new_version_id, 'FIRST', 'ID-001-WASH-APP', '外观',
            '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。',
            '弹簧、胶塞、套筒、手柄、齿条、芯杆、螺盖清洗干燥后表面及内部应无液珠；表面应清洁，无黑点、无异物等。',
            NULL, NULL, NULL, NULL, b'0', 'BOOLEAN', 5, NULL,
            @actor, @actor, @target_tenant_id
        ),
        (
            @new_version_id, 'PATROL', 'ID-001-WASH-APP', '外观',
            '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。',
            '弹簧、胶塞、套筒、手柄、齿条、芯杆、螺盖清洗干燥后表面及内部应无液珠；表面应清洁，无黑点、无异物等。',
            NULL, NULL, NULL, NULL, b'0', 'BOOLEAN', NULL, 0.050000,
            @actor, @actor, @target_tenant_id
        ),
        (
            @new_version_id, 'FINAL', 'ID-001-WASH-APP', '外观',
            '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。',
            '弹簧、胶塞、套筒、手柄、齿条、芯杆、螺盖清洗干燥后表面及内部应无液珠；表面应清洁，无黑点、无异物等。',
            NULL, NULL, NULL, NULL, b'0', 'BOOLEAN', 3, NULL,
            @actor, @actor, @target_tenant_id
        );

    UPDATE mes_qa_inspection_regulation
    SET lifecycle_status = 'PUBLISHED',
        current_version_id = @new_version_id,
        updater = @actor
    WHERE id = @new_regulation_id
      AND owner_module = 'MES_QA'
      AND lifecycle_status = 'DRAFT'
      AND current_version_id IS NULL
      AND tenant_id = @target_tenant_id
      AND deleted = b'0';
    SET v_affected = ROW_COUNT();
    IF v_affected <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Formal cleaning regulation publish count changed';
    END IF;

    INSERT INTO mes_pqc_inspection_task (
        active_order_id, work_order_id, route_id, route_version_id,
        route_process_id, process_id, regulation_version_id,
        inspection_type, business_date, shift_code, round_no,
        planned_inspection_quantity, actual_inspection_quantity, task_status,
        creator, updater, tenant_id
    ) VALUES
        (@target_active_order_id, @target_work_order_id, @target_route_id, @target_route_version_id,
         @target_route_process_id, @target_process_id, @new_version_id,
         'FIRST', @target_business_date, 'FIRST', 1, 5, 0, 'PENDING',
         @actor, @actor, @target_tenant_id),
        (@target_active_order_id, @target_work_order_id, @target_route_id, @target_route_version_id,
         @target_route_process_id, @target_process_id, @new_version_id,
         'PATROL', @target_business_date, 'AM', 1, 113, 0, 'PENDING',
         @actor, @actor, @target_tenant_id),
        (@target_active_order_id, @target_work_order_id, @target_route_id, @target_route_version_id,
         @target_route_process_id, @target_process_id, @new_version_id,
         'PATROL', @target_business_date, 'PM', 1, 113, 0, 'PENDING',
         @actor, @actor, @target_tenant_id),
        (@target_active_order_id, @target_work_order_id, @target_route_id, @target_route_version_id,
         @target_route_process_id, @target_process_id, @new_version_id,
         'FINAL', @target_business_date, 'FINAL', 1, 3, 0, 'PENDING',
         @actor, @actor, @target_tenant_id);

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
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Formal cleaning regulation postcondition failed';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM mes_qa_inspection_regulation_item
    WHERE regulation_version_id = @new_version_id
      AND item_code = 'ID-001-WASH-APP'
      AND inspection_method NOT LIKE '%测试夹具%'
      AND standard_text NOT LIKE '%符合/不符合%'
      AND tenant_id = @target_tenant_id
      AND deleted = b'0';
    IF v_count <> 3 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Formal cleaning item postcondition failed';
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
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Formal cleaning pending task postcondition failed';
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
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Cancelled fixture task audit changed';
    END IF;

    COMMIT;

    SELECT @new_regulation_id AS new_regulation_id,
           @new_version_id AS new_version_id,
           @target_business_date AS task_business_date;
END$$
DELIMITER ;

CALL codex_create_formal_cleaning_qa();
DROP PROCEDURE codex_create_formal_cleaning_qa;
