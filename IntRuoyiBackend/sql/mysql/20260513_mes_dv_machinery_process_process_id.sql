-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
DROP PROCEDURE IF EXISTS intruoyi_add_mes_dv_machinery_process_column;
DELIMITER $$
CREATE PROCEDURE intruoyi_add_mes_dv_machinery_process_column(
    IN p_column_name varchar(64),
    IN p_column_definition varchar(1000)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = 'mes_dv_machinery_process'
           AND column_name = p_column_name
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `mes_dv_machinery_process` ADD COLUMN `', p_column_name, '` ', p_column_definition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL intruoyi_add_mes_dv_machinery_process_column('process_id', 'bigint NULL COMMENT ''工序编号'' AFTER `machinery_id`');

DROP PROCEDURE IF EXISTS intruoyi_add_mes_dv_machinery_process_column;

UPDATE `mes_dv_machinery_process` mp
JOIN `mes_pro_process` p
    ON p.`name` = mp.`process_name`
   AND p.`deleted` = 0
   AND p.`status` = 0
SET mp.`process_id` = p.`id`
WHERE mp.`process_id` IS NULL;
