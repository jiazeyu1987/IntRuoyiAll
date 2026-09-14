-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260809_mes_process_pool_report_shared_allocation; type=schema; riskLevel=medium
-- 一线生产提交后立即形成初始订单分配及订单工序完成状态；此时尚无组长复核，相关 review ID 必须可空。

DROP PROCEDURE IF EXISTS `migrate_mes_frontline_selected_initial_allocation`;

DELIMITER $$

CREATE PROCEDURE `migrate_mes_frontline_selected_initial_allocation`()
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.tables
         WHERE table_schema = DATABASE()
           AND table_name = 'mes_pro_process_pool_report_allocation'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'mes_pro_process_pool_report_allocation is required';
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.tables
         WHERE table_schema = DATABASE()
           AND table_name = 'mes_pro_process_pool_order_process_completion'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'mes_pro_process_pool_order_process_completion is required';
    END IF;

    ALTER TABLE `mes_pro_process_pool_report_allocation`
        MODIFY COLUMN `review_id` bigint DEFAULT NULL
            COMMENT '组长复核记录ID；一线初始分配时为空，组长确认后产生复核记录',
        MODIFY COLUMN `allocation_mode` varchar(32) NOT NULL
            COMMENT '分配方式：FRONTLINE_SELECTED/FIFO/MANUAL/SYSTEM';

    ALTER TABLE `mes_pro_process_pool_order_process_completion`
        MODIFY COLUMN `last_review_id` bigint DEFAULT NULL
            COMMENT '最后一次班组长复核记录ID；一线初始分配时为空，组长确认后产生复核记录';

    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = 'mes_pro_process_pool_report_allocation'
           AND column_name = 'review_id'
           AND is_nullable = 'YES'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'report allocation review_id must be nullable';
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = 'mes_pro_process_pool_order_process_completion'
           AND column_name = 'last_review_id'
           AND is_nullable = 'YES'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'order process completion last_review_id must be nullable';
    END IF;
END$$

DELIMITER ;

CALL `migrate_mes_frontline_selected_initial_allocation`();
DROP PROCEDURE IF EXISTS `migrate_mes_frontline_selected_initial_allocation`;
