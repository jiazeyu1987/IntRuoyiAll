-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
DROP PROCEDURE IF EXISTS intruoyi_add_dcc_distribution_recipient_column;
DELIMITER $$
CREATE PROCEDURE intruoyi_add_dcc_distribution_recipient_column(
    IN p_column_name varchar(64),
    IN p_column_definition varchar(1000)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = 'dcc_controlled_file_distribution_recipient'
           AND column_name = p_column_name
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `dcc_controlled_file_distribution_recipient` ADD COLUMN `', p_column_name, '` ', p_column_definition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL intruoyi_add_dcc_distribution_recipient_column('ack_comment', 'varchar(1000) NULL COMMENT ''电子发放签收意见'' AFTER `acknowledged_at`');

DROP PROCEDURE IF EXISTS intruoyi_add_dcc_distribution_recipient_column;

DROP PROCEDURE IF EXISTS intruoyi_add_dcc_distribution_column;
DELIMITER $$
CREATE PROCEDURE intruoyi_add_dcc_distribution_column(
    IN p_column_name varchar(64),
    IN p_column_definition varchar(1000)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = 'dcc_controlled_file_distribution'
           AND column_name = p_column_name
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `dcc_controlled_file_distribution` ADD COLUMN `', p_column_name, '` ', p_column_definition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL intruoyi_add_dcc_distribution_column('recovered_by', 'bigint NULL COMMENT ''纸质发放回收人'' AFTER `acknowledged_at`');
CALL intruoyi_add_dcc_distribution_column('recovered_at', 'datetime NULL COMMENT ''纸质发放回收时间'' AFTER `recovered_by`');

DROP PROCEDURE IF EXISTS intruoyi_add_dcc_distribution_column;
