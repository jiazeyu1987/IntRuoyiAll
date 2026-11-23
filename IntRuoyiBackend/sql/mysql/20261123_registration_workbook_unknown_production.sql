-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261030_dcc_registration_certificate_regulatory_facts; type=schema; riskLevel=medium
-- Historical workbooks may contain no verified production relationship.
ALTER TABLE dcc_registration_certificate_snapshot
  DROP CHECK chk_dcc_reg_cert_production_relation,
  MODIFY COLUMN entrusted_production BIT(1) NULL COMMENT '是否委托生产，历史资料未确认时为空',
  MODIFY COLUMN self_production BIT(1) NULL COMMENT '是否自行生产，历史资料未确认时为空',
  ADD CONSTRAINT chk_dcc_reg_cert_production_relation_with_history CHECK (
    JSON_TYPE(entrusted_enterprises_json) = 'ARRAY' AND (
      (entrusted_production IS NULL AND self_production IS NULL AND entrusted_enterprise_count = 0
        AND COALESCE(JSON_TYPE(JSON_EXTRACT(facts_json, '$.historicalSheet')) = 'STRING', FALSE))
      OR (entrusted_production IS NOT NULL AND self_production IS NOT NULL AND (
        (entrusted_production = b'0' AND entrusted_enterprise_count = 0)
        OR (entrusted_production = b'1' AND entrusted_enterprise_count >= 1)
      ))
    )
  );
