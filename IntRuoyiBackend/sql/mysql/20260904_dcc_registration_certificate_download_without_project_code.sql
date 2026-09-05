-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260818_dcc_registration_certificate_access; type=schema; riskLevel=medium
-- 2026-09-04: allow registration certificate download access requests when the certificate has no project code.

ALTER TABLE `dcc_registration_certificate_access_request`
  DROP CHECK `chk_dcc_reg_cert_access_request_project`;
