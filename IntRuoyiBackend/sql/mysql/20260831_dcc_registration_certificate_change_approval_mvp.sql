-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260818_dcc_registration_certificate_lifecycle,20260828_dcc_registration_certificate_upload_approval_request_type; type=schema; riskLevel=medium
-- Purpose: Keep registration-certificate change submissions pending until the existing formal approval flow finishes.

ALTER TABLE `dcc_registration_certificate_change`
  DROP CHECK `chk_dcc_reg_cert_change_status`;

ALTER TABLE `dcc_registration_certificate_lifecycle_event`
  DROP CHECK `chk_dcc_reg_cert_lifecycle_event_type`;

ALTER TABLE `dcc_registration_certificate_change`
  ADD COLUMN `approval_request_id` bigint DEFAULT NULL COMMENT 'Formal approval request id' AFTER `event_id`,
  ADD COLUMN `reviewer_user_id` bigint DEFAULT NULL COMMENT 'Formal approval actor user id' AFTER `actor_id`,
  ADD COLUMN `reviewed_at` datetime DEFAULT NULL COMMENT 'Formal approval time' AFTER `reviewer_user_id`;

ALTER TABLE `dcc_registration_certificate_change`
  MODIFY COLUMN `applied_at` datetime DEFAULT NULL COMMENT 'Applied time';

ALTER TABLE `dcc_registration_certificate_lifecycle_event`
  ADD CONSTRAINT `chk_dcc_reg_cert_lifecycle_event_type`
    CHECK (`event_type` IN ('RENEWAL_UPLOADED', 'ACTIVATION_APPLIED', 'SUPPORTING_DOCUMENT_UPLOADED',
      'SUPPORTING_DOCUMENT_CONFIRMED', 'SUPPORTING_DOCUMENT_REJECTED', 'SUPPORTING_DOCUMENT_EFFECTIVE',
      'CHANGE_SUBMITTED', 'CHANGE_APPLIED', 'CANDIDATE_VOIDED', 'CERTIFICATE_VOIDED'));

ALTER TABLE `dcc_registration_certificate_change`
  ADD UNIQUE KEY `uk_dcc_reg_cert_change_approval_request` (`tenant_id`, `approval_request_id`),
  ADD CONSTRAINT `chk_dcc_reg_cert_change_status`
    CHECK (`status` IN ('PENDING_APPROVAL', 'APPLIED', 'REJECTED', 'VOIDED'));
