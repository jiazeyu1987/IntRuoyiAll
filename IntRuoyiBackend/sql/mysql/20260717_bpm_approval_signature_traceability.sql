-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260715_bpm_approval_signature_record,20260706_dcc_signature_image_evidence_chain; type=schema; riskLevel=medium
-- 为新增审批中心签名记录补齐签名图片版本与哈希快照，历史记录不做回填。

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_bpm_signature_column;
DELIMITER $$
CREATE PROCEDURE ensure_bpm_signature_column(IN target_column VARCHAR(64), IN ddl_statement TEXT)
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'bpm_approval_signature_record'
      AND COLUMN_NAME = target_column
  ) THEN
    SET @ddl = ddl_statement;
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;

CALL ensure_bpm_signature_column('signature_image_id',
  'ALTER TABLE `bpm_approval_signature_record` ADD COLUMN `signature_image_id` bigint DEFAULT NULL COMMENT ''签名图片版本ID快照'' AFTER `password_verified`');
CALL ensure_bpm_signature_column('signature_image_version_no',
  'ALTER TABLE `bpm_approval_signature_record` ADD COLUMN `signature_image_version_no` int DEFAULT NULL COMMENT ''签名图片版本号快照'' AFTER `signature_image_id`');
CALL ensure_bpm_signature_column('signature_image_file_id',
  'ALTER TABLE `bpm_approval_signature_record` ADD COLUMN `signature_image_file_id` bigint DEFAULT NULL COMMENT ''签名图片文件ID快照'' AFTER `signature_image_version_no`');
CALL ensure_bpm_signature_column('signature_image_file_url',
  'ALTER TABLE `bpm_approval_signature_record` ADD COLUMN `signature_image_file_url` varchar(512) DEFAULT NULL COMMENT ''签名图片文件URL快照'' AFTER `signature_image_file_id`');
CALL ensure_bpm_signature_column('signature_image_sha256',
  'ALTER TABLE `bpm_approval_signature_record` ADD COLUMN `signature_image_sha256` varchar(128) DEFAULT NULL COMMENT ''签名图片SHA-256快照'' AFTER `signature_image_file_url`');
CALL ensure_bpm_signature_column('signature_image_content_type',
  'ALTER TABLE `bpm_approval_signature_record` ADD COLUMN `signature_image_content_type` varchar(128) DEFAULT NULL COMMENT ''签名图片内容类型快照'' AFTER `signature_image_sha256`');
CALL ensure_bpm_signature_column('signature_image_file_size',
  'ALTER TABLE `bpm_approval_signature_record` ADD COLUMN `signature_image_file_size` bigint DEFAULT NULL COMMENT ''签名图片大小快照'' AFTER `signature_image_content_type`');
CALL ensure_bpm_signature_column('signature_image_status_snapshot',
  'ALTER TABLE `bpm_approval_signature_record` ADD COLUMN `signature_image_status_snapshot` varchar(32) DEFAULT NULL COMMENT ''签名图片状态快照'' AFTER `signature_image_file_size`');
CALL ensure_bpm_signature_column('signature_image_verified_status',
  'ALTER TABLE `bpm_approval_signature_record` ADD COLUMN `signature_image_verified_status` varchar(32) DEFAULT NULL COMMENT ''签名图片校验状态'' AFTER `signature_image_status_snapshot`');

DROP PROCEDURE IF EXISTS ensure_bpm_signature_column;
