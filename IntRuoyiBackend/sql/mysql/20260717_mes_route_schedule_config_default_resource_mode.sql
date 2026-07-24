-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260715_mes_schedule_capacity_mode_unification; type=schema; riskLevel=low
-- 将路线工序排产配置默认产能模式固定为资源计算；仅调整列默认值和小时产能注释，不改写存量业务数据。

ALTER TABLE `mes_pro_route_schedule_config`
    MODIFY COLUMN `capacity_mode` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
        NOT NULL DEFAULT 'RESOURCE_CALCULATED'
        COMMENT '产能模式：RESOURCE_CALCULATED/MANUAL_OVERRIDE/INFINITE_FORMULA',
    MODIFY COLUMN `hourly_capacity` decimal(18,6) NULL DEFAULT NULL COMMENT '产能覆盖每小时产能';
