-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260818_dcc_registration_certificate_access; type=schema; riskLevel=medium
-- 2026-08-28: allow registration certificate upload/renewal submissions to reuse the native approval request table.

ALTER TABLE `dcc_registration_certificate_access_request`
  DROP CHECK `chk_dcc_reg_cert_access_request_type`;

ALTER TABLE `dcc_registration_certificate_access_request`
  ADD CONSTRAINT `chk_dcc_reg_cert_access_request_type` CHECK (`request_type` IN
    ('VIEW_OLD_CERTIFICATE', 'DOWNLOAD_FILE', 'UPLOAD_CERTIFICATE'));
