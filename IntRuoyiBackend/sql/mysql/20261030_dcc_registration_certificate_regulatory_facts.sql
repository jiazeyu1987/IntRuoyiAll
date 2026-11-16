-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260830_dcc_registration_certificate_upload_nullable_fields,20260831_dcc_registration_certificate_change_approval_mvp; type=schema; riskLevel=medium
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS migrate_registration_facts_20261030;
DELIMITER $$
CREATE PROCEDURE migrate_registration_facts_20261030()
BEGIN
 IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_registration_certificate_snapshot' AND COLUMN_NAME='facts_json') THEN
  ALTER TABLE dcc_registration_certificate_snapshot ADD COLUMN facts_json JSON NULL COMMENT 'Certificate category and complete approved regulatory facts';
 END IF;
END$$
DELIMITER ;
CALL migrate_registration_facts_20261030();
DROP PROCEDURE migrate_registration_facts_20261030;
DROP TRIGGER IF EXISTS trg_dcc_reg_cert_snapshot_immutable_bu;
UPDATE dcc_registration_certificate_snapshot s
JOIN dcc_registration_certificate_version v ON v.id=s.version_id AND v.tenant_id=s.tenant_id
JOIN dcc_registration_certificate c ON c.id=v.certificate_id AND c.tenant_id=v.tenant_id
LEFT JOIN dcc_project_code pc ON pc.id=c.project_code_id AND pc.tenant_id=c.tenant_id AND pc.deleted=0
SET s.facts_json=JSON_OBJECT('certificateType','DOMESTIC','certificateStatus',IF(v.expiry_date<CURRENT_DATE(),'EXPIRED','VALID'),
 'ownProduct',CAST(IF(c.product_master_id IS NOT NULL,'true','false') AS JSON),
 'projectCode',pc.project_code,
 'certificateNo',v.certificate_no,'classification',v.classification,'remark',v.remark,
 'firstObtainedDate',DATE_FORMAT(c.first_obtained_date,'%Y-%m-%d'),
 'approvalDate',DATE_FORMAT(v.approval_date,'%Y-%m-%d'),'effectiveDate',DATE_FORMAT(v.effective_date,'%Y-%m-%d'),
 'expiryDate',DATE_FORMAT(v.expiry_date,'%Y-%m-%d')),
 s.update_time=s.update_time
WHERE s.facts_json IS NULL;
DELIMITER $$
CREATE TRIGGER trg_dcc_reg_cert_snapshot_immutable_bu BEFORE UPDATE ON dcc_registration_certificate_snapshot FOR EACH ROW
BEGIN
 DECLARE linked_version_status VARCHAR(32);
 IF NOT(OLD.id<=>NEW.id) OR NOT(OLD.version_id<=>NEW.version_id) OR NOT(OLD.tenant_id<=>NEW.tenant_id) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Cross-version snapshot reattachment is forbidden';
 END IF;
 SELECT status INTO linked_version_status FROM dcc_registration_certificate_version WHERE id=OLD.version_id AND tenant_id=OLD.tenant_id AND deleted=b'0';
 IF linked_version_status IS NULL OR linked_version_status<>'DRAFT' THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Formal registration certificate snapshot is append-only';
 END IF;
END$$
DELIMITER ;
ALTER TABLE dcc_registration_certificate_snapshot MODIFY facts_json JSON NOT NULL;
ALTER TABLE dcc_registration_certificate_file DROP CHECK chk_dcc_reg_cert_file_kind;
ALTER TABLE dcc_registration_certificate_file ADD CONSTRAINT chk_dcc_reg_cert_file_kind CHECK(file_kind IN
 ('REGISTRATION_CERTIFICATE','CERTIFICATE_ATTACHMENT','FDA_PRODUCT_DESCRIPTION','CHANGE_APPROVAL','RENEWAL_ACCEPTANCE_RECEIPT','RENEWAL_SUPPLEMENT_NOTICE'));

ALTER TABLE dcc_registration_certificate_change_item DROP CHECK chk_dcc_reg_cert_change_item_type;
ALTER TABLE dcc_registration_certificate_change_item ADD CONSTRAINT chk_dcc_reg_cert_change_item_type
CHECK(item_type IN ('PRODUCT_NAME','MODEL_SPECIFICATION','STRUCTURE_COMPOSITION','INTENDED_USE','TECHNICAL_REQUIREMENTS','REGISTRANT_NAME','RESIDENCE_ADDRESS','PRODUCTION_ADDRESS','OTHER_CONTENT','CERTIFICATE_INFORMATION','PRODUCTION_RELATION'));
