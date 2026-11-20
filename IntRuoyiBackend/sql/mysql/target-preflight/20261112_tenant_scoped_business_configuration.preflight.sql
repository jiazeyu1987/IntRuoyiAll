-- release-target-preflight: migrationId=20261112_tenant_scoped_business_configuration; allowedEnvironments=test,backup,prod
-- Read-only baseline predicates from this migration; mutation-time guards remain authoritative.
SELECT CASE
 WHEN (EXISTS (SELECT config_key FROM infra_config WHERE deleted=b'0'
      AND config_key <> 'system.gxp.audit-mode' AND config_key NOT LIKE 'runtime-control.%'
      GROUP BY config_key HAVING COUNT(*) > 1)) THEN 'TARGET_PREFLIGHT_BLOCKED:20261112_tenant_scoped_business_configuration:CONDITION_1'
 ELSE 'TARGET_PREFLIGHT_PASS:20261112_tenant_scoped_business_configuration' END;
