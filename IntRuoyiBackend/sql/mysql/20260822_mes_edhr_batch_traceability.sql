-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260608_edhr_batch_execution_schema; type=schema; riskLevel=high
-- Flow 7 immutable batch origin, trace links and manifest.
-- This migration only stores formal upstream IDs/snapshots; it never infers or creates them.

DROP PROCEDURE IF EXISTS ensure_mes_edhr_batch_traceability_schema;
DELIMITER $$
CREATE PROCEDURE ensure_mes_edhr_batch_traceability_schema()
BEGIN
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.TABLES
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'mes_pro_edhr_batch_execution'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing mes_pro_edhr_batch_execution; apply eDHR batch execution schema first';
  END IF;

  CREATE TABLE IF NOT EXISTS mes_pro_edhr_batch_execution_origin (
    id bigint NOT NULL AUTO_INCREMENT,
    batch_execution_id bigint NOT NULL,
    entry_type varchar(40) NOT NULL,
    origin_key varchar(180) NOT NULL,
    active_order_id bigint DEFAULT NULL,
    work_order_id bigint DEFAULT NULL,
    completion_transaction_id bigint DEFAULT NULL,
    completion_version int DEFAULT NULL,
    completion_backfill_receipt_id bigint DEFAULT NULL,
    completion_backfill_receipt_hash char(64) DEFAULT NULL,
    pick_list_binding_id bigint DEFAULT NULL,
    pick_list_id bigint DEFAULT NULL,
    pick_list_binding_version int DEFAULT NULL,
    has_actual_loss bit(1) DEFAULT NULL,
    source_snapshot_hash char(64) NOT NULL,
    batch_provision_receipt_id bigint NOT NULL,
    batch_provision_status varchar(16) NOT NULL,
    source_credential_id varchar(128) DEFAULT NULL,
    source_credential_hash char(64) DEFAULT NULL,
    source_bundle_hash char(64) NOT NULL,
    idempotency_key varchar(180) NOT NULL,
    relation_status varchar(32) NOT NULL,
    relation_reason varchar(500) DEFAULT NULL,
    captured_by bigint DEFAULT NULL,
    captured_at datetime NOT NULL,
    creator varchar(64) DEFAULT '',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted bit(1) NOT NULL DEFAULT b'0',
    tenant_id bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_mes_edhr_trace_origin_key (tenant_id, batch_execution_id, origin_key, deleted),
    UNIQUE KEY uk_mes_edhr_trace_origin_idempotency (tenant_id, idempotency_key, deleted),
    UNIQUE KEY uk_mes_edhr_trace_active_completion (tenant_id, active_order_id, completion_transaction_id, deleted),
    KEY idx_mes_edhr_trace_origin_active_order (tenant_id, active_order_id, deleted),
    KEY idx_mes_edhr_trace_origin_work_order (tenant_id, work_order_id, deleted)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES eDHR batch immutable origin';

  IF NOT EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'mes_pro_edhr_batch_execution_origin'
        AND COLUMN_NAME = 'has_actual_loss'
  ) THEN
    ALTER TABLE mes_pro_edhr_batch_execution_origin
      ADD COLUMN has_actual_loss bit(1) DEFAULT NULL AFTER pick_list_binding_version;
  END IF;

  IF EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'mes_pro_edhr_batch_execution_origin'
        AND COLUMN_NAME = 'source_credential_id'
  ) THEN
    ALTER TABLE mes_pro_edhr_batch_execution_origin
      MODIFY COLUMN source_credential_id varchar(128) DEFAULT NULL;
  END IF;

  CREATE TABLE IF NOT EXISTS mes_pro_edhr_batch_execution_trace_link (
    id bigint NOT NULL AUTO_INCREMENT,
    batch_execution_id bigint NOT NULL,
    origin_id bigint NOT NULL,
    link_type varchar(48) NOT NULL,
    source_object_type varchar(64) NOT NULL,
    source_object_id bigint DEFAULT NULL,
    source_line_id bigint DEFAULT NULL,
    source_event_id bigint DEFAULT NULL,
    source_version int DEFAULT NULL,
    source_identity_key varchar(240) NOT NULL,
    idempotency_key varchar(180) DEFAULT NULL,
    snapshot_json longtext NOT NULL,
    snapshot_hash char(64) NOT NULL,
    relation_status varchar(32) NOT NULL,
    relation_reason varchar(500) DEFAULT NULL,
    captured_by bigint DEFAULT NULL,
    captured_at datetime NOT NULL,
    creator varchar(64) DEFAULT '',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted bit(1) NOT NULL DEFAULT b'0',
    tenant_id bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_mes_edhr_trace_link_identity (tenant_id, batch_execution_id, source_identity_key, deleted),
    KEY idx_mes_edhr_trace_link_source (tenant_id, source_object_type, source_object_id, source_line_id, deleted),
    KEY idx_mes_edhr_trace_link_batch (tenant_id, batch_execution_id, link_type, deleted)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES eDHR batch immutable trace link';

  CREATE TABLE IF NOT EXISTS mes_pro_edhr_batch_execution_trace_manifest (
    id bigint NOT NULL AUTO_INCREMENT,
    batch_execution_id bigint NOT NULL,
    manifest_version int NOT NULL,
    previous_manifest_hash char(64) DEFAULT NULL,
    manifest_json longtext NOT NULL,
    manifest_hash char(64) NOT NULL,
    seal_reason varchar(500) NOT NULL,
    sealed_by bigint DEFAULT NULL,
    sealed_at datetime NOT NULL,
    creator varchar(64) DEFAULT '',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted bit(1) NOT NULL DEFAULT b'0',
    tenant_id bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_mes_edhr_trace_manifest_version (tenant_id, batch_execution_id, manifest_version, deleted),
    UNIQUE KEY uk_mes_edhr_trace_manifest_hash (tenant_id, batch_execution_id, manifest_hash, deleted),
    KEY idx_mes_edhr_trace_manifest_batch (tenant_id, batch_execution_id, manifest_version)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES eDHR batch immutable trace manifest';

  CREATE TABLE IF NOT EXISTS mes_pro_edhr_batch_trace_outbox_event (
    id bigint NOT NULL AUTO_INCREMENT,
    event_id varchar(180) NOT NULL,
    idempotency_key varchar(180) NOT NULL,
    batch_execution_id bigint NOT NULL,
    origin_id bigint DEFAULT NULL,
    origin_link_id bigint DEFAULT NULL,
    event_type varchar(80) NOT NULL,
    mapping_status varchar(40) NOT NULL,
    error_code varchar(80) DEFAULT NULL,
    reason varchar(500) DEFAULT NULL,
    trace_link_hash char(64) DEFAULT NULL,
    source_snapshot_hash char(64) DEFAULT NULL,
    source_bundle_hash char(64) DEFAULT NULL,
    manifest_version int DEFAULT NULL,
    payload_json longtext NOT NULL,
    payload_hash char(64) NOT NULL,
    retryable bit(1) NOT NULL DEFAULT b'0',
    occurred_at datetime NOT NULL,
    creator varchar(64) DEFAULT '',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted bit(1) NOT NULL DEFAULT b'0',
    tenant_id bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_mes_edhr_trace_outbox_event (tenant_id, event_id, deleted),
    UNIQUE KEY uk_mes_edhr_trace_outbox_idempotency (tenant_id, idempotency_key, deleted),
    KEY idx_mes_edhr_trace_outbox_batch (tenant_id, batch_execution_id, occurred_at),
    KEY idx_mes_edhr_trace_outbox_status (tenant_id, mapping_status, retryable, occurred_at)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES eDHR Flow 7 Tx-C outbox';
END$$
DELIMITER ;
CALL ensure_mes_edhr_batch_traceability_schema();
DROP PROCEDURE IF EXISTS ensure_mes_edhr_batch_traceability_schema;

DROP TRIGGER IF EXISTS trg_mes_edhr_trace_origin_no_update;
DROP TRIGGER IF EXISTS trg_mes_edhr_trace_origin_no_delete;
DROP TRIGGER IF EXISTS trg_mes_edhr_trace_link_no_update;
DROP TRIGGER IF EXISTS trg_mes_edhr_trace_link_no_delete;
DROP TRIGGER IF EXISTS trg_mes_edhr_trace_manifest_no_update;
DROP TRIGGER IF EXISTS trg_mes_edhr_trace_manifest_no_delete;
DROP TRIGGER IF EXISTS trg_mes_edhr_trace_outbox_no_update;
DROP TRIGGER IF EXISTS trg_mes_edhr_trace_outbox_no_delete;
DELIMITER $$
CREATE TRIGGER trg_mes_edhr_trace_origin_no_update
BEFORE UPDATE ON mes_pro_edhr_batch_execution_origin
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'MES eDHR batch origins are append-only';
END$$
CREATE TRIGGER trg_mes_edhr_trace_origin_no_delete
BEFORE DELETE ON mes_pro_edhr_batch_execution_origin
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'MES eDHR batch origins are append-only';
END$$
CREATE TRIGGER trg_mes_edhr_trace_link_no_update
BEFORE UPDATE ON mes_pro_edhr_batch_execution_trace_link
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'MES eDHR batch trace links are append-only';
END$$
CREATE TRIGGER trg_mes_edhr_trace_link_no_delete
BEFORE DELETE ON mes_pro_edhr_batch_execution_trace_link
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'MES eDHR batch trace links are append-only';
END$$
CREATE TRIGGER trg_mes_edhr_trace_manifest_no_update
BEFORE UPDATE ON mes_pro_edhr_batch_execution_trace_manifest
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'MES eDHR batch trace manifests are append-only';
END$$
CREATE TRIGGER trg_mes_edhr_trace_manifest_no_delete
BEFORE DELETE ON mes_pro_edhr_batch_execution_trace_manifest
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'MES eDHR batch trace manifests are append-only';
END$$
CREATE TRIGGER trg_mes_edhr_trace_outbox_no_update
BEFORE UPDATE ON mes_pro_edhr_batch_trace_outbox_event
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'MES eDHR batch trace outbox is append-only';
END$$
CREATE TRIGGER trg_mes_edhr_trace_outbox_no_delete
BEFORE DELETE ON mes_pro_edhr_batch_trace_outbox_event
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'MES eDHR batch trace outbox is append-only';
END$$
DELIMITER ;
