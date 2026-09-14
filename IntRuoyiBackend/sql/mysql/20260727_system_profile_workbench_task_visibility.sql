-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260708_system_user_table_column_config; type=schema; riskLevel=medium
CREATE TABLE IF NOT EXISTS `system_profile_workbench_task_visibility` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `user_id` bigint NOT NULL COMMENT '用户编号',
    `task_key` varchar(160) NOT NULL COMMENT '个人工作台任务唯一 Key',
    `task_type` varchar(64) NOT NULL COMMENT '任务类型',
    `source` varchar(64) NOT NULL COMMENT '任务来源',
    `business_id` varchar(80) DEFAULT NULL COMMENT '业务编号',
    `detail` varchar(500) DEFAULT NULL COMMENT '任务摘要',
    `hidden_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '隐藏时间',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_system_profile_workbench_task_visibility` (`tenant_id`, `user_id`, `task_key`, `deleted`),
    KEY `idx_system_profile_workbench_task_visibility_user` (`tenant_id`, `user_id`, `hidden_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='个人工作台任务隐藏表';
