-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260818_dcc_registration_certificate_access; type=schema; riskLevel=medium
-- 2026-09-04: registration certificate download requests may target certificates without project code.
-- The download filename keeps an empty first segment when no project code exists.

ALTER TABLE `dcc_registration_certificate_access_request`
  DROP CHECK `chk_dcc_reg_cert_access_request_project`;
