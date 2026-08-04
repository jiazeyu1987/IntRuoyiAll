-- RRM M6 local-only E2E fixture for tenant 1 / 芋道源码.
-- Purpose: add one formal PENDING PQC task for the first pressure-pump process so real UI submission can create
-- a second same-filter process-pool event for AC-D32 pagination verification.
-- This file must not create mes_pro_process_pool_event rows or mark tasks SUBMITTED.

DROP PROCEDURE IF EXISTS seed_rrm_m6_pqc_d32_same_filter_task;
DELIMITER $$
CREATE PROCEDURE seed_rrm_m6_pqc_d32_same_filter_task()
BEGIN
    DECLARE v_tenant_id BIGINT DEFAULT 1;
    DECLARE v_active_order_id BIGINT DEFAULT 12;
    DECLARE v_work_order_id BIGINT DEFAULT 980008;
    DECLARE v_route_id BIGINT DEFAULT 922119;
    DECLARE v_route_version_id BIGINT DEFAULT 448;
    DECLARE v_route_process_id BIGINT DEFAULT 928609;
    DECLARE v_process_id BIGINT DEFAULT 922985;
    DECLARE v_regulation_version_id BIGINT DEFAULT 16;
    DECLARE v_actual_employee_id BIGINT DEFAULT 659;
    DECLARE v_inspection_type VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'PATROL';
    DECLARE v_shift_code VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'DAY';
    DECLARE v_round_no INT DEFAULT 1;
    DECLARE v_planned_quantity INT DEFAULT 15;
    DECLARE v_fixture_remark VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'RRM M6 D32 same-filter local E2E fixture';

    DECLARE v_tenant_count INT DEFAULT 0;
    DECLARE v_active_order_count INT DEFAULT 0;
    DECLARE v_route_process_count INT DEFAULT 0;
    DECLARE v_work_order_count INT DEFAULT 0;
    DECLARE v_regulation_count INT DEFAULT 0;
    DECLARE v_existing_today_task_id BIGINT DEFAULT NULL;
    DECLARE v_existing_today_task_status VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL;
    DECLARE v_existing_today_event_count INT DEFAULT 0;
    DECLARE v_pending_same_filter_event_count INT DEFAULT 0;
    DECLARE v_pending_same_filter_task_id BIGINT DEFAULT NULL;
    DECLARE v_pending_same_filter_task_count INT DEFAULT 0;
    DECLARE v_next_business_date DATE DEFAULT CURDATE();
    DECLARE v_existing_identity_count INT DEFAULT 0;

    SET v_route_process_id = 928609;
    SET v_process_id = 922985;

    SELECT COUNT(*) INTO v_tenant_count
    FROM system_tenant
    WHERE id = v_tenant_id AND deleted = b'0' AND status = 0;

    SELECT COUNT(*) INTO v_active_order_count
    FROM mes_pro_process_pool_active_order
    WHERE id = v_active_order_id
      AND tenant_id = v_tenant_id
      AND work_order_id = v_work_order_id
      AND route_id = v_route_id
      AND route_version_id = v_route_version_id
      AND active_status = CONVERT('ACTIVE' USING utf8mb4) COLLATE utf8mb4_unicode_ci
      AND deleted = b'0';

    SELECT COUNT(*) INTO v_route_process_count
    FROM mes_pro_route_process
    WHERE id = v_route_process_id
      AND tenant_id = v_tenant_id
      AND route_id = v_route_id
      AND process_id = v_process_id
      AND deleted = b'0';

    SELECT COUNT(*) INTO v_work_order_count
    FROM mes_pro_work_order
    WHERE id = v_work_order_id
      AND tenant_id = v_tenant_id
      AND code = CONVERT('RRM-20260801-PP-MO-001' USING utf8mb4) COLLATE utf8mb4_unicode_ci
      AND product_id = 902149
      AND deleted = b'0';

    SELECT COUNT(*) INTO v_regulation_count
    FROM mes_qa_inspection_regulation
    WHERE id = 16
      AND tenant_id = v_tenant_id
      AND product_id = 902149
      AND route_id = v_route_id
      AND route_version_id = v_route_version_id
      AND route_process_id = v_route_process_id
      AND process_id = v_process_id
      AND lifecycle_status = CONVERT('PUBLISHED' USING utf8mb4) COLLATE utf8mb4_unicode_ci
      AND current_version_id = v_regulation_version_id
      AND deleted = b'0';

    IF v_tenant_count <> 1
        OR v_active_order_count <> 1
        OR v_route_process_count <> 1
        OR v_work_order_count <> 1
        OR v_regulation_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RRM M6 D32 fixture failed: formal tenant/order/route-process/regulation prerequisites are missing';
    END IF;

    SELECT MIN(id), MIN(task_status) INTO v_existing_today_task_id, v_existing_today_task_status
    FROM mes_pqc_inspection_task
    WHERE tenant_id = v_tenant_id
      AND active_order_id = v_active_order_id
      AND route_process_id = v_route_process_id
      AND inspection_type = v_inspection_type
      AND business_date = CURDATE()
      AND shift_code = v_shift_code
      AND round_no = v_round_no
      AND deleted = b'0';

    IF v_existing_today_task_id IS NOT NULL
        AND v_existing_today_task_status NOT IN (
            CONVERT('PENDING' USING utf8mb4) COLLATE utf8mb4_unicode_ci,
            CONVERT('SUBMITTED' USING utf8mb4) COLLATE utf8mb4_unicode_ci
        ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RRM M6 D32 fixture failed: same identity task exists but is neither PENDING nor SUBMITTED';
    END IF;

    SELECT COUNT(*) INTO v_existing_today_event_count
    FROM mes_pro_process_pool_event pool_event
    WHERE pool_event.tenant_id = v_tenant_id
      AND pool_event.work_order_id = v_work_order_id
      AND pool_event.route_process_id = v_route_process_id
      AND pool_event.process_id = v_process_id
      AND pool_event.actual_employee_id = v_actual_employee_id
      AND pool_event.event_type = CONVERT('PQC_INSPECTION' USING utf8mb4) COLLATE utf8mb4_unicode_ci
      AND DATE(pool_event.server_submit_time) = CURDATE()
      AND CAST(JSON_UNQUOTE(JSON_EXTRACT(pool_event.raw_payload, '$.pqcTaskId')) AS UNSIGNED) = v_existing_today_task_id
      AND JSON_UNQUOTE(JSON_EXTRACT(pool_event.raw_payload, '$.inspectionType')) = v_inspection_type
      AND CAST(JSON_UNQUOTE(JSON_EXTRACT(pool_event.raw_payload, '$.roundNo')) AS UNSIGNED) = v_round_no
      AND pool_event.deleted = b'0';

    IF v_existing_today_task_status = CONVERT('SUBMITTED' USING utf8mb4) COLLATE utf8mb4_unicode_ci
        AND v_existing_today_event_count = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RRM M6 D32 fixture failed: today same-filter task is SUBMITTED but has no process-pool event';
    END IF;

    SELECT COUNT(*) INTO v_pending_same_filter_event_count
    FROM mes_pro_process_pool_event pool_event
    LEFT JOIN mes_pqc_inspection_task pqc_task
      ON pqc_task.tenant_id = pool_event.tenant_id
     AND pqc_task.id = CAST(JSON_UNQUOTE(JSON_EXTRACT(pool_event.raw_payload, '$.pqcTaskId')) AS UNSIGNED)
     AND pqc_task.deleted = b'0'
    LEFT JOIN (
        SELECT review_log.tenant_id,
               review_log.event_id,
               review_log.review_status
        FROM mes_pro_process_pool_submission_review review_log
        INNER JOIN (
            SELECT tenant_id,
                   event_id,
                   MAX(id) AS latest_id
            FROM mes_pro_process_pool_submission_review
            WHERE deleted = b'0'
            GROUP BY tenant_id, event_id
        ) latest_review
          ON latest_review.tenant_id = review_log.tenant_id
         AND latest_review.latest_id = review_log.id
        WHERE review_log.deleted = b'0'
    ) latest_submission_review
      ON latest_submission_review.tenant_id = pool_event.tenant_id
     AND latest_submission_review.event_id = pool_event.id
    WHERE pool_event.tenant_id = v_tenant_id
      AND pool_event.work_order_id = v_work_order_id
      AND pool_event.route_process_id = v_route_process_id
      AND pool_event.process_id = v_process_id
      AND pool_event.actual_employee_id = v_actual_employee_id
      AND pool_event.event_type = CONVERT('PQC_INSPECTION' USING utf8mb4) COLLATE utf8mb4_unicode_ci
      AND DATE(pool_event.server_submit_time) = CURDATE()
      AND pqc_task.inspection_type = v_inspection_type
      AND pqc_task.round_no = v_round_no
      AND COALESCE(
              latest_submission_review.review_status,
              CONVERT('PENDING' USING utf8mb4) COLLATE utf8mb4_unicode_ci
          ) = CONVERT('PENDING' USING utf8mb4) COLLATE utf8mb4_unicode_ci
      AND pool_event.deleted = b'0';

    IF v_pending_same_filter_event_count < 2 THEN
        SELECT COUNT(*), MIN(id) INTO v_pending_same_filter_task_count, v_pending_same_filter_task_id
        FROM mes_pqc_inspection_task
        WHERE tenant_id = v_tenant_id
          AND active_order_id = v_active_order_id
          AND route_process_id = v_route_process_id
          AND process_id = v_process_id
          AND regulation_version_id = v_regulation_version_id
          AND inspection_type = v_inspection_type
          AND business_date >= CURDATE()
          AND shift_code = v_shift_code
          AND round_no = v_round_no
          AND planned_inspection_quantity = v_planned_quantity
          AND task_status = CONVERT('PENDING' USING utf8mb4) COLLATE utf8mb4_unicode_ci
          AND deleted = b'0';

        WHILE v_pending_same_filter_event_count + v_pending_same_filter_task_count < 2 DO
            SELECT COUNT(*) INTO v_existing_identity_count
            FROM mes_pqc_inspection_task
            WHERE tenant_id = v_tenant_id
              AND active_order_id = v_active_order_id
              AND route_process_id = v_route_process_id
              AND inspection_type = v_inspection_type
              AND business_date = v_next_business_date
              AND shift_code = v_shift_code
              AND round_no = v_round_no
              AND deleted = b'0';

            IF v_existing_identity_count = 0 THEN
                INSERT INTO mes_pqc_inspection_task
                    (active_order_id, work_order_id, route_id, route_version_id, route_process_id, process_id,
                     regulation_version_id, inspection_type, business_date, shift_code, round_no,
                     planned_inspection_quantity, actual_inspection_quantity, task_status,
                     creator, updater, tenant_id)
                VALUES
                    (v_active_order_id, v_work_order_id, v_route_id, v_route_version_id, v_route_process_id, v_process_id,
                     v_regulation_version_id, v_inspection_type, v_next_business_date, v_shift_code, v_round_no,
                     v_planned_quantity, 0, 'PENDING',
                     'codex-rrm-m6-d32', 'codex-rrm-m6-d32', v_tenant_id);
                SET v_pending_same_filter_task_id = LAST_INSERT_ID();
                SET v_pending_same_filter_task_count = v_pending_same_filter_task_count + 1;
            ELSE
                SET v_next_business_date = DATE_ADD(v_next_business_date, INTERVAL 1 DAY);
            END IF;
        END WHILE;

    END IF;

    SELECT COUNT(*), MIN(id) INTO v_pending_same_filter_task_count, v_pending_same_filter_task_id
    FROM mes_pqc_inspection_task
    WHERE tenant_id = v_tenant_id
      AND active_order_id = v_active_order_id
      AND route_process_id = v_route_process_id
      AND process_id = v_process_id
      AND regulation_version_id = v_regulation_version_id
      AND inspection_type = v_inspection_type
      AND business_date >= CURDATE()
      AND shift_code = v_shift_code
      AND round_no = v_round_no
      AND planned_inspection_quantity = v_planned_quantity
      AND task_status = CONVERT('PENDING' USING utf8mb4) COLLATE utf8mb4_unicode_ci
      AND deleted = b'0';

    IF v_pending_same_filter_event_count + v_pending_same_filter_task_count < 2 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RRM M6 D32 fixture failed: no pending same-filter task is available for real UI submission';
    END IF;
END$$
DELIMITER ;

START TRANSACTION;
CALL seed_rrm_m6_pqc_d32_same_filter_task();
COMMIT;
DROP PROCEDURE IF EXISTS seed_rrm_m6_pqc_d32_same_filter_task;

SELECT task.id,
       task.tenant_id,
       task.active_order_id,
       task.work_order_id,
       task.route_process_id,
       task.process_id,
       task.regulation_version_id,
       task.inspection_type,
       task.business_date,
       task.shift_code,
       task.round_no,
       task.planned_inspection_quantity,
       task.actual_inspection_quantity,
       task.task_status,
       'RRM M6 D32 same-filter local E2E fixture' AS fixture_remark
FROM mes_pqc_inspection_task task
WHERE task.tenant_id = 1
  AND task.active_order_id = 12
  AND task.route_process_id = 928609
  AND task.process_id = 922985
  AND task.inspection_type = CONVERT('PATROL' USING utf8mb4) COLLATE utf8mb4_unicode_ci
      AND task.business_date >= CURDATE()
  AND task.shift_code = CONVERT('DAY' USING utf8mb4) COLLATE utf8mb4_unicode_ci
  AND task.round_no = 1
  AND task.deleted = b'0'
ORDER BY task.id;
