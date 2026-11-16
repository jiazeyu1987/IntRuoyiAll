-- Reviewed local-test deployment for int_main 8a50e3ef5.
-- Execute once after backup and absent-schema checks. Do not rerun partially applied DDL.
-- Registers credential identity metadata only; does not qualify an existing employee,
-- change passwords or roles, or rewrite any historical signature/audit/business record.
-- Production application-role grants and historical approval ownership activation
-- remain separate from this local schema deployment.

-- Source 20261010_system_signature_credential_authority_v2.sql; SHA-256 f586d69eb39a34b7285cc456a6acaa2b81bb2d7af607464a2adad62e43d1da4d
-- DRAFT ONLY: task 20261006-approval-center-review-preflight. Never executed by this task.
-- Deployment requires approval, the formal tenant inventory, and current schema/trigger verification.
-- Based on ruoyi-vue-pro.sql, 20260908_system_signature_password_t1.sql and
-- 20260830_system_user_lifecycle_deactivation.sql. This migration intentionally fails on rerun.
ALTER TABLE system_users
    ADD COLUMN signature_credential_generation CHAR(36) NULL,
    ADD COLUMN signature_credential_version BIGINT NULL;

-- Only mutable identity metadata is initialized. No historical signature/audit payload is rewritten.
UPDATE system_users SET signature_credential_generation = UUID(), signature_credential_version = 1, update_time = update_time;
ALTER TABLE system_users
    MODIFY COLUMN signature_credential_generation CHAR(36) NOT NULL,
    MODIFY COLUMN signature_credential_version BIGINT NOT NULL;

CREATE TABLE system_approval_authority_revision (
    tenant_id BIGINT NOT NULL,
    revision BIGINT NOT NULL,
    updated_at DATETIME(0) NOT NULL,
    PRIMARY KEY (tenant_id),
    CONSTRAINT chk_approval_authority_tenant CHECK (tenant_id > 0),
    CONSTRAINT chk_approval_authority_revision CHECK (revision > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Enumerate the formal tenant table; no request-driven insertion and no implicit tenant zero.
INSERT INTO system_approval_authority_revision (tenant_id, revision, updated_at)
SELECT id, 1, UTC_TIMESTAMP(0) FROM system_tenant WHERE id > 0 AND deleted = b'0';

CREATE TABLE system_signature_authentication_event (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    actor_id BIGINT NOT NULL,
    identity_domain VARCHAR(32) NOT NULL,
    operator_id BIGINT NOT NULL,
    request_id VARCHAR(64) NOT NULL,
    operation_type VARCHAR(32) NOT NULL,
    outcome VARCHAR(64) NOT NULL,
    occurred_at DATETIME(0) NOT NULL,
    credential_version BIGINT NOT NULL,
    credential_generation CHAR(36) NOT NULL,
    creator VARCHAR(64) NULL,
    create_time DATETIME(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) NULL,
    update_time DATETIME(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BIT(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (id),
    KEY idx_signature_authentication_request (tenant_id, request_id, id),
    CONSTRAINT chk_signature_authentication_domain CHECK (identity_domain IN ('SYSTEM_USER', 'MES_EMPLOYEE_PROFILE')),
    CONSTRAINT chk_signature_authentication_identity CHECK (tenant_id > 0 AND actor_id > 0 AND operator_id > 0 AND credential_version > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DELIMITER $$
CREATE TRIGGER system_users_signature_identity_insert
BEFORE INSERT ON system_users FOR EACH ROW
BEGIN
    -- The database owns generation, even when an import supplies a previously used primary key.
    SET NEW.signature_credential_generation = UUID();
    SET NEW.signature_credential_version = 1;
END$$
CREATE TRIGGER system_users_signature_identity_update
BEFORE UPDATE ON system_users FOR EACH ROW
BEGIN
    IF NOT (NEW.signature_credential_generation <=> OLD.signature_credential_generation) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Signature identity generation is immutable';
    END IF;
    IF NOT (NEW.signature_credential_version <=> OLD.signature_credential_version) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Signature credential version is database owned';
    END IF;
    IF NOT (NEW.password <=> OLD.password)
       OR NOT (NEW.password_credential_status <=> OLD.password_credential_status)
       OR NOT (NEW.password_update_time <=> OLD.password_update_time)
       OR NOT (NEW.status <=> OLD.status)
       OR NOT (NEW.deleted <=> OLD.deleted)
       OR NOT (NEW.lifecycle_effective_time <=> OLD.lifecycle_effective_time)
       OR NOT (NEW.lifecycle_deactivated_time <=> OLD.lifecycle_deactivated_time)
       OR NOT (NEW.login_locked <=> OLD.login_locked)
       OR NOT (NEW.login_locked_time <=> OLD.login_locked_time) THEN
        IF OLD.signature_credential_version = 9223372036854775807 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Signature credential version exhausted';
        END IF;
        SET NEW.signature_credential_version = OLD.signature_credential_version + 1;
    ELSE
        SET NEW.signature_credential_version = OLD.signature_credential_version;
    END IF;
END$$
CREATE TRIGGER system_signature_authentication_event_no_update
BEFORE UPDATE ON system_signature_authentication_event FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Signature authentication event is immutable';
END$$
CREATE TRIGGER system_signature_authentication_event_no_delete
BEFORE DELETE ON system_signature_authentication_event FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Signature authentication event is immutable';
END$$
DELIMITER ;

-- Source 20261010_edhr_approval_content_v2_draft.sql; SHA-256 d12d6b91755e1dfb6d244e825169957cdde270cd4395013b6c0f737ba179305c
-- SOURCE DRAFT ONLY. NOT EXECUTED. Requires reviewed migration/backfill and DB-role grants.
ALTER TABLE system_electronic_signature ADD COLUMN evidence_format_version varchar(8) NULL,
 ADD COLUMN command_hash_version varchar(8) NULL;
ALTER TABLE mes_pro_process_pool_team_employee_profile
 ADD COLUMN signature_credential_generation varchar(36) NULL,
 ADD COLUMN signature_credential_version bigint NULL,
 ADD COLUMN signature_credential_status varchar(24) NULL,
 ADD COLUMN signature_failure_count int NOT NULL DEFAULT 0,
 ADD COLUMN signature_failure_window_start_at datetime NULL,
 ADD COLUMN signature_locked_until datetime NULL;
-- Existing profiles deliberately remain unauthenticatable until explicit qualification/backfill.
-- Initialize identity metadata before installing its immutable-generation trigger.
-- Credential status remains NULL: this grants no signing qualification.
UPDATE mes_pro_process_pool_team_employee_profile
 SET signature_credential_generation=UUID(), signature_credential_version=1, update_time=update_time;
CREATE TABLE mes_employee_signature_password_history (
 id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY, tenant_id bigint NOT NULL,
 employee_profile_id bigint NOT NULL,password_hash varchar(255) NOT NULL,changed_at datetime NOT NULL,
 KEY ix_mes_employee_signature_history(tenant_id,employee_profile_id,id)
);
DELIMITER $$
CREATE TRIGGER mes_employee_signature_generation_insert BEFORE INSERT ON mes_pro_process_pool_team_employee_profile
FOR EACH ROW BEGIN
 SET NEW.signature_credential_generation=UUID();
 SET NEW.signature_credential_version=1;
END$$
CREATE TRIGGER mes_employee_signature_generation_update BEFORE UPDATE ON mes_pro_process_pool_team_employee_profile
FOR EACH ROW BEGIN
 SET NEW.signature_credential_generation=OLD.signature_credential_generation;
 IF NOT(NEW.signature_password_hash <=> OLD.signature_password_hash)
 OR NOT(NEW.signature_credential_status <=> OLD.signature_credential_status)
 OR NOT(NEW.enabled <=> OLD.enabled) OR NOT(NEW.disabled_at <=> OLD.disabled_at)
 OR NOT(NEW.system_user_id <=> OLD.system_user_id)
 OR NOT(NEW.signature_locked_until <=> OLD.signature_locked_until) THEN
  IF OLD.signature_credential_version IS NULL THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='signature credential backfill required'; END IF;
  SET NEW.signature_credential_version=OLD.signature_credential_version+1;
 ELSE SET NEW.signature_credential_version=OLD.signature_credential_version;
 END IF;
END$$
DELIMITER ;
CREATE TABLE bpm_approval_review_prepare (
 id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,tenant_id bigint NOT NULL,actor_id bigint NOT NULL,
 prepare_id char(36) NOT NULL,owner_code varchar(32) NOT NULL,source_task_type varchar(64) NOT NULL,
 source_task_id varchar(64) NOT NULL,process_instance_id varchar(128) NULL,
 subject_type varchar(64) NOT NULL,subject_id varchar(128) NOT NULL,
 content_policy_version varchar(64) NOT NULL,subject_version varchar(128) NOT NULL,
 preview_content_hash char(64) NOT NULL,scope_code varchar(64) NOT NULL,
 canonical_business_content_json longtext NOT NULL,prepare_record_hash char(64) NOT NULL,
 prepared_at datetime NOT NULL,expires_at datetime NOT NULL,UNIQUE KEY uq_approval_prepare(tenant_id,prepare_id)
);
CREATE TABLE bpm_approval_review_receipt (
 id bigint NOT NULL PRIMARY KEY,tenant_id bigint NOT NULL,actor_id bigint NOT NULL,operator_id bigint NOT NULL,
 identity_domain varchar(32) NOT NULL,idempotency_key char(36) NOT NULL,request_hash char(64) NOT NULL,
 request_hash_version varchar(32) NOT NULL,prepare_id char(36) NOT NULL,identity_json longtext NOT NULL,
 content_policy_version varchar(64) NOT NULL,subject_version varchar(128) NOT NULL,
 preview_content_hash char(64) NOT NULL,canonical_business_content_json longtext NOT NULL,
 action_code varchar(32) NOT NULL,reason varchar(4000) NOT NULL,center_signature_id bigint NOT NULL,
 center_evidence_hash char(64) NOT NULL,signature_creation_audit_id bigint NOT NULL,
 result_code varchar(32) NOT NULL,resulting_status varchar(64) NOT NULL,domain_links_json longtext NOT NULL,
 response_json longtext NOT NULL,receipt_audit_id bigint NOT NULL,receipt_record_hash char(64) NOT NULL,
 committed_at datetime NOT NULL,UNIQUE KEY uq_approval_receipt(tenant_id,idempotency_key),
 UNIQUE KEY uq_approval_receipt_audit(tenant_id,receipt_audit_id)
);
-- Deployment must grant prepare/receipt/history INSERT+SELECT only to the application role.
-- Deny UPDATE/DELETE/TRUNCATE and require privileged, separately audited retention operations.

-- Source 20261010_approval_process_ownership_draft.sql; SHA-256 171c450d2cd1b5c3d5e55579a7bcc9816f89d7715ed535e6bd103623bd07fb4e
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

-- Source 20261009_mes_edhr_source_creation_binding.sql; SHA-256 60de554fd41d3252b99bdffe1562580d3d8b663571744eadc9a866311929a81e
-- DRAFT ONLY: source candidate; no migration execution or production activation authorized.
-- Append-only historical source binding. No BaseDO, deletion flag or mutable pointer.
CREATE TABLE mes_edhr_source_creation_binding (
  id bigint NOT NULL AUTO_INCREMENT,
  tenant_id bigint NOT NULL,
  owner_protocol varchar(64) NOT NULL,
  source_row_id bigint NOT NULL,
  source_receipt_id varchar(128) NOT NULL,
  source_version varchar(64) NOT NULL,
  source_hash char(64) NOT NULL,
  request_hash_version varchar(32) NOT NULL,
  request_hash char(64) NOT NULL,
  creation_event_id bigint NOT NULL,
  creation_event_hash char(64) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_mes_edhr_creation_row (tenant_id,owner_protocol,source_row_id),
  UNIQUE KEY uk_mes_edhr_creation_receipt (tenant_id,owner_protocol,source_receipt_id,source_version),
  UNIQUE KEY uk_mes_edhr_creation_event (tenant_id,creation_event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

-- Source 20261010_mes_edhr_material_associations.sql; SHA-256 35349b99e656a41cf9b73b3a8002cbdfc530367d8ef3ebd4938e16edb642fd67
-- DRAFT ONLY. Not executed; immutable association rows, no historical reconstruction.
CREATE TABLE mes_edhr_release_submission_material_association (
 id bigint NOT NULL AUTO_INCREMENT, tenant_id bigint NOT NULL,
 association_id char(36) NOT NULL, association_protocol varchar(64) NOT NULL, association_version int NOT NULL,
 release_transaction_id bigint NOT NULL, batch_execution_id bigint NOT NULL, submission_event_id bigint NOT NULL,
 submission_transaction_version int NOT NULL, approval_work_task_id bigint NOT NULL,
 source_method varchar(32) NOT NULL, submit_idempotency_key varchar(128) NOT NULL, submission_key_digest char(64) NOT NULL,
 submitted_by bigint NOT NULL, submission_evidence_hash char(64) NOT NULL, precheck_snapshot_hash char(64) NOT NULL,
 material_source_row_id bigint NOT NULL, material_receipt_id varchar(128) NOT NULL, material_version int NOT NULL,
 material_receipt_hash char(64) NOT NULL, material_manifest_hash char(64) NOT NULL, source_snapshot_hash char(64) NOT NULL,
 material_creation_event_id bigint NOT NULL, material_creation_event_hash char(64) NOT NULL,
 association_payload_json longtext NOT NULL, association_hash char(64) NOT NULL,
 submission_audit_event_id bigint NOT NULL, submission_audit_event_hash char(64) NOT NULL,
 PRIMARY KEY(id), UNIQUE KEY uk_submission_identity(tenant_id,release_transaction_id,submission_key_digest),
 UNIQUE KEY uk_submission_association(tenant_id,association_id), UNIQUE KEY uk_submission_source(tenant_id,submission_event_id),
 UNIQUE KEY uk_submission_task(tenant_id,approval_work_task_id), UNIQUE KEY uk_submission_audit(tenant_id,submission_audit_event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
CREATE TABLE mes_edhr_managed_handoff_material_association (
 id bigint NOT NULL AUTO_INCREMENT, tenant_id bigint NOT NULL, association_id char(36) NOT NULL,
 application_id bigint NOT NULL, release_transaction_id bigint NOT NULL, manager_work_task_id bigint NOT NULL,
 source_method varchar(32) NOT NULL, source_work_task_id bigint NOT NULL, source_expected_application_version int NOT NULL,
 source_idempotency_key varchar(128) NOT NULL, handoff_key varchar(80) NOT NULL, handoff_request_json longtext NOT NULL,
 association_payload_json longtext NOT NULL, association_hash char(64) NOT NULL,
 handoff_audit_event_id bigint NOT NULL, handoff_audit_event_hash char(64) NOT NULL,
 PRIMARY KEY(id), UNIQUE KEY uk_handoff_identity(tenant_id,handoff_key), UNIQUE KEY uk_handoff_association(tenant_id,association_id),
 UNIQUE KEY uk_handoff_task(tenant_id,manager_work_task_id), UNIQUE KEY uk_handoff_audit(tenant_id,handoff_audit_event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;