-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=low
-- Add one centrally managed tenant selection for fresh login pages.
-- Fail before changing the schema when the required initial tenant is absent.
DELIMITER $$
CREATE PROCEDURE `assert_initial_default_login_tenant_20261126`()
BEGIN
  IF (SELECT COUNT(*) FROM `system_tenant`
      WHERE `name` = '芋道源码' AND `deleted` = b'0' AND `status` = 0) <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Exactly one enabled initial tenant 芋道源码 is required';
  END IF;
END$$
DELIMITER ;
CALL `assert_initial_default_login_tenant_20261126`();
DROP PROCEDURE `assert_initial_default_login_tenant_20261126`;

ALTER TABLE `system_tenant`
  ADD COLUMN `default_login` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否为登录默认租户';

UPDATE `system_tenant`
SET `default_login` = b'1'
WHERE `name` = '芋道源码' AND `deleted` = b'0';
