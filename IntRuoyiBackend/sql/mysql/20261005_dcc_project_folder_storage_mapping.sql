-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261001_dcc_b_project_file_placement,20260930_dcc_b_project_attributes_folders,20260513_dcc_base_schema; type=schema; riskLevel=medium
-- LD03: additive internal database storage mapping only. No legacy rows, files, signatures, NAS or ACL DML.
CREATE TABLE IF NOT EXISTS dcc_project_folder_storage_mapping (
 id BIGINT NOT NULL AUTO_INCREMENT,
 tenant_id BIGINT NOT NULL,
 project_code_id BIGINT NOT NULL,
 project_folder_id BIGINT NOT NULL,
 category_id BIGINT NOT NULL,
 base_directory_id BIGINT NOT NULL,
 storage_directory_id BIGINT NOT NULL,
 creator VARCHAR(64) DEFAULT NULL,
 updater VARCHAR(64) DEFAULT NULL,
 create_time DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
 update_time DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
 deleted BIT NOT NULL DEFAULT b'0',
 PRIMARY KEY(id),
 UNIQUE KEY uk_dcc_folder_category_storage(tenant_id,project_folder_id,category_id),
 UNIQUE KEY uk_dcc_storage_mapping_leaf(tenant_id,storage_directory_id),
 KEY idx_dcc_storage_mapping_base(tenant_id,base_directory_id),
 KEY idx_dcc_storage_mapping_project(tenant_id,project_code_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Exact internal project folder category storage mapping';
