-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260513_dcc_base_schema; type=schema; riskLevel=medium
-- Persist DCC submit idempotency evidence and reject duplicate keys per tenant/submitter.

ALTER TABLE `dcc_controlled_file`
  ADD COLUMN `submit_idempotency_key` VARCHAR(128) NULL AFTER `process_definition_key`,
  ADD COLUMN `submit_payload_hash` CHAR(64) NULL AFTER `submit_idempotency_key`,
  ADD UNIQUE KEY `uk_dcc_file_submit_idempotency`
    (`tenant_id`, `submitter_id`, `submit_idempotency_key`, `deleted`);
