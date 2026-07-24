-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260513_dcc_base_schema; type=schema; riskLevel=low
-- Adds explicit controlled-file change type for upload approval flow: NEW / REVISION / OBSOLETE.

DROP PROCEDURE IF EXISTS intruoyi_add_dcc_controlled_file_change_type;
DELIMITER //
CREATE PROCEDURE intruoyi_add_dcc_controlled_file_change_type()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'dcc_controlled_file'
      AND column_name = 'change_type'
  ) THEN
    ALTER TABLE `dcc_controlled_file`
      ADD COLUMN `change_type` varchar(32) NOT NULL DEFAULT 'NEW' COMMENT '变更方式：NEW/REVISION/OBSOLETE' AFTER `process_type`;
  END IF;
END//
DELIMITER ;

CALL intruoyi_add_dcc_controlled_file_change_type();
DROP PROCEDURE IF EXISTS intruoyi_add_dcc_controlled_file_change_type;
