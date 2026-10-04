-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260903_dcc_controlled_file_related_file,20260906_dcc_new_file_lifecycle_p4; type=schema; riskLevel=medium
-- D-owned tables only. No historical Master/Version writes and no automatic reference version switching.
CREATE TABLE IF NOT EXISTS dcc_project_file_reference (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 tenant_id BIGINT NOT NULL, project_id BIGINT NOT NULL, folder_id BIGINT NOT NULL,
 master_id BIGINT NOT NULL, selected_controlled_file_id BIGINT NOT NULL,
 created_by BIGINT NOT NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE (tenant_id, project_id, folder_id, master_id),
 KEY idx_dcc_reference_master (tenant_id, master_id, project_id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS dcc_current_file_relation (
 tenant_id BIGINT NOT NULL, source_master_id BIGINT NOT NULL, related_master_id BIGINT NOT NULL,
 PRIMARY KEY (tenant_id, source_master_id, related_master_id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS dcc_current_file_relation_set (
 tenant_id BIGINT NOT NULL, source_master_id BIGINT NOT NULL, controlled_file_id BIGINT NOT NULL,
 row_version BIGINT NOT NULL DEFAULT 0,
 PRIMARY KEY (tenant_id, source_master_id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS dcc_relation_change_command (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 tenant_id BIGINT NOT NULL, source_master_id BIGINT NOT NULL, source_file_id BIGINT NOT NULL,
 actor_id BIGINT NOT NULL, idempotency_key VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
 payload_hash CHAR(64) NOT NULL, resulting_version BIGINT NOT NULL,
 related_master_ids VARCHAR(8192) NOT NULL,
 UNIQUE (tenant_id, source_master_id, idempotency_key)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS dcc_relation_arrangement (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 tenant_id BIGINT NOT NULL, source_file_id BIGINT NOT NULL, application_round VARCHAR(64) NOT NULL,
 related_master_id BIGINT NOT NULL, assignee_user_id BIGINT NOT NULL, due_at DATETIME NOT NULL,
 arranged_by BIGINT NOT NULL,
 UNIQUE (tenant_id, source_file_id, application_round, related_master_id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS dcc_relation_controlled_event (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 tenant_id BIGINT NOT NULL, master_id BIGINT NOT NULL, controlled_file_id BIGINT NOT NULL,
 application_round VARCHAR(64) NOT NULL, event_key VARCHAR(128) NOT NULL, controlled_at DATETIME NOT NULL,
 UNIQUE (tenant_id, controlled_file_id, application_round), UNIQUE (tenant_id, event_key)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS dcc_relation_remediation_task (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 tenant_id BIGINT NOT NULL, event_id BIGINT NOT NULL, related_master_id BIGINT NOT NULL,
 assignee_user_id BIGINT NOT NULL, due_at DATETIME NOT NULL, status VARCHAR(16) NOT NULL,
 UNIQUE (tenant_id, event_id, related_master_id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS dcc_relation_notification_outbox (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 tenant_id BIGINT NOT NULL, event_id BIGINT NOT NULL, related_master_id BIGINT NOT NULL,
 recipient_user_id BIGINT NOT NULL, source_file_id BIGINT NOT NULL, due_at DATETIME NOT NULL,
 business_key VARCHAR(192) NOT NULL, status VARCHAR(16) NOT NULL,
 file_number_snapshot VARCHAR(128) NOT NULL, version_no_snapshot VARCHAR(64) NOT NULL,
 platform_message_id BIGINT NULL, failure_code VARCHAR(64) NULL,
 UNIQUE (tenant_id, business_key), UNIQUE (tenant_id, event_id, related_master_id)
) ENGINE=InnoDB;
