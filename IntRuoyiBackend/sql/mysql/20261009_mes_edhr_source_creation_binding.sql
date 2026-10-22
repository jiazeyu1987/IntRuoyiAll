-- DRAFT ONLY: source candidate; no migration execution or production activation authorized.
-- Append-only historical source binding. No BaseDO, deletion flag or mutable pointer.
CREATE TABLE mes_edhr_source_creation_binding (
  id bigint NOT NULL AUTO_INCREMENT,
  tenant_id bigint NOT NULL,
  owner_protocol varchar(64) NOT NULL,
  source_row_id bigint NOT NULL,
  source_receipt_id varchar(128) NOT NULL,
  source_version varchar(64) NOT NULL,
  source_hash char(64) NOT NULL,
  request_hash_version varchar(32) NOT NULL,
  request_hash char(64) NOT NULL,
  creation_event_id bigint NOT NULL,
  creation_event_hash char(64) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_mes_edhr_creation_row (tenant_id,owner_protocol,source_row_id),
  UNIQUE KEY uk_mes_edhr_creation_receipt (tenant_id,owner_protocol,source_receipt_id,source_version),
  UNIQUE KEY uk_mes_edhr_creation_event (tenant_id,creation_event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
