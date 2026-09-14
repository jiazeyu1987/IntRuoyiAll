-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- 统一电子签名 T1：账户密码状态、密码历史和永久用户名唯一。
-- 发布前置：若同一租户内历史或当前用户名已重复，本迁移 fail fast，不自动合并账户。

DROP PROCEDURE IF EXISTS intruoyi_add_system_users_esign_t1_column;

DELIMITER $$
CREATE PROCEDURE intruoyi_add_system_users_esign_t1_column(
    IN p_column_name varchar(64),
    IN p_column_definition varchar(512)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.COLUMNS
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

CALL intruoyi_add_system_users_esign_t1_column('password_credential_status',
    'varchar(32) NOT NULL DEFAULT ''ACTIVE'' COMMENT ''密码凭据状态：ACTIVE可用 INITIAL初始凭据 RESET_REQUIRED重置后必须改密'' AFTER `password_update_time`'
);

CALL intruoyi_add_system_users_esign_t1_column('canonical_username',
    'varchar(64) NULL COMMENT ''规范化用户账号：trim + lower-case + Unicode NFC'' AFTER `username`'
);

UPDATE `system_users`
   SET `canonical_username` = LOWER(TRIM(`username`))
 WHERE `canonical_username` IS NULL
    OR `canonical_username` = '';

DROP PROCEDURE IF EXISTS intruoyi_add_system_users_esign_t1_column;

CREATE TABLE IF NOT EXISTS `system_user_password_history` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `user_id` bigint NOT NULL COMMENT '用户编号',
    `password_hash` varchar(100) NOT NULL COMMENT '历史密码哈希',
    `changed_at` datetime NOT NULL COMMENT '变更时间',
    `source_type` varchar(32) NOT NULL COMMENT '来源类型',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_system_user_password_history_user` (`tenant_id`, `user_id`, `changed_at`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户密码历史表';

DROP PROCEDURE IF EXISTS intruoyi_add_system_users_tenant_username_permanent_index;

DELIMITER $$
CREATE PROCEDURE intruoyi_add_system_users_tenant_username_permanent_index()
BEGIN
    IF EXISTS (
        SELECT 1
          FROM (
                SELECT `tenant_id`, `canonical_username`, COUNT(*) AS duplicate_count
                  FROM `system_users`
                 GROUP BY `tenant_id`, `canonical_username`
                HAVING COUNT(*) > 1
               ) duplicate_users
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'system_users contains duplicate tenant canonical username values; resolve identities before adding permanent unique index';
    END IF;

    ALTER TABLE `system_users`
        MODIFY COLUMN `canonical_username` varchar(64) NOT NULL COMMENT '规范化用户账号：trim + lower-case + Unicode NFC';

    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.STATISTICS
         WHERE table_schema = DATABASE()
           AND table_name = 'system_users'
           AND index_name = 'uk_system_users_tenant_canonical_username'
    ) THEN
        ALTER TABLE `system_users`
            ADD UNIQUE KEY `uk_system_users_tenant_canonical_username` (`tenant_id`, `canonical_username`);
    END IF;
END$$
DELIMITER ;

CALL intruoyi_add_system_users_tenant_username_permanent_index();

DROP PROCEDURE IF EXISTS intruoyi_add_system_users_tenant_username_permanent_index;
