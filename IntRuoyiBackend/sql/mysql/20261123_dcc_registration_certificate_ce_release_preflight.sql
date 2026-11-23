-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260818_dcc_registration_certificate_reminder,20261030_dcc_registration_certificate_regulatory_facts,20260818_dcc_registration_certificate_lifecycle; type=schema; riskLevel=high; requiresTargetPreflight=true
-- Check every tenant and historical row; never infer or rewrite approval states.
-- Run with application writers stopped; target preflight must use the same data predicates.
DROP PROCEDURE IF EXISTS release_20261123_dcc_ce_preflight;
DELIMITER $$
CREATE PROCEDURE release_20261123_dcc_ce_preflight()
BEGIN
  IF (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE()
      AND CONSTRAINT_TYPE='CHECK' AND ENFORCED='YES' AND (
       (TABLE_NAME='dcc_registration_certificate_reminder_occurrence' AND CONSTRAINT_NAME='chk_dcc_reg_cert_reminder_occurrence_threshold') OR
       (TABLE_NAME='dcc_registration_certificate_file' AND CONSTRAINT_NAME='chk_dcc_reg_cert_file_kind') OR
       (TABLE_NAME='dcc_registration_certificate_supporting_document' AND CONSTRAINT_NAME IN ('chk_dcc_reg_cert_support_type','chk_dcc_reg_cert_support_status')))) <> 4 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='DCC_CE_REQUIRED_CHECKS_MISSING_OR_NOT_ENFORCED';
  END IF;
  IF EXISTS (SELECT 1 FROM dcc_registration_certificate_supporting_document
      WHERE status IS NULL OR status NOT IN ('EFFECTIVE','REJECTED','VOIDED')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='DCC_CE_SUPPORT_STATUS_REQUIRES_EXPLICIT_REMEDIATION';
  END IF;
  IF EXISTS (SELECT 1 FROM dcc_registration_certificate_supporting_document
      WHERE document_type IS NULL OR document_type NOT IN ('RENEWAL_ACCEPTANCE_RECEIPT','RENEWAL_SUPPLEMENT_NOTICE','CE_RENEWAL_CONTRACT','CE_RENEWAL_QUOTATION')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='DCC_CE_UNSUPPORTED_DOCUMENT_TYPE';
  END IF;
  IF EXISTS (SELECT 1 FROM dcc_registration_certificate_file
      WHERE file_kind IS NULL OR file_kind NOT IN ('REGISTRATION_CERTIFICATE','CERTIFICATE_ATTACHMENT','FDA_PRODUCT_DESCRIPTION','CHANGE_APPROVAL','RENEWAL_ACCEPTANCE_RECEIPT','RENEWAL_SUPPLEMENT_NOTICE','CE_RENEWAL_CONTRACT','CE_RENEWAL_QUOTATION')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='DCC_CE_UNSUPPORTED_FILE_KIND';
  END IF;
  IF EXISTS (SELECT 1 FROM dcc_registration_certificate_reminder_occurrence
      WHERE threshold_level IS NULL OR threshold_level NOT IN ('T_30','T_8','T_2','T_1','CE_T_18','CE_T_12','CE_T_3')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='DCC_CE_UNSUPPORTED_REMINDER_THRESHOLD';
  END IF;
END$$
DELIMITER ;
CALL release_20261123_dcc_ce_preflight();
DROP PROCEDURE release_20261123_dcc_ce_preflight;
