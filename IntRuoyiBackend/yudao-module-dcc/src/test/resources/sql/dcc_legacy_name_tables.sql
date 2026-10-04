CREATE TABLE IF NOT EXISTS dcc_legacy_source_name_scope (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, tenant_id BIGINT NOT NULL, scope_id VARCHAR(64) NOT NULL,
 manifest_sha256 CHAR(64) NOT NULL, facts_sha256 CHAR(64) NOT NULL, bytes_receipt_sha256 CHAR(64) NOT NULL,
 user_decision_sha256 CHAR(64) NOT NULL, scope_identity_sha256 CHAR(64) NOT NULL,
 status VARCHAR(16) NOT NULL, claim_count INT NOT NULL, version_count INT NOT NULL,
 source_count INT NOT NULL, edge_count INT NOT NULL, name_count INT NOT NULL,
 verified_at DATETIME(6) NOT NULL, activated_at DATETIME(6), actor_id BIGINT NOT NULL,
 reason VARCHAR(2000) NOT NULL, request_id VARCHAR(128) NOT NULL,
 UNIQUE (tenant_id, scope_id), UNIQUE (tenant_id, manifest_sha256)
);
CREATE TABLE IF NOT EXISTS dcc_legacy_source_name_evidence (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, tenant_id BIGINT NOT NULL, verification_scope_id BIGINT NOT NULL,
 legacy_claim_id BIGINT NOT NULL, legacy_master_id BIGINT NOT NULL, controlled_file_id BIGINT NOT NULL,
 source_file_id BIGINT NOT NULL, config_id BIGINT NOT NULL, source_path VARCHAR(1024) NOT NULL, storage_type INT NOT NULL, storage_endpoint VARCHAR(1024) NOT NULL, storage_bucket VARCHAR(256) NOT NULL, storage_region VARCHAR(256), storage_path_style TINYINT NOT NULL, version_no VARCHAR(64) NOT NULL,
 source_original_file_name VARCHAR(256) NOT NULL, source_name_key VARBINARY(1024) AS (CAST(source_original_file_name AS VARBINARY)),
 expected_sha256 CHAR(64) NOT NULL, actual_sha256 CHAR(64) NOT NULL,
 expected_size BIGINT NOT NULL, actual_size BIGINT NOT NULL, bytes_status VARCHAR(16) NOT NULL,
 expected_version_count INT NOT NULL, claim_normalized_name VARCHAR(256) NOT NULL,
 claim_project_id BIGINT, claim_leaf_id BIGINT, claim_number VARCHAR(128),
 master_project_id BIGINT, master_leaf_id BIGINT, master_number VARCHAR(128),
 metadata_identity_sha256 CHAR(64) NOT NULL, preimage_sha256 CHAR(64) NOT NULL, proof_row_sha256 CHAR(64) NOT NULL,
 obsolete_time DATETIME(6), retain_until DATETIME(6), released_time DATETIME(6),
 UNIQUE (tenant_id, verification_scope_id, legacy_claim_id, controlled_file_id),
 UNIQUE (tenant_id, verification_scope_id, controlled_file_id)
);
CREATE INDEX IF NOT EXISTS idx_dcc_legacy_name_owner ON dcc_legacy_source_name_evidence(tenant_id,source_name_key,legacy_master_id);
CREATE TABLE IF NOT EXISTS dcc_source_name_reservation (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, tenant_id BIGINT NOT NULL,
 source_original_file_name VARCHAR(256) NOT NULL, source_name_key VARBINARY(1024) AS (CAST(source_original_file_name AS VARBINARY)),
 reservation_kind VARCHAR(16) NOT NULL, verification_scope_id BIGINT, modern_claim_id BIGINT, modern_master_id BIGINT,
 generation BIGINT NOT NULL, active TINYINT NOT NULL, actor_id BIGINT, reason VARCHAR(2000),
 create_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), update_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 released_time DATETIME(6),
 UNIQUE (tenant_id, source_name_key)
);
