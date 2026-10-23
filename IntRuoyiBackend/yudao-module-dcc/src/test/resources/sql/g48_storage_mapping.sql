CREATE TABLE IF NOT EXISTS dcc_project_folder_storage_mapping (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, tenant_id BIGINT NOT NULL, project_code_id BIGINT NOT NULL,
 project_folder_id BIGINT NOT NULL, category_id BIGINT NOT NULL, base_directory_id BIGINT NOT NULL,
 storage_directory_id BIGINT NOT NULL, creator VARCHAR(64),updater VARCHAR(64),create_time TIMESTAMP,
 update_time TIMESTAMP,deleted BIT NOT NULL DEFAULT 0,
 UNIQUE(tenant_id,project_folder_id,category_id),UNIQUE(tenant_id,storage_directory_id)
);
DELETE FROM dcc_project_folder_storage_mapping;
