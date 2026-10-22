-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260719_dcc_file_type_taxonomy; type=schema; riskLevel=medium
-- Formal Category is the only approval-source configuration owner.
-- Existing categories receive explicit PUBLIC_MATRIX only when the new column is first added.
-- APPLICATION_CUSTOM is selected through authorized category management. No runtime/schema default.
-- Does not modify Master, Version, application, signature or public matrix rows.
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS migrate_dcc_category_approval_source_mode;
DELIMITER $$
CREATE PROCEDURE migrate_dcc_category_approval_source_mode()
BEGIN
    DECLARE source_column_count INT DEFAULT 0;
    DECLARE valid_column_count INT DEFAULT 0;
    DECLARE category_table_count INT DEFAULT 0;
    DECLARE category_identity_count INT DEFAULT 0;
    DECLARE invalid_source_count BIGINT DEFAULT 0;

    SELECT COUNT(*) INTO category_table_count
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dcc_file_category';
    SELECT COUNT(*) INTO category_identity_count
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dcc_file_category'
      AND COLUMN_NAME IN ('id', 'tenant_id', 'deleted', 'file_type_taxonomy_id');
    IF category_table_count <> 1 OR category_identity_count <> 4 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'CATEGORY_APPROVAL_SOURCE_REQUIRED_SCHEMA_MISSING';
    END IF;

    SELECT COUNT(*) INTO source_column_count
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dcc_file_category'
      AND COLUMN_NAME = 'approval_source_mode';
    IF source_column_count = 0 THEN
        ALTER TABLE `dcc_file_category`
            ADD COLUMN `approval_source_mode` VARCHAR(32) NULL
            COMMENT 'Explicit PUBLIC_MATRIX or APPLICATION_CUSTOM approval source' AFTER `file_type_taxonomy_id`;
        UPDATE `dcc_file_category`
        SET `approval_source_mode` = 'PUBLIC_MATRIX'
        WHERE `approval_source_mode` IS NULL;
        ALTER TABLE `dcc_file_category`
            MODIFY COLUMN `approval_source_mode` VARCHAR(32) NOT NULL
            COMMENT 'Explicit PUBLIC_MATRIX or APPLICATION_CUSTOM approval source';
    ELSE
        SELECT COUNT(*) INTO valid_column_count
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dcc_file_category'
          AND COLUMN_NAME = 'approval_source_mode' AND DATA_TYPE = 'varchar'
          AND CHARACTER_MAXIMUM_LENGTH = 32 AND IS_NULLABLE = 'NO' AND COLUMN_DEFAULT IS NULL;
        IF source_column_count <> 1 OR valid_column_count <> 1 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'CATEGORY_APPROVAL_SOURCE_SCHEMA_INVALID';
        END IF;
    END IF;

    SELECT COUNT(*) INTO invalid_source_count FROM `dcc_file_category`
    WHERE `approval_source_mode` IS NULL
       OR BINARY `approval_source_mode` NOT IN ('PUBLIC_MATRIX', 'APPLICATION_CUSTOM');
    IF invalid_source_count <> 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'CATEGORY_APPROVAL_SOURCE_VALUE_INVALID';
    END IF;
END$$
DELIMITER ;
CALL migrate_dcc_category_approval_source_mode();
DROP PROCEDURE IF EXISTS migrate_dcc_category_approval_source_mode;
