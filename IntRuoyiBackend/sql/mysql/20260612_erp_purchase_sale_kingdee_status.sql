-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- ERP purchase/sale orders: persist Kingdee source close and cancel states.
-- MySQL 8.0 compatibility: use information_schema guards instead of ADD COLUMN IF NOT EXISTS.

SET @erp_purchase_order_close_status_sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'erp_purchase_order'
              AND COLUMN_NAME = 'kingdee_close_status'
        ),
        'SELECT 1',
        'ALTER TABLE `erp_purchase_order` ADD COLUMN `kingdee_close_status` varchar(16) DEFAULT NULL COMMENT ''金蝶关闭状态'' AFTER `remark`'
    )
);
PREPARE stmt FROM @erp_purchase_order_close_status_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @erp_purchase_order_cancel_status_sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'erp_purchase_order'
              AND COLUMN_NAME = 'kingdee_cancel_status'
        ),
        'SELECT 1',
        'ALTER TABLE `erp_purchase_order` ADD COLUMN `kingdee_cancel_status` varchar(16) DEFAULT NULL COMMENT ''金蝶作废状态'' AFTER `kingdee_close_status`'
    )
);
PREPARE stmt FROM @erp_purchase_order_cancel_status_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @erp_sale_order_close_status_sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'erp_sale_order'
              AND COLUMN_NAME = 'kingdee_close_status'
        ),
        'SELECT 1',
        'ALTER TABLE `erp_sale_order` ADD COLUMN `kingdee_close_status` varchar(16) DEFAULT NULL COMMENT ''金蝶关闭状态'' AFTER `remark`'
    )
);
PREPARE stmt FROM @erp_sale_order_close_status_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @erp_sale_order_cancel_status_sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'erp_sale_order'
              AND COLUMN_NAME = 'kingdee_cancel_status'
        ),
        'SELECT 1',
        'ALTER TABLE `erp_sale_order` ADD COLUMN `kingdee_cancel_status` varchar(16) DEFAULT NULL COMMENT ''金蝶作废状态'' AFTER `kingdee_close_status`'
    )
);
PREPARE stmt FROM @erp_sale_order_cancel_status_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
