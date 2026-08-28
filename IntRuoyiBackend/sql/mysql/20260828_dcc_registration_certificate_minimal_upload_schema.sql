-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260817_dcc_registration_certificate_core,20260818_dcc_registration_certificate_access; type=schema; riskLevel=medium
-- 2026-08-28: registration-certificate upload only collects the user-approved visible fields.
-- The snapshot production relation remains explicitly empty until a later approved change supplies production facts.

ALTER TABLE `dcc_registration_certificate_access_request`
  DROP CHECK `chk_dcc_reg_cert_access_request_type`;

ALTER TABLE `dcc_registration_certificate_access_request`
  ADD CONSTRAINT `chk_dcc_reg_cert_access_request_type` CHECK (
    `request_type` IN ('VIEW_OLD_CERTIFICATE', 'DOWNLOAD_FILE', 'UPLOAD_CERTIFICATE')
  );

ALTER TABLE `dcc_registration_certificate_snapshot`
  DROP CHECK `chk_dcc_reg_cert_production_relation`;

ALTER TABLE `dcc_registration_certificate_snapshot`
  ADD CONSTRAINT `chk_dcc_reg_cert_production_relation` CHECK (
    JSON_TYPE(`entrusted_enterprises_json`) = 'ARRAY'
    AND (
      (
        `entrusted_production` = b'0'
        AND `self_production` = b'0'
        AND `entrusted_enterprise_count` = 0
      )
      OR (
        (`entrusted_production` = b'1' OR `self_production` = b'1')
        AND (
          (`entrusted_production` = b'1' AND `entrusted_enterprise_count` >= 1)
          OR (`entrusted_production` = b'0' AND `entrusted_enterprise_count` = 0)
        )
      )
    )
  );
