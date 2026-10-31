-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260818_dcc_registration_certificate_access; type=schema; riskLevel=low; requiresTargetPreflight=true; applyOrder=10; supersedes=20260904_dcc_registration_certificate_download_optional_project_code,20260904_dcc_registration_certificate_download_project_optional,20260904_dcc_registration_certificate_download_without_project_code
-- Consolidate three historical copies of the same optional-project-code change.
-- The current access schema already has the intended nullable column and no project CHECK.
-- Preserve all original source bytes and all request rows; remove only the named CHECK when present.
-- This is one atomic ALTER when needed, not transactional DML; no rollback claim is made for MySQL DDL.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS ensure_dcc_optional_download_project_20261023;
DELIMITER $$
CREATE PROCEDURE ensure_dcc_optional_download_project_20261023()
BEGIN
  IF NOT (EXISTS (
  SELECT 1 FROM information_schema.TABLES
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_registration_certificate_access_request'
    AND TABLE_TYPE='BASE TABLE' AND ENGINE='InnoDB'
)
AND EXISTS (
  SELECT 1 FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_registration_certificate_access_request'
    AND COLUMN_NAME='project_code_id' AND COLUMN_TYPE='bigint' AND IS_NULLABLE='YES'
)
AND NOT EXISTS (
  SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME='dcc_registration_certificate_access_request'
    AND CONSTRAINT_NAME='chk_dcc_reg_cert_access_request_project' AND CONSTRAINT_TYPE<>'CHECK'
)) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT='DCC optional project requires the existing InnoDB access table and nullable bigint project_code_id';
  END IF;
  IF EXISTS (
    SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME='dcc_registration_certificate_access_request'
      AND CONSTRAINT_NAME='chk_dcc_reg_cert_access_request_project' AND CONSTRAINT_TYPE='CHECK'
  ) THEN
    ALTER TABLE `dcc_registration_certificate_access_request`
      DROP CHECK `chk_dcc_reg_cert_access_request_project`;
  END IF;
  IF EXISTS (
    SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME='dcc_registration_certificate_access_request'
      AND CONSTRAINT_NAME='chk_dcc_reg_cert_access_request_project'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT='DCC optional project final constraint state was not reached';
  END IF;
END$$
DELIMITER ;
CALL ensure_dcc_optional_download_project_20261023();
DROP PROCEDURE IF EXISTS ensure_dcc_optional_download_project_20261023;
