-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260811_dcc_signature_copy_binding; type=schema; riskLevel=medium
-- Freeze the controlled-copy object path as part of future immutable signature bindings.

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS add_dcc_signature_binding_object_key;
DELIMITER //
CREATE PROCEDURE add_dcc_signature_binding_object_key()
BEGIN
  IF NOT EXISTS (
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'dcc_controlled_file_signature_binding'
        AND COLUMN_NAME = 'controlled_copy_object_key'
  ) THEN
    ALTER TABLE `dcc_controlled_file_signature_binding`
      ADD COLUMN `controlled_copy_object_key` varchar(1024) NULL
      COMMENT '绑定时发布受控副本对象路径'
      AFTER `controlled_copy_file_id`;
  END IF;

  UPDATE `dcc_controlled_file_signature_binding` binding_record
  INNER JOIN `infra_file` controlled_copy_file
          ON controlled_copy_file.`id` = binding_record.`controlled_copy_file_id`
  SET binding_record.`controlled_copy_object_key` = controlled_copy_file.`path`
  WHERE binding_record.`controlled_copy_object_key` IS NULL
     OR binding_record.`controlled_copy_object_key` = '';

  IF EXISTS (
      SELECT 1
      FROM `dcc_controlled_file_signature_binding`
      WHERE `controlled_copy_object_key` IS NULL
         OR `controlled_copy_object_key` = ''
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Cannot backfill signature binding object key because the controlled copy file is missing';
  END IF;

  ALTER TABLE `dcc_controlled_file_signature_binding`
    MODIFY COLUMN `controlled_copy_object_key` varchar(1024) NOT NULL
    COMMENT '绑定时发布受控副本对象路径';
END//
DELIMITER ;

CALL add_dcc_signature_binding_object_key();
DROP PROCEDURE IF EXISTS add_dcc_signature_binding_object_key;
