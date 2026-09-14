-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260908_system_signature_password_t1; type=schema; riskLevel=medium
-- T2：统一登录/签名失败计数窗口与锁定时长
-- 说明：只补充认证锁定窗口字段；失败计数、锁定和自动解锁判断由服务端统一执行。

DROP PROCEDURE IF EXISTS intruoyi_add_system_users_esign_t2_column;

DELIMITER $$
CREATE PROCEDURE intruoyi_add_system_users_esign_t2_column(
    IN p_column_name varchar(64),
    IN p_column_definition text
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = 'system_users'
           AND column_name = p_column_name
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `system_users` ADD COLUMN `', p_column_name, '` ', p_column_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL intruoyi_add_system_users_esign_t2_column('login_failure_window_start_time',
    'datetime DEFAULT NULL COMMENT ''登录/签名失败计数窗口开始时间'' AFTER `login_failure_count`');

DROP PROCEDURE IF EXISTS intruoyi_add_system_users_esign_t2_column;
