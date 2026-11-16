-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=none; type=schema; riskLevel=low
-- Append-only tenant catalog; existing route/production snapshots are not rewritten.
SET NAMES utf8mb4;
CREATE TABLE IF NOT EXISTS mes_pro_loss_reason_sequence (
  tenant_id BIGINT NOT NULL PRIMARY KEY,
  next_number BIGINT NOT NULL DEFAULT 1
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS mes_pro_loss_reason (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  reason_number BIGINT NOT NULL,
  reason_code VARCHAR(40) NOT NULL,
  reason_name VARCHAR(128) NOT NULL,
  creator VARCHAR(64) NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_loss_number (tenant_id, reason_number),
  UNIQUE KEY uk_loss_code (tenant_id, reason_code),
  UNIQUE KEY uk_loss_name (tenant_id, reason_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;
