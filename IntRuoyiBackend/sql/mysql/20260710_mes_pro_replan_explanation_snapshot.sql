-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
CREATE TABLE IF NOT EXISTS `mes_pro_replan_explanation_snapshot` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `request_id` varchar(64) NOT NULL COMMENT '重排请求编号',
  `trigger_source` varchar(32) NOT NULL COMMENT '触发来源：MANUAL/NIGHTLY',
  `capacity_mode` varchar(32) NOT NULL COMMENT '产能模式',
  `reason` varchar(500) NULL COMMENT '重排原因',
  `operator_id` bigint NULL COMMENT '操作人编号',
  `operator_name` varchar(64) NULL COMMENT '操作人名称',
  `request_start_time` datetime NOT NULL COMMENT '排产开始时间',
  `applied_at` datetime NOT NULL COMMENT '成功应用时间',
  `snapshot_json` longtext NOT NULL COMMENT '重排说明快照',
  `creator` varchar(64) NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_mes_pro_replan_explanation_request` (`tenant_id`, `request_id`, `deleted`) USING BTREE,
  KEY `idx_mes_pro_replan_explanation_latest` (`tenant_id`, `applied_at` DESC, `id` DESC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'MES 成功重排说明快照';
