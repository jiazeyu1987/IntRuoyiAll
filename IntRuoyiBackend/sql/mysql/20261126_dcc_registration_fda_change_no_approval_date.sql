-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260818_dcc_registration_certificate_lifecycle; type=schema; riskLevel=medium
-- FDA has no change approval date; domestic and CE remain mandatory in the service.
SET NAMES utf8mb4;
ALTER TABLE dcc_registration_certificate_change MODIFY COLUMN approval_date date NULL COMMENT 'Domestic/CE change approval date; FDA not applicable';
