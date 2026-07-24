-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- ERP Kingdee 自动同步运行基础设施。
-- 设计边界：只记录同步水位和运行状态；业务对象 upsert 由各同步服务独立处理。

CREATE TABLE IF NOT EXISTS `erp_kingdee_sync_watermark` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `sync_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '同步类型',
  `last_success_time` datetime NULL DEFAULT NULL COMMENT '最近成功水位时间',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_erp_kingdee_sync_watermark_type` (`tenant_id` ASC, `sync_type` ASC, `deleted` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'ERP Kingdee 同步水位';

CREATE TABLE IF NOT EXISTS `erp_kingdee_sync_run` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `sync_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '同步类型',
  `trigger_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '触发类型',
  `status` tinyint NOT NULL COMMENT '运行状态',
  `window_start_time` datetime NULL DEFAULT NULL COMMENT '窗口开始时间',
  `window_end_time` datetime NULL DEFAULT NULL COMMENT '窗口结束时间',
  `started_at` datetime NOT NULL COMMENT '开始时间',
  `ended_at` datetime NULL DEFAULT NULL COMMENT '结束时间',
  `created_count` int NOT NULL DEFAULT 0 COMMENT '新增数量',
  `updated_count` int NOT NULL DEFAULT 0 COMMENT '更新数量',
  `skipped_count` int NOT NULL DEFAULT 0 COMMENT '跳过数量',
  `failed_count` int NOT NULL DEFAULT 0 COMMENT '失败数量',
  `failure_message` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '失败信息',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_erp_kingdee_sync_run_type_status` (`tenant_id` ASC, `sync_type` ASC, `status` ASC, `deleted` ASC) USING BTREE,
  INDEX `idx_erp_kingdee_sync_run_create_time` (`tenant_id` ASC, `create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'ERP Kingdee 同步运行记录';
