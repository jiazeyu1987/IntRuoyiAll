-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260930_dcc_b_project_attributes_folders; type=schema; riskLevel=medium
-- No account is preselected and no historical request is rewritten.
SET NAMES utf8mb4;
CREATE TABLE IF NOT EXISTS dcc_project_reviewer_config (
 tenant_id BIGINT NOT NULL PRIMARY KEY,
 reviewer_user_id BIGINT NOT NULL,
 reviewer_username VARCHAR(128) NOT NULL,
 reviewer_nickname VARCHAR(128) NOT NULL,
 version_no INT NOT NULL,
 updated_by BIGINT NOT NULL,
 change_reason VARCHAR(500) NOT NULL,
 update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
DROP PROCEDURE IF EXISTS ensure_dcc_reviewer_snapshot_column;
DELIMITER //
CREATE PROCEDURE ensure_dcc_reviewer_snapshot_column(IN column_name_value VARCHAR(64), IN definition_value TEXT)
BEGIN
 IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
   AND TABLE_NAME='dcc_project_product_create_request' AND COLUMN_NAME=column_name_value) THEN
  SET @dcc_reviewer_ddl=CONCAT('ALTER TABLE dcc_project_product_create_request ADD COLUMN ',column_name_value,' ',definition_value);
  PREPARE dcc_reviewer_statement FROM @dcc_reviewer_ddl; EXECUTE dcc_reviewer_statement; DEALLOCATE PREPARE dcc_reviewer_statement;
 END IF;
END//
DELIMITER ;
CALL ensure_dcc_reviewer_snapshot_column('configured_reviewer_user_id','BIGINT NULL');
CALL ensure_dcc_reviewer_snapshot_column('configured_reviewer_username','VARCHAR(128) NULL');
CALL ensure_dcc_reviewer_snapshot_column('configured_reviewer_nickname','VARCHAR(128) NULL');
DROP PROCEDURE IF EXISTS ensure_dcc_reviewer_snapshot_column;
