-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium

DROP PROCEDURE IF EXISTS ensure_erp_kingdee_sync_record_tenant_unique;

DELIMITER $$

CREATE PROCEDURE ensure_erp_kingdee_sync_record_tenant_unique(
    IN p_table_name VARCHAR(128),
    IN p_old_index_name VARCHAR(128),
    IN p_new_index_name VARCHAR(128),
    IN p_new_index_columns VARCHAR(512)
)
BEGIN
    DECLARE v_old_index_count INT DEFAULT 0;
    DECLARE v_new_index_count INT DEFAULT 0;
    DECLARE v_message VARCHAR(255);
    DECLARE v_sql TEXT;

    SELECT COUNT(1)
      INTO v_old_index_count
      FROM INFORMATION_SCHEMA.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = p_table_name
       AND INDEX_NAME = p_old_index_name;

    SELECT COUNT(1)
      INTO v_new_index_count
      FROM INFORMATION_SCHEMA.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = p_table_name
       AND INDEX_NAME = p_new_index_name;

    IF v_old_index_count = 0 AND v_new_index_count = 0 THEN
        SET v_message = CONCAT('Missing required old or new Kingdee sync unique index on ', p_table_name);
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_message;
    END IF;

    IF v_old_index_count > 0 THEN
        SET v_sql = CONCAT('ALTER TABLE `', p_table_name, '` DROP INDEX `', p_old_index_name, '`');
        SET @kingdee_tenant_unique_sql = v_sql;
        PREPARE stmt FROM @kingdee_tenant_unique_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;

    IF v_new_index_count = 0 THEN
        SET v_sql = CONCAT(
            'ALTER TABLE `', p_table_name, '` ADD UNIQUE KEY `', p_new_index_name, '` ', p_new_index_columns
        );
        SET @kingdee_tenant_unique_sql = v_sql;
        PREPARE stmt FROM @kingdee_tenant_unique_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

DELIMITER ;

CALL ensure_erp_kingdee_sync_record_tenant_unique(
    'erp_kingdee_supplier_sync_record',
    'uk_erp_kingdee_supplier_source',
    'uk_erp_kingdee_supplier_tenant_source',
    '(`tenant_id`, `source_supplier_number`, `deleted`)'
);

CALL ensure_erp_kingdee_sync_record_tenant_unique(
    'erp_kingdee_customer_sync_record',
    'uk_erp_kingdee_customer_source',
    'uk_erp_kingdee_customer_tenant_source',
    '(`tenant_id`, `source_customer_number`, `deleted`)'
);

CALL ensure_erp_kingdee_sync_record_tenant_unique(
    'erp_kingdee_purchase_order_sync_record',
    'uk_erp_kingdee_po_sync_source',
    'uk_erp_kingdee_po_sync_tenant_source',
    '(`tenant_id`, `source_form_id`, `source_fid`, `deleted`)'
);

CALL ensure_erp_kingdee_sync_record_tenant_unique(
    'erp_kingdee_sale_order_sync_record',
    'uk_erp_kingdee_sale_source',
    'uk_erp_kingdee_sale_tenant_source',
    '(`tenant_id`, `source_form_id`, `source_fid`, `deleted`)'
);

DROP PROCEDURE IF EXISTS ensure_erp_kingdee_sync_record_tenant_unique;
