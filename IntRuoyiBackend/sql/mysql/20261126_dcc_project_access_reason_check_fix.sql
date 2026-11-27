-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261112_dcc_project_access_reason_optional; type=schema; riskLevel=low; requiresTargetPreflight=true
-- 项目正式授权的变更原因改为选填，不修改已有授权或其他权限约束。
SET NAMES utf8mb4;

DELIMITER $$
DROP PROCEDURE IF EXISTS `dcc_project_access_reason_optional_20261126`$$
CREATE PROCEDURE `dcc_project_access_reason_optional_20261126`()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dcc_project_access_rule'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'DCC project access rule table missing';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dcc_project_access_rule'
      AND COLUMN_NAME = 'change_reason' AND DATA_TYPE = 'varchar'
      AND CHARACTER_MAXIMUM_LENGTH = 512
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Unexpected DCC change_reason column definition';
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'dcc_project_access_rule'
      AND CONSTRAINT_NAME = 'ck_dcc_project_access_reason' AND CONSTRAINT_TYPE = 'CHECK'
  ) THEN
    IF NOT EXISTS (
      SELECT 1 FROM information_schema.CHECK_CONSTRAINTS
      WHERE CONSTRAINT_SCHEMA = DATABASE() AND CONSTRAINT_NAME = 'ck_dcc_project_access_reason'
        AND REPLACE(REPLACE(REPLACE(REPLACE(LOWER(CHECK_CLAUSE), CHAR(96), ''), ' ', ''), '(', ''), ')', '') = 'char_lengthtrimchange_reason>0'
    ) THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Unexpected DCC change_reason check expression';
    END IF;

    ALTER TABLE `dcc_project_access_rule` DROP CHECK `ck_dcc_project_access_reason`;
  END IF;

  ALTER TABLE `dcc_project_access_rule` MODIFY COLUMN `change_reason` varchar(512) NULL;
END$$
CALL `dcc_project_access_reason_optional_20261126`()$$
DROP PROCEDURE `dcc_project_access_reason_optional_20261126`$$
DELIMITER ;
