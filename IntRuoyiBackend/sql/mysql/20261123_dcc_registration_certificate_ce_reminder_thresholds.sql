-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260818_dcc_registration_certificate_reminder; type=schema; riskLevel=medium
-- CE reminders have independent recipients. Existing recipient configuration is deliberately unchanged.
ALTER TABLE `dcc_registration_certificate_reminder_occurrence`
  DROP CHECK `chk_dcc_reg_cert_reminder_occurrence_threshold`;
ALTER TABLE `dcc_registration_certificate_reminder_occurrence`
  ADD CONSTRAINT `chk_dcc_reg_cert_reminder_occurrence_threshold` CHECK (`threshold_level` IN
    ('T_30', 'T_8', 'T_2', 'T_1', 'CE_T_18', 'CE_T_12', 'CE_T_3'));
