-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260612_mes_edhr_void_reopen_supplement; type=schema; riskLevel=low
-- Persist immutable storage identity and PDF/A-1b validation evidence for newly generated batch archives.

DROP PROCEDURE IF EXISTS ensure_mes_edhr_batch_archive_pdfa_columns;
DELIMITER $$
CREATE PROCEDURE ensure_mes_edhr_batch_archive_pdfa_columns()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_edhr_batch_execution_archive'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'mes_pro_edhr_batch_execution_archive is missing';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_edhr_batch_execution_archive'
      AND COLUMN_NAME = 'file_id'
  ) THEN
    ALTER TABLE `mes_pro_edhr_batch_execution_archive`
      ADD COLUMN `file_id` bigint DEFAULT NULL COMMENT '受保护归档文件ID' AFTER `file_path`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_edhr_batch_execution_archive'
      AND COLUMN_NAME = 'storage_retention_json'
  ) THEN
    ALTER TABLE `mes_pro_edhr_batch_execution_archive`
      ADD COLUMN `storage_retention_json` longtext DEFAULT NULL COMMENT '对象锁与保留证据' AFTER `file_id`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_edhr_batch_execution_archive'
      AND COLUMN_NAME = 'pdfa_profile'
  ) THEN
    ALTER TABLE `mes_pro_edhr_batch_execution_archive`
      ADD COLUMN `pdfa_profile` varchar(16) DEFAULT NULL COMMENT 'PDF/A合规类型' AFTER `content_hash`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_edhr_batch_execution_archive'
      AND COLUMN_NAME = 'pdfa_validation_status'
  ) THEN
    ALTER TABLE `mes_pro_edhr_batch_execution_archive`
      ADD COLUMN `pdfa_validation_status` varchar(16) DEFAULT NULL COMMENT 'PDF/A校验状态' AFTER `pdfa_profile`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_edhr_batch_execution_archive'
      AND COLUMN_NAME = 'pdfa_validated_at'
  ) THEN
    ALTER TABLE `mes_pro_edhr_batch_execution_archive`
      ADD COLUMN `pdfa_validated_at` datetime DEFAULT NULL COMMENT 'PDF/A校验时间' AFTER `pdfa_validation_status`;
  END IF;
END$$
DELIMITER ;

CALL ensure_mes_edhr_batch_archive_pdfa_columns();

DROP PROCEDURE IF EXISTS ensure_mes_edhr_batch_archive_pdfa_columns;
