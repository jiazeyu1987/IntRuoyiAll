-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- MES 排产工单池 P1：排产工单、工序快照、ERP 差异基础表与菜单权限。
-- 设计边界：生产工单继续作为 ERP 镜像；排产工单承接承诺交期、优先级、数量和路线快照。

CREATE TABLE IF NOT EXISTS `mes_pro_schedule_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '排产工单编码',
  `work_order_id` bigint NOT NULL COMMENT '来源生产工单ID',
  `erp_work_order_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'ERP工单编码',
  `product_id` bigint NOT NULL COMMENT '产品ID',
  `quantity` decimal(18, 6) NOT NULL COMMENT '排产数量',
  `promise_date` date NOT NULL COMMENT '承诺交期',
  `priority_no` int NOT NULL DEFAULT 100 COMMENT '优先级排序',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态',
  `diff_status` tinyint NOT NULL DEFAULT 0 COMMENT 'ERP差异状态',
  `risk_status` tinyint NOT NULL DEFAULT 0 COMMENT '风险状态',
  `route_id` bigint NOT NULL COMMENT '工艺路线ID',
  `route_version` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '路线版本',
  `schedule_config_version` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '排产配置版本',
  `source_snapshot_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '来源生产工单快照',
  `route_snapshot_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '路线快照',
  `capacity_snapshot_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '产能快照',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_mes_pro_schedule_order_code_tenant` (`tenant_id` ASC, `code` ASC, `deleted` ASC) USING BTREE,
  UNIQUE INDEX `uk_mes_pro_schedule_order_work_order_tenant` (`tenant_id` ASC, `work_order_id` ASC, `deleted` ASC) USING BTREE,
  INDEX `idx_mes_pro_schedule_order_promise_date` (`tenant_id` ASC, `promise_date` ASC) USING BTREE,
  INDEX `idx_mes_pro_schedule_order_product_id` (`tenant_id` ASC, `product_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'MES 排产工单';

CREATE TABLE IF NOT EXISTS `mes_pro_schedule_order_process` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `schedule_order_id` bigint NOT NULL COMMENT '排产工单ID',
  `route_process_id` bigint NOT NULL COMMENT '路线工序ID',
  `process_id` bigint NOT NULL COMMENT '工序ID',
  `sort` int NULL DEFAULT NULL COMMENT '排序',
  `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否参与排产',
  `capacity_source` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '产能来源',
  `hourly_capacity_total` decimal(18, 6) NULL DEFAULT 0 COMMENT '快照小时总产能',
  `shift_hours` decimal(10, 2) NULL DEFAULT 10.50 COMMENT '快照班次小时数',
  `shift_capacity_total` decimal(18, 6) NULL DEFAULT 0 COMMENT '快照班次总产能',
  `production_quantity_factor` decimal(18,6) NOT NULL DEFAULT 1.000000 COMMENT '生产数量系数快照',
  `resource_snapshot_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '工序资源快照',
  `planned_quantity` decimal(18, 6) NOT NULL DEFAULT 0 COMMENT '计划数量',
  `reported_quantity` decimal(18, 6) NOT NULL DEFAULT 0 COMMENT '已归属报工数量',
  `remaining_quantity` decimal(18, 6) NOT NULL DEFAULT 0 COMMENT '剩余数量',
  `planned_start_time` datetime NULL DEFAULT NULL COMMENT '计划开始时间',
  `planned_end_time` datetime NULL DEFAULT NULL COMMENT '计划结束时间',
  `key_process_flag` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否关键工序',
  `bottleneck_flag` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否瓶颈',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_mes_pro_schedule_order_process_order` (`tenant_id` ASC, `schedule_order_id` ASC, `sort` ASC) USING BTREE,
  INDEX `idx_mes_pro_schedule_order_process_route_process` (`tenant_id` ASC, `route_process_id` ASC) USING BTREE,
  INDEX `idx_mes_pro_schedule_order_process_process` (`tenant_id` ASC, `process_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'MES 排产工单工序快照';

CREATE TABLE IF NOT EXISTS `mes_pro_schedule_order_diff` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `schedule_order_id` bigint NOT NULL COMMENT '排产工单ID',
  `work_order_id` bigint NOT NULL COMMENT '来源生产工单ID',
  `diff_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '差异类型',
  `old_value_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '旧值',
  `new_value_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '新值',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '处理状态',
  `resolved_by` bigint NULL DEFAULT NULL COMMENT '处理人',
  `resolved_time` datetime NULL DEFAULT NULL COMMENT '处理时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_mes_pro_schedule_order_diff_order` (`tenant_id` ASC, `schedule_order_id` ASC) USING BTREE,
  INDEX `idx_mes_pro_schedule_order_diff_work_order` (`tenant_id` ASC, `work_order_id` ASC) USING BTREE,
  INDEX `idx_mes_pro_schedule_order_diff_status` (`tenant_id` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'MES 排产工单 ERP 差异';

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 5580, '排产工单池', '', 2, 2, 5700, 'schedule-order', 'ep:operation', 'mes/pro/scheduleorder/index', 'MesProScheduleOrder', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 5580 OR (`parent_id` = 5700 AND `path` = 'schedule-order' AND `deleted` = b'0'));

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 5581, '排产工单查询', 'mes:pro-schedule-order:query', 3, 1, 5580, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 5580 AND `deleted` = b'0')
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 5581 OR (`parent_id` = 5580 AND `permission` = 'mes:pro-schedule-order:query' AND `deleted` = b'0'));

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 5582, '排产工单创建', 'mes:pro-schedule-order:create', 3, 2, 5580, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 5580 AND `deleted` = b'0')
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 5582 OR (`parent_id` = 5580 AND `permission` = 'mes:pro-schedule-order:create' AND `deleted` = b'0'));

UPDATE `system_menu`
SET `name` = '排产工单池',
    `permission` = '',
    `type` = 2,
    `sort` = 2,
    `parent_id` = 5700,
    `path` = 'schedule-order',
    `icon` = 'ep:operation',
    `component` = 'mes/pro/scheduleorder/index',
    `component_name` = 'MesProScheduleOrder',
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `deleted` = b'0',
    `updater` = '1',
    `update_time` = NOW()
WHERE `id` = 5580;

UPDATE `system_menu`
SET `name` = '排产工单查询',
    `permission` = 'mes:pro-schedule-order:query',
    `type` = 3,
    `sort` = 1,
    `parent_id` = 5580,
    `path` = '',
    `icon` = '',
    `component` = '',
    `component_name` = '',
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `deleted` = b'0',
    `updater` = '1',
    `update_time` = NOW()
WHERE `id` = 5581;

UPDATE `system_menu`
SET `name` = '排产工单创建',
    `permission` = 'mes:pro-schedule-order:create',
    `type` = 3,
    `sort` = 2,
    `parent_id` = 5580,
    `path` = '',
    `icon` = '',
    `component` = '',
    `component_name` = '',
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `deleted` = b'0',
    `updater` = '1',
    `update_time` = NOW()
WHERE `id` = 5582;

UPDATE `system_tenant_package`
SET `menu_ids` = CAST(JSON_ARRAY_APPEND(CAST(`menu_ids` AS JSON), '$', 5580) AS CHAR),
    `updater` = '1',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND JSON_VALID(`menu_ids`)
  AND JSON_CONTAINS(CAST(`menu_ids` AS JSON), CAST('5700' AS JSON), '$')
  AND NOT JSON_CONTAINS(CAST(`menu_ids` AS JSON), CAST('5580' AS JSON), '$');

UPDATE `system_tenant_package`
SET `menu_ids` = CAST(JSON_ARRAY_APPEND(CAST(`menu_ids` AS JSON), '$', 5581) AS CHAR),
    `updater` = '1',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND JSON_VALID(`menu_ids`)
  AND JSON_CONTAINS(CAST(`menu_ids` AS JSON), CAST('5700' AS JSON), '$')
  AND NOT JSON_CONTAINS(CAST(`menu_ids` AS JSON), CAST('5581' AS JSON), '$');

UPDATE `system_tenant_package`
SET `menu_ids` = CAST(JSON_ARRAY_APPEND(CAST(`menu_ids` AS JSON), '$', 5582) AS CHAR),
    `updater` = '1',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND JSON_VALID(`menu_ids`)
  AND JSON_CONTAINS(CAST(`menu_ids` AS JSON), CAST('5700' AS JSON), '$')
  AND NOT JSON_CONTAINS(CAST(`menu_ids` AS JSON), CAST('5582' AS JSON), '$');

INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT r.`id`, m.`id`, '1', NOW(), '1', NOW(), b'0', r.`tenant_id`
FROM `system_role` r
JOIN `system_tenant` t ON t.`id` = r.`tenant_id` AND t.`deleted` = b'0'
JOIN `system_tenant_package` tp ON tp.`id` = t.`package_id` AND tp.`deleted` = b'0'
JOIN `system_menu` m ON m.`id` IN (5580, 5581, 5582) AND m.`deleted` = b'0'
WHERE r.`deleted` = b'0'
  AND r.`code` = 'tenant_admin'
  AND JSON_VALID(tp.`menu_ids`)
  AND JSON_CONTAINS(CAST(tp.`menu_ids` AS JSON), CAST(CONCAT('', m.`id`) AS JSON), '$')
  AND NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` rm
    WHERE rm.`role_id` = r.`id`
      AND rm.`menu_id` = m.`id`
      AND rm.`tenant_id` = r.`tenant_id`
      AND rm.`deleted` = b'0'
  );

INSERT INTO `infra_job` (`id`, `name`, `status`, `handler_name`, `handler_param`, `cron_expression`, `retry_count`, `retry_interval`, `monitor_timeout`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 5600, '每天凌晨 2 点同步 ERP 生产工单', 1, 'kingdeeProductionOrderSyncJob', '', '0 0 2 * * ?', 3, 60, 0, '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (
  SELECT 1 FROM `infra_job`
  WHERE (`id` = 5600 OR `handler_name` = 'kingdeeProductionOrderSyncJob')
    AND `deleted` = b'0'
);

UPDATE `infra_job`
SET `name` = '每天凌晨 2 点同步 ERP 生产工单',
    `status` = 1,
    `handler_name` = 'kingdeeProductionOrderSyncJob',
    `handler_param` = '',
    `cron_expression` = '0 0 2 * * ?',
    `retry_count` = 3,
    `retry_interval` = 60,
    `monitor_timeout` = 0,
    `updater` = '1',
    `update_time` = NOW(),
    `deleted` = b'0'
WHERE `id` = 5600
   OR `handler_name` = 'kingdeeProductionOrderSyncJob';

INSERT INTO `infra_job` (`id`, `name`, `status`, `handler_name`, `handler_param`, `cron_expression`, `retry_count`, `retry_interval`, `monitor_timeout`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 5601, '每天凌晨 2 点 30 分重排 MES 排产工单', 1, 'mesProNightlyReplanJob', '', '0 30 2 * * ?', 3, 60, 0, '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (
  SELECT 1 FROM `infra_job`
  WHERE (`id` = 5601 OR `handler_name` = 'mesProNightlyReplanJob')
    AND `deleted` = b'0'
);

UPDATE `infra_job`
SET `name` = '每天凌晨 2 点 30 分重排 MES 排产工单',
    `status` = 1,
    `handler_name` = 'mesProNightlyReplanJob',
    `handler_param` = '',
    `cron_expression` = '0 30 2 * * ?',
    `retry_count` = 3,
    `retry_interval` = 60,
    `monitor_timeout` = 0,
    `updater` = '1',
    `update_time` = NOW(),
    `deleted` = b'0'
WHERE `id` = 5601
   OR `handler_name` = 'mesProNightlyReplanJob';
