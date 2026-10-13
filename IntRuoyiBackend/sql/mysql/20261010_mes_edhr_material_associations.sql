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