-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=low
-- Purpose: Expand infra_config.value so full Kingdee production connection JSON can store SimPas appId, signedData, and timestamp.
-- Recovery: Re-run this idempotent migration after restoring a database backup if infra_config.value is still shorter than 2000.
-- Rollback: Only shrink after scanning all infra_config.value lengths and confirming no row exceeds the target length.

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_infra_config_value_expand_20260902;
DELIMITER //
CREATE PROCEDURE ensure_infra_config_value_expand_20260902()
BEGIN
  DECLARE column_count INT DEFAULT 0;
  DECLARE data_type_value VARCHAR(64) DEFAULT NULL;
  DECLARE character_maximum_length_value BIGINT DEFAULT NULL;
  DECLARE is_nullable_value VARCHAR(8) DEFAULT NULL;
  DECLARE character_set_name_value VARCHAR(64) DEFAULT NULL;
  DECLARE collation_name_value VARCHAR(64) DEFAULT NULL;

  SELECT COUNT(*)
    INTO column_count
    FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'infra_config'
     AND COLUMN_NAME = 'value';

  IF column_count <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing infra_config.value column for Kingdee connection config expansion';
  END IF;

  SELECT DATA_TYPE,
         CHARACTER_MAXIMUM_LENGTH,
         IS_NULLABLE,
         CHARACTER_SET_NAME,
         COLLATION_NAME
    INTO data_type_value,
         character_maximum_length_value,
         is_nullable_value,
         character_set_name_value,
         collation_name_value
    FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'infra_config'
     AND COLUMN_NAME = 'value'
   LIMIT 1;

  IF data_type_value <> 'varchar'
      OR character_maximum_length_value IS NULL
      OR character_maximum_length_value > 2000
      OR is_nullable_value <> 'NO'
      OR character_set_name_value <> 'utf8mb4'
      OR collation_name_value <> 'utf8mb4_unicode_ci' THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Unexpected infra_config.value column type before Kingdee connection config expansion';
  END IF;
END//
DELIMITER ;

CALL ensure_infra_config_value_expand_20260902();
DROP PROCEDURE IF EXISTS ensure_infra_config_value_expand_20260902;

ALTER TABLE `infra_config`
  MODIFY COLUMN `value` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '参数键值';

DROP PROCEDURE IF EXISTS verify_infra_config_value_expand_20260902;
DELIMITER //
CREATE PROCEDURE verify_infra_config_value_expand_20260902()
BEGIN
  IF NOT EXISTS (
      SELECT 1
        FROM information_schema.COLUMNS
       WHERE TABLE_SCHEMA = DATABASE()
         AND TABLE_NAME = 'infra_config'
         AND COLUMN_NAME = 'value'
         AND DATA_TYPE = 'varchar'
         AND CHARACTER_MAXIMUM_LENGTH = 2000
         AND IS_NULLABLE = 'NO'
         AND CHARACTER_SET_NAME = 'utf8mb4'
         AND COLLATION_NAME = 'utf8mb4_unicode_ci'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'infra_config.value expansion to varchar(2000) failed';
  END IF;
END//
DELIMITER ;

CALL verify_infra_config_value_expand_20260902();
DROP PROCEDURE IF EXISTS verify_infra_config_value_expand_20260902;
