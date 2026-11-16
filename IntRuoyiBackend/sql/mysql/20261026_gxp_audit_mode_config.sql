-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=config; riskLevel=high
-- Before first execution, explicitly SET @intruoyi_gxp_audit_initial_mode to the existing deployment's mode.
-- FORMAL / LOCAL_POLICY / DEVELOPMENT_DISABLED. No implicit mode change or environment fallback.
-- Reuses infra:config:query/update and the existing basic configuration page; no new grants.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS ensure_gxp_audit_mode_20261026;
DELIMITER //
CREATE PROCEDURE ensure_gxp_audit_mode_20261026()
BEGIN
  IF (SELECT COUNT(*) FROM infra_config
      WHERE config_key = 'system.gxp.audit-mode' AND deleted = b'0') > 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Duplicate GxP audit mode config';
  END IF;
  IF EXISTS (SELECT 1 FROM infra_config
      WHERE config_key = 'system.gxp.audit-mode' AND deleted = b'0'
        AND (value IS NULL OR BINARY value NOT IN ('FORMAL', 'LOCAL_POLICY', 'DEVELOPMENT_DISABLED')
             OR visible IS NULL OR visible <> b'1' OR type IS NULL OR type <> 1)) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid GxP audit mode config';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM infra_config
      WHERE config_key = 'system.gxp.audit-mode' AND deleted = b'0') THEN
    IF @intruoyi_gxp_audit_initial_mode IS NULL
       OR BINARY @intruoyi_gxp_audit_initial_mode NOT IN ('FORMAL', 'LOCAL_POLICY', 'DEVELOPMENT_DISABLED') THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Explicit intruoyi_gxp_audit_initial_mode is required';
    END IF;
    INSERT INTO infra_config
      (`category`, `type`, `name`, `config_key`, `value`, `visible`, `remark`,
       `creator`, `create_time`, `updater`, `update_time`, `deleted`)
    VALUES ('gxp', 1, 'GxP审计模式', 'system.gxp.audit-mode', @intruoyi_gxp_audit_initial_mode,
      b'1', '所有租户共用；修改后各后端实例重启生效。本地模式受环境与数据源限制。',
      '20261026-gxp-mode', NOW(), '20261026-gxp-mode', NOW(), b'0');
  END IF;
END//
DELIMITER ;
CALL ensure_gxp_audit_mode_20261026();
DROP PROCEDURE IF EXISTS ensure_gxp_audit_mode_20261026;
