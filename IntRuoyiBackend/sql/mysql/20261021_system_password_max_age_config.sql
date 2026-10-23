-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=config; riskLevel=low
-- 复用配置管理菜单及 infra:config:query/update 权限，不新增角色或租户套餐授权。
-- 必须在启用读取此配置的新后端前执行。已有合法配置保留原值，不重置用户密码更新时间。
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS ensure_password_max_age_20261021;
DELIMITER //
CREATE PROCEDURE ensure_password_max_age_20261021()
BEGIN
  IF (SELECT COUNT(*) FROM infra_config
      WHERE config_key = 'system.user.password-max-age-days' AND deleted = b'0') > 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Duplicate system password max age config';
  END IF;
  IF EXISTS (SELECT 1 FROM infra_config
      WHERE config_key = 'system.user.password-max-age-days' AND deleted = b'0'
        AND (value IS NULL OR value NOT REGEXP '^[1-9][0-9]{0,9}$' OR value REGEXP '[^0-9]'
             OR CAST(value AS DECIMAL(20, 0)) > 2147483647)) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid system password max age config';
  END IF;
  INSERT INTO infra_config
    (`category`, `type`, `name`, `config_key`, `value`, `visible`, `remark`,
     `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 'security', 1, '系统账号密码有效期（天）', 'system.user.password-max-age-days', '90', b'0',
    '所有租户的系统账号共用；从上次修改密码起计算。保存后在后续登录和系统账号签名校验时生效；缩短周期可能使部分账号立即过期。',
    '20261021-password-policy', NOW(), '20261021-password-policy', NOW(), b'0'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM infra_config
    WHERE config_key = 'system.user.password-max-age-days' AND deleted = b'0');

  UPDATE infra_config SET type = 1, updater = '20261021-password-policy', update_time = NOW()
  WHERE config_key = 'system.user.password-max-age-days' AND deleted = b'0' AND type <> 1;
END//
DELIMITER ;
CALL ensure_password_max_age_20261021();
DROP PROCEDURE IF EXISTS ensure_password_max_age_20261021;
