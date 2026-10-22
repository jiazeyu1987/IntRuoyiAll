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
