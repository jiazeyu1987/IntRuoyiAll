-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
CREATE TABLE IF NOT EXISTS `mes_pro_edhr_work_task_assignment_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `route_process_id` bigint NOT NULL COMMENT '路线工序ID',
  `task_type` varchar(32) NOT NULL COMMENT '任务类型：FILL/REVIEW/REWORK',
  `assignee_user_id` bigint NOT NULL COMMENT '任务责任人用户ID',
  `review_user_id` bigint DEFAULT NULL COMMENT '审核人用户ID；填写规则可指定提交后的审核人',
  `due_minutes` int DEFAULT NULL COMMENT '任务处理时限分钟；为空表示必须由业务在创建前补齐正式规则',
  `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_edhr_work_task_rule` (`tenant_id`, `route_process_id`, `task_type`, `deleted`),
  KEY `idx_mes_pro_edhr_work_task_rule_user` (`tenant_id`, `assignee_user_id`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MES eDHR工作任务分配规则';

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_work_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_code` varchar(64) NOT NULL COMMENT '工作任务编码',
  `task_type` varchar(32) NOT NULL COMMENT '任务类型：FILL/REVIEW/REWORK/FINAL_REVIEW/ARCHIVE',
  `batch_execution_id` bigint NOT NULL COMMENT '批次执行ID',
  `batch_task_id` bigint NOT NULL COMMENT '批次工序任务ID',
  `execution_id` bigint DEFAULT NULL COMMENT '单张批记录执行ID',
  `work_order_id` bigint DEFAULT NULL COMMENT '工单ID',
  `work_order_code` varchar(64) DEFAULT NULL COMMENT '工单编码快照',
  `batch_code` varchar(128) DEFAULT NULL COMMENT '批次号',
  `route_id` bigint DEFAULT NULL COMMENT '工艺路线ID',
  `route_process_id` bigint NOT NULL COMMENT '路线工序ID',
  `process_id` bigint DEFAULT NULL COMMENT '工序ID',
  `process_name` varchar(255) DEFAULT NULL COMMENT '工序名称快照',
  `assignee_user_id` bigint NOT NULL COMMENT '责任人用户ID',
  `source_user_id` bigint DEFAULT NULL COMMENT '来源用户ID，例如提交人或审核人',
  `status` varchar(32) NOT NULL COMMENT '状态：TODO/DOING/DONE/CANCELED/OVERDUE',
  `due_time` datetime DEFAULT NULL COMMENT '到期时间',
  `overdue_at` datetime DEFAULT NULL COMMENT '逾期标记时间',
  `overdue_reason` varchar(500) DEFAULT NULL COMMENT '逾期原因',
  `started_at` datetime DEFAULT NULL COMMENT '开始处理时间',
  `completed_at` datetime DEFAULT NULL COMMENT '完成时间',
  `action_url` varchar(500) NOT NULL COMMENT '处理入口URL',
  `remark` varchar(500) DEFAULT NULL COMMENT '任务说明',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_edhr_work_task_active` (`tenant_id`, `batch_task_id`, `task_type`, `status`, `deleted`),
  KEY `idx_mes_pro_edhr_work_task_my` (`tenant_id`, `assignee_user_id`, `status`, `create_time`),
  KEY `idx_mes_pro_edhr_work_task_due` (`tenant_id`, `status`, `due_time`, `deleted`),
  KEY `idx_mes_pro_edhr_work_task_execution` (`tenant_id`, `execution_id`),
  KEY `idx_mes_pro_edhr_work_task_batch` (`tenant_id`, `batch_execution_id`, `batch_task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MES eDHR工作任务';

DROP PROCEDURE IF EXISTS ensure_mes_edhr_work_task_flow_schema;
DELIMITER $$
CREATE PROCEDURE ensure_mes_edhr_work_task_flow_schema()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM `information_schema`.`COLUMNS`
    WHERE `TABLE_SCHEMA` = DATABASE()
      AND `TABLE_NAME` = 'mes_pro_edhr_work_task_assignment_rule'
      AND `COLUMN_NAME` = 'due_minutes'
  ) THEN
    ALTER TABLE `mes_pro_edhr_work_task_assignment_rule`
      ADD COLUMN `due_minutes` int DEFAULT NULL COMMENT '任务处理时限分钟；为空表示必须由业务在创建前补齐正式规则' AFTER `review_user_id`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `information_schema`.`STATISTICS`
    WHERE `TABLE_SCHEMA` = DATABASE()
      AND `TABLE_NAME` = 'mes_pro_edhr_work_task'
      AND `INDEX_NAME` = 'idx_mes_pro_edhr_work_task_due'
  ) THEN
    ALTER TABLE `mes_pro_edhr_work_task`
      ADD INDEX `idx_mes_pro_edhr_work_task_due` (`tenant_id`, `status`, `due_time`, `deleted`);
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `information_schema`.`COLUMNS`
    WHERE `TABLE_SCHEMA` = DATABASE()
      AND `TABLE_NAME` = 'mes_pro_edhr_work_task'
      AND `COLUMN_NAME` = 'overdue_at'
  ) THEN
    ALTER TABLE `mes_pro_edhr_work_task`
      ADD COLUMN `overdue_at` datetime DEFAULT NULL COMMENT '逾期标记时间' AFTER `due_time`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `information_schema`.`COLUMNS`
    WHERE `TABLE_SCHEMA` = DATABASE()
      AND `TABLE_NAME` = 'mes_pro_edhr_work_task'
      AND `COLUMN_NAME` = 'overdue_reason'
  ) THEN
    ALTER TABLE `mes_pro_edhr_work_task`
      ADD COLUMN `overdue_reason` varchar(500) DEFAULT NULL COMMENT '逾期原因' AFTER `overdue_at`;
  END IF;
END$$
DELIMITER ;

CALL ensure_mes_edhr_work_task_flow_schema();

DROP PROCEDURE IF EXISTS ensure_mes_edhr_work_task_flow_schema;

INSERT INTO `system_notify_template`
(`name`, `code`, `type`, `nickname`, `content`, `params`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 'eDHR填写任务通知', 'MES_EDHR_FILL_TASK_ASSIGNED', 2, 'eDHR任务中心',
       '工作到你了：请填写工单{workOrderCode}批次{batchCode}的{processName}批记录。入口：{actionUrl}',
       '["workOrderCode","batchCode","processName","actionUrl","workTaskId"]', 0, 'eDHR工作任务流转',
       'edhr-work-task-flow', NOW(), 'edhr-work-task-flow', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_notify_template` WHERE `code` = 'MES_EDHR_FILL_TASK_ASSIGNED' AND `deleted` = b'0');

INSERT INTO `system_notify_template`
(`name`, `code`, `type`, `nickname`, `content`, `params`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 'eDHR审核任务通知', 'MES_EDHR_REVIEW_TASK_ASSIGNED', 2, 'eDHR任务中心',
       '工作到你了：请审核工单{workOrderCode}批次{batchCode}的{processName}批记录。入口：{actionUrl}',
       '["workOrderCode","batchCode","processName","actionUrl","workTaskId"]', 0, 'eDHR工作任务流转',
       'edhr-work-task-flow', NOW(), 'edhr-work-task-flow', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_notify_template` WHERE `code` = 'MES_EDHR_REVIEW_TASK_ASSIGNED' AND `deleted` = b'0');

INSERT INTO `system_notify_template`
(`name`, `code`, `type`, `nickname`, `content`, `params`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 'eDHR批准任务通知', 'MES_EDHR_APPROVE_TASK_ASSIGNED', 2, 'eDHR任务中心',
       '工作到你了：请批准工单{workOrderCode}批次{batchCode}的{processName}批记录。入口：{actionUrl}',
       '["workOrderCode","batchCode","processName","actionUrl","workTaskId"]', 0, 'eDHR工作任务流转',
       'edhr-work-task-flow', NOW(), 'edhr-work-task-flow', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_notify_template` WHERE `code` = 'MES_EDHR_APPROVE_TASK_ASSIGNED' AND `deleted` = b'0');

INSERT INTO `system_notify_template`
(`name`, `code`, `type`, `nickname`, `content`, `params`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 'eDHR驳回修改任务通知', 'MES_EDHR_REWORK_TASK_ASSIGNED', 2, 'eDHR任务中心',
       '工作到你了：工单{workOrderCode}批次{batchCode}的{processName}批记录已驳回，请修改后重新提交。入口：{actionUrl}',
       '["workOrderCode","batchCode","processName","actionUrl","workTaskId"]', 0, 'eDHR工作任务流转',
       'edhr-work-task-flow', NOW(), 'edhr-work-task-flow', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_notify_template` WHERE `code` = 'MES_EDHR_REWORK_TASK_ASSIGNED' AND `deleted` = b'0');

INSERT INTO `system_notify_template`
(`name`, `code`, `type`, `nickname`, `content`, `params`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 'eDHR工作任务逾期提醒', 'MES_EDHR_WORK_TASK_OVERDUE', 2, 'eDHR任务中心',
       '工作任务已逾期：工单{workOrderCode}批次{batchCode}的{processName}批记录应于{dueTime}前处理。入口：{actionUrl}',
       '["workOrderCode","batchCode","processName","dueTime","actionUrl","workTaskId"]', 0, 'eDHR工作任务逾期自动处理',
       'edhr-work-task-flow', NOW(), 'edhr-work-task-flow', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_notify_template` WHERE `code` = 'MES_EDHR_WORK_TASK_OVERDUE' AND `deleted` = b'0');

INSERT INTO `infra_job`
(`id`, `name`, `status`, `handler_name`, `handler_param`, `cron_expression`, `retry_count`, `retry_interval`, `monitor_timeout`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 5610, 'eDHR工作任务逾期处理 Job', 1, 'mesEdhrWorkTaskOverdueJob', '{"limit":200}', '0 0/5 * * * ?', 0, 0, 0, 'edhr-work-task-flow', NOW(), 'edhr-work-task-flow', NOW(), b'0'
WHERE NOT EXISTS (
  SELECT 1 FROM `infra_job`
  WHERE (`id` = 5610 OR `handler_name` = 'mesEdhrWorkTaskOverdueJob')
    AND `deleted` = b'0'
);

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900230, 'eDHR工作任务', 'mes:pro-edhr-work-task:query', 2, 0, 900220, '/mes/pro/feedback/edhr-work-task', 'ep:list', 'mes/pro/edhr-work-task/WorkTaskBoardPage', 'MesProEdhrWorkTaskBoardPage', 0, b'1', b'1', b'1', 'edhr-work-task-flow', NOW(), 'edhr-work-task-flow', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900230 OR `permission` = 'mes:pro-edhr-work-task:query');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900231, 'eDHR工作任务查询', 'mes:pro-edhr-work-task:query', 3, 1, 900230, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-work-task-flow', NOW(), 'edhr-work-task-flow', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-work-task:query' AND `type` = 3);

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900232, 'eDHR工作任务处理', 'mes:pro-edhr-work-task:update', 3, 2, 900230, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-work-task-flow', NOW(), 'edhr-work-task-flow', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-work-task:update');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900233, 'eDHR任务规则查询', 'mes:pro-edhr-work-task-rule:query', 3, 3, 900230, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-work-task-flow', NOW(), 'edhr-work-task-flow', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-work-task-rule:query');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900234, 'eDHR任务规则维护', 'mes:pro-edhr-work-task-rule:update', 3, 4, 900230, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-work-task-flow', NOW(), 'edhr-work-task-flow', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-work-task-rule:update');

DROP PROCEDURE IF EXISTS ensure_mes_edhr_work_task_flow_menus;
DELIMITER $$
CREATE PROCEDURE ensure_mes_edhr_work_task_flow_menus()
BEGIN
  IF EXISTS (
      SELECT 1 FROM `system_tenant_package` AS `package`
      WHERE `package`.`deleted` = b'0'
        AND NOT JSON_VALID(`package`.`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot merge eDHR work task menus';
  END IF;

  IF (SELECT COUNT(*) FROM `system_menu` WHERE `deleted` = b'0' AND `id` IN (900230, 900231, 900232, 900233, 900234)) <> 5 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR work task system_menu rows; cannot merge tenant package menu_ids';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_work_task_target_packages`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_work_task_target_packages` AS
  SELECT `package`.`id` AS `package_id`
  FROM `system_tenant_package` AS `package`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`)
    AND JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('900220' AS JSON), '$');

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_work_task_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_work_task_menu_ids` AS
  SELECT `id`
  FROM `system_menu`
  WHERE `id` IN (900230, 900231, 900232, 900233, 900234)
    AND `deleted` = b'0';

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_work_task_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_work_task_package_menu_ids` (
    `package_id` bigint NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`package_id`, `menu_id`)
  );

  INSERT IGNORE INTO `tmp_mes_edhr_work_task_package_menu_ids` (`package_id`, `menu_id`)
  SELECT
    `target_package`.`package_id`,
    CAST(`existing_menu`.`menu_id` AS UNSIGNED)
  FROM `tmp_mes_edhr_work_task_target_packages` AS `target_package`
  JOIN `system_tenant_package` AS `package`
    ON `package`.`id` = `target_package`.`package_id`
   AND `package`.`deleted` = b'0'
   AND JSON_VALID(`package`.`menu_ids`)
  JOIN JSON_TABLE(
    `package`.`menu_ids`,
    '$[*]' COLUMNS (`menu_id` bigint PATH '$')
  ) AS `existing_menu`;

  INSERT IGNORE INTO `tmp_mes_edhr_work_task_package_menu_ids` (`package_id`, `menu_id`)
  SELECT
    `target_package`.`package_id`,
    `menu`.`id`
  FROM `tmp_mes_edhr_work_task_target_packages` AS `target_package`
  CROSS JOIN `tmp_mes_edhr_work_task_menu_ids` AS `menu`;

  UPDATE `system_tenant_package` AS `package`
  JOIN (
    SELECT DISTINCT
      `package_id`,
      JSON_ARRAYAGG(`menu_id`) OVER (
        PARTITION BY `package_id`
        ORDER BY `menu_id`
        ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
      ) AS `menu_ids`
    FROM `tmp_mes_edhr_work_task_package_menu_ids`
  ) AS `merged`
    ON `merged`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = `merged`.`menu_ids`,
      `package`.`updater` = 'edhr-work-task-flow',
      `package`.`update_time` = NOW();

  INSERT INTO `system_role_menu` (
    `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    `role`.`id`,
    `menu`.`id`,
    'edhr-work-task-flow',
    NOW(),
    'edhr-work-task-flow',
    NOW(),
    b'0',
    `role`.`tenant_id`
  FROM `system_role` AS `role`
  JOIN `system_tenant` AS `tenant`
    ON `tenant`.`id` = `role`.`tenant_id`
   AND `tenant`.`deleted` = b'0'
  JOIN `tmp_mes_edhr_work_task_target_packages` AS `target_package`
    ON `target_package`.`package_id` = `tenant`.`package_id`
  JOIN `system_menu` AS `menu`
    ON `menu`.`id` IN (900230, 900231, 900232, 900233, 900234)
   AND `menu`.`deleted` = b'0'
  WHERE `role`.`deleted` = b'0'
    AND `role`.`code` = 'tenant_admin'
    AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` AS `existing`
      WHERE `existing`.`role_id` = `role`.`id`
        AND `existing`.`menu_id` = `menu`.`id`
        AND `existing`.`tenant_id` = `role`.`tenant_id`
        AND `existing`.`deleted` = b'0'
    );

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_work_task_package_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_work_task_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_work_task_target_packages`;
END$$
DELIMITER ;

CALL ensure_mes_edhr_work_task_flow_menus();

DROP PROCEDURE IF EXISTS ensure_mes_edhr_work_task_flow_menus;
