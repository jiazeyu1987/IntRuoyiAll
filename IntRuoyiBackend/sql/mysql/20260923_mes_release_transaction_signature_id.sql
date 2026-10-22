-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260618_mes_edhr_release_transaction_lifecycle; type=schema; riskLevel=low
-- Store the formal electronic signature identity on the release transaction.
-- Existing release rows are intentionally left unchanged.
DROP PROCEDURE IF EXISTS add_mes_release_transaction_signature_id_20260923;
DELIMITER $$
CREATE PROCEDURE add_mes_release_transaction_signature_id_20260923()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_edhr_release_transaction'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing release transaction prerequisite table';
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_edhr_release_transaction'
      AND COLUMN_NAME = 'approval_signature_id'
  ) THEN
    ALTER TABLE `mes_pro_edhr_release_transaction`
      ADD COLUMN `approval_signature_id` BIGINT NULL;
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_edhr_release_transaction'
      AND COLUMN_NAME = 'approval_signature_id'
      AND DATA_TYPE = 'bigint'
      AND IS_NULLABLE = 'YES'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Release approval signature column has an incompatible definition';
  END IF;
END$$
DELIMITER ;
CALL add_mes_release_transaction_signature_id_20260923();
DROP PROCEDURE add_mes_release_transaction_signature_id_20260923;
