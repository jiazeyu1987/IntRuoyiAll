-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- Upgrade infra_file_content.content to LONGBLOB so DB file storage can hold
-- large DCC original PDFs used by preview and final publication flows.
-- Safe to run repeatedly: only alters the column when it is not already LONGBLOB.

DROP PROCEDURE IF EXISTS ensure_infra_file_content_longblob;
DELIMITER $$
CREATE PROCEDURE ensure_infra_file_content_longblob()
BEGIN
  IF EXISTS (
      SELECT 1
      FROM information_schema.TABLES
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'infra_file_content'
  ) AND EXISTS (
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'infra_file_content'
        AND COLUMN_NAME = 'content'
        AND LOWER(DATA_TYPE) <> 'longblob'
  ) THEN
    ALTER TABLE `infra_file_content`
      MODIFY COLUMN `content` LONGBLOB NOT NULL COMMENT '文件内容';
  END IF;
END$$
DELIMITER ;

CALL ensure_infra_file_content_longblob();

DROP PROCEDURE IF EXISTS ensure_infra_file_content_longblob;
