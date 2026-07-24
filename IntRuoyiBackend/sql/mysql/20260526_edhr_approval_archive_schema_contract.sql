-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- eDHR V1 approval close-loop and archive gate schema contract.
-- This script is intentionally explicit: historical SUBMITTED sealed archives are exposed as risk rows,
-- not silently upgraded, backfilled, or treated as compatible closed records.

DROP PROCEDURE IF EXISTS ensure_edhr_approval_archive_schema_contract;
DELIMITER $$
CREATE PROCEDURE ensure_edhr_approval_archive_schema_contract()
BEGIN
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.TABLES
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'mes_pro_batch_record_execution'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing mes_pro_batch_record_execution; apply base eDHR schema first';
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM information_schema.TABLES
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'mes_pro_batch_record_execution_signature'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing mes_pro_batch_record_execution_signature; apply base eDHR schema first';
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM information_schema.TABLES
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'mes_pro_batch_record_execution_archive'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing mes_pro_batch_record_execution_archive; apply sql/mysql/20260525_edhr_archive_schema.sql first';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_batch_record_execution' AND COLUMN_NAME = 'active_context_key') THEN
    ALTER TABLE `mes_pro_batch_record_execution` ADD COLUMN `active_context_key` varchar(255) DEFAULT NULL COMMENT '活动态上下文唯一键' AFTER `cell_values_json`;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_batch_record_execution' AND COLUMN_NAME = 'process_definition_key') THEN
    ALTER TABLE `mes_pro_batch_record_execution` ADD COLUMN `process_definition_key` varchar(128) DEFAULT NULL COMMENT 'BPM流程定义Key' AFTER `active_context_key`;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_batch_record_execution' AND COLUMN_NAME = 'process_instance_id') THEN
    ALTER TABLE `mes_pro_batch_record_execution` ADD COLUMN `process_instance_id` varchar(64) DEFAULT NULL COMMENT 'BPM流程实例ID' AFTER `process_definition_key`;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_batch_record_execution' AND COLUMN_NAME = 'submitted_by') THEN
    ALTER TABLE `mes_pro_batch_record_execution` ADD COLUMN `submitted_by` bigint DEFAULT NULL COMMENT '提交人' AFTER `process_instance_id`;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_batch_record_execution' AND COLUMN_NAME = 'submitted_at') THEN
    ALTER TABLE `mes_pro_batch_record_execution` ADD COLUMN `submitted_at` datetime DEFAULT NULL COMMENT '提交时间' AFTER `submitted_by`;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_batch_record_execution' AND COLUMN_NAME = 'approved_by') THEN
    ALTER TABLE `mes_pro_batch_record_execution` ADD COLUMN `approved_by` bigint DEFAULT NULL COMMENT '审批通过人' AFTER `submitted_at`;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_batch_record_execution' AND COLUMN_NAME = 'approved_at') THEN
    ALTER TABLE `mes_pro_batch_record_execution` ADD COLUMN `approved_at` datetime DEFAULT NULL COMMENT '审批通过时间' AFTER `approved_by`;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_batch_record_execution' AND COLUMN_NAME = 'rejected_by') THEN
    ALTER TABLE `mes_pro_batch_record_execution` ADD COLUMN `rejected_by` bigint DEFAULT NULL COMMENT '驳回人' AFTER `approved_at`;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_batch_record_execution' AND COLUMN_NAME = 'rejected_at') THEN
    ALTER TABLE `mes_pro_batch_record_execution` ADD COLUMN `rejected_at` datetime DEFAULT NULL COMMENT '驳回时间' AFTER `rejected_by`;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_batch_record_execution' AND COLUMN_NAME = 'reject_reason') THEN
    ALTER TABLE `mes_pro_batch_record_execution` ADD COLUMN `reject_reason` varchar(500) DEFAULT NULL COMMENT '驳回原因' AFTER `rejected_at`;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_batch_record_execution' AND COLUMN_NAME = 'closed_at') THEN
    ALTER TABLE `mes_pro_batch_record_execution` ADD COLUMN `closed_at` datetime DEFAULT NULL COMMENT '关闭时间' AFTER `reject_reason`;
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'mes_pro_batch_record_execution'
        AND INDEX_NAME = 'uk_execution_active_context'
  ) THEN
    ALTER TABLE `mes_pro_batch_record_execution`
      ADD UNIQUE KEY `uk_execution_active_context` (`tenant_id`, `active_context_key`);
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'mes_pro_batch_record_execution'
        AND INDEX_NAME = 'uk_execution_process_instance'
  ) THEN
    ALTER TABLE `mes_pro_batch_record_execution`
      ADD UNIQUE KEY `uk_execution_process_instance` (`tenant_id`, `process_instance_id`);
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'mes_pro_batch_record_execution'
        AND INDEX_NAME = 'idx_execution_tracking'
  ) THEN
    ALTER TABLE `mes_pro_batch_record_execution`
      ADD KEY `idx_execution_tracking` (`tenant_id`, `status`, `work_order_id`, `batch_code`, `route_process_id`, `workstation_id`, `update_time`);
  END IF;

  CREATE TABLE IF NOT EXISTS `mes_pro_batch_record_approval_snapshot` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `execution_id` bigint NOT NULL COMMENT '执行记录ID',
    `process_definition_key` varchar(128) NOT NULL COMMENT 'BPM流程定义Key',
    `process_definition_id` varchar(64) DEFAULT NULL COMMENT 'BPM流程定义ID',
    `process_instance_id` varchar(64) DEFAULT NULL COMMENT 'BPM流程实例ID',
    `approval_status` varchar(32) NOT NULL COMMENT '审批状态',
    `snapshot_json` longtext NOT NULL COMMENT '审批快照JSON',
    `snapshot_hash` char(64) NOT NULL COMMENT '审批快照SHA-256',
    `current_bpm_task_id` varchar(64) DEFAULT NULL COMMENT '当前BPM任务ID',
    `current_task_definition_key` varchar(128) DEFAULT NULL COMMENT '当前BPM节点Key',
    `submit_signature_id` bigint DEFAULT NULL COMMENT '提交签名ID',
    `approve_signature_id` bigint DEFAULT NULL COMMENT '审批通过签名ID',
    `reject_signature_id` bigint DEFAULT NULL COMMENT '驳回签名ID',
    `submitted_by` bigint DEFAULT NULL COMMENT '提交人',
    `submitted_at` datetime DEFAULT NULL COMMENT '提交时间',
    `approved_by` bigint DEFAULT NULL COMMENT '审批通过人',
    `approved_at` datetime DEFAULT NULL COMMENT '审批通过时间',
    `rejected_by` bigint DEFAULT NULL COMMENT '驳回人',
    `rejected_at` datetime DEFAULT NULL COMMENT '驳回时间',
    `reject_reason` varchar(500) DEFAULT NULL COMMENT '驳回原因',
    `closed_at` datetime DEFAULT NULL COMMENT '关闭时间',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_edhr_approval_execution` (`tenant_id`, `execution_id`),
    UNIQUE KEY `uk_edhr_approval_process_instance` (`tenant_id`, `process_instance_id`),
    KEY `idx_edhr_approval_status` (`tenant_id`, `approval_status`, `update_time`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES eDHR审批快照';

  IF EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'mes_pro_batch_record_approval_snapshot'
        AND COLUMN_NAME = 'process_instance_id'
        AND IS_NULLABLE = 'NO'
  ) THEN
    ALTER TABLE `mes_pro_batch_record_approval_snapshot`
      MODIFY COLUMN `process_instance_id` varchar(64) DEFAULT NULL COMMENT 'BPM流程实例ID';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_batch_record_execution_signature' AND COLUMN_NAME = 'process_instance_id') THEN
    ALTER TABLE `mes_pro_batch_record_execution_signature` ADD COLUMN `process_instance_id` varchar(64) DEFAULT NULL COMMENT 'BPM流程实例ID' AFTER `signed_at`;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_batch_record_execution_signature' AND COLUMN_NAME = 'bpm_task_id') THEN
    ALTER TABLE `mes_pro_batch_record_execution_signature` ADD COLUMN `bpm_task_id` varchar(64) DEFAULT NULL COMMENT 'BPM任务ID' AFTER `process_instance_id`;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_batch_record_execution_signature' AND COLUMN_NAME = 'bpm_task_definition_key') THEN
    ALTER TABLE `mes_pro_batch_record_execution_signature` ADD COLUMN `bpm_task_definition_key` varchar(128) DEFAULT NULL COMMENT 'BPM任务定义Key' AFTER `bpm_task_id`;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_batch_record_execution_signature' AND COLUMN_NAME = 'bpm_task_name') THEN
    ALTER TABLE `mes_pro_batch_record_execution_signature` ADD COLUMN `bpm_task_name` varchar(255) DEFAULT NULL COMMENT 'BPM任务名称' AFTER `bpm_task_definition_key`;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_batch_record_execution_signature' AND COLUMN_NAME = 'approval_result') THEN
    ALTER TABLE `mes_pro_batch_record_execution_signature` ADD COLUMN `approval_result` varchar(32) DEFAULT NULL COMMENT '审批结果' AFTER `bpm_task_name`;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_batch_record_execution_signature' AND COLUMN_NAME = 'reason') THEN
    ALTER TABLE `mes_pro_batch_record_execution_signature` ADD COLUMN `reason` varchar(500) DEFAULT NULL COMMENT '审批意见或驳回原因' AFTER `approval_result`;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_batch_record_execution_signature' AND COLUMN_NAME = 'actor_name') THEN
    ALTER TABLE `mes_pro_batch_record_execution_signature` ADD COLUMN `actor_name` varchar(64) DEFAULT NULL COMMENT '签名人名称快照' AFTER `reason`;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_batch_record_execution_archive' AND COLUMN_NAME = 'approval_snapshot_id') THEN
    ALTER TABLE `mes_pro_batch_record_execution_archive` ADD COLUMN `approval_snapshot_id` bigint DEFAULT NULL COMMENT '审批快照ID' AFTER `signature_hash`;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_batch_record_execution_archive' AND COLUMN_NAME = 'approval_snapshot_hash') THEN
    ALTER TABLE `mes_pro_batch_record_execution_archive` ADD COLUMN `approval_snapshot_hash` char(64) DEFAULT NULL COMMENT '审批快照摘要' AFTER `approval_snapshot_id`;
  END IF;
END$$
DELIMITER ;

CALL ensure_edhr_approval_archive_schema_contract();

DROP PROCEDURE IF EXISTS ensure_edhr_approval_archive_schema_contract;

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900033, 'eDHR批次执行', 'mes:pro-edhr-batch-execution:query', 2, 991, 5700, 'feedback/edhr-batch-execution', 'ep:document-checked', 'mes/pro/edhr-batch/BatchExecutionListPage', 'MesProEdhrBatchExecutionListPage', 0, b'1', b'1', b'1', 'edhr-approval-contract', NOW(), 'edhr-approval-contract', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900033 OR `path` = 'feedback/edhr-batch-execution');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900024, 'eDHR审批', 'mes:pro-batch-record-execution:approve', 2, 992, 5700, 'feedback/edhr-approval', 'ep:stamp', 'mes/pro/edhr/ApprovalPage', 'MesProFeedbackEdhrApproval', 0, b'1', b'1', b'1', 'edhr-approval-contract', NOW(), 'edhr-approval-contract', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900024 OR `path` = 'feedback/edhr-approval');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900025, 'eDHR追踪', 'mes:pro-batch-record-execution:track', 2, 993, 5700, 'feedback/edhr-tracking', 'ep:position', 'mes/pro/edhr/TrackingPage', 'MesProFeedbackEdhrTracking', 0, b'1', b'1', b'1', 'edhr-approval-contract', NOW(), 'edhr-approval-contract', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900025 OR `path` = 'feedback/edhr-tracking');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900026, 'eDHR签名记录', 'mes:pro-batch-record-execution:signature-query', 2, 994, 5700, 'feedback/edhr-signatures', 'ep:edit-pen', 'mes/pro/edhr/SignaturePage', 'MesProFeedbackEdhrSignatures', 0, b'1', b'1', b'1', 'edhr-approval-contract', NOW(), 'edhr-approval-contract', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900026 OR `path` = 'feedback/edhr-signatures');

UPDATE `system_menu`
SET `component_name` = CASE `id`
        WHEN 900033 THEN 'MesProEdhrBatchExecutionListPage'
        WHEN 900024 THEN 'MesProFeedbackEdhrApproval'
        WHEN 900025 THEN 'MesProFeedbackEdhrTracking'
        WHEN 900026 THEN 'MesProFeedbackEdhrSignatures'
      END,
    `updater` = 'edhr-approval-contract',
    `update_time` = NOW()
WHERE `id` IN (900033, 900024, 900025, 900026)
  AND NOT (`component_name` <=> CASE `id`
        WHEN 900033 THEN 'MesProEdhrBatchExecutionListPage'
        WHEN 900024 THEN 'MesProFeedbackEdhrApproval'
        WHEN 900025 THEN 'MesProFeedbackEdhrTracking'
        WHEN 900026 THEN 'MesProFeedbackEdhrSignatures'
      END);

UPDATE `system_menu`
SET `parent_id` = 5700,
    `type` = 2,
    `updater` = 'edhr-approval-contract',
    `update_time` = NOW()
WHERE `id` IN (900033, 900024, 900025, 900026)
  AND (`parent_id` <> 5700 OR `type` <> 2);

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900020, '电子批记录审批', 'mes:pro-batch-record-execution:approve', 3, 10, 900002, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-approval-contract', NOW(), 'edhr-approval-contract', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-batch-record-execution:approve');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900021, '电子批记录追踪', 'mes:pro-batch-record-execution:track', 3, 11, 900002, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-approval-contract', NOW(), 'edhr-approval-contract', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-batch-record-execution:track');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900022, '电子批记录签名查询', 'mes:pro-batch-record-execution:signature-query', 3, 12, 900002, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-approval-contract', NOW(), 'edhr-approval-contract', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-batch-record-execution:signature-query');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900017, '电子批记录归档查询', 'mes:pro-batch-record-execution-archive:query', 3, 13, 900002, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-approval-contract', NOW(), 'edhr-approval-contract', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-batch-record-execution-archive:query');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900018, '电子批记录归档生成', 'mes:pro-batch-record-execution-archive:create', 3, 14, 900002, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-approval-contract', NOW(), 'edhr-approval-contract', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-batch-record-execution-archive:create');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900019, '电子批记录归档下载', 'mes:pro-batch-record-execution-archive:download', 3, 15, 900002, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-approval-contract', NOW(), 'edhr-approval-contract', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-batch-record-execution-archive:download');

UPDATE `system_menu`
SET `parent_id` = 900002,
    `updater` = 'edhr-approval-contract',
    `update_time` = NOW()
WHERE `permission` IN (
    'mes:pro-batch-record-execution:approve',
    'mes:pro-batch-record-execution:track',
    'mes:pro-batch-record-execution:signature-query',
    'mes:pro-batch-record-execution-archive:query',
    'mes:pro-batch-record-execution-archive:create',
    'mes:pro-batch-record-execution-archive:download'
  )
  AND `type` = 3
  AND `parent_id` <> 900002;

DROP PROCEDURE IF EXISTS ensure_edhr_approval_archive_tenant_package_menus;
DELIMITER $$
CREATE PROCEDURE ensure_edhr_approval_archive_tenant_package_menus()
BEGIN
  IF EXISTS (
      SELECT 1
      FROM `system_tenant_package` AS `package`
      WHERE `package`.`deleted` = b'0'
        AND NOT JSON_VALID(`package`.`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot merge eDHR approval/archive permissions';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_approval_required_feedback_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_edhr_approval_required_feedback_menu_ids` AS
  SELECT `id`
  FROM `system_menu`
  WHERE `deleted` = b'0'
    AND `id` IN (
        5550, 5551, 5552, 5553, 5554, 5555, 5969
    );

  IF (SELECT COUNT(*) FROM `tmp_edhr_approval_required_feedback_menu_ids`) <> 7 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing production feedback system_menu page or permission rows; cannot merge tenant package menu_ids';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_approval_permission_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_edhr_approval_permission_menu_ids` AS
  SELECT `id`
  FROM `system_menu`
  WHERE `deleted` = b'0'
    AND `id` IN (
        5550, 5551, 5552, 5553, 5554, 5555, 5969,
        900017, 900018, 900019, 900020, 900021, 900022,
        900033, 900024, 900025, 900026
    );

  IF (SELECT COUNT(*) FROM `tmp_edhr_approval_permission_menu_ids`) <> 17 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing production feedback or eDHR approval/archive system_menu pages or permissions; cannot merge tenant package menu_ids';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_approval_permission_target_packages`;
  CREATE TEMPORARY TABLE `tmp_edhr_approval_permission_target_packages` AS
  SELECT DISTINCT
      `package`.`id` AS `package_id`
  FROM `system_tenant_package` AS `package`
  INNER JOIN JSON_TABLE(
      `package`.`menu_ids`,
      '$[*]' COLUMNS (`menu_id` BIGINT PATH '$')
  ) AS `existing_menu`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`)
    AND `existing_menu`.`menu_id` IN (5700, 900002);

  DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_approval_permission_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_edhr_approval_permission_package_menu_ids` (
      `package_id` BIGINT NOT NULL,
      `menu_id` BIGINT NOT NULL,
      PRIMARY KEY (`package_id`, `menu_id`)
  );

  INSERT IGNORE INTO `tmp_edhr_approval_permission_package_menu_ids` (`package_id`, `menu_id`)
  SELECT DISTINCT
      `target`.`package_id`,
      CAST(`existing_menu`.`menu_id` AS UNSIGNED) AS `menu_id`
  FROM `tmp_edhr_approval_permission_target_packages` AS `target`
  INNER JOIN `system_tenant_package` AS `package`
          ON `package`.`id` = `target`.`package_id`
         AND `package`.`deleted` = b'0'
         AND JSON_VALID(`package`.`menu_ids`)
  INNER JOIN JSON_TABLE(
      `package`.`menu_ids`,
      '$[*]' COLUMNS (`menu_id` BIGINT PATH '$')
  ) AS `existing_menu`;

  INSERT IGNORE INTO `tmp_edhr_approval_permission_package_menu_ids` (`package_id`, `menu_id`)
  SELECT DISTINCT
      `target`.`package_id`,
      `permission_menu`.`id` AS `menu_id`
  FROM `tmp_edhr_approval_permission_target_packages` AS `target`
  CROSS JOIN `tmp_edhr_approval_permission_menu_ids` AS `permission_menu`;

  DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_approval_permission_package_menu_json`;
  CREATE TEMPORARY TABLE `tmp_edhr_approval_permission_package_menu_json` AS
  SELECT
      `package_id`,
      JSON_ARRAYAGG(`menu_id`) AS `menu_ids`
  FROM (
      SELECT `package_id`, `menu_id`
      FROM `tmp_edhr_approval_permission_package_menu_ids`
      ORDER BY `package_id`, `menu_id`
  ) AS `merged_menu_ids`
  GROUP BY `package_id`;

  UPDATE `system_tenant_package` AS `package`
  INNER JOIN `tmp_edhr_approval_permission_package_menu_json` AS `merged`
          ON `merged`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = `merged`.`menu_ids`,
      `package`.`updater` = 'edhr-approval-contract',
      `package`.`update_time` = NOW()
  WHERE `package`.`deleted` = b'0';

  DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_approval_permission_target_roles`;
  CREATE TEMPORARY TABLE `tmp_edhr_approval_permission_target_roles` AS
  SELECT DISTINCT
      `tenant`.`id` AS `tenant_id`,
      `role`.`id` AS `role_id`
  FROM `system_tenant` AS `tenant`
  INNER JOIN `tmp_edhr_approval_permission_target_packages` AS `target_package`
          ON `target_package`.`package_id` = `tenant`.`package_id`
  INNER JOIN `system_role` AS `role`
          ON `role`.`tenant_id` = `tenant`.`id`
         AND `role`.`code` = 'tenant_admin'
         AND `role`.`deleted` = b'0'
  WHERE `tenant`.`deleted` = b'0'
    AND `tenant`.`package_id` <> 0;

  UPDATE `system_role_menu` AS `role_menu`
  INNER JOIN `tmp_edhr_approval_permission_target_roles` AS `target_role`
          ON `target_role`.`tenant_id` = `role_menu`.`tenant_id`
         AND `target_role`.`role_id` = `role_menu`.`role_id`
  INNER JOIN `tmp_edhr_approval_permission_menu_ids` AS `permission_menu`
          ON `permission_menu`.`id` = `role_menu`.`menu_id`
  SET `role_menu`.`deleted` = b'0',
      `role_menu`.`updater` = 'edhr-approval-contract',
      `role_menu`.`update_time` = NOW()
  WHERE `role_menu`.`deleted` = b'1';

  INSERT INTO `system_role_menu`
  (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT
      `target_role`.`role_id`,
      `permission_menu`.`id`,
      'edhr-approval-contract',
      NOW(),
      'edhr-approval-contract',
      NOW(),
      b'0',
      `target_role`.`tenant_id`
  FROM `tmp_edhr_approval_permission_target_roles` AS `target_role`
  CROSS JOIN `tmp_edhr_approval_permission_menu_ids` AS `permission_menu`
  WHERE NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` AS `existing`
      WHERE `existing`.`tenant_id` = `target_role`.`tenant_id`
        AND `existing`.`role_id` = `target_role`.`role_id`
        AND `existing`.`menu_id` = `permission_menu`.`id`
        AND `existing`.`deleted` = b'0'
  );

  DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_approval_permission_target_roles`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_approval_permission_package_menu_json`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_approval_permission_package_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_approval_permission_target_packages`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_approval_permission_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_approval_required_feedback_menu_ids`;
END$$
DELIMITER ;

CALL ensure_edhr_approval_archive_tenant_package_menus();

DROP PROCEDURE IF EXISTS ensure_edhr_approval_archive_tenant_package_menus;

SELECT 'EDHR_SUBMITTED_ARCHIVE_RISK' AS `risk_code`,
       `execution`.`tenant_id`,
       `execution`.`id` AS `execution_id`,
       `execution`.`status`,
       `archive`.`id` AS `archive_id`,
       `archive`.`archive_status`
FROM `mes_pro_batch_record_execution` AS `execution`
INNER JOIN `mes_pro_batch_record_execution_archive` AS `archive`
        ON `archive`.`tenant_id` = `execution`.`tenant_id`
       AND `archive`.`execution_id` = `execution`.`id`
       AND `archive`.`deleted` = b'0'
       AND `archive`.`archive_status` = 'SEALED'
WHERE `execution`.`deleted` = b'0'
  AND `execution`.`status` <> 3;
