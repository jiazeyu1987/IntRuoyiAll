-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260612_erp_kingdee_sync_runtime; type=schema; riskLevel=medium
-- 20260805_erp_nas_table_auto_sync
-- NAS 表格自动同步：新增租户级 ERP 表导出计划、明细、运行日志和全局调度 Job。
-- 设计边界：业务配置只存 erp_nas_table_sync_* 表；infra_job 只保存全局 handler，不保存表选择、NAS 目录或其它业务配置。

CREATE TABLE IF NOT EXISTS `erp_nas_table_sync_plan` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  `enabled` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否启用',
  `daily_start_time` time NULL DEFAULT NULL COMMENT '每日开始时间',
  `cron_expression` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '业务计划 CRON 表达式',
  `nas_directory` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'NAS 相对目录',
  `file_name_pattern` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '文件名规则',
  `job_id` bigint NULL DEFAULT NULL COMMENT '关联 infra_job 编号',
  `last_run_id` bigint NULL DEFAULT NULL COMMENT '最近运行编号',
  `last_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '最近运行状态',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_erp_nas_table_sync_plan_tenant` (`tenant_id` ASC, `deleted` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'ERP NAS 表格自动同步计划';

CREATE TABLE IF NOT EXISTS `erp_nas_table_sync_plan_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  `plan_id` bigint NOT NULL COMMENT '计划编号',
  `sync_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'ERP 表类型',
  `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序',
  `sheet_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Sheet 名称',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_erp_nas_table_sync_plan_item_type` (`plan_id` ASC, `sync_type` ASC, `deleted` ASC) USING BTREE,
  INDEX `idx_erp_nas_table_sync_plan_item_plan` (`tenant_id` ASC, `plan_id` ASC, `sort_order` ASC, `deleted` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'ERP NAS 表格自动同步计划明细';

CREATE TABLE IF NOT EXISTS `erp_nas_table_sync_run` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  `plan_id` bigint NOT NULL COMMENT '计划编号',
  `trigger_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '触发类型',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '运行状态',
  `started_at` datetime NOT NULL COMMENT '开始时间',
  `ended_at` datetime NULL DEFAULT NULL COMMENT '结束时间',
  `output_path` varchar(700) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '输出 NAS 路径',
  `total_table_count` int NOT NULL DEFAULT 0 COMMENT '总表数',
  `success_table_count` int NOT NULL DEFAULT 0 COMMENT '成功表数',
  `failed_table_count` int NOT NULL DEFAULT 0 COMMENT '失败表数',
  `failure_message` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '失败信息',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_erp_nas_table_sync_run_plan_started` (`tenant_id` ASC, `plan_id` ASC, `started_at` ASC, `deleted` ASC) USING BTREE,
  INDEX `idx_erp_nas_table_sync_run_status` (`tenant_id` ASC, `status` ASC, `deleted` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'ERP NAS 表格自动同步运行记录';

CREATE TABLE IF NOT EXISTS `erp_nas_table_sync_run_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  `run_id` bigint NOT NULL COMMENT '运行编号',
  `sync_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'ERP 表类型',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '运行状态',
  `sheet_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Sheet 名称',
  `row_count` int NOT NULL DEFAULT 0 COMMENT '行数',
  `failure_message` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '失败信息',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_erp_nas_table_sync_run_item_type` (`run_id` ASC, `sync_type` ASC, `deleted` ASC) USING BTREE,
  INDEX `idx_erp_nas_table_sync_run_item_run` (`tenant_id` ASC, `run_id` ASC, `deleted` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'ERP NAS 表格自动同步运行明细';

INSERT INTO `infra_job` (`id`, `name`, `status`, `handler_name`, `handler_param`, `cron_expression`, `retry_count`, `retry_interval`, `monitor_timeout`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 5608, 'NAS 表格自动同步 Job', 2, 'erpNasTableAutoSyncJob', '', '0 * * * * ?', 0, 0, 0, '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `infra_job`
  WHERE (`id` = 5608 OR `handler_name` = 'erpNasTableAutoSyncJob')
    AND `deleted` = b'0'
);

UPDATE `infra_job`
SET `name` = 'NAS 表格自动同步 Job',
    `handler_name` = 'erpNasTableAutoSyncJob',
    `handler_param` = '',
    `cron_expression` = '0 * * * * ?',
    `retry_count` = 0,
    `retry_interval` = 0,
    `monitor_timeout` = 0,
    `updater` = '1',
    `update_time` = NOW()
WHERE (`id` = 5608 OR `handler_name` = 'erpNasTableAutoSyncJob')
  AND `deleted` = b'0';
