-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261030_dcc_registration_certificate_regulatory_facts; type=schema; riskLevel=medium
SET NAMES utf8mb4;
ALTER TABLE dcc_registration_certificate_file DROP CHECK chk_dcc_reg_cert_file_kind;
ALTER TABLE dcc_registration_certificate_file ADD CONSTRAINT chk_dcc_reg_cert_file_kind CHECK(file_kind IN
 ('REGISTRATION_CERTIFICATE','CERTIFICATE_ATTACHMENT','FDA_PRODUCT_DESCRIPTION','CHANGE_APPROVAL','RENEWAL_ACCEPTANCE_RECEIPT','RENEWAL_SUPPLEMENT_NOTICE','CE_RENEWAL_CONTRACT','CE_RENEWAL_QUOTATION'));
ALTER TABLE dcc_registration_certificate_supporting_document DROP CHECK chk_dcc_reg_cert_support_type;
ALTER TABLE dcc_registration_certificate_supporting_document ADD CONSTRAINT chk_dcc_reg_cert_support_type CHECK(document_type IN
 ('RENEWAL_ACCEPTANCE_RECEIPT','RENEWAL_SUPPLEMENT_NOTICE','CE_RENEWAL_CONTRACT','CE_RENEWAL_QUOTATION'));
