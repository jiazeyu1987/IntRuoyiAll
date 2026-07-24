-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260618_mes_edhr_release_precheck_engine; type=schema; riskLevel=medium
-- Repair existing eDHR release transaction tables that were created before lifecycle columns were applied.
-- This migration only adds nullable lifecycle columns required by MesProEdhrReleaseTransactionDO.

DROP PROCEDURE IF EXISTS ensure_mes_edhr_release_transaction_lifecycle_columns;
DELIMITER $$
CREATE PROCEDURE ensure_mes_edhr_release_transaction_lifecycle_columns()
BEGIN
  IF NOT EXISTS (
      SELECT 1 FROM `information_schema`.`COLUMNS`
      WHERE `TABLE_SCHEMA` = DATABASE()
        AND `TABLE_NAME` = 'mes_pro_edhr_release_transaction'
        AND `COLUMN_NAME` = 'submit_idempotency_key'
  ) THEN
    ALTER TABLE `mes_pro_edhr_release_transaction`
      ADD COLUMN `submit_idempotency_key` varchar(128) DEFAULT NULL COMMENT '提交幂等键' AFTER `precheck_snapshot_json`;
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM `information_schema`.`COLUMNS`
      WHERE `TABLE_SCHEMA` = DATABASE()
        AND `TABLE_NAME` = 'mes_pro_edhr_release_transaction'
        AND `COLUMN_NAME` = 'submitted_by'
  ) THEN
    ALTER TABLE `mes_pro_edhr_release_transaction`
      ADD COLUMN `submitted_by` bigint DEFAULT NULL COMMENT '提交人' AFTER `submit_idempotency_key`;
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM `information_schema`.`COLUMNS`
      WHERE `TABLE_SCHEMA` = DATABASE()
        AND `TABLE_NAME` = 'mes_pro_edhr_release_transaction'
        AND `COLUMN_NAME` = 'submitted_at'
  ) THEN
    ALTER TABLE `mes_pro_edhr_release_transaction`
      ADD COLUMN `submitted_at` datetime DEFAULT NULL COMMENT '提交时间' AFTER `submitted_by`;
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM `information_schema`.`COLUMNS`
      WHERE `TABLE_SCHEMA` = DATABASE()
        AND `TABLE_NAME` = 'mes_pro_edhr_release_transaction'
        AND `COLUMN_NAME` = 'approval_idempotency_key'
  ) THEN
    ALTER TABLE `mes_pro_edhr_release_transaction`
      ADD COLUMN `approval_idempotency_key` varchar(128) DEFAULT NULL COMMENT '批准幂等键' AFTER `submitted_at`;
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM `information_schema`.`COLUMNS`
      WHERE `TABLE_SCHEMA` = DATABASE()
        AND `TABLE_NAME` = 'mes_pro_edhr_release_transaction'
        AND `COLUMN_NAME` = 'approved_by'
  ) THEN
    ALTER TABLE `mes_pro_edhr_release_transaction`
      ADD COLUMN `approved_by` bigint DEFAULT NULL COMMENT '批准人' AFTER `approval_idempotency_key`;
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM `information_schema`.`COLUMNS`
      WHERE `TABLE_SCHEMA` = DATABASE()
        AND `TABLE_NAME` = 'mes_pro_edhr_release_transaction'
        AND `COLUMN_NAME` = 'approved_at'
  ) THEN
    ALTER TABLE `mes_pro_edhr_release_transaction`
      ADD COLUMN `approved_at` datetime DEFAULT NULL COMMENT '批准时间' AFTER `approved_by`;
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM `information_schema`.`COLUMNS`
      WHERE `TABLE_SCHEMA` = DATABASE()
        AND `TABLE_NAME` = 'mes_pro_edhr_release_transaction'
        AND `COLUMN_NAME` = 'approval_signoff_evidence_hash'
  ) THEN
    ALTER TABLE `mes_pro_edhr_release_transaction`
      ADD COLUMN `approval_signoff_evidence_hash` char(64) DEFAULT NULL COMMENT '批准签核证据摘要' AFTER `approved_at`;
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM `information_schema`.`COLUMNS`
      WHERE `TABLE_SCHEMA` = DATABASE()
        AND `TABLE_NAME` = 'mes_pro_edhr_release_transaction'
        AND `COLUMN_NAME` = 'approval_opinion'
  ) THEN
    ALTER TABLE `mes_pro_edhr_release_transaction`
      ADD COLUMN `approval_opinion` varchar(500) DEFAULT NULL COMMENT '审批意见' AFTER `approval_signoff_evidence_hash`;
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM `information_schema`.`COLUMNS`
      WHERE `TABLE_SCHEMA` = DATABASE()
        AND `TABLE_NAME` = 'mes_pro_edhr_release_transaction'
        AND `COLUMN_NAME` = 'rejected_by'
  ) THEN
    ALTER TABLE `mes_pro_edhr_release_transaction`
      ADD COLUMN `rejected_by` bigint DEFAULT NULL COMMENT '驳回人' AFTER `approval_opinion`;
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM `information_schema`.`COLUMNS`
      WHERE `TABLE_SCHEMA` = DATABASE()
        AND `TABLE_NAME` = 'mes_pro_edhr_release_transaction'
        AND `COLUMN_NAME` = 'rejected_at'
  ) THEN
    ALTER TABLE `mes_pro_edhr_release_transaction`
      ADD COLUMN `rejected_at` datetime DEFAULT NULL COMMENT '驳回时间' AFTER `rejected_by`;
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM `information_schema`.`COLUMNS`
      WHERE `TABLE_SCHEMA` = DATABASE()
        AND `TABLE_NAME` = 'mes_pro_edhr_release_transaction'
        AND `COLUMN_NAME` = 'reject_reason'
  ) THEN
    ALTER TABLE `mes_pro_edhr_release_transaction`
      ADD COLUMN `reject_reason` varchar(500) DEFAULT NULL COMMENT '驳回原因' AFTER `rejected_at`;
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM `information_schema`.`COLUMNS`
      WHERE `TABLE_SCHEMA` = DATABASE()
        AND `TABLE_NAME` = 'mes_pro_edhr_release_transaction'
        AND `COLUMN_NAME` = 'withdrawn_by'
  ) THEN
    ALTER TABLE `mes_pro_edhr_release_transaction`
      ADD COLUMN `withdrawn_by` bigint DEFAULT NULL COMMENT '撤回人' AFTER `reject_reason`;
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM `information_schema`.`COLUMNS`
      WHERE `TABLE_SCHEMA` = DATABASE()
        AND `TABLE_NAME` = 'mes_pro_edhr_release_transaction'
        AND `COLUMN_NAME` = 'withdrawn_at'
  ) THEN
    ALTER TABLE `mes_pro_edhr_release_transaction`
      ADD COLUMN `withdrawn_at` datetime DEFAULT NULL COMMENT '撤回时间' AFTER `withdrawn_by`;
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM `information_schema`.`COLUMNS`
      WHERE `TABLE_SCHEMA` = DATABASE()
        AND `TABLE_NAME` = 'mes_pro_edhr_release_transaction'
        AND `COLUMN_NAME` = 'withdraw_reason'
  ) THEN
    ALTER TABLE `mes_pro_edhr_release_transaction`
      ADD COLUMN `withdraw_reason` varchar(500) DEFAULT NULL COMMENT '撤回原因' AFTER `withdrawn_at`;
  END IF;
END$$
DELIMITER ;

CALL ensure_mes_edhr_release_transaction_lifecycle_columns();

DROP PROCEDURE IF EXISTS ensure_mes_edhr_release_transaction_lifecycle_columns;
