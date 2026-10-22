-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260922_dcc_task_assignee_snapshot; type=schema; riskLevel=medium
-- IC-1: additive columns only; no historical Master/Version backfill; no default timezone/lead days.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS dcc_a_ic1_schema;
DELIMITER $$
CREATE PROCEDURE dcc_a_ic1_schema()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_controlled_file_master' AND COLUMN_NAME='latest_controlled_file_id') THEN
    ALTER TABLE dcc_controlled_file_master ADD COLUMN latest_controlled_file_id BIGINT NULL;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_controlled_file' AND COLUMN_NAME='controlled_time') THEN
    ALTER TABLE dcc_controlled_file ADD COLUMN controlled_time DATETIME NULL;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_controlled_file' AND COLUMN_NAME='activated_time') THEN
    ALTER TABLE dcc_controlled_file ADD COLUMN activated_time DATETIME NULL;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_controlled_file' AND COLUMN_NAME='distributed_time') THEN
    ALTER TABLE dcc_controlled_file ADD COLUMN distributed_time DATETIME NULL;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_controlled_file_task_assignee_snapshot' AND COLUMN_NAME='leader_user_id') THEN
    ALTER TABLE dcc_controlled_file_task_assignee_snapshot ADD COLUMN leader_user_id BIGINT NULL;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_controlled_file_task_assignee_snapshot' AND COLUMN_NAME='process_instance_id') THEN
    ALTER TABLE dcc_controlled_file_task_assignee_snapshot ADD COLUMN process_instance_id VARCHAR(128) NULL;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_controlled_file_task_assignee_snapshot' AND COLUMN_NAME='assignment_signature_id') THEN
    ALTER TABLE dcc_controlled_file_task_assignee_snapshot ADD COLUMN assignment_signature_id BIGINT NULL;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_controlled_file_task_assignee_snapshot' AND COLUMN_NAME='assigned_time') THEN
    ALTER TABLE dcc_controlled_file_task_assignee_snapshot ADD COLUMN assigned_time DATETIME NULL;
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_controlled_file_task_assignee_snapshot' AND INDEX_NAME='uk_dcc_task_assignee_obligation' AND COLUMN_NAME='process_instance_id') THEN
    DO 0;
  ELSE
    ALTER TABLE dcc_controlled_file_task_assignee_snapshot DROP INDEX uk_dcc_task_assignee_obligation, ADD UNIQUE KEY uk_dcc_task_assignee_obligation(tenant_id,controlled_file_id,process_instance_id,stage_code,department_id,deleted);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_controlled_file' AND COLUMN_NAME='distribution_payload_hash') THEN
    ALTER TABLE dcc_controlled_file ADD COLUMN distribution_payload_hash CHAR(64) NULL;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_controlled_file_task_assignee_snapshot' AND COLUMN_NAME='assignment_payload_hash') THEN
    ALTER TABLE dcc_controlled_file_task_assignee_snapshot ADD COLUMN assignment_payload_hash CHAR(64) NULL;
  END IF;
  ALTER TABLE dcc_controlled_file_obsolete_audit MODIFY COLUMN operator_id BIGINT NULL;
END$$
DELIMITER ;
CALL dcc_a_ic1_schema();
DROP PROCEDURE dcc_a_ic1_schema;
CREATE TABLE IF NOT EXISTS dcc_workflow_lifecycle_event (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 tenant_id BIGINT NOT NULL, event_key VARCHAR(255) NOT NULL,
 event_type VARCHAR(32) NOT NULL, master_id BIGINT NOT NULL, controlled_file_id BIGINT NOT NULL,
 previous_active_file_id BIGINT NULL, version_no VARCHAR(64) NOT NULL,
 approval_process_instance_id VARCHAR(128) NOT NULL, occurred_at DATETIME NOT NULL,
 delivery_status VARCHAR(32) NOT NULL,
 UNIQUE KEY uk_dcc_a_lifecycle_event(tenant_id,event_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- operator_id NULL is the explicit system-activation actor; no human signature is invented.
-- Consumers retain PENDING until an actual, idempotent D owner delivery succeeds.

CREATE TABLE IF NOT EXISTS dcc_workflow_obsolete_archive (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 tenant_id BIGINT NOT NULL, controlled_file_id BIGINT NOT NULL,
 obsolete_time DATETIME NOT NULL, status VARCHAR(32) NOT NULL,
 UNIQUE(tenant_id,controlled_file_id)
);
