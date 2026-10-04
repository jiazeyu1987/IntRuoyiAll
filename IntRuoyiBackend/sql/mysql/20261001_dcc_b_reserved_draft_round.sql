-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261001_dcc_application_round_link; type=schema; riskLevel=medium
-- CC-2: NULL BPM denotes a real unsubmitted file draft. Keep both existing unique identities.
ALTER TABLE dcc_application_round_link MODIFY COLUMN bpm_round VARCHAR(64) COLLATE utf8mb4_bin NULL;
SET @dcc_b_fork_ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'dcc_project_application_attributes' AND column_name = 'source_application_id') = 0, 'ALTER TABLE dcc_project_application_attributes ADD COLUMN source_application_id BIGINT NULL', 'SELECT 1');
PREPARE dcc_b_fork_stmt FROM @dcc_b_fork_ddl;
EXECUTE dcc_b_fork_stmt;
DEALLOCATE PREPARE dcc_b_fork_stmt;
SET @dcc_b_fork_ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'dcc_project_application_attributes' AND column_name = 'source_application_round') = 0, 'ALTER TABLE dcc_project_application_attributes ADD COLUMN source_application_round INT NULL', 'SELECT 1');
PREPARE dcc_b_fork_stmt FROM @dcc_b_fork_ddl;
EXECUTE dcc_b_fork_stmt;
DEALLOCATE PREPARE dcc_b_fork_stmt;
