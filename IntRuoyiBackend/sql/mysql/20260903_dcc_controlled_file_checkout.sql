-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- Add checkout state required by the current DCC controlled-file mapper.
-- This migration is safe to rerun against legacy databases.
DROP PROCEDURE IF EXISTS ensure_dcc_checkout_column;
DELIMITER $$
CREATE PROCEDURE ensure_dcc_checkout_column(
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
        SET @ddl = CONCAT(
            'ALTER TABLE `dcc_controlled_file` ADD COLUMN `',
            p_column_name,
            '` ',
            p_column_definition
        );
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL ensure_dcc_checkout_column(
    'checked_out_by',
    'bigint DEFAULT NULL COMMENT ''当前检出人用户ID'' AFTER `finalization_error`'
);
CALL ensure_dcc_checkout_column(
    'checked_out_time',
    'datetime DEFAULT NULL COMMENT ''当前检出时间'' AFTER `checked_out_by`'
);

DROP PROCEDURE IF EXISTS ensure_dcc_checkout_index;
DELIMITER $$
CREATE PROCEDURE ensure_dcc_checkout_index()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'dcc_controlled_file'
          AND index_name = 'idx_dcc_controlled_file_checkout'
    ) THEN
        ALTER TABLE `dcc_controlled_file`
            ADD KEY `idx_dcc_controlled_file_checkout`
                (`tenant_id`, `checked_out_by`, `checked_out_time`);
    END IF;
END$$
DELIMITER ;

CALL ensure_dcc_checkout_index();

DROP PROCEDURE IF EXISTS ensure_dcc_checkout_index;
DROP PROCEDURE IF EXISTS ensure_dcc_checkout_column;
