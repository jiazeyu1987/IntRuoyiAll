-- release-target-preflight: migrationId=20261023_dcc_registration_certificate_download_project_optional_identity; allowedEnvironments=test,backup,prod
SELECT CASE WHEN EXISTS (
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
) THEN 'TARGET_PREFLIGHT_PASS:20261023_dcc_registration_certificate_download_project_optional_identity' ELSE 'TARGET_PREFLIGHT_BLOCKED:20261023_dcc_registration_certificate_download_project_optional_identity:ACCESS_SCHEMA_CONTRACT_MISMATCH' END;
