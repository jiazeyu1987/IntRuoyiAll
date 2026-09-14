-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260822_mes_edhr_batch_traceability; type=schema; riskLevel=medium
-- Flow 6 durable provisioning state. Flow 7 may advance this record only after Tx-C mapping succeeds.
-- Allowed provisioning states: BATCH_PROVISIONING, BATCH_PROVISIONING_RETRYABLE,
-- BATCH_PROVISIONING_BLOCKED, BATCH_READY.
DROP PROCEDURE IF EXISTS ensure_mes_edhr_batch_provisioning_status;
DELIMITER $$
CREATE PROCEDURE ensure_mes_edhr_batch_provisioning_status()
BEGIN
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
       WHERE TABLE_SCHEMA = DATABASE()
         AND TABLE_NAME = 'mes_pro_edhr_batch_execution'
         AND COLUMN_NAME = 'provisioning_status'
  ) THEN
    ALTER TABLE mes_pro_edhr_batch_execution
      ADD COLUMN provisioning_status varchar(48) DEFAULT NULL COMMENT 'Flow 6/7 建批交接状态'
      AFTER status;
  END IF;
END$$
DELIMITER ;
CALL ensure_mes_edhr_batch_provisioning_status();
DROP PROCEDURE IF EXISTS ensure_mes_edhr_batch_provisioning_status;

CREATE TABLE IF NOT EXISTS mes_pro_edhr_batch_provisioning_record (
  id bigint NOT NULL AUTO_INCREMENT,
  tenant_id bigint NOT NULL DEFAULT 0,
  batch_execution_id bigint NOT NULL,
  entry_type varchar(48) NOT NULL,
  entry_business_id varchar(180) NOT NULL,
  source_credential_id varchar(128) NOT NULL,
  source_credential_hash char(64) DEFAULT NULL,
  source_snapshot_hash char(64) NOT NULL,
  source_bundle_hash char(64) NOT NULL,
  source_version varchar(64) NOT NULL,
  idempotency_key varchar(180) NOT NULL,
  status varchar(48) NOT NULL,
  error_code varchar(96) DEFAULT NULL,
  attempt_count int NOT NULL DEFAULT 1,
  mapping_event_id varchar(180) DEFAULT NULL,
  mapping_idempotency_key varchar(180) DEFAULT NULL,
  creator varchar(64) DEFAULT '',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater varchar(64) DEFAULT '',
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (id),
  UNIQUE KEY uk_mes_edhr_batch_provisioning_batch (tenant_id, batch_execution_id, deleted),
  UNIQUE KEY uk_mes_edhr_batch_provisioning_idempotency (tenant_id, idempotency_key, deleted),
  KEY idx_mes_edhr_batch_provisioning_status (tenant_id, status, update_time),
  KEY idx_mes_edhr_batch_provisioning_source (tenant_id, source_credential_id, source_snapshot_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES Flow 6 batch provisioning state';
