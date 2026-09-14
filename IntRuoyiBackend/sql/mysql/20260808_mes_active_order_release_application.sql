-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260802_mes_process_pool_active_order_process_snapshot; type=schema; riskLevel=medium
-- MES 生产组长活跃订单申请放行：记录资料生成申请、阻塞项、幂等键和负责人待办关联
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_active_order_release_application` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `active_order_id` bigint NOT NULL COMMENT '活跃订单ID',
    `work_order_id` bigint NOT NULL COMMENT '生产订单ID',
    `work_order_code` varchar(128) DEFAULT NULL COMMENT '生产工单号',
    `route_id` bigint NOT NULL COMMENT '正式工艺路线ID',
    `route_version_id` bigint NOT NULL COMMENT '正式工艺路线版本ID',
    `product_id` bigint DEFAULT NULL COMMENT '产品ID',
    `batch_code` varchar(128) DEFAULT NULL COMMENT '生产批号',
    `batch_execution_id` bigint DEFAULT NULL COMMENT 'eDHR批次执行ID',
    `release_transaction_id` bigint DEFAULT NULL COMMENT 'eDHR放行事务ID',
    `release_approval_work_task_id` bigint DEFAULT NULL COMMENT '生产负责人放行待办ID',
    `application_status` varchar(32) NOT NULL COMMENT '申请状态：BLOCKED/PENDING_RELEASE_APPROVAL',
    `source_snapshot_hash` varchar(128) NOT NULL COMMENT '来源快照哈希',
    `request_idempotency_key` varchar(128) NOT NULL COMMENT '请求幂等键',
    `business_idempotency_key` varchar(255) NOT NULL COMMENT '业务幂等键',
    `blocker_snapshot_json` json DEFAULT NULL COMMENT '阻塞项快照',
    `dossier_summary_json` json DEFAULT NULL COMMENT '资料摘要快照',
    `applied_by` bigint NOT NULL COMMENT '申请人用户ID',
    `applied_at` datetime NOT NULL COMMENT '申请时间',
    `last_precheck_at` datetime DEFAULT NULL COMMENT '最近放行预检时间',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mes_pp_active_order_release_request` (`tenant_id`, `active_order_id`, `request_idempotency_key`, `deleted`),
    UNIQUE KEY `uk_mes_pp_active_order_release_business` (`tenant_id`, `active_order_id`, `business_idempotency_key`, `deleted`),
    KEY `idx_mes_pp_active_order_release_status` (`tenant_id`, `application_status`, `applied_at`),
    KEY `idx_mes_pp_active_order_release_transaction` (`tenant_id`, `release_transaction_id`, `release_approval_work_task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 工序池活跃订单放行资料申请';

DROP PROCEDURE IF EXISTS ensure_mes_active_order_release_application_menu;
DELIMITER $$
CREATE PROCEDURE ensure_mes_active_order_release_application_menu()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 900310
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing process-pool team leader parent menu 900310; cannot insert release application permission';
  END IF;

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 900451, '活跃订单申请放行', 'mes:pro-process-pool-team-leader:release-apply', 3, 5, 900310, '', '', '', '', 0, b'1', b'1', b'1', 'active-order-release-application', NOW(), 'active-order-release-application', NOW(), b'0'
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 900451
       OR `permission` = 'mes:pro-process-pool-team-leader:release-apply'
  );

  INSERT INTO `system_role_menu` (
    `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    `role`.`id`,
    900451,
    'active-order-release-application',
    NOW(),
    'active-order-release-application',
    NOW(),
    b'0',
    `role`.`tenant_id`
  FROM `system_role` AS `role`
  WHERE `role`.`deleted` = b'0'
    AND `role`.`code` = 'tenant_admin'
    AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` AS `existing`
      WHERE `existing`.`role_id` = `role`.`id`
        AND `existing`.`menu_id` = 900451
        AND `existing`.`tenant_id` = `role`.`tenant_id`
        AND `existing`.`deleted` = b'0'
    );
END$$
DELIMITER ;

CALL ensure_mes_active_order_release_application_menu();

DROP PROCEDURE IF EXISTS ensure_mes_active_order_release_application_menu;
