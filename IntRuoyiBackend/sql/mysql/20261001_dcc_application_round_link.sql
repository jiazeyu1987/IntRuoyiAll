-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260930_dcc_b_project_attributes_folders; type=schema; riskLevel=medium
-- Explicit identity mapping only. No historical application or signature backfill.
CREATE TABLE IF NOT EXISTS dcc_application_round_link (
 id BIGINT NOT NULL AUTO_INCREMENT,
 tenant_id BIGINT NOT NULL,
 project_id BIGINT NOT NULL,
 application_type VARCHAR(16) COLLATE utf8mb4_bin NOT NULL,
 application_id BIGINT NOT NULL,
 bpm_round VARCHAR(64) COLLATE utf8mb4_bin NOT NULL,
 attribute_round INT NOT NULL,
 PRIMARY KEY(id),
 UNIQUE KEY uk_dcc_round_bpm(tenant_id,application_type,application_id,bpm_round),
 UNIQUE KEY uk_dcc_round_number(tenant_id,application_type,application_id,attribute_round)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
