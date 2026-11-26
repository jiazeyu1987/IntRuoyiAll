-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium; requiresTargetPreflight=true
CREATE TABLE mes_pro_replan_request (
 tenant_id BIGINT NOT NULL,
 operator_id BIGINT NOT NULL,
 request_key VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 payload_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 result_json LONGTEXT NULL,
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY (tenant_id, operator_id, request_key)
) ENGINE=InnoDB COMMENT='Transactional manual replan idempotency receipts';
