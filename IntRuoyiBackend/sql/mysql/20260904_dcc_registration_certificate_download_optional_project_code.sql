-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260818_dcc_registration_certificate_access; type=schema; riskLevel=medium
-- 2026-09-04: registration certificate download requests may target certificates without project code.
-- The download filename keeps an empty first segment when no project code exists.

SET NAMES utf8mb4;

DELIMITER $$

DROP PROCEDURE IF EXISTS `drop_dcc_reg_cert_access_request_project_check_20260904a`$$
CREATE PROCEDURE `drop_dcc_reg_cert_access_request_project_check_20260904a`()
BEGIN
  IF NOT EXISTS (
    SELECT 1
      FROM information_schema.TABLES AS table_metadata
     WHERE table_metadata.TABLE_SCHEMA = DATABASE()
       AND table_metadata.TABLE_NAME = 'dcc_registration_certificate_access_request'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC access request table missing for optional project code migration';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM information_schema.TABLE_CONSTRAINTS AS table_constraint
     WHERE table_constraint.CONSTRAINT_SCHEMA = DATABASE()
       AND table_constraint.TABLE_NAME = 'dcc_registration_certificate_access_request'
       AND table_constraint.CONSTRAINT_NAME = 'chk_dcc_reg_cert_access_request_project'
       AND table_constraint.CONSTRAINT_TYPE = 'CHECK'
  ) THEN
    ALTER TABLE `dcc_registration_certificate_access_request`
      DROP CHECK `chk_dcc_reg_cert_access_request_project`;
  END IF;
END$$

CALL `drop_dcc_reg_cert_access_request_project_check_20260904a`()$$
DROP PROCEDURE IF EXISTS `drop_dcc_reg_cert_access_request_project_check_20260904a`$$

DELIMITER ;
