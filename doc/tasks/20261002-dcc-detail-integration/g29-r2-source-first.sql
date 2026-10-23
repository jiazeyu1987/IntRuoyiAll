SELECT JSON_OBJECT('g28Phase','SESSION_IDENTITY','database',DATABASE(),'serverUuid',@@server_uuid,'mysqlVersion',VERSION(),'pageSize',@@innodb_page_size,'rowFormat',@@innodb_default_row_format,'intendedDatabase','ruoyi-vue-pro');
-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260930_dcc_c_revision_identity,20260811_dcc_source_ownership; type=schema; riskLevel=medium
-- Standalone forward migration: empty additive sidecars only. No historical DML, no audit approval or seed.
-- Complete raw UTF-8 keys preserve case, extension and trailing spaces. 8+1024 bytes fit InnoDB 3072.
SET NAMES utf8mb4;
CREATE TABLE IF NOT EXISTS dcc_legacy_source_name_scope (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, tenant_id BIGINT NOT NULL, scope_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 manifest_sha256 CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL, facts_sha256 CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL, bytes_receipt_sha256 CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 user_decision_sha256 CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL, scope_identity_sha256 CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 status VARCHAR(16) NOT NULL, claim_count INT NOT NULL, version_count INT NOT NULL,
 source_count INT NOT NULL, edge_count INT NOT NULL, name_count INT NOT NULL,
 verified_at DATETIME(6) NOT NULL, activated_at DATETIME(6), actor_id BIGINT NOT NULL,
 reason VARCHAR(2000) NOT NULL, request_id VARCHAR(128) NOT NULL,
 UNIQUE KEY uk_dcc_legacy_scope_id (tenant_id, scope_id), UNIQUE KEY uk_dcc_legacy_scope_manifest (tenant_id, manifest_sha256),
 CHECK (status IN ('PREPARED','VERIFIED','INVALID')),
 CHECK (claim_count>0 AND version_count>0 AND source_count>0 AND edge_count>0 AND name_count>0),
 CHECK (manifest_sha256 REGEXP '^[0-9a-f]{64}$' AND facts_sha256 REGEXP '^[0-9a-f]{64}$' AND bytes_receipt_sha256 REGEXP '^[0-9a-f]{64}$' AND user_decision_sha256 REGEXP '^[0-9a-f]{64}$' AND scope_identity_sha256 REGEXP '^[0-9a-f]{64}$')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
CREATE TABLE IF NOT EXISTS dcc_legacy_source_name_evidence (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, tenant_id BIGINT NOT NULL, verification_scope_id BIGINT NOT NULL,
 legacy_claim_id BIGINT NOT NULL, legacy_master_id BIGINT NOT NULL, controlled_file_id BIGINT NOT NULL,
 source_file_id BIGINT NOT NULL, config_id BIGINT NOT NULL, source_path VARCHAR(1024) NOT NULL, storage_type INT NOT NULL, storage_endpoint VARCHAR(1024) NOT NULL, storage_bucket VARCHAR(256) NOT NULL, storage_region VARCHAR(256), storage_path_style TINYINT NOT NULL, version_no VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 source_original_file_name VARCHAR(256) NOT NULL, source_name_key VARBINARY(1024) GENERATED ALWAYS AS (CONVERT(source_original_file_name USING BINARY)) STORED,
 expected_sha256 CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL, actual_sha256 CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 expected_size BIGINT NOT NULL, actual_size BIGINT NOT NULL, bytes_status VARCHAR(16) NOT NULL,
 expected_version_count INT NOT NULL, claim_normalized_name VARCHAR(256) NOT NULL,
 claim_project_id BIGINT, claim_leaf_id BIGINT, claim_number VARCHAR(128),
 master_project_id BIGINT, master_leaf_id BIGINT, master_number VARCHAR(128),
 metadata_identity_sha256 CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL, preimage_sha256 CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL, proof_row_sha256 CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 obsolete_time DATETIME(6), retain_until DATETIME(6), released_time DATETIME(6),
 UNIQUE KEY uk_dcc_legacy_claim_version (tenant_id, verification_scope_id, legacy_claim_id, controlled_file_id),
 UNIQUE KEY uk_dcc_legacy_scope_version (tenant_id, verification_scope_id, controlled_file_id),
 KEY idx_dcc_legacy_name_owner (tenant_id,source_name_key,legacy_master_id),
 CHECK (bytes_status='MATCH' AND expected_sha256=actual_sha256 AND expected_size=actual_size AND expected_size>=0 AND expected_version_count>0),
 CHECK (expected_sha256 REGEXP '^[0-9a-f]{64}$' AND actual_sha256 REGEXP '^[0-9a-f]{64}$' AND metadata_identity_sha256 REGEXP '^[0-9a-f]{64}$' AND preimage_sha256 REGEXP '^[0-9a-f]{64}$' AND proof_row_sha256 REGEXP '^[0-9a-f]{64}$')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS dcc_source_name_reservation (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, tenant_id BIGINT NOT NULL,
 source_original_file_name VARCHAR(256) NOT NULL, source_name_key VARBINARY(1024) GENERATED ALWAYS AS (CONVERT(source_original_file_name USING BINARY)) STORED,
 reservation_kind VARCHAR(16) NOT NULL, verification_scope_id BIGINT, modern_claim_id BIGINT, modern_master_id BIGINT,
 generation BIGINT NOT NULL, active TINYINT NOT NULL, actor_id BIGINT, reason VARCHAR(2000),
 create_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), update_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 released_time DATETIME(6),
 UNIQUE KEY uk_dcc_source_name_registry (tenant_id, source_name_key),
 CHECK (generation>0 AND active IN (0,1)),
 CHECK ((reservation_kind='LEGACY_GROUP' AND active=1 AND verification_scope_id IS NOT NULL AND modern_claim_id IS NULL AND modern_master_id IS NULL) OR (reservation_kind='MODERN' AND active=1 AND verification_scope_id IS NULL AND modern_claim_id IS NOT NULL AND modern_master_id IS NOT NULL) OR (reservation_kind='NONE' AND active=0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

INSERT INTO infra_release_migration(release_tag,migration_id,file_name,sha256,target_environment,status,started_at,finished_at,operation_id,creator,updater,deleted,tenant_id) VALUES('dcc-g28-local-new-sidecars','20261003_dcc_legacy_source_name_occupancy','sql/mysql/20261003_dcc_legacy_source_name_occupancy.sql','621fe041064ac07c5bfe995abd556c3f9436b0253c883a3b7d2b33014e68de2a','test','APPLIED',UTC_TIMESTAMP(),UTC_TIMESTAMP(),'dcc-g28-local-new-sidecars','dcc-g28-local-task','dcc-g28-local-task',b'0',0);
SELECT JSON_OBJECT('g28Phase','FIRST_COMPLETE','migrationId','20261003_dcc_legacy_source_name_occupancy');
