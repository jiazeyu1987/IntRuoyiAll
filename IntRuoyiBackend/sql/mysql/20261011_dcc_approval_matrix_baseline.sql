-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260513_dcc_base_schema,20261011_dcc_application_effective_distribution; type=schema; riskLevel=medium
-- IC13 original public baseline and runtime source identity only. No historical backfill or derived routes.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS ensure_dcc_matrix_baseline_column;
DELIMITER //
CREATE PROCEDURE ensure_dcc_matrix_baseline_column(IN target_table VARCHAR(64),IN field_name VARCHAR(64),
    IN field_ddl TEXT,IN expected_type VARCHAR(64),IN expected_charset VARCHAR(64),IN expected_collation VARCHAR(64))
BEGIN
  IF target_table NOT IN ('dcc_controlled_file','dcc_controlled_file_route_snapshot')
     OR NOT EXISTS(SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=target_table AND ENGINE='InnoDB') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC13 requires exact existing InnoDB file and snapshot tables';
  END IF;
  IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=target_table AND COLUMN_NAME=field_name) THEN
    SET @dcc_matrix_baseline_ddl=CONCAT('ALTER TABLE ',target_table,' ADD COLUMN ',field_name,' ',field_ddl);
    PREPARE dcc_matrix_baseline_statement FROM @dcc_matrix_baseline_ddl;
    EXECUTE dcc_matrix_baseline_statement;
    DEALLOCATE PREPARE dcc_matrix_baseline_statement;
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=target_table AND COLUMN_NAME=field_name
      AND COLUMN_TYPE=expected_type AND IS_NULLABLE='YES' AND COLUMN_DEFAULT IS NULL AND EXTRA='' AND COALESCE(GENERATION_EXPRESSION,'')=''
      AND (CHARACTER_SET_NAME <=> expected_charset) AND (COLLATION_NAME <=> expected_collation))<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC13 original matrix baseline column definition drift';
  END IF;
END//
DELIMITER ;
CALL ensure_dcc_matrix_baseline_column('dcc_controlled_file','draft_approval_matrix_json','LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL','longtext','utf8mb4','utf8mb4_bin');
CALL ensure_dcc_matrix_baseline_column('dcc_controlled_file_route_snapshot','source_approval_matrix_id','BIGINT NULL','bigint',NULL,NULL);
CALL ensure_dcc_matrix_baseline_column('dcc_controlled_file_route_snapshot','source_approval_matrix_version','INT NULL','int',NULL,NULL);
CALL ensure_dcc_matrix_baseline_column('dcc_controlled_file_route_snapshot','source_approval_matrix_digest','VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL','varchar(64)','ascii','ascii_bin');
CALL ensure_dcc_matrix_baseline_column('dcc_controlled_file_route_snapshot','derivation_version','VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL','varchar(64)','ascii','ascii_bin');
CALL ensure_dcc_matrix_baseline_column('dcc_controlled_file_route_snapshot','source_approval_matrix_json','LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL','longtext','utf8mb4','utf8mb4_bin');
CALL ensure_dcc_matrix_baseline_column('dcc_controlled_file_route_snapshot','runtime_approval_nodes_json','LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL','longtext','utf8mb4','utf8mb4_bin');
DROP PROCEDURE IF EXISTS ensure_dcc_matrix_baseline_column;
