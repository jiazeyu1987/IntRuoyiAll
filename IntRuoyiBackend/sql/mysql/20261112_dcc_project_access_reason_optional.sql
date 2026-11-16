-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260911_dcc_project_access_rule; type=schema; riskLevel=low
-- 项目正式授权的变更原因改为选填，不修改已有授权或其他权限约束。
SET NAMES utf8mb4;

DELIMITER $$
DROP PROCEDURE IF EXISTS `dcc_project_access_reason_optional_20261112`$$
CREATE PROCEDURE `dcc_project_access_reason_optional_20261112`()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dcc_project_access_rule'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'DCC project access rule table missing';
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'dcc_project_access_rule'
      AND CONSTRAINT_NAME = 'chk_dcc_project_access_reason' AND CONSTRAINT_TYPE = 'CHECK'
  ) THEN
    ALTER TABLE `dcc_project_access_rule` DROP CHECK `chk_dcc_project_access_reason`;
  END IF;

  ALTER TABLE `dcc_project_access_rule` MODIFY COLUMN `change_reason` varchar(512) NULL;
END$$
CALL `dcc_project_access_reason_optional_20261112`()$$
DROP PROCEDURE `dcc_project_access_reason_optional_20261112`$$
DELIMITER ;
