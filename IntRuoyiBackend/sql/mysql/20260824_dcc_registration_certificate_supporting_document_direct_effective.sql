-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260818_dcc_registration_certificate_lifecycle; type=schema; riskLevel=high
-- 2026-08-24: supporting documents are effective immediately after the formal file and scope checks.
-- No template validation or document-control confirmation is performed by this migration.

UPDATE `dcc_registration_certificate_supporting_document`
   SET `status` = 'EFFECTIVE',
       `confirmed_at` = COALESCE(`confirmed_at`, `uploaded_at`),
       `confirmed_by` = COALESCE(`confirmed_by`, `uploaded_by`)
 WHERE `status` IN ('PENDING_CONFIRMATION', 'CONFIRMED');

ALTER TABLE `dcc_registration_certificate_lifecycle_event`
  DROP CHECK `chk_dcc_reg_cert_lifecycle_event_type`;

ALTER TABLE `dcc_registration_certificate_lifecycle_event`
  ADD CONSTRAINT `chk_dcc_reg_cert_lifecycle_event_type` CHECK (`event_type` IN
    ('RENEWAL_UPLOADED', 'ACTIVATION_APPLIED', 'SUPPORTING_DOCUMENT_UPLOADED',
     'SUPPORTING_DOCUMENT_CONFIRMED', 'SUPPORTING_DOCUMENT_REJECTED',
     'SUPPORTING_DOCUMENT_EFFECTIVE', 'CHANGE_APPLIED', 'CANDIDATE_VOIDED',
     'CERTIFICATE_VOIDED'));

ALTER TABLE `dcc_registration_certificate_supporting_document`
  DROP CHECK `chk_dcc_reg_cert_support_status`;

ALTER TABLE `dcc_registration_certificate_supporting_document`
  ADD CONSTRAINT `chk_dcc_reg_cert_support_status` CHECK (`status` IN
    ('EFFECTIVE', 'REJECTED', 'VOIDED'));

DELETE FROM `system_menu`
 WHERE `permission` = 'dcc:registration-certificate:supporting-document:confirm';
