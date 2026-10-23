-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260930_dcc_b_project_attributes_folders; type=schema; riskLevel=medium
-- INT-B-01: no automatic folder/directory conversion and no historical backfill.
CREATE TABLE IF NOT EXISTS dcc_project_file_placement (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 tenant_id BIGINT NOT NULL, project_code_id BIGINT NOT NULL, project_folder_id BIGINT NOT NULL,
 controlled_file_id BIGINT NOT NULL, storage_directory_id BIGINT NOT NULL,
 creator VARCHAR(64), updater VARCHAR(64), create_time DATETIME, update_time DATETIME,
 deleted BIT NOT NULL DEFAULT 0,
 CONSTRAINT uk_dcc_b_placement_version UNIQUE (tenant_id, controlled_file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
