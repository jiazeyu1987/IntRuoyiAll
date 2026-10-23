-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260930_dcc_b_project_attributes_folders; type=schema; riskLevel=medium
-- Manual project folders have no source template. Existing source identities remain unchanged.
ALTER TABLE dcc_project_folder
 MODIFY COLUMN source_template_id BIGINT NULL,
 MODIFY COLUMN source_node_key VARCHAR(64) NULL;
