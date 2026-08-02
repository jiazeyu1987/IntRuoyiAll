-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260802_mes_pqc_inspection_task,20260802_mes_process_pool_active_order_transfer_trace,20260802_mes_process_pool_team_leader_scope_extended; type=config; riskLevel=medium
-- 岗位需求分解矩阵 M6：迁移切换前正式来源完整性预检
-- This preflight is read-only except for temporary stored procedures created and dropped in-session.

DROP PROCEDURE IF EXISTS assert_rrm_m6_active_order_conflicts;
DELIMITER $$
CREATE PROCEDURE assert_rrm_m6_active_order_conflicts()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM `mes_pro_process_pool_active_order`
        WHERE `deleted` = b'0'
          AND `business_status` = 'ACTIVE'
        GROUP BY `tenant_id`, `work_order_id`, `route_id`, `route_version_id`
        HAVING COUNT(*) > 1
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '岗位需求分解矩阵M6预检失败：双活跃来源冲突';
    END IF;
END$$
DELIMITER ;

DROP PROCEDURE IF EXISTS assert_rrm_m6_open_order_authority;
DELIMITER $$
CREATE PROCEDURE assert_rrm_m6_open_order_authority()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM `mes_pro_process_pool_active_order`
        WHERE `deleted` = b'0'
          AND `business_status` = 'ACTIVE'
          AND (`route_id` IS NULL
            OR `route_version_id` IS NULL
            OR `erp_fixed_quantity_snapshot` IS NULL
            OR `business_status` IS NULL)
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '岗位需求分解矩阵M6预检失败：开放订单缺路线版本或系数';
    END IF;
END$$
DELIMITER ;

DROP PROCEDURE IF EXISTS assert_rrm_m6_pqc_task_authority;
DELIMITER $$
CREATE PROCEDURE assert_rrm_m6_pqc_task_authority()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM `mes_pqc_inspection_task`
        WHERE `deleted` = b'0'
          AND `task_status` <> 'CANCELLED'
          AND (`active_order_id` IS NULL
            OR `work_order_id` IS NULL
            OR `route_id` IS NULL
            OR `route_version_id` IS NULL
            OR `route_process_id` IS NULL
            OR `regulation_version_id` IS NULL
            OR `planned_inspection_quantity` IS NULL)
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '岗位需求分解矩阵M6预检失败：开放PQC缺任务身份或规程版本';
    END IF;
END$$
DELIMITER ;

DROP PROCEDURE IF EXISTS assert_rrm_m6_batch_record_binding_authority;
DELIMITER $$
CREATE PROCEDURE assert_rrm_m6_batch_record_binding_authority()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM `mes_pro_route_flow_process_batch_record`
        WHERE `deleted` = b'0'
          AND (`batch_record_report_id` IS NULL OR `batch_record_report_id` = '')
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '岗位需求分解矩阵M6预检失败：正式批记录绑定缺失或冲突';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM `mes_pro_route_flow_process_batch_record`
        WHERE `deleted` = b'0'
          AND `form_slot_type` = 'MAIN'
          AND `record_category` = 'BATCH_RECORD'
        GROUP BY `tenant_id`, `route_process_id`, `use_type`, `report_sort`
        HAVING COUNT(*) > 1
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '岗位需求分解矩阵M6预检失败：正式批记录绑定缺失或冲突';
    END IF;
END$$
DELIMITER ;

CALL assert_rrm_m6_active_order_conflicts();
CALL assert_rrm_m6_open_order_authority();
CALL assert_rrm_m6_pqc_task_authority();
CALL assert_rrm_m6_batch_record_binding_authority();

DROP PROCEDURE IF EXISTS assert_rrm_m6_active_order_conflicts;
DROP PROCEDURE IF EXISTS assert_rrm_m6_open_order_authority;
DROP PROCEDURE IF EXISTS assert_rrm_m6_pqc_task_authority;
DROP PROCEDURE IF EXISTS assert_rrm_m6_batch_record_binding_authority;
