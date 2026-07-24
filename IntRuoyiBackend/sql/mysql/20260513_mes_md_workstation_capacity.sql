-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
DROP PROCEDURE IF EXISTS intruoyi_add_mes_md_workstation_column;
DELIMITER $$
CREATE PROCEDURE intruoyi_add_mes_md_workstation_column(
    IN p_column_name varchar(64),
    IN p_column_definition varchar(1000)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = 'mes_md_workstation'
           AND column_name = p_column_name
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `mes_md_workstation` ADD COLUMN `', p_column_name, '` ', p_column_definition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL intruoyi_add_mes_md_workstation_column('single_standard_hourly_capacity', 'decimal(10,2) NULL COMMENT ''单人标准小时产能'' AFTER `area_id`');

DROP PROCEDURE IF EXISTS intruoyi_add_mes_md_workstation_column;
