-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
CREATE TABLE IF NOT EXISTS `mes_pro_edhr_batch_execution` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `batch_execution_code` varchar(64) NOT NULL COMMENT '批次执行编码',
  `work_order_id` bigint NOT NULL COMMENT '工单ID',
  `work_order_code` varchar(64) DEFAULT NULL COMMENT '工单编码快照',
  `batch_code` varchar(128) NOT NULL COMMENT '批次号',
  `active_context_key` varchar(255) DEFAULT NULL COMMENT '活动态上下文唯一键',
  `attempt_no` int NOT NULL DEFAULT 1 COMMENT '同生产批号执行尝试序号',
  `source_rejected_batch_execution_id` bigint DEFAULT NULL COMMENT '来源拒收批次执行ID',
  `superseded_by_batch_execution_id` bigint DEFAULT NULL COMMENT '被重做的新批次执行ID',
  `reexecuted_by_change_event_id` bigint DEFAULT NULL COMMENT '触发重做变更事件ID',
  `product_id` bigint DEFAULT NULL COMMENT '产品ID',
  `product_code` varchar(64) DEFAULT NULL COMMENT '产品编码快照',
  `product_name` varchar(255) DEFAULT NULL COMMENT '产品名称快照',
  `route_id` bigint NOT NULL COMMENT '工艺路线ID',
  `route_code` varchar(64) DEFAULT NULL COMMENT '工艺路线编码快照',
  `route_name` varchar(255) DEFAULT NULL COMMENT '工艺路线名称快照',
  `status` int NOT NULL DEFAULT 0 COMMENT '批次状态',
  `task_total` int NOT NULL DEFAULT 0 COMMENT '任务总数',
  `task_approved_count` int NOT NULL DEFAULT 0 COMMENT '已批准任务数',
  `blocked_count` int NOT NULL DEFAULT 0 COMMENT '阻塞任务数',
  `aggregate_hash` char(64) DEFAULT NULL COMMENT '批次聚合hash',
  `close_signature_id` bigint DEFAULT NULL COMMENT '关闭签名ID',
  `closed_by` bigint DEFAULT NULL COMMENT '关闭人',
  `closed_at` datetime DEFAULT NULL COMMENT '关闭时间',
  `reject_signature_id` bigint DEFAULT NULL COMMENT '质量终态拒收签名ID',
  `rejected_by` bigint DEFAULT NULL COMMENT '质量终态拒收人',
  `rejected_at` datetime DEFAULT NULL COMMENT '质量终态拒收时间',
  `reject_reason` varchar(500) DEFAULT NULL COMMENT '质量终态拒收原因',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_edhr_batch_execution_active_context` (`tenant_id`, `active_context_key`),
  KEY `idx_mes_pro_edhr_batch_execution_status` (`tenant_id`, `status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MES eDHR批次执行';

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_batch_execution_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `batch_execution_id` bigint NOT NULL COMMENT '批次执行ID',
  `route_process_id` bigint NOT NULL COMMENT '路线工序ID',
  `route_process_sort` int NOT NULL COMMENT '路线工序排序',
  `process_id` bigint DEFAULT NULL COMMENT '工序ID',
  `process_code` varchar(64) DEFAULT NULL COMMENT '工序编码快照',
  `process_name` varchar(255) DEFAULT NULL COMMENT '工序名称快照',
  `batch_record_report_id` varchar(64) DEFAULT NULL COMMENT '批记录报表ID',
  `batch_record_report_name` varchar(255) DEFAULT NULL COMMENT '批记录报表名称',
  `batch_record_sort` int NOT NULL DEFAULT 1 COMMENT '同工序批记录执行顺序',
  `execution_mode` varchar(16) NOT NULL DEFAULT 'SEQUENTIAL' COMMENT '同工序执行模式：SEQUENTIAL/PARALLEL',
  `execution_id` bigint DEFAULT NULL COMMENT '单张批记录执行ID',
  `status` int NOT NULL DEFAULT 0 COMMENT '任务状态',
  `required_flag` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否必需',
  `blocker_code` varchar(64) DEFAULT NULL COMMENT '阻塞码',
  `blocker_message` varchar(500) DEFAULT NULL COMMENT '阻塞说明',
  `opened_by` bigint DEFAULT NULL COMMENT '打开人',
  `opened_at` datetime DEFAULT NULL COMMENT '打开时间',
  `submitted_at` datetime DEFAULT NULL COMMENT '提交时间',
  `approved_at` datetime DEFAULT NULL COMMENT '审批通过时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_edhr_batch_task_process_report` (`tenant_id`, `batch_execution_id`, `route_process_id`, `batch_record_sort`, `deleted`),
  KEY `idx_mes_pro_edhr_batch_task_execution` (`tenant_id`, `execution_id`),
  KEY `idx_mes_pro_edhr_batch_task_status` (`tenant_id`, `batch_execution_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MES eDHR批次工序任务';

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_batch_execution_signature` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `batch_execution_id` bigint NOT NULL COMMENT '批次执行ID',
  `actor_id` bigint DEFAULT NULL COMMENT '签名人ID',
  `actor_name` varchar(64) DEFAULT NULL COMMENT '签名人名称快照',
  `action_type` varchar(32) NOT NULL COMMENT '签名动作',
  `signature_mode` varchar(32) NOT NULL COMMENT '签名方式',
  `password_verified` bit(1) NOT NULL DEFAULT b'0' COMMENT '密码是否校验通过',
  `comment` varchar(500) DEFAULT NULL COMMENT '签名意见',
  `signed_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '签名时间',
  `selected_signed_at` datetime DEFAULT NULL COMMENT '用户选择的签名显示时间',
  `signature_display_at` datetime DEFAULT NULL COMMENT '打印、归档和签名历史展示时间',
  `signature_time_mode` varchar(32) NOT NULL DEFAULT 'SERVER_TIME' COMMENT '签名时间模式：SERVER_TIME/USER_SELECTED',
  `selected_time_zone` varchar(64) DEFAULT NULL COMMENT '选择签名时间对应时区',
  `selected_time_reason` varchar(500) DEFAULT NULL COMMENT '选择签名时间原因',
  `selected_time_policy_version` varchar(64) DEFAULT NULL COMMENT '签名时间策略版本',
  `selected_time_audit_hash` char(64) DEFAULT NULL COMMENT '签名时间选择审计摘要',
  `signature_challenge_hash` char(64) DEFAULT NULL COMMENT '签名挑战hash',
  `aggregate_hash` char(64) DEFAULT NULL COMMENT '批次聚合hash',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_mes_pro_edhr_batch_signature_batch` (`tenant_id`, `batch_execution_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MES eDHR批次签名';

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_batch_execution_archive` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `batch_execution_id` bigint NOT NULL COMMENT '批次执行ID',
  `artifact_type` varchar(64) NOT NULL COMMENT '归档类型',
  `archive_version` int NOT NULL COMMENT '归档版本',
  `archive_status` varchar(32) NOT NULL COMMENT '归档状态',
  `file_name` varchar(255) DEFAULT NULL COMMENT '文件名',
  `content_type` varchar(128) DEFAULT NULL COMMENT '文件类型',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小',
  `file_path` varchar(500) DEFAULT NULL COMMENT '文件路径',
  `content_hash` char(64) DEFAULT NULL COMMENT '内容hash',
  `source_manifest_json` longtext DEFAULT NULL COMMENT '归档源清单',
  `generated_by` bigint DEFAULT NULL COMMENT '生成人',
  `generated_at` datetime DEFAULT NULL COMMENT '生成时间',
  `sealed_signature_id` bigint DEFAULT NULL COMMENT '封存签名ID',
  `failure_reason` varchar(500) DEFAULT NULL COMMENT '失败原因',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_mes_pro_edhr_batch_archive_batch` (`tenant_id`, `batch_execution_id`, `archive_version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MES eDHR批次最终归档';

DROP PROCEDURE IF EXISTS ensure_mes_edhr_batch_execution_quality_reject_columns;
DELIMITER $$
CREATE PROCEDURE ensure_mes_edhr_batch_execution_quality_reject_columns()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_edhr_batch_execution' AND COLUMN_NAME = 'reject_signature_id') THEN
    ALTER TABLE `mes_pro_edhr_batch_execution` ADD COLUMN `reject_signature_id` bigint DEFAULT NULL COMMENT '质量终态拒收签名ID' AFTER `closed_at`;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_edhr_batch_execution' AND COLUMN_NAME = 'rejected_by') THEN
    ALTER TABLE `mes_pro_edhr_batch_execution` ADD COLUMN `rejected_by` bigint DEFAULT NULL COMMENT '质量终态拒收人' AFTER `reject_signature_id`;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_edhr_batch_execution' AND COLUMN_NAME = 'rejected_at') THEN
    ALTER TABLE `mes_pro_edhr_batch_execution` ADD COLUMN `rejected_at` datetime DEFAULT NULL COMMENT '质量终态拒收时间' AFTER `rejected_by`;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_edhr_batch_execution' AND COLUMN_NAME = 'reject_reason') THEN
    ALTER TABLE `mes_pro_edhr_batch_execution` ADD COLUMN `reject_reason` varchar(500) DEFAULT NULL COMMENT '质量终态拒收原因' AFTER `rejected_at`;
  END IF;
END$$
DELIMITER ;

CALL ensure_mes_edhr_batch_execution_quality_reject_columns();

DROP PROCEDURE IF EXISTS ensure_mes_edhr_batch_execution_quality_reject_columns;

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900033, 'eDHR批次执行', 'mes:pro-edhr-batch-execution:query', 2, 995, 5700, 'feedback/edhr-batch-execution', 'ep:document-checked', 'mes/pro/edhr-batch/BatchExecutionListPage', 'MesProEdhrBatchExecutionListPage', 0, b'1', b'1', b'1', 'edhr-batch-execution', NOW(), 'edhr-batch-execution', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900033 OR `path` = 'feedback/edhr-batch-execution');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900034, 'eDHR批次执行查询', 'mes:pro-edhr-batch-execution:query', 3, 1, 900033, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-batch-execution', NOW(), 'edhr-batch-execution', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-batch-execution:query' AND `type` = 3);

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900035, 'eDHR批次执行创建', 'mes:pro-edhr-batch-execution:create', 3, 2, 900033, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-batch-execution', NOW(), 'edhr-batch-execution', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-batch-execution:create');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900036, 'eDHR批次执行更新', 'mes:pro-edhr-batch-execution:update', 3, 3, 900033, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-batch-execution', NOW(), 'edhr-batch-execution', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-batch-execution:update');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900041, 'eDHR批次质量终态拒收', 'mes:pro-edhr-batch-execution:quality-reject', 3, 8, 900033, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-batch-execution', NOW(), 'edhr-batch-execution', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-batch-execution:quality-reject');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900042, 'eDHR批次执行全量查看', 'mes:pro-edhr-batch-execution:overview', 3, 9, 900033, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-batch-execution', NOW(), 'edhr-batch-execution', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-batch-execution:overview');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900037, 'eDHR批次执行关闭', 'mes:pro-edhr-batch-execution:close', 3, 4, 900033, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-batch-execution', NOW(), 'edhr-batch-execution', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-batch-execution:close');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900038, 'eDHR批次归档查询', 'mes:pro-edhr-batch-execution-archive:query', 3, 5, 900033, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-batch-execution', NOW(), 'edhr-batch-execution', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-batch-execution-archive:query');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900039, 'eDHR批次归档生成', 'mes:pro-edhr-batch-execution-archive:create', 3, 6, 900033, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-batch-execution', NOW(), 'edhr-batch-execution', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-batch-execution-archive:create');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900040, 'eDHR批次归档下载', 'mes:pro-edhr-batch-execution-archive:download', 3, 7, 900033, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-batch-execution', NOW(), 'edhr-batch-execution', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-batch-execution-archive:download');

UPDATE `system_menu`
SET `parent_id` = 900033,
    `updater` = 'edhr-batch-execution',
    `update_time` = NOW()
WHERE `permission` IN (
    'mes:pro-edhr-batch-execution:query',
    'mes:pro-edhr-batch-execution:create',
    'mes:pro-edhr-batch-execution:update',
    'mes:pro-edhr-batch-execution:close',
    'mes:pro-edhr-batch-execution:quality-reject',
    'mes:pro-edhr-batch-execution:overview',
    'mes:pro-edhr-batch-execution-archive:query',
    'mes:pro-edhr-batch-execution-archive:create',
    'mes:pro-edhr-batch-execution-archive:download'
  )
  AND `type` = 3
  AND `parent_id` <> 900033;

DROP PROCEDURE IF EXISTS ensure_edhr_batch_execution_tenant_package_menus;
DELIMITER $$
CREATE PROCEDURE ensure_edhr_batch_execution_tenant_package_menus()
BEGIN
  IF EXISTS (
      SELECT 1
      FROM `system_tenant_package` AS `package`
      WHERE `package`.`deleted` = b'0'
        AND NOT JSON_VALID(`package`.`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot merge eDHR batch execution menus';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_batch_execution_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_edhr_batch_execution_menu_ids` AS
  SELECT `id`
  FROM `system_menu`
  WHERE `deleted` = b'0'
    AND `id` IN (900033, 900034, 900035, 900036, 900037, 900038, 900039, 900040, 900041, 900042);

  IF (SELECT COUNT(*) FROM `tmp_edhr_batch_execution_menu_ids`) <> 10 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR batch execution system_menu rows; cannot merge tenant package menu_ids';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_batch_execution_target_packages`;
  CREATE TEMPORARY TABLE `tmp_edhr_batch_execution_target_packages` AS
  SELECT DISTINCT
      `package`.`id` AS `package_id`
  FROM `system_tenant_package` AS `package`
  INNER JOIN JSON_TABLE(
      `package`.`menu_ids`,
      '$[*]' COLUMNS (`menu_id` BIGINT PATH '$')
  ) AS `existing_menu`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`)
    AND `existing_menu`.`menu_id` = 5700;

  DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_batch_execution_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_edhr_batch_execution_package_menu_ids` (
      `package_id` BIGINT NOT NULL,
      `menu_id` BIGINT NOT NULL,
      PRIMARY KEY (`package_id`, `menu_id`)
  );

  INSERT IGNORE INTO `tmp_edhr_batch_execution_package_menu_ids` (`package_id`, `menu_id`)
  SELECT DISTINCT
      `target`.`package_id`,
      CAST(`existing_menu`.`menu_id` AS UNSIGNED) AS `menu_id`
  FROM `tmp_edhr_batch_execution_target_packages` AS `target`
  INNER JOIN `system_tenant_package` AS `package`
          ON `package`.`id` = `target`.`package_id`
         AND `package`.`deleted` = b'0'
         AND JSON_VALID(`package`.`menu_ids`)
  INNER JOIN JSON_TABLE(
      `package`.`menu_ids`,
      '$[*]' COLUMNS (`menu_id` BIGINT PATH '$')
  ) AS `existing_menu`;

  INSERT IGNORE INTO `tmp_edhr_batch_execution_package_menu_ids` (`package_id`, `menu_id`)
  SELECT DISTINCT
      `target`.`package_id`,
      `menu`.`id`
  FROM `tmp_edhr_batch_execution_target_packages` AS `target`
  CROSS JOIN `tmp_edhr_batch_execution_menu_ids` AS `menu`;

  DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_batch_execution_package_menu_json`;
  CREATE TEMPORARY TABLE `tmp_edhr_batch_execution_package_menu_json` AS
  SELECT
      `package_id`,
      JSON_ARRAYAGG(`menu_id`) AS `menu_ids`
  FROM (
      SELECT `package_id`, `menu_id`
      FROM `tmp_edhr_batch_execution_package_menu_ids`
      ORDER BY `package_id`, `menu_id`
  ) AS `merged_menu_ids`
  GROUP BY `package_id`;

  UPDATE `system_tenant_package` AS `package`
  INNER JOIN `tmp_edhr_batch_execution_package_menu_json` AS `merged`
          ON `merged`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = `merged`.`menu_ids`,
      `package`.`updater` = 'edhr-batch-execution',
      `package`.`update_time` = NOW()
  WHERE `package`.`deleted` = b'0';

  DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_batch_execution_target_roles`;
  CREATE TEMPORARY TABLE `tmp_edhr_batch_execution_target_roles` AS
  SELECT DISTINCT
      `tenant`.`id` AS `tenant_id`,
      `role`.`id` AS `role_id`
  FROM `system_tenant` AS `tenant`
  INNER JOIN `tmp_edhr_batch_execution_target_packages` AS `target_package`
          ON `target_package`.`package_id` = `tenant`.`package_id`
  INNER JOIN `system_role` AS `role`
          ON `role`.`tenant_id` = `tenant`.`id`
         AND `role`.`code` = 'tenant_admin'
         AND `role`.`deleted` = b'0'
  WHERE `tenant`.`deleted` = b'0'
    AND `tenant`.`package_id` <> 0;

  UPDATE `system_role_menu` AS `role_menu`
  INNER JOIN `tmp_edhr_batch_execution_target_roles` AS `target_role`
          ON `target_role`.`tenant_id` = `role_menu`.`tenant_id`
         AND `target_role`.`role_id` = `role_menu`.`role_id`
  INNER JOIN `tmp_edhr_batch_execution_menu_ids` AS `menu`
          ON `menu`.`id` = `role_menu`.`menu_id`
  SET `role_menu`.`deleted` = b'0',
      `role_menu`.`updater` = 'edhr-batch-execution',
      `role_menu`.`update_time` = NOW()
  WHERE `role_menu`.`deleted` = b'1';

  INSERT INTO `system_role_menu`
  (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT
      `target_role`.`role_id`,
      `menu`.`id`,
      'edhr-batch-execution',
      NOW(),
      'edhr-batch-execution',
      NOW(),
      b'0',
      `target_role`.`tenant_id`
  FROM `tmp_edhr_batch_execution_target_roles` AS `target_role`
  CROSS JOIN `tmp_edhr_batch_execution_menu_ids` AS `menu`
  WHERE NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` AS `existing`
      WHERE `existing`.`tenant_id` = `target_role`.`tenant_id`
        AND `existing`.`role_id` = `target_role`.`role_id`
        AND `existing`.`menu_id` = `menu`.`id`
        AND `existing`.`deleted` = b'0'
  );

  DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_batch_execution_target_roles`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_batch_execution_package_menu_json`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_batch_execution_package_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_batch_execution_target_packages`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_batch_execution_menu_ids`;
END$$
DELIMITER ;

CALL ensure_edhr_batch_execution_tenant_package_menus();

DROP PROCEDURE IF EXISTS ensure_edhr_batch_execution_tenant_package_menus;
