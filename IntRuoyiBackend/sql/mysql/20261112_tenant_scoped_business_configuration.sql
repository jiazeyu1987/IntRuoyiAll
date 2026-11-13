-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261112_tenant_kingdee_configuration; type=data; riskLevel=high
-- Requires 20261112_tenant_kingdee_configuration.sql. Snapshot once; never overwrite tenant edits.
-- Platform startup/audit and deployment parameters remain in infra_config, outside tenant CRUD.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS seed_tenant_business_configuration;
DELIMITER //
CREATE PROCEDURE seed_tenant_business_configuration()
BEGIN
  IF EXISTS (SELECT config_key FROM infra_config WHERE deleted=b'0'
      AND config_key <> 'system.gxp.audit-mode' AND config_key NOT LIKE 'runtime-control.%'
      GROUP BY config_key HAVING COUNT(*) > 1) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='TENANT_BUSINESS_DUPLICATE_SOURCE_CONFIGURATION';
  END IF;
  INSERT INTO infra_tenant_config
    (tenant_id, category, type, name, config_key, value, visible, remark, creator, updater)
  SELECT t.id, c.category, c.type, c.name, c.config_key, c.value, c.visible, c.remark, c.creator, c.updater
  FROM system_tenant t CROSS JOIN infra_config c
  WHERE t.deleted=b'0' AND c.deleted=b'0'
    AND c.config_key <> 'system.gxp.audit-mode' AND c.config_key NOT LIKE 'runtime-control.%'
    AND NOT EXISTS (SELECT 1 FROM infra_tenant_config x
                    WHERE x.tenant_id=t.id AND BINARY x.config_key=BINARY c.config_key);
END//
DELIMITER ;
CALL seed_tenant_business_configuration();
DROP PROCEDURE IF EXISTS seed_tenant_business_configuration;
