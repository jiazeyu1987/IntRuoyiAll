-- release-migration: allowedEnvironments=test,backup,prod; type=schema; riskLevel=high
-- Snapshot existing explicit ERP settings once per tenant. Never read global ERP settings at runtime.
-- Deploy the application and this migration together. Source configuration rows are retained.
-- Recovery requires retaining tenant snapshots; reverting to shared configuration loses isolation.
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS infra_tenant_config (
  id bigint NOT NULL AUTO_INCREMENT,
  tenant_id bigint NOT NULL,
  category varchar(50) NOT NULL,
  type tinyint NOT NULL,
  name varchar(100) NOT NULL,
  config_key varchar(100) NOT NULL,
  value longtext NOT NULL,
  visible bit(1) NOT NULL,
  remark varchar(500) DEFAULT NULL,
  creator varchar(64) DEFAULT '',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater varchar(64) DEFAULT '',
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted bit(1) NOT NULL DEFAULT b'0',
  active_config_key varchar(100) GENERATED ALWAYS AS (CASE WHEN deleted=b'0' THEN config_key ELSE NULL END) STORED,
  PRIMARY KEY (id),
  UNIQUE KEY uk_tenant_config_key (tenant_id, active_config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS seed_tenant_kingdee_configuration;
DELIMITER //
CREATE PROCEDURE seed_tenant_kingdee_configuration()
BEGIN
  IF (SELECT COUNT(DISTINCT config_key) FROM infra_config WHERE deleted=b'0'
      AND config_key IN ('yudao.erp.kingdee.config', 'yudao.erp.kingdee.connection.production',
                        'yudao.erp.kingdee.connection.active', 'yudao.erp.kingdee.external-write-enabled')) <> 4 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='TENANT_KINGDEE_EXPLICIT_SOURCE_CONFIGURATION_REQUIRED';
  END IF;
  IF EXISTS (SELECT config_key FROM infra_config WHERE deleted=b'0'
      AND config_key IN ('yudao.erp.kingdee.config', 'yudao.erp.kingdee.connection.production',
                        'yudao.erp.kingdee.connection.active', 'yudao.erp.kingdee.external-write-enabled')
      GROUP BY config_key HAVING COUNT(*) > 1) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='TENANT_KINGDEE_DUPLICATE_SOURCE_CONFIGURATION';
  END IF;
  INSERT INTO infra_tenant_config
    (tenant_id, category, type, name, config_key, value, visible, remark, creator, updater)
  SELECT t.id, c.category, c.type, c.name, c.config_key, c.value, c.visible, c.remark, c.creator, c.updater
  FROM system_tenant t CROSS JOIN infra_config c
  WHERE t.deleted=b'0' AND c.deleted=b'0'
    AND c.config_key IN ('yudao.erp.kingdee.config', 'yudao.erp.kingdee.connection.production',
                        'yudao.erp.kingdee.connection.active', 'yudao.erp.kingdee.external-write-enabled')
    AND NOT EXISTS (SELECT 1 FROM infra_tenant_config x
                    WHERE x.tenant_id=t.id AND BINARY x.config_key=BINARY c.config_key);
END//
DELIMITER ;
CALL seed_tenant_kingdee_configuration();
DROP PROCEDURE IF EXISTS seed_tenant_kingdee_configuration;
