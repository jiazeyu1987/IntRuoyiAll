-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260513_dcc_base_schema; type=schema; riskLevel=medium
-- Subject IDs precede type so existing UPPER(subject_type) predicates can retain their exact semantics.
-- Additive non-unique index only: no grant, tenant, role, menu or business row is changed.
DELIMITER $$
CREATE PROCEDURE `ensure_dcc_directory_subject_index`()
BEGIN
    DECLARE existing_parts int DEFAULT 0;
    DECLARE correct_parts int DEFAULT 0;
    DECLARE required_columns int DEFAULT 0;
    SELECT COUNT(*) INTO required_columns
      FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dcc_directory_access_rule'
       AND IS_NULLABLE = 'NO'
       AND ((COLUMN_NAME IN ('tenant_id', 'subject_id') AND DATA_TYPE = 'bigint')
         OR (COLUMN_NAME IN ('deleted', 'active') AND DATA_TYPE = 'tinyint')
         OR (COLUMN_NAME = 'subject_type' AND DATA_TYPE = 'varchar' AND CHARACTER_MAXIMUM_LENGTH = 32));
    IF required_columns <> 5 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Directory subject index column prerequisites mismatch';
    END IF;
    SELECT COUNT(*), COALESCE(SUM(
        NON_UNIQUE = 1 AND SUB_PART IS NULL AND INDEX_TYPE = 'BTREE' AND IS_VISIBLE = 'YES'
        AND ((SEQ_IN_INDEX = 1 AND COLUMN_NAME = 'tenant_id')
          OR (SEQ_IN_INDEX = 2 AND COLUMN_NAME = 'deleted')
          OR (SEQ_IN_INDEX = 3 AND COLUMN_NAME = 'active')
          OR (SEQ_IN_INDEX = 4 AND COLUMN_NAME = 'subject_id')
          OR (SEQ_IN_INDEX = 5 AND COLUMN_NAME = 'subject_type'))), 0)
      INTO existing_parts, correct_parts
      FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dcc_directory_access_rule'
       AND INDEX_NAME = 'idx_dcc_access_rule_subject';
    IF existing_parts = 0 THEN
        ALTER TABLE `dcc_directory_access_rule`
            ADD INDEX `idx_dcc_access_rule_subject`
                (`tenant_id`, `deleted`, `active`, `subject_id`, `subject_type`),
            ALGORITHM = INPLACE, LOCK = NONE;
    ELSEIF existing_parts <> 5 OR correct_parts <> 5 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Directory subject index existing definition mismatch';
    END IF;
END$$
DELIMITER ;
CALL `ensure_dcc_directory_subject_index`();
DROP PROCEDURE `ensure_dcc_directory_subject_index`;
