-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=config; riskLevel=low
-- Reuses infra:config:query/update; no new menu or permission. Controls UI visibility only.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS ensure_infra_test_mode_20261021;
DELIMITER //
CREATE PROCEDURE ensure_infra_test_mode_20261021()
BEGIN
  IF (SELECT COUNT(*) FROM infra_config
      WHERE config_key = 'system.ui.test-mode-enabled' AND deleted = b'0') > 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Duplicate test mode config';
  END IF;
  IF EXISTS (SELECT 1 FROM infra_config
      WHERE config_key = 'system.ui.test-mode-enabled' AND deleted = b'0'
        AND (value IS NULL OR BINARY value NOT IN ('true', 'false') OR visible <> b'1')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid or invisible test mode config';
  END IF;
  INSERT INTO infra_config
    (`category`, `type`, `name`, `config_key`, `value`, `visible`, `remark`,
     `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 'ui', 1, '测试模式', 'system.ui.test-mode-enabled', 'false', b'1',
    '控制测试、模拟、测试数据清理入口显隐；不替代业务权限校验。',
    '20261021-test-mode', NOW(), '20261021-test-mode', NOW(), b'0'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM infra_config
    WHERE config_key = 'system.ui.test-mode-enabled' AND deleted = b'0');
END//
DELIMITER ;
CALL ensure_infra_test_mode_20261021();
DROP PROCEDURE IF EXISTS ensure_infra_test_mode_20261021;
