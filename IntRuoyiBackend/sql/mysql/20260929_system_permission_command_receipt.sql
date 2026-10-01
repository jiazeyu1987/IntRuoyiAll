-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260924_gxp_audit_event_v2; type=schema; riskLevel=high
-- F01 isolated protocol storage, NOT approval to issue V2 commands or activate a policy.
-- Additive only: no event backfill, no inferred legacy receipts, no business data changes.
-- Application identity must have SELECT/INSERT only on this table; grants are deployment-owned.
CREATE TABLE IF NOT EXISTS `system_gxp_command_receipt` (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  `operation_id` varchar(96) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `subject_id` varchar(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `source_key` varbinary(1024) NOT NULL,
  `source_key_sha256` binary(32) NOT NULL,
  `source_payload_hash` binary(32) NOT NULL,
  `identity_json` json NOT NULL,
  `source_json` json NOT NULL,
  `audit_event_id` bigint NOT NULL,
  `audit_event_hash` binary(32) NOT NULL,
  `result_json` json NOT NULL,
  `created_at_utc` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_permission_receipt_source` (`tenant_id`,`operation_id`,`source_key_sha256`),
  UNIQUE KEY `uk_permission_receipt_event` (`tenant_id`,`audit_event_id`),
  KEY `idx_permission_receipt_subject` (`tenant_id`,`subject_id`,`id`)
) ENGINE=InnoDB;

-- No destructive down migration: retain successful facts, roll forward after schema verification.
-- Dedicated MySQL first/repeat/shape validation is a separate gate; this file has not been executed.
