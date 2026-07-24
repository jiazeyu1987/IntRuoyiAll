-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
DROP PROCEDURE IF EXISTS intruoyi_add_mes_route_process_column;
DELIMITER $$
CREATE PROCEDURE intruoyi_add_mes_route_process_column(
    IN p_column_name varchar(64),
    IN p_column_definition varchar(1000)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = 'mes_pro_route_process'
           AND column_name = p_column_name
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `mes_pro_route_process` ADD COLUMN `', p_column_name, '` ', p_column_definition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL intruoyi_add_mes_route_process_column('batch_record_report_id', 'varchar(64) DEFAULT NULL COMMENT ''默认批记录报表 ID'' AFTER `check_flag`');

DROP PROCEDURE IF EXISTS intruoyi_add_mes_route_process_column;
