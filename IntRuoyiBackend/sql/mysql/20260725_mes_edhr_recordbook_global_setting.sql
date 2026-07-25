-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260721_mes_edhr_golden_finger_admin_permission; type=config-seed; riskLevel=medium
-- eDHR global recordbook runtime switch. Missing or invalid runtime config must fail fast in backend.

SET NAMES utf8mb4;
START TRANSACTION;

DROP PROCEDURE IF EXISTS ensure_mes_edhr_recordbook_global_setting_20260725;
DELIMITER //
CREATE PROCEDURE ensure_mes_edhr_recordbook_global_setting_20260725()
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
     WHERE config_key = 'mes.edhr.recordbook.global.enabled'
       AND deleted = b'0'
  ) > 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Duplicate eDHR recordbook global setting config';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM infra_config
     WHERE config_key = 'mes.edhr.recordbook.global.enabled'
       AND deleted = b'0'
       AND value NOT IN ('true', 'false')
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid eDHR recordbook global setting config value';
  END IF;

  INSERT INTO infra_config (
    `category`, `type`, `name`, `config_key`, `value`, `visible`, `remark`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
  )
  SELECT
    'mes', 1, 'eDHR 记录本全局开关',
    'mes.edhr.recordbook.global.enabled', 'true', b'1',
    '金手指专用全局开关；关闭后所有用户只能走批记录流程，记录本入口和写入被运行态门禁禁止。',
    '20260725-edhr-recordbook-global-setting', NOW(),
    '20260725-edhr-recordbook-global-setting', NOW(), b'0'
  FROM DUAL
  WHERE NOT EXISTS (
    SELECT 1
      FROM infra_config
     WHERE config_key = 'mes.edhr.recordbook.global.enabled'
       AND deleted = b'0'
  );
END//
DELIMITER ;

CALL ensure_mes_edhr_recordbook_global_setting_20260725();

DROP PROCEDURE IF EXISTS ensure_mes_edhr_recordbook_global_setting_20260725;

COMMIT;
