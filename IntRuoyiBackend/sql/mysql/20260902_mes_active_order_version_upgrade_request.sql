-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260822_mes_process_pool_active_order_completion_receipt,20260719_business_approval_policy,20260902_mes_active_order_version_upgrade_bpm_seed; type=schema; riskLevel=medium
-- MES active-order latest-version upgrade approval request.
-- This migration is additive only. It stores immutable current/target snapshots and does not create replacement orders.

CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_active_order_version_upgrade_request` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '活跃订单版本升级申请编号',
    `source_active_order_id` bigint NOT NULL COMMENT '来源活跃订单编号',
    `source_work_order_id` bigint NOT NULL COMMENT '来源生产工单编号',
    `source_batch_execution_id` bigint DEFAULT NULL COMMENT '来源 eDHR 批次执行编号',
    `target_active_order_id` bigint DEFAULT NULL COMMENT '审批通过后新建活跃订单编号',
    `target_batch_execution_id` bigint DEFAULT NULL COMMENT '审批通过后新建批次执行编号',
    `request_code` varchar(64) NOT NULL COMMENT '版本升级申请单号',
    `idempotency_key` varchar(128) NOT NULL COMMENT '提交幂等键',
    `request_status` varchar(32) NOT NULL COMMENT '申请状态：PENDING_APPROVAL/APPROVED/REJECTED/CANCELLED/APPLIED',
    `approval_status` varchar(32) NOT NULL COMMENT '审批状态：PENDING/APPROVED/REJECTED/CANCELLED',
    `freeze_status` varchar(32) NOT NULL COMMENT '冻结状态：OLD_ORDER_FROZEN/RELEASED/APPLIED',
    `approval_process_instance_id` varchar(128) DEFAULT NULL COMMENT '审批流程实例编号',
    `upgrade_reason` varchar(500) NOT NULL COMMENT '版本升级原因',
    `current_snapshot_json` json NOT NULL COMMENT '来源订单当前版本冻结快照',
    `target_snapshot_json` json NOT NULL COMMENT '全部最新正式版本目标快照',
    `snapshot_hash` char(64) NOT NULL COMMENT '当前与目标快照 SHA-256 哈希',
    `requested_by` bigint NOT NULL COMMENT '申请人',
    `requested_at` datetime NOT NULL COMMENT '申请时间',
    `applied_at` datetime DEFAULT NULL COMMENT '审批通过并创建新订单时间',
    `cancelled_at` datetime DEFAULT NULL COMMENT '取消或驳回时间',
    `result_message` varchar(500) DEFAULT NULL COMMENT '结果说明',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mes_pp_active_order_upgrade_code` (`tenant_id`, `request_code`, `deleted`),
    UNIQUE KEY `uk_mes_pp_active_order_upgrade_idempotency` (`tenant_id`, `source_active_order_id`, `idempotency_key`, `deleted`),
    KEY `idx_mes_pp_active_order_upgrade_source_status` (`tenant_id`, `source_active_order_id`, `request_status`, `deleted`),
    KEY `idx_mes_pp_active_order_upgrade_target` (`tenant_id`, `target_active_order_id`, `deleted`),
    KEY `idx_mes_pp_active_order_upgrade_approval` (`tenant_id`, `approval_process_instance_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 活跃订单版本升级重启审批申请';

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_mes_active_order_version_upgrade_approval;

DELIMITER //
CREATE PROCEDURE ensure_mes_active_order_version_upgrade_approval()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'bpm_business_approval_policy'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'MES active-order version-upgrade policy requires bpm_business_approval_policy';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM `act_re_procdef` AS `proc`
    WHERE `proc`.`KEY_` = 'mes-active-order-version-upgrade-v1'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'MES active-order version-upgrade policy requires active-order BPM process definition';
  END IF;

  INSERT INTO `system_menu` (
    `id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`
  )
  SELECT 900452, '活跃订单版本升级', 'mes:pro-process-pool-team-leader:version-upgrade', 3, 6, 900310,
         '', '', '', '', 0, b'1', b'1', b'1', 'active-order-version-upgrade', NOW(), 'active-order-version-upgrade', NOW(), b'0'
  WHERE EXISTS (
      SELECT 1 FROM `system_menu` AS `parent`
      WHERE `parent`.`id` = 900310
        AND `parent`.`deleted` = b'0'
    )
    AND NOT EXISTS (
      SELECT 1 FROM `system_menu` AS `existing`
      WHERE `existing`.`deleted` = b'0'
        AND (
          `existing`.`id` = 900452
          OR (`existing`.`permission` = 'mes:pro-process-pool-team-leader:version-upgrade' AND `existing`.`type` = 3)
        )
    );

  IF NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `permission` = 'mes:pro-process-pool-team-leader:version-upgrade'
      AND `type` = 3
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'MES active-order version-upgrade permission menu is required';
  END IF;

  INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT `role`.`id`, `menu`.`id`, 'active-order-version-upgrade', NOW(), 'active-order-version-upgrade', NOW(), b'0'
  FROM `system_role` AS `role`
  JOIN `system_menu` AS `menu`
    ON `menu`.`deleted` = b'0'
   AND `menu`.`permission` = 'mes:pro-process-pool-team-leader:version-upgrade'
   AND `menu`.`type` = 3
  WHERE `role`.`deleted` = b'0'
    AND `role`.`status` = 0
    AND `role`.`code` IN ('super_admin', 'pqc_leader_permission')
    AND EXISTS (
      SELECT 1
      FROM `system_role_menu` AS `maintain_binding`
      JOIN `system_menu` AS `maintain_menu`
        ON `maintain_menu`.`id` = `maintain_binding`.`menu_id`
       AND `maintain_menu`.`deleted` = b'0'
       AND `maintain_menu`.`permission` = 'mes:pro-process-pool-team-leader:maintain'
      WHERE `maintain_binding`.`role_id` = `role`.`id`
        AND `maintain_binding`.`deleted` = b'0'
    )
    AND NOT EXISTS (
      SELECT 1 FROM `system_role_menu` AS `existing`
      WHERE `existing`.`role_id` = `role`.`id`
        AND `existing`.`menu_id` = `menu`.`id`
        AND `existing`.`deleted` = b'0'
    );

  IF EXISTS (
    SELECT 1
    FROM `bpm_business_approval_policy` AS `policy`
    WHERE `policy`.`deleted` = b'0'
      AND `policy`.`data_domain` = 'MES'
      AND `policy`.`system_code` = 'MES'
      AND `policy`.`object_type` = 'MES_ACTIVE_ORDER'
      AND `policy`.`action_code` = 'VERSION_UPGRADE_RESTART'
      AND `policy`.`object_state` = 'VERSION_UPGRADE_PENDING'
      AND `policy`.`status` = 'PUBLISHED'
      AND COALESCE(`policy`.`effect_executor_code`, '') <> 'MES_ACTIVE_ORDER_VERSION_UPGRADE_RESTART'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Conflicting published MES active-order version-upgrade approval policy';
  END IF;

  INSERT INTO `bpm_business_approval_policy` (
    `tenant_id`, `data_domain`, `system_code`, `object_type`, `action_code`, `object_state`,
    `policy_mode`, `process_definition_key`, `effect_executor_code`, `status`, `remark`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
  )
  SELECT `tenant`.`id`, 'MES', 'MES', 'MES_ACTIVE_ORDER', 'VERSION_UPGRADE_RESTART', 'VERSION_UPGRADE_PENDING',
         'BPM_REQUIRED', 'mes-active-order-version-upgrade-v1', 'MES_ACTIVE_ORDER_VERSION_UPGRADE_RESTART', 'PUBLISHED',
         'Active order version-upgrade restart approval policy', 'codex', NOW(), 'codex', NOW(), b'0'
  FROM `system_tenant` AS `tenant`
  WHERE `tenant`.`deleted` = b'0'
    AND `tenant`.`status` = 0
    AND EXISTS (
      SELECT 1 FROM `act_re_procdef` AS `proc`
      WHERE `proc`.`KEY_` = 'mes-active-order-version-upgrade-v1'
        AND `proc`.`TENANT_ID_` = CAST(`tenant`.`id` AS CHAR)
    )
    AND NOT EXISTS (
      SELECT 1 FROM `bpm_business_approval_policy` AS `existing`
      WHERE `existing`.`deleted` = b'0'
        AND `existing`.`tenant_id` = `tenant`.`id`
        AND `existing`.`data_domain` = 'MES'
        AND `existing`.`system_code` = 'MES'
        AND `existing`.`object_type` = 'MES_ACTIVE_ORDER'
        AND `existing`.`action_code` = 'VERSION_UPGRADE_RESTART'
        AND `existing`.`object_state` = 'VERSION_UPGRADE_PENDING'
        AND `existing`.`status` = 'PUBLISHED'
    );

  IF EXISTS (
    SELECT 1
    FROM `bpm_business_approval_policy`
    WHERE `deleted` = b'0'
      AND `data_domain` = 'MES'
      AND `system_code` = 'MES'
      AND `object_type` = 'MES_ACTIVE_ORDER'
      AND `action_code` = 'VERSION_UPGRADE_RESTART'
      AND `object_state` = 'VERSION_UPGRADE_PENDING'
      AND `status` = 'PUBLISHED'
    GROUP BY `tenant_id`, `object_type`, `action_code`, `object_state`
    HAVING COUNT(*) > 1
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Duplicate published MES active-order version-upgrade approval policy';
  END IF;
END//
DELIMITER ;

CALL ensure_mes_active_order_version_upgrade_approval();

DROP PROCEDURE IF EXISTS ensure_mes_active_order_version_upgrade_approval;
