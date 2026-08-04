-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260730_mes_process_pool_team_leader; type=schema; riskLevel=medium
-- P0 生产执行主闭环：班组长复核必须持久化正式电子签名证据

DROP PROCEDURE IF EXISTS ensure_mes_pp_review_signature_column;
DELIMITER $$
CREATE PROCEDURE ensure_mes_pp_review_signature_column(
    IN p_column_name varchar(64),
    IN p_column_definition varchar(1000),
    IN p_after_column varchar(64)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'mes_pro_process_pool_submission_review'
          AND COLUMN_NAME = p_column_name
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `mes_pro_process_pool_submission_review` ADD COLUMN `',
            p_column_name, '` ', p_column_definition, ' AFTER `', p_after_column, '`');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL ensure_mes_pp_review_signature_column(
    'review_signature_id',
    'bigint DEFAULT NULL COMMENT ''复核电子签名ID''',
    'reviewed_at'
);
CALL ensure_mes_pp_review_signature_column(
    'review_signature_user_id',
    'bigint DEFAULT NULL COMMENT ''复核电子签名用户ID''',
    'review_signature_id'
);
CALL ensure_mes_pp_review_signature_column(
    'review_signature_snapshot_json',
    'json DEFAULT NULL COMMENT ''复核电子签名快照JSON''',
    'review_signature_user_id'
);

DROP PROCEDURE IF EXISTS ensure_mes_pp_review_signature_column;

SET @idx_exists := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_process_pool_submission_review'
      AND INDEX_NAME = 'idx_mes_pp_review_signature'
);
SET @sql := IF(@idx_exists = 0,
    'ALTER TABLE `mes_pro_process_pool_submission_review` ADD KEY `idx_mes_pp_review_signature` (`tenant_id`, `review_signature_id`)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
