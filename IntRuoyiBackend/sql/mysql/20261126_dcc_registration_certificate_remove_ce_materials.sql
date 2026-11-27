-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261123_dcc_registration_certificate_ce_supporting_materials; type=schema; riskLevel=medium; requiresTargetPreflight=true
-- CE contracts and quotations are outside the confirmed requirements. Preserve all historical rows.
-- Stop application writers and pass the matching read-only target preflight before applying.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS validate_reg_cert_materials_20261126;
DELIMITER $$
CREATE PROCEDURE validate_reg_cert_materials_20261126()
BEGIN
  IF (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
      WHERE CONSTRAINT_SCHEMA=DATABASE() AND CONSTRAINT_TYPE='CHECK' AND ENFORCED='YES'
        AND ((TABLE_NAME='dcc_registration_certificate_file' AND CONSTRAINT_NAME='chk_dcc_reg_cert_file_kind')
          OR (TABLE_NAME='dcc_registration_certificate_supporting_document' AND CONSTRAINT_NAME='chk_dcc_reg_cert_support_type'))) <> 2 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='DCC_MATERIAL_REQUIRED_CHECKS_MISSING_OR_NOT_ENFORCED';
  END IF;
  IF EXISTS (SELECT 1 FROM dcc_registration_certificate_file WHERE file_kind IS NULL OR file_kind NOT IN
      ('REGISTRATION_CERTIFICATE','CERTIFICATE_ATTACHMENT','FDA_PRODUCT_DESCRIPTION','CHANGE_APPROVAL','RENEWAL_ACCEPTANCE_RECEIPT','RENEWAL_SUPPLEMENT_NOTICE'))
     OR EXISTS (SELECT 1 FROM dcc_registration_certificate_supporting_document WHERE document_type IS NULL OR document_type NOT IN
      ('RENEWAL_ACCEPTANCE_RECEIPT','RENEWAL_SUPPLEMENT_NOTICE')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='DCC_UNSUPPORTED_MATERIALS_REQUIRE_EXPLICIT_REMEDIATION';
  END IF;
END$$
DELIMITER ;
CALL validate_reg_cert_materials_20261126();
DROP PROCEDURE validate_reg_cert_materials_20261126;
ALTER TABLE dcc_registration_certificate_file
  DROP CHECK chk_dcc_reg_cert_file_kind,
  ADD CONSTRAINT chk_dcc_reg_cert_file_kind CHECK(file_kind IN
    ('REGISTRATION_CERTIFICATE','CERTIFICATE_ATTACHMENT','FDA_PRODUCT_DESCRIPTION','CHANGE_APPROVAL','RENEWAL_ACCEPTANCE_RECEIPT','RENEWAL_SUPPLEMENT_NOTICE'));
ALTER TABLE dcc_registration_certificate_supporting_document
  DROP CHECK chk_dcc_reg_cert_support_type,
  ADD CONSTRAINT chk_dcc_reg_cert_support_type CHECK(document_type IN
    ('RENEWAL_ACCEPTANCE_RECEIPT','RENEWAL_SUPPLEMENT_NOTICE'));
