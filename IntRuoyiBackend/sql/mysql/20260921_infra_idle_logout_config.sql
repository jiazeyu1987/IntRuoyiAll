-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=config; riskLevel=low
-- Seed the admin-maintained idle logout duration. Missing or invalid values must fail fast in frontend/backend validation.

SET NAMES utf8mb4;
START TRANSACTION;

DROP PROCEDURE IF EXISTS ensure_infra_idle_logout_config_20260921;
DELIMITER //
CREATE PROCEDURE ensure_infra_idle_logout_config_20260921()
BEGIN
  IF (
    SELECT COUNT(*)
      FROM information_schema.tables
     WHERE table_schema = DATABASE()
       AND table_name = 'infra_config'
  ) <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing infra_config table';
  END IF;

  IF (
    SELECT COUNT(*)
      FROM infra_config
     WHERE config_key = 'system.login.idle-timeout-minutes'
       AND deleted = b'0'
  ) > 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Duplicate idle logout config';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM infra_config
     WHERE config_key = 'system.login.idle-timeout-minutes'
       AND deleted = b'0'
       AND (
         value IS NULL
         OR value NOT REGEXP '^[1-9][0-9]{0,3}$'
         OR CAST(value AS UNSIGNED) < 1
         OR CAST(value AS UNSIGNED) > 1440
       )
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid idle logout config value';
  END IF;

  INSERT INTO infra_config (
    `category`, `type`, `name`, `config_key`, `value`, `visible`, `remark`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
  )
  SELECT
    'login-security', 1, '后台登录-空闲自动退出分钟数',
    'system.login.idle-timeout-minutes', '15', b'1',
    '单位：分钟；允许 1 到 1440 的整数。认证布局按该值控制无操作自动退出。',
    '20260921-configurable-idle-logout', NOW(),
    '20260921-configurable-idle-logout', NOW(), b'0'
  FROM DUAL
  WHERE NOT EXISTS (
    SELECT 1
      FROM infra_config
     WHERE config_key = 'system.login.idle-timeout-minutes'
       AND deleted = b'0'
  );
END//
DELIMITER ;

CALL ensure_infra_idle_logout_config_20260921();

DROP PROCEDURE IF EXISTS ensure_infra_idle_logout_config_20260921;

COMMIT;
