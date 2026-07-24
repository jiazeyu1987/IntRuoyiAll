-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
DROP PROCEDURE IF EXISTS intruoyi_add_dcc_controlled_file_training_column;
DELIMITER $$
CREATE PROCEDURE intruoyi_add_dcc_controlled_file_training_column(
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

CALL intruoyi_add_dcc_controlled_file_training_column('training_record_file_id', 'bigint NULL COMMENT ''申请人培训记录文件 ID'' AFTER `drawing_pdf_file_id`');

DROP PROCEDURE IF EXISTS intruoyi_add_dcc_controlled_file_training_column;
