-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260930_dcc_a_lifecycle; type=schema; riskLevel=medium
-- G06: server canonical CHECKIN identity. Historical checkout rows remain NULL and are never backfilled.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS ensure_dcc_checkin_payload_hash;
DELIMITER //
CREATE PROCEDURE ensure_dcc_checkin_payload_hash()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_controlled_file_checkout' AND ENGINE='InnoDB') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='G06 requires the actual InnoDB checkout lifecycle table';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_controlled_file_checkout' AND COLUMN_NAME='checkin_payload_hash') THEN
    ALTER TABLE dcc_controlled_file_checkout ADD COLUMN checkin_payload_hash CHAR(64) NULL
      COMMENT '服务器完整检入请求规范SHA-256，含有序普通附件票据';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_controlled_file_checkout' AND COLUMN_NAME='checkin_payload_hash'
      AND COLUMN_TYPE='char(64)' AND IS_NULLABLE='YES' AND COLUMN_DEFAULT IS NULL
      AND EXTRA='' AND COALESCE(GENERATION_EXPRESSION,'')='')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='G06 checkout request hash definition drift';
  END IF;
END//
DELIMITER ;
CALL ensure_dcc_checkin_payload_hash();
DROP PROCEDURE IF EXISTS ensure_dcc_checkin_payload_hash;
