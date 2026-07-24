-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
DROP PROCEDURE IF EXISTS intruoyi_add_dcc_controlled_file_column;
DELIMITER $$
CREATE PROCEDURE intruoyi_add_dcc_controlled_file_column(
    IN p_column_name varchar(64),
    IN p_column_definition varchar(1000)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = 'dcc_controlled_file'
           AND column_name = p_column_name
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `dcc_controlled_file` ADD COLUMN `', p_column_name, '` ', p_column_definition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL intruoyi_add_dcc_controlled_file_column('drawing_pdf_file_id', 'bigint NULL COMMENT ''图纸源文件对应 PDF 文件 ID'' AFTER `original_file_id`');
CALL intruoyi_add_dcc_controlled_file_column('product_code', 'varchar(32) NULL COMMENT ''14 位产品编号'' AFTER `file_number`');
CALL intruoyi_add_dcc_controlled_file_column('need_training', 'bit(1) NOT NULL DEFAULT b''0'' COMMENT ''是否需要申请人上传培训记录'' AFTER `product_code`');
CALL intruoyi_add_dcc_controlled_file_column('process_type', 'varchar(32) NOT NULL DEFAULT ''CONTROLLED_FILE'' COMMENT ''DCC 流程类型'' AFTER `need_training`');

DROP PROCEDURE IF EXISTS intruoyi_add_dcc_controlled_file_column;
