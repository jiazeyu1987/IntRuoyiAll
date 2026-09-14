-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260630_mes_pro_work_order_erp_snapshot_fields; type=schema; riskLevel=low
-- 生产工单增加金蝶需求单据与规格型号快照字段。

SET @mes_pro_work_order_table_exists = (
    SELECT COUNT(1)
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_work_order'
);
SET @mes_pro_work_order_table_check_sql = IF(
    @mes_pro_work_order_table_exists = 1,
    'SELECT 1',
    'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''missing required table mes_pro_work_order'''
);
PREPARE mes_pro_work_order_table_check_stmt FROM @mes_pro_work_order_table_check_sql;
EXECUTE mes_pro_work_order_table_check_stmt;
DEALLOCATE PREPARE mes_pro_work_order_table_check_stmt;

SET @mes_pro_work_order_demand_bill_no_exists = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_work_order'
      AND COLUMN_NAME = 'demand_bill_no'
);
SET @mes_pro_work_order_demand_bill_no_sql = IF(
    @mes_pro_work_order_demand_bill_no_exists = 0,
    'ALTER TABLE `mes_pro_work_order` ADD COLUMN `demand_bill_no` varchar(128) DEFAULT NULL COMMENT ''金蝶需求单据号'' AFTER `order_source_code`',
    'SELECT 1'
);
PREPARE mes_pro_work_order_demand_bill_no_stmt FROM @mes_pro_work_order_demand_bill_no_sql;
EXECUTE mes_pro_work_order_demand_bill_no_stmt;
DEALLOCATE PREPARE mes_pro_work_order_demand_bill_no_stmt;

SET @mes_pro_work_order_material_specification_exists = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_work_order'
      AND COLUMN_NAME = 'material_specification'
);
SET @mes_pro_work_order_material_specification_sql = IF(
    @mes_pro_work_order_material_specification_exists = 0,
    'ALTER TABLE `mes_pro_work_order` ADD COLUMN `material_specification` varchar(512) DEFAULT NULL COMMENT ''金蝶规格型号'' AFTER `product_id`',
    'SELECT 1'
);
PREPARE mes_pro_work_order_material_specification_stmt FROM @mes_pro_work_order_material_specification_sql;
EXECUTE mes_pro_work_order_material_specification_stmt;
DEALLOCATE PREPARE mes_pro_work_order_material_specification_stmt;

UPDATE `mes_pro_work_order` work_order
LEFT JOIN `mes_md_item` item
       ON item.`id` = work_order.`product_id`
      AND item.`deleted` = b'0'
SET work_order.`demand_bill_no` = COALESCE(NULLIF(work_order.`demand_bill_no`, ''), NULLIF(work_order.`order_source_code`, '')),
    work_order.`material_specification` = COALESCE(NULLIF(work_order.`material_specification`, ''), NULLIF(item.`specification`, ''))
WHERE work_order.`deleted` = b'0'
  AND (work_order.`demand_bill_no` IS NULL
       OR work_order.`demand_bill_no` = ''
       OR work_order.`material_specification` IS NULL
       OR work_order.`material_specification` = '');
