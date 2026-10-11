-- SOURCE DRAFT ONLY. NOT EXECUTED. Requires separately authorized MySQL deployment.
-- Both facts are append-only. There is no deleted/default-owner/update/delete migration.
CREATE TABLE bpm_approval_process_ownership (
  id BIGINT NOT NULL AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  process_instance_id VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  process_definition_id VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  process_definition_key VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  business_key VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL,
  projection_code VARCHAR(32) NOT NULL,
  authority_type VARCHAR(32) NOT NULL,
  authority_id VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  authority_version VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  created_at DATETIME(3) NOT NULL,
  writer_code VARCHAR(64) NOT NULL,
  evidence_digest CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_approval_owner_tenant_process (tenant_id,process_instance_id),
  KEY idx_approval_owner_projection (tenant_id,projection_code,process_instance_id),
  CONSTRAINT chk_approval_owner_projection CHECK (projection_code IN ('DCC_TASK','NATIVE_FORM','NATIVE_BUSINESS','NATIVE_EXTERNAL_EDHR','NATIVE_BPM')),
  CONSTRAINT chk_approval_owner_digest CHECK (evidence_digest REGEXP '^[0-9a-f]{64}$'),
  CONSTRAINT chk_approval_owner_writer CHECK (
    (writer_code='DCC_CONTROLLED_FILE_SUBMIT' AND projection_code='DCC_TASK' AND authority_type='CONTROLLED_FILE') OR
    (writer_code='FORM_ACTION_SUBMIT' AND projection_code='NATIVE_FORM' AND authority_type='FORM_ACTION') OR
    (writer_code='BUSINESS_APPROVAL_SUBMIT' AND projection_code='NATIVE_BUSINESS' AND authority_type='BUSINESS_REQUEST') OR
    (writer_code='EDHR_BUSINESS_APPROVAL_SUBMIT' AND projection_code='NATIVE_EXTERNAL_EDHR' AND authority_type='BUSINESS_REQUEST') OR
    (writer_code='REGISTRATION_CERTIFICATE_SUBMIT' AND projection_code='NATIVE_BUSINESS' AND authority_type='REGISTRATION_REQUEST') OR
    (writer_code='BPM_MANUAL_START' AND projection_code='NATIVE_BPM' AND authority_type='BPM_START') OR
    (writer_code='OA_LEAVE_SUBMIT' AND projection_code='NATIVE_BUSINESS' AND authority_type='OA_LEAVE') OR
    (writer_code='CRM_CONTRACT_SUBMIT' AND projection_code='NATIVE_BUSINESS' AND authority_type='CRM_CONTRACT') OR
    (writer_code='CRM_RECEIVABLE_SUBMIT' AND projection_code='NATIVE_BUSINESS' AND authority_type='CRM_RECEIVABLE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE bpm_approval_selected_candidate (
  tenant_id BIGINT NOT NULL,
  process_instance_id VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  user_id BIGINT NOT NULL,
  selection_kind VARCHAR(32) NOT NULL,
  PRIMARY KEY (tenant_id,process_instance_id,user_id,selection_kind),
  KEY idx_approval_candidate_actor (tenant_id,user_id,selection_kind,process_instance_id),
  CONSTRAINT fk_approval_candidate_owner FOREIGN KEY (tenant_id,process_instance_id)
    REFERENCES bpm_approval_process_ownership(tenant_id,process_instance_id),
  CONSTRAINT chk_approval_candidate_user CHECK (user_id > 0),
  CONSTRAINT chk_approval_candidate_kind CHECK (selection_kind='REGISTRATION_CERTIFICATE')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DELIMITER $$
CREATE TRIGGER trg_approval_owner_no_update BEFORE UPDATE ON bpm_approval_process_ownership
FOR EACH ROW BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='APPROVAL_OWNERSHIP_IMMUTABLE'; END$$
CREATE TRIGGER trg_approval_owner_no_delete BEFORE DELETE ON bpm_approval_process_ownership
FOR EACH ROW BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='APPROVAL_OWNERSHIP_IMMUTABLE'; END$$
CREATE TRIGGER trg_approval_candidate_no_update BEFORE UPDATE ON bpm_approval_selected_candidate
FOR EACH ROW BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='APPROVAL_SELECTED_CANDIDATE_IMMUTABLE'; END$$
CREATE TRIGGER trg_approval_candidate_no_delete BEFORE DELETE ON bpm_approval_selected_candidate
FOR EACH ROW BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='APPROVAL_SELECTED_CANDIDATE_IMMUTABLE'; END$$
DELIMITER ;
