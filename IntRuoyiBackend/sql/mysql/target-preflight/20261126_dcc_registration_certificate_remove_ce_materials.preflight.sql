-- release-target-preflight: migrationId=20261126_dcc_registration_certificate_remove_ce_materials; allowedEnvironments=test,backup,prod
-- Read-only, including deleted rows. Report conflicts without deleting or converting historical evidence.
SELECT CASE
WHEN
  (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA=DATABASE() AND CONSTRAINT_TYPE='CHECK' AND ENFORCED='YES'
      AND ((TABLE_NAME='dcc_registration_certificate_file' AND CONSTRAINT_NAME='chk_dcc_reg_cert_file_kind')
        OR (TABLE_NAME='dcc_registration_certificate_supporting_document' AND CONSTRAINT_NAME='chk_dcc_reg_cert_support_type'))) <> 2
  THEN 'TARGET_PREFLIGHT_BLOCKED:20261126_dcc_registration_certificate_remove_ce_materials:REQUIRED_CHECKS_MISSING_OR_NOT_ENFORCED'
WHEN EXISTS (SELECT 1 FROM dcc_registration_certificate_file WHERE file_kind IS NULL OR file_kind NOT IN
    ('REGISTRATION_CERTIFICATE','CERTIFICATE_ATTACHMENT','FDA_PRODUCT_DESCRIPTION','CHANGE_APPROVAL','RENEWAL_ACCEPTANCE_RECEIPT','RENEWAL_SUPPLEMENT_NOTICE'))
  THEN 'TARGET_PREFLIGHT_BLOCKED:20261126_dcc_registration_certificate_remove_ce_materials:UNSUPPORTED_FILE_KIND'
WHEN EXISTS (SELECT 1 FROM dcc_registration_certificate_supporting_document WHERE document_type IS NULL OR document_type NOT IN
    ('RENEWAL_ACCEPTANCE_RECEIPT','RENEWAL_SUPPLEMENT_NOTICE'))
  THEN 'TARGET_PREFLIGHT_BLOCKED:20261126_dcc_registration_certificate_remove_ce_materials:UNSUPPORTED_DOCUMENT_TYPE'
ELSE 'TARGET_PREFLIGHT_PASS:20261126_dcc_registration_certificate_remove_ce_materials' END;
