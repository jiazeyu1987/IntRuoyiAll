-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
DROP PROCEDURE IF EXISTS intruoyi_add_mes_batch_record_report_column;
DELIMITER $$
CREATE PROCEDURE intruoyi_add_mes_batch_record_report_column(
    IN p_column_name varchar(64),
    IN p_column_definition varchar(1000)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = 'mes_pro_batch_record_report'
           AND column_name = p_column_name
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `mes_pro_batch_record_report` ADD COLUMN `', p_column_name, '` ', p_column_definition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL intruoyi_add_mes_batch_record_report_column('route_key', 'varchar(32) NOT NULL DEFAULT ''LEGACY'' COMMENT ''识别路线'' AFTER `sample_key`');

DROP PROCEDURE IF EXISTS intruoyi_add_mes_batch_record_report_column;

UPDATE `mes_pro_batch_record_report`
SET `route_key` = 'LEGACY'
WHERE `route_key` IS NULL OR `route_key` = '';

DROP PROCEDURE IF EXISTS intruoyi_update_mes_batch_record_report_route_index;
DELIMITER $$
CREATE PROCEDURE intruoyi_update_mes_batch_record_report_route_index()
BEGIN
    IF EXISTS (
        SELECT 1
          FROM information_schema.statistics
         WHERE table_schema = DATABASE()
           AND table_name = 'mes_pro_batch_record_report'
           AND index_name = 'uk_mes_batch_record_report_sample_table'
    ) THEN
        ALTER TABLE `mes_pro_batch_record_report`
            DROP INDEX `uk_mes_batch_record_report_sample_table`;
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.statistics
         WHERE table_schema = DATABASE()
           AND table_name = 'mes_pro_batch_record_report'
           AND index_name = 'uk_mes_batch_record_report_sample_route_table'
    ) THEN
        ALTER TABLE `mes_pro_batch_record_report`
            ADD UNIQUE KEY `uk_mes_batch_record_report_sample_route_table` (`sample_key`, `route_key`, `source_table_index`);
    END IF;
END$$
DELIMITER ;

CALL intruoyi_update_mes_batch_record_report_route_index();

DROP PROCEDURE IF EXISTS intruoyi_update_mes_batch_record_report_route_index;
