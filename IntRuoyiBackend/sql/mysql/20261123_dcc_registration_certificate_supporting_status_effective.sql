-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260818_dcc_registration_certificate_lifecycle; type=schema; riskLevel=medium
-- Supporting-document uploads take effect immediately. Align the status CHECK
-- with that behavior without changing historical materials or menu permissions.
-- Precondition: no PENDING_CONFIRMATION/CONFIRMED rows remain; MySQL rejects
-- this atomic ALTER if the existing data does not satisfy the target constraint.
ALTER TABLE `dcc_registration_certificate_supporting_document`
  DROP CHECK `chk_dcc_reg_cert_support_status`,
  ADD CONSTRAINT `chk_dcc_reg_cert_support_status`
    CHECK (`status` IN ('EFFECTIVE', 'REJECTED', 'VOIDED'));
