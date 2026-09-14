-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=config; riskLevel=medium
DROP PROCEDURE IF EXISTS intruoyi_add_system_users_column;
DELIMITER $$
CREATE PROCEDURE intruoyi_add_system_users_column(
    IN p_column_name varchar(64),
    IN p_column_definition varchar(1000)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = 'system_users'
           AND column_name = p_column_name
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `system_users` ADD COLUMN `', p_column_name, '` ', p_column_definition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL intruoyi_add_system_users_column('login_failure_count', 'int NOT NULL DEFAULT 0 COMMENT ''登录失败次数'' AFTER `status`');
CALL intruoyi_add_system_users_column('login_locked', 'tinyint(1) NOT NULL DEFAULT 0 COMMENT ''登录锁定标记（0未锁定 1锁定）'' AFTER `login_failure_count`');
CALL intruoyi_add_system_users_column('login_locked_time', 'datetime NULL DEFAULT NULL COMMENT ''登录锁定时间'' AFTER `login_locked`');

DROP PROCEDURE IF EXISTS intruoyi_add_system_users_column;

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 21, '账号已锁定', '21', 'system_login_result', 0, 'warning', '', '登陆结果 - 账号已锁定', '', NOW(), '', NOW(), b'0'
WHERE NOT EXISTS (
    SELECT 1
      FROM `system_dict_data`
     WHERE `dict_type` = 'system_login_result'
       AND `value` = '21'
);
