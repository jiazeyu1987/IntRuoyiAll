-- Isolated H2 tables copied from the synchronized formal round/reference migrations.
CREATE TABLE IF NOT EXISTS dcc_application_round_link (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, tenant_id BIGINT NOT NULL, project_id BIGINT NOT NULL,
 application_type VARCHAR(16) NOT NULL, application_id BIGINT NOT NULL, bpm_round VARCHAR(64) NULL,
 attribute_round INT NOT NULL,
 UNIQUE(tenant_id,application_type,application_id,bpm_round),
 UNIQUE(tenant_id,application_type,application_id,attribute_round)
);
CREATE TABLE IF NOT EXISTS dcc_project_file_reference (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, tenant_id BIGINT NOT NULL, project_id BIGINT NOT NULL,
 folder_id BIGINT NOT NULL, master_id BIGINT NOT NULL, selected_controlled_file_id BIGINT NOT NULL,
 created_by BIGINT NOT NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(tenant_id,project_id,folder_id,master_id)
);
