-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260827_system_user_login_lock; type=schema; riskLevel=medium
-- 设计边界：账号离职/转岗停用必须写入 system_users 正式字段，并由 userLifecycleDeactivateJob 按单据生效时间处理。

DROP PROCEDURE IF EXISTS intruoyi_add_system_users_lifecycle_column;
DELIMITER $$
CREATE PROCEDURE intruoyi_add_system_users_lifecycle_column(
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

CALL intruoyi_add_system_users_lifecycle_column('lifecycle_document_type', 'varchar(32) NULL DEFAULT NULL COMMENT ''账号生命周期单据类型：RESIGNATION离职单 TRANSFER转岗单'' AFTER `login_locked_time`');
CALL intruoyi_add_system_users_lifecycle_column('lifecycle_document_no', 'varchar(64) NULL DEFAULT NULL COMMENT ''离职/转岗单号'' AFTER `lifecycle_document_type`');
CALL intruoyi_add_system_users_lifecycle_column('lifecycle_document_time', 'datetime NULL DEFAULT NULL COMMENT ''离职/转岗单据时间'' AFTER `lifecycle_document_no`');
CALL intruoyi_add_system_users_lifecycle_column('lifecycle_effective_time', 'datetime NULL DEFAULT NULL COMMENT ''账号停用生效时间'' AFTER `lifecycle_document_time`');
CALL intruoyi_add_system_users_lifecycle_column('lifecycle_deactivated_time', 'datetime NULL DEFAULT NULL COMMENT ''账号联动停用时间，应与离职/转岗单生效时间一致'' AFTER `lifecycle_effective_time`');

DROP PROCEDURE IF EXISTS intruoyi_add_system_users_lifecycle_column;

DROP PROCEDURE IF EXISTS intruoyi_add_system_users_lifecycle_index;
DELIMITER $$
CREATE PROCEDURE intruoyi_add_system_users_lifecycle_index()
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.statistics
         WHERE table_schema = DATABASE()
           AND table_name = 'system_users'
           AND index_name = 'idx_system_users_lifecycle_due'
    ) THEN
        ALTER TABLE `system_users`
            ADD INDEX `idx_system_users_lifecycle_due` (`tenant_id`, `lifecycle_deactivated_time`, `lifecycle_effective_time`, `deleted`);
    END IF;
END$$
DELIMITER ;

CALL intruoyi_add_system_users_lifecycle_index();

DROP PROCEDURE IF EXISTS intruoyi_add_system_users_lifecycle_index;

INSERT INTO `infra_job`
  (`name`, `status`, `handler_name`, `handler_param`, `cron_expression`,
   `retry_count`, `retry_interval`, `monitor_timeout`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '每分钟处理用户离职/转岗到期停用', 1, 'userLifecycleDeactivateJob', '{"limit":200}', '0 * * * * ?',
       3, 60, 0, '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (
    SELECT 1
      FROM `infra_job`
     WHERE `handler_name` = 'userLifecycleDeactivateJob'
);

UPDATE `infra_job`
   SET `name` = '每分钟处理用户离职/转岗到期停用',
       `status` = 1,
       `handler_name` = 'userLifecycleDeactivateJob',
       `handler_param` = '{"limit":200}',
       `cron_expression` = '0 * * * * ?',
       `retry_count` = 3,
       `retry_interval` = 60,
       `monitor_timeout` = 0,
       `updater` = '1',
       `update_time` = NOW(),
       `deleted` = b'0'
 WHERE `handler_name` = 'userLifecycleDeactivateJob';
