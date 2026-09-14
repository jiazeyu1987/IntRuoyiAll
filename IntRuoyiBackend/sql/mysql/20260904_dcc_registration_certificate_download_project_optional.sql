-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260818_dcc_registration_certificate_access; type=schema; riskLevel=low
-- A registration-certificate download request may be submitted without a project code.

SET NAMES utf8mb4;
START TRANSACTION;

ALTER TABLE `dcc_registration_certificate_access_request`
  DROP CHECK `chk_dcc_reg_cert_access_request_project`;

COMMIT;
