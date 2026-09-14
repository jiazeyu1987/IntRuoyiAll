-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260612_erp_kingdee_sync_runtime; type=schema; riskLevel=medium
-- 20260805_erp_kingdee_table_auto_sync
-- ERP 表格自动同步：新增租户级 Kingdee/ERP 拉取同步计划和全局 dispatcher Job。
-- 设计边界：只保存自动触发配置；实际同步继续复用现有 Kingdee JobHandler、erp_kingdee_sync_run 和 erp_kingdee_sync_watermark。

CREATE TABLE IF NOT EXISTS `erp_kingdee_table_auto_sync_plan` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  `enabled` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否启用',
  `daily_start_time` time NULL DEFAULT NULL COMMENT '每日开始时间',
  `cron_expression` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '业务计划 CRON 表达式',
  `job_id` bigint NULL DEFAULT NULL COMMENT '关联 infra_job 编号',
  `last_auto_run_date` date NULL DEFAULT NULL COMMENT '最近自动执行日期',
  `last_run_time` datetime NULL DEFAULT NULL COMMENT '最近执行时间',
  `last_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '最近执行状态',
  `last_message` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '最近执行信息',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_erp_kingdee_table_auto_sync_plan_tenant` (`tenant_id` ASC, `deleted` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'ERP 表格自动同步计划';

CREATE TABLE IF NOT EXISTS `erp_kingdee_table_auto_sync_plan_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  `plan_id` bigint NOT NULL COMMENT '计划编号',
  `sync_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'ERP 同步类型',
  `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_erp_kingdee_table_auto_sync_plan_item_type` (`plan_id` ASC, `sync_type` ASC, `deleted` ASC) USING BTREE,
  INDEX `idx_erp_kingdee_table_auto_sync_plan_item_plan` (`tenant_id` ASC, `plan_id` ASC, `sort_order` ASC, `deleted` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'ERP 表格自动同步计划明细';

INSERT INTO `infra_job` (`id`, `name`, `status`, `handler_name`, `handler_param`, `cron_expression`, `retry_count`, `retry_interval`, `monitor_timeout`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 5609, 'ERP 表格自动同步 Job', 2, 'erpKingdeeTableAutoSyncJob', '', '0 * * * * ?', 0, 0, 0, '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `infra_job`
  WHERE (`id` = 5609 OR `handler_name` = 'erpKingdeeTableAutoSyncJob')
    AND `deleted` = b'0'
);

UPDATE `infra_job`
SET `name` = 'ERP 表格自动同步 Job',
    `handler_name` = 'erpKingdeeTableAutoSyncJob',
    `handler_param` = '',
    `cron_expression` = '0 * * * * ?',
    `retry_count` = 0,
    `retry_interval` = 0,
    `monitor_timeout` = 0,
    `updater` = '1',
    `update_time` = NOW()
WHERE (`id` = 5609 OR `handler_name` = 'erpKingdeeTableAutoSyncJob')
  AND `deleted` = b'0';
