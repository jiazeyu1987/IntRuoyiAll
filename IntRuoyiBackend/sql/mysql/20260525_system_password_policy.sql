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

CALL intruoyi_add_system_users_column('password_update_time', 'datetime NULL DEFAULT NULL COMMENT ''密码最后更新时间'' AFTER `password`');

DROP PROCEDURE IF EXISTS intruoyi_add_system_users_column;

UPDATE `system_users`
SET `password_update_time` = COALESCE(`update_time`, `create_time`, NOW())
WHERE `password_update_time` IS NULL;
