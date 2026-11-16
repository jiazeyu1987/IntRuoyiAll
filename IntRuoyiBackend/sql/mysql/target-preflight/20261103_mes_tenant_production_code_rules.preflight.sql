-- release-target-preflight: migrationId=20261103_mes_tenant_production_code_rules; allowedEnvironments=test,backup,prod
-- Reject existing conflicting data; absent seed rows are created by the migration.
SELECT CASE
 WHEN ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
 AND TABLE_NAME IN ('mes_md_auto_code_rule','mes_md_auto_code_part','system_tenant') AND TABLE_TYPE='BASE TABLE')<>3) THEN 'TARGET_PREFLIGHT_BLOCKED:20261103_mes_tenant_production_code_rules:CONDITION_2'
 ELSE 'TARGET_PREFLIGHT_PASS:20261103_mes_tenant_production_code_rules' END;
