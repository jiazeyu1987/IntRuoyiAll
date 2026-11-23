-- release-target-preflight: migrationId=20261123_dcc_registration_certificate_ce_release_preflight; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
  NOT ((SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE()
      AND CONSTRAINT_TYPE='CHECK' AND ENFORCED='YES' AND (
       (TABLE_NAME='dcc_registration_certificate_reminder_occurrence' AND CONSTRAINT_NAME='chk_dcc_reg_cert_reminder_occurrence_threshold') OR
       (TABLE_NAME='dcc_registration_certificate_file' AND CONSTRAINT_NAME='chk_dcc_reg_cert_file_kind') OR
       (TABLE_NAME='dcc_registration_certificate_supporting_document' AND CONSTRAINT_NAME IN ('chk_dcc_reg_cert_support_type','chk_dcc_reg_cert_support_status')))) <> 4)
  AND NOT (EXISTS (SELECT 1 FROM dcc_registration_certificate_supporting_document
      WHERE status IS NULL OR status NOT IN ('EFFECTIVE','REJECTED','VOIDED')))
  AND NOT (EXISTS (SELECT 1 FROM dcc_registration_certificate_supporting_document
      WHERE document_type IS NULL OR document_type NOT IN ('RENEWAL_ACCEPTANCE_RECEIPT','RENEWAL_SUPPLEMENT_NOTICE','CE_RENEWAL_CONTRACT','CE_RENEWAL_QUOTATION')))
  AND NOT (EXISTS (SELECT 1 FROM dcc_registration_certificate_file
      WHERE file_kind IS NULL OR file_kind NOT IN ('REGISTRATION_CERTIFICATE','CERTIFICATE_ATTACHMENT','FDA_PRODUCT_DESCRIPTION','CHANGE_APPROVAL','RENEWAL_ACCEPTANCE_RECEIPT','RENEWAL_SUPPLEMENT_NOTICE','CE_RENEWAL_CONTRACT','CE_RENEWAL_QUOTATION')))
  AND NOT (EXISTS (SELECT 1 FROM dcc_registration_certificate_reminder_occurrence
      WHERE threshold_level IS NULL OR threshold_level NOT IN ('T_30','T_8','T_2','T_1','CE_T_18','CE_T_12','CE_T_3')))
THEN 'TARGET_PREFLIGHT_PASS:20261123_dcc_registration_certificate_ce_release_preflight' ELSE 'TARGET_PREFLIGHT_BLOCKED:20261123_dcc_registration_certificate_ce_release_preflight' END;
