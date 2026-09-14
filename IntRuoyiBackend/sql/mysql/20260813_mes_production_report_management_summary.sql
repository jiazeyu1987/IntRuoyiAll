-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260809_mes_process_pool_report_shared_allocation; type=schema; riskLevel=medium
-- 20260813_mes_production_report_management_summary
-- 生产组长报工管理正式汇总读模型：将输出、分配、放行和管理状态固化到报工事件。
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_mes_production_report_management_summary_20260813;
DELIMITER $$
CREATE PROCEDURE ensure_mes_production_report_management_summary_20260813()
BEGIN
    DECLARE v_table_count int DEFAULT 0;
    DECLARE v_column_count int DEFAULT 0;
    DECLARE v_index_count int DEFAULT 0;
    DECLARE v_invalid_output_count bigint DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        RESIGNAL;
    END;

    SELECT COUNT(*) INTO v_table_count
      FROM information_schema.tables
     WHERE table_schema = DATABASE()
       AND table_name IN (
           'mes_pro_process_pool_event',
           'mes_pro_process_pool_quantity_fragment',
           'mes_pro_process_pool_report_allocation',
           'mes_pro_process_pool_active_order_release_application',
           'mes_pro_edhr_release_transaction'
       );
    IF v_table_count <> 5 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Missing production report management source tables';
    END IF;

    SELECT COUNT(*) INTO v_column_count
      FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_event'
       AND column_name IN (
           'report_management_status',
           'report_output_quantity',
           'report_allocated_quantity',
           'report_unallocated_quantity',
           'report_release_status'
       );
    IF v_column_count = 0 THEN
        ALTER TABLE `mes_pro_process_pool_event`
            ADD COLUMN `report_management_status` varchar(32) NOT NULL DEFAULT 'NOT_APPLICABLE'
                COMMENT '报工管理状态：NOT_APPLICABLE/UNALLOCATED/PARTIALLY_ALLOCATED/PENDING_RELEASE/ARCHIVED'
                AFTER `signature_snapshot`,
            ADD COLUMN `report_output_quantity` decimal(24,6) NOT NULL DEFAULT 0.000000
                COMMENT '生产报工完成数量正式汇总' AFTER `report_management_status`,
            ADD COLUMN `report_allocated_quantity` decimal(24,6) NOT NULL DEFAULT 0.000000
                COMMENT '生产报工当前已分配数量正式汇总' AFTER `report_output_quantity`,
            ADD COLUMN `report_unallocated_quantity` decimal(24,6) NOT NULL DEFAULT 0.000000
                COMMENT '生产报工当前未分配数量正式汇总' AFTER `report_allocated_quantity`,
            ADD COLUMN `report_release_status` varchar(32) NOT NULL DEFAULT 'NOT_APPLICABLE'
                COMMENT '分配订单放行状态：NOT_APPLICABLE/NOT_ALLOCATED/NOT_RELEASED/PARTIALLY_RELEASED/RELEASED'
                AFTER `report_unallocated_quantity`;
    ELSEIF v_column_count <> 5 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Production report management summary columns are partially migrated';
    END IF;

    SELECT COUNT(*) INTO v_invalid_output_count
      FROM mes_pro_process_pool_event event
      LEFT JOIN (
          SELECT production_submit_event_id,
                 COUNT(*) AS output_fragment_count,
                 SUM(total_quantity) AS output_fragment_quantity
            FROM mes_pro_process_pool_quantity_fragment
           WHERE deleted = 0
             AND source_quantity_type = 'OUTPUT'
           GROUP BY production_submit_event_id
      ) output_fragment
        ON output_fragment.production_submit_event_id = event.id
     WHERE event.deleted = 0
       AND event.event_type = 'PRODUCTION_SUBMIT'
       AND (
           (JSON_EXTRACT(event.raw_payload, '$.outputQuantity') IS NOT NULL AND (
               JSON_TYPE(JSON_EXTRACT(event.raw_payload, '$.outputQuantity')) NOT IN ('INTEGER', 'DOUBLE')
               OR CAST(JSON_UNQUOTE(JSON_EXTRACT(
                   event.raw_payload, '$.outputQuantity')) AS DECIMAL(24, 6)) <= 0
           ))
           OR COALESCE(output_fragment.output_fragment_count, 0) > 1
           OR (COALESCE(output_fragment.output_fragment_count, 0) = 1
               AND output_fragment.output_fragment_quantity <= 0)
           OR (JSON_EXTRACT(event.raw_payload, '$.outputQuantity') IS NOT NULL
               AND COALESCE(output_fragment.output_fragment_count, 0) = 1
               AND ABS(CAST(JSON_UNQUOTE(JSON_EXTRACT(
                   event.raw_payload, '$.outputQuantity')) AS DECIMAL(24, 6))
                   - output_fragment.output_fragment_quantity) > 0.000001)
           OR (JSON_EXTRACT(event.raw_payload, '$.outputQuantity') IS NULL
               AND COALESCE(output_fragment.output_fragment_count, 0) = 0
               AND COALESCE(JSON_UNQUOTE(JSON_EXTRACT(
                   event.raw_payload, '$.sourceType')), '') <> 'pqcContextSubmitRoot')
       );
    IF v_invalid_output_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Production report output sources are inconsistent';
    END IF;

    DROP TEMPORARY TABLE IF EXISTS tmp_mes_report_management_summary_20260813;
    CREATE TEMPORARY TABLE tmp_mes_report_management_summary_20260813 AS
    SELECT event.id AS event_id,
           event.tenant_id,
           COALESCE(CAST(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(
               event.raw_payload, '$.outputQuantity')), '') AS DECIMAL(24, 6)),
               output_fragment.output_fragment_quantity, 0) AS output_quantity,
           COALESCE(allocation_summary.allocated_quantity, 0) AS allocated_quantity,
           COALESCE(allocation_summary.allocated_order_count, 0) AS allocated_order_count,
           COALESCE(allocation_summary.released_order_count, 0) AS released_order_count
      FROM mes_pro_process_pool_event event
      LEFT JOIN (
          SELECT production_submit_event_id,
                 SUM(total_quantity) AS output_fragment_quantity
            FROM mes_pro_process_pool_quantity_fragment
           WHERE deleted = 0
             AND source_quantity_type = 'OUTPUT'
           GROUP BY production_submit_event_id
      ) output_fragment
        ON output_fragment.production_submit_event_id = event.id
      LEFT JOIN (
          SELECT allocation.tenant_id,
                 allocation.event_id,
                 SUM(allocation.allocated_quantity) AS allocated_quantity,
                 COUNT(DISTINCT allocation.active_order_id) AS allocated_order_count,
                 COUNT(DISTINCT CASE WHEN released_order.active_order_id IS NOT NULL
                     THEN allocation.active_order_id END) AS released_order_count
            FROM mes_pro_process_pool_report_allocation allocation
            LEFT JOIN (
                SELECT DISTINCT release_application.tenant_id, release_application.active_order_id
                  FROM mes_pro_process_pool_active_order_release_application release_application
                  INNER JOIN mes_pro_edhr_release_transaction release_transaction
                    ON release_transaction.tenant_id = release_application.tenant_id
                   AND release_transaction.id = release_application.release_transaction_id
                   AND release_transaction.deleted = 0
                   AND release_transaction.release_status = 'RELEASED'
                 WHERE release_application.deleted = 0
            ) released_order
              ON released_order.tenant_id = allocation.tenant_id
             AND released_order.active_order_id = allocation.active_order_id
           WHERE allocation.deleted = 0
             AND allocation.lifecycle_status = 'CURRENT'
           GROUP BY allocation.tenant_id, allocation.event_id
      ) allocation_summary
        ON allocation_summary.tenant_id = event.tenant_id
       AND allocation_summary.event_id = event.id
     WHERE event.deleted = 0
       AND event.event_type = 'PRODUCTION_SUBMIT';

    UPDATE mes_pro_process_pool_event event
    INNER JOIN tmp_mes_report_management_summary_20260813 summary
       ON summary.event_id = event.id
      AND summary.tenant_id = event.tenant_id
       SET event.report_output_quantity = summary.output_quantity,
           event.report_allocated_quantity = summary.allocated_quantity,
           event.report_unallocated_quantity = GREATEST(
               summary.output_quantity - summary.allocated_quantity, 0),
           event.report_release_status = CASE
               WHEN summary.allocated_order_count = 0 THEN 'NOT_ALLOCATED'
               WHEN summary.released_order_count = 0 THEN 'NOT_RELEASED'
               WHEN summary.released_order_count < summary.allocated_order_count THEN 'PARTIALLY_RELEASED'
               ELSE 'RELEASED'
           END,
           event.report_management_status = CASE
               WHEN summary.output_quantity <= 0 THEN 'NOT_APPLICABLE'
               WHEN summary.allocated_quantity = 0 THEN 'UNALLOCATED'
               WHEN summary.allocated_quantity < summary.output_quantity THEN 'PARTIALLY_ALLOCATED'
               WHEN summary.allocated_order_count > 0
                AND summary.released_order_count = summary.allocated_order_count THEN 'ARCHIVED'
               ELSE 'PENDING_RELEASE'
           END;

    DROP TEMPORARY TABLE tmp_mes_report_management_summary_20260813;

    SELECT COUNT(*) INTO v_index_count
      FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_event'
       AND index_name = 'idx_mes_pp_event_report_management';
    IF v_index_count = 0 THEN
        ALTER TABLE `mes_pro_process_pool_event`
            ADD KEY `idx_mes_pp_event_report_management`
                (`tenant_id`, `deleted`, `event_type`, `route_process_id`,
                 `report_management_status`, `server_submit_time`, `id`);
    END IF;
END$$
DELIMITER ;

CALL ensure_mes_production_report_management_summary_20260813();
DROP PROCEDURE IF EXISTS ensure_mes_production_report_management_summary_20260813;
