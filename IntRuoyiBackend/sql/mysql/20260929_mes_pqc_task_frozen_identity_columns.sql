-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260802_mes_pqc_inspection_task; type=schema; riskLevel=medium
-- PQT-SCHEMA-01: nullable task-carried identity snapshots, not current-master backfill.
-- All existing definitions are checked BEFORE the first ALTER; no automatic conversion.
-- MySQL DDL commits independently: retain compatible partial additions and rerun after
-- resolving the external failure. Do not claim transactional DDL rollback.
-- Application rollback retains these additive columns; never auto-drop populated facts.
-- Full service deployment still requires its other QA/C00/C015/item-identity migrations.

DROP PROCEDURE IF EXISTS check_pqt_frozen_identity;
DROP PROCEDURE IF EXISTS add_pqt_frozen_identity;
DELIMITER $$
CREATE PROCEDURE check_pqt_frozen_identity(IN require_all BOOLEAN)
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE target_column VARCHAR(64);
    DECLARE expected_type VARCHAR(16);
    DECLARE expected_length INT;
    DECLARE table_collation VARCHAR(64);
    DECLARE found_count INT;
    DECLARE valid_count INT;
    DECLARE failure_message VARCHAR(128);

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
         WHERE table_schema = DATABASE() AND table_name = 'mes_pqc_inspection_task'
           AND table_type = 'BASE TABLE' AND engine = 'InnoDB'
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'PQT requires existing formal InnoDB task table';
    END IF;
    SELECT t.table_collation INTO table_collation FROM information_schema.tables t
     WHERE t.table_schema = DATABASE() AND t.table_name = 'mes_pqc_inspection_task';
    IF table_collation IS NULL OR table_collation NOT REGEXP '^utf8mb4_[a-zA-Z0-9_]+$' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'PQT requires existing utf8mb4 task collation';
    END IF;

    WHILE i <= 5 DO
        SET target_column = CASE i
            WHEN 1 THEN 'dcc_project_code_id' WHEN 2 THEN 'dcc_project_code'
            WHEN 3 THEN 'dcc_project_name' WHEN 4 THEN 'qa_regulation_id'
            WHEN 5 THEN 'qa_regulation_version_no' END;
        SET expected_type = IF(i IN (1, 4), 'bigint', 'varchar');
        SET expected_length = CASE i WHEN 2 THEN 64 WHEN 3 THEN 255 WHEN 5 THEN 32 ELSE NULL END;
        SELECT COUNT(*) INTO found_count FROM information_schema.columns c
         WHERE c.table_schema = DATABASE() AND c.table_name = 'mes_pqc_inspection_task'
           AND c.column_name = target_column;
        IF found_count = 0 THEN
            IF require_all THEN
                SET failure_message = CONCAT('PQT postcheck missing column: ', target_column);
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = failure_message;
            END IF;
        ELSE
            SELECT COUNT(*) INTO valid_count FROM information_schema.columns c
             WHERE c.table_schema = DATABASE() AND c.table_name = 'mes_pqc_inspection_task'
               AND c.column_name = target_column
               AND c.data_type = expected_type
               AND c.is_nullable = 'YES' AND c.column_default IS NULL
               AND c.extra = '' AND COALESCE(c.generation_expression, '') = ''
               AND (
                   (expected_type = 'bigint'
                    AND c.column_type REGEXP '^bigint(\\([0-9]+\\))?$'
                    AND c.character_set_name IS NULL AND c.collation_name IS NULL)
                   OR
                   (expected_type = 'varchar' AND c.character_maximum_length = expected_length
                    AND c.character_set_name = 'utf8mb4'
                    AND BINARY c.collation_name = BINARY table_collation)
               );
            IF valid_count <> 1 THEN
                SET failure_message = CONCAT('PQT incompatible column: ', target_column);
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = failure_message;
            END IF;
        END IF;
        SET i = i + 1;
    END WHILE;
END$$

CREATE PROCEDURE add_pqt_frozen_identity()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE target_column VARCHAR(64);
    DECLARE column_definition VARCHAR(255);
    DECLARE table_collation VARCHAR(64);

    -- No target-table mutation may precede this complete five-column preflight.
    CALL check_pqt_frozen_identity(FALSE);
    SELECT t.table_collation INTO table_collation FROM information_schema.tables t
     WHERE t.table_schema = DATABASE() AND t.table_name = 'mes_pqc_inspection_task';
    WHILE i <= 5 DO
        SET target_column = CASE i
            WHEN 1 THEN 'dcc_project_code_id' WHEN 2 THEN 'dcc_project_code'
            WHEN 3 THEN 'dcc_project_name' WHEN 4 THEN 'qa_regulation_id'
            WHEN 5 THEN 'qa_regulation_version_no' END;
        SET column_definition = CASE i
            WHEN 1 THEN 'BIGINT' WHEN 2 THEN 'VARCHAR(64)'
            WHEN 3 THEN 'VARCHAR(255)' WHEN 4 THEN 'BIGINT'
            WHEN 5 THEN 'VARCHAR(32)' END;
        IF i IN (2, 3, 5) THEN
            SET column_definition = CONCAT(column_definition, ' CHARACTER SET utf8mb4 COLLATE ', table_collation);
        END IF;
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns c
             WHERE c.table_schema = DATABASE() AND c.table_name = 'mes_pqc_inspection_task'
               AND c.column_name = target_column
        ) THEN
            SET @pqt_identity_ddl = CONCAT('ALTER TABLE `mes_pqc_inspection_task` ADD COLUMN `',
                target_column, '` ', column_definition, ' NULL DEFAULT NULL');
            PREPARE pqt_identity_stmt FROM @pqt_identity_ddl;
            EXECUTE pqt_identity_stmt;
            DEALLOCATE PREPARE pqt_identity_stmt;
        END IF;
        SET i = i + 1;
    END WHILE;
    CALL check_pqt_frozen_identity(TRUE);
    SET @pqt_identity_ddl = NULL;
END$$
DELIMITER ;

CALL add_pqt_frozen_identity();
DROP PROCEDURE IF EXISTS add_pqt_frozen_identity;
DROP PROCEDURE IF EXISTS check_pqt_frozen_identity;
