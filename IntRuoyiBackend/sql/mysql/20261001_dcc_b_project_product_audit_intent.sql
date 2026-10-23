-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260930_dcc_b_project_attributes_folders; type=schema; riskLevel=medium
-- B-REV-01: explicit human intent and write-attempt identity. No historical business backfill.
SET @dcc_b_audit_ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'dcc_project_product_create_request' AND column_name = 'creation_reason') = 0, 'ALTER TABLE dcc_project_product_create_request ADD COLUMN creation_reason VARCHAR(500) NULL', 'SELECT 1');
PREPARE dcc_b_audit_stmt FROM @dcc_b_audit_ddl;
EXECUTE dcc_b_audit_stmt;
DEALLOCATE PREPARE dcc_b_audit_stmt;
SET @dcc_b_audit_ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'dcc_project_product_create_request' AND column_name = 'write_attempt_no') = 0, 'ALTER TABLE dcc_project_product_create_request ADD COLUMN write_attempt_no INT NULL', 'SELECT 1');
PREPARE dcc_b_audit_stmt FROM @dcc_b_audit_ddl;
EXECUTE dcc_b_audit_stmt;
DEALLOCATE PREPARE dcc_b_audit_stmt;
SET @dcc_b_audit_ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'dcc_project_product_create_request' AND column_name = 'write_reason') = 0, 'ALTER TABLE dcc_project_product_create_request ADD COLUMN write_reason VARCHAR(500) NULL', 'SELECT 1');
PREPARE dcc_b_audit_stmt FROM @dcc_b_audit_ddl;
EXECUTE dcc_b_audit_stmt;
DEALLOCATE PREPARE dcc_b_audit_stmt;
SET @dcc_b_audit_ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'dcc_project_product_create_request' AND column_name = 'write_operator_user_id') = 0, 'ALTER TABLE dcc_project_product_create_request ADD COLUMN write_operator_user_id BIGINT NULL', 'SELECT 1');
PREPARE dcc_b_audit_stmt FROM @dcc_b_audit_ddl;
EXECUTE dcc_b_audit_stmt;
DEALLOCATE PREPARE dcc_b_audit_stmt;
