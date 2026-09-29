-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260608_edhr_batch_execution_schema,20260618_mes_edhr_release_transaction_lifecycle,20260908_gxp_audit_trail_core; type=schema; riskLevel=medium
-- Creates batch-record deviations and their one-to-one handling/tenant-month sequence, permissions, and role-owned GxP operation registration.
-- The migration does not rewrite historical batch, review, signature, or role assignments.

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_deviation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `deviation_code` varchar(64) NOT NULL COMMENT '偏差编号：PC-YYYYMM-流水号',
  `batch_execution_id` bigint NOT NULL COMMENT '正式批记录ID',
  `batch_execution_code` varchar(64) NOT NULL COMMENT '正式批记录编号快照',
  `work_order_id` bigint DEFAULT NULL COMMENT '工单ID快照',
  `work_order_code` varchar(64) DEFAULT NULL COMMENT '工单编号快照',
  `batch_code` varchar(128) DEFAULT NULL COMMENT '生产批号快照',
  `level` varchar(16) NOT NULL COMMENT '等级：NORMAL/CRITICAL',
  `discovery_department_id` bigint DEFAULT NULL COMMENT '发现部门ID',
  `discovery_department_name` varchar(128) DEFAULT NULL COMMENT '发现部门名称',
  `discoverer_id` bigint DEFAULT NULL COMMENT '发现人ID',
  `discoverer_name` varchar(128) DEFAULT NULL COMMENT '发现人名称',
  `discovered_at` datetime DEFAULT NULL COMMENT '发现时间',
  `discovery_location` varchar(255) DEFAULT NULL COMMENT '发现地点或工序',
  `product_name` varchar(255) DEFAULT NULL COMMENT '产品名称快照',
  `product_specification` varchar(512) DEFAULT NULL COMMENT '产品规格快照',
  `equipment_or_system` varchar(255) DEFAULT NULL COMMENT '设备或系统',
  `reported_at` datetime DEFAULT NULL COMMENT '偏差上报日期',
  `receiver_id` bigint DEFAULT NULL COMMENT '接收人ID',
  `receiver_name` varchar(128) DEFAULT NULL COMMENT '接收人名称',
  `category_codes_json` json DEFAULT NULL COMMENT '偏差类别编码列表',
  `description` text COMMENT '偏差现象及偏离标准描述',
  `emergency_action` text COMMENT '现场紧急处置措施',
  `level_basis` text COMMENT '偏差等级判定依据',
  `initiator_user_id` bigint NOT NULL COMMENT '发起编制人ID',
  `initiator_name` varchar(128) DEFAULT NULL COMMENT '发起编制人名称快照',
  `initiated_at` datetime NOT NULL COMMENT '发起时间（服务器时间）',
  `initiator_signature_id` bigint DEFAULT NULL COMMENT '发起电子签名ID',
  `initiator_content_hash` char(64) DEFAULT NULL COMMENT '发起内容签名摘要',
  `status` varchar(16) NOT NULL DEFAULT 'OPEN' COMMENT '偏差状态：OPEN/CLOSED',
  `close_reason` varchar(32) DEFAULT NULL COMMENT '关闭原因：NORMAL_COMPLETED/TRANSFERRED_TO_NCR',
  `closed_at` datetime DEFAULT NULL COMMENT '关闭时间（服务器时间）',
  `nonconformance_review_id` bigint DEFAULT NULL COMMENT '关联不合格评审ID',
  `create_idempotency_key` varchar(128) NOT NULL COMMENT '发起请求幂等键',
  `create_payload_hash` char(64) NOT NULL COMMENT '发起请求规范化载荷摘要',
  `version` int NOT NULL DEFAULT 1 COMMENT '业务版本',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_edhr_deviation_code` (`tenant_id`, `deviation_code`),
  UNIQUE KEY `uk_mes_edhr_deviation_create_idempotency` (`tenant_id`, `create_idempotency_key`),
  KEY `idx_mes_edhr_deviation_batch_status` (`tenant_id`, `batch_execution_id`, `status`, `deleted`),
  KEY `idx_mes_edhr_deviation_batch_level` (`tenant_id`, `batch_execution_id`, `level`, `status`),
  KEY `idx_mes_edhr_deviation_ncr` (`tenant_id`, `nonconformance_review_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MES eDHR批记录偏差';

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_deviation_handling` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `deviation_id` bigint NOT NULL COMMENT '偏差ID',
  `investigation_started_at` datetime DEFAULT NULL COMMENT '调查日期',
  `planned_completed_at` datetime DEFAULT NULL COMMENT '计划完成日期',
  `completed_at` datetime DEFAULT NULL COMMENT '实际完成日期',
  `investigation_members_json` json DEFAULT NULL COMMENT '调查组成员',
  `root_cause_analysis` text COMMENT '原因调查和根源分析',
  `impact_scope` text COMMENT '影响范围',
  `risk_assessment` text COMMENT '风险评估',
  `product_disposition` text COMMENT '产品处理措施',
  `nonconformance_review_code` varchar(64) DEFAULT NULL COMMENT '不合格评审单号',
  `corrective_owner_id` bigint DEFAULT NULL COMMENT '整改责任人ID',
  `corrective_owner_name` varchar(128) DEFAULT NULL COMMENT '整改责任人名称',
  `corrective_due_at` datetime DEFAULT NULL COMMENT '整改预计完成时间',
  `capa_required` bit(1) DEFAULT NULL COMMENT '是否执行CAPA',
  `capa_code` varchar(64) DEFAULT NULL COMMENT 'CAPA编号',
  `capa_attachments_json` json DEFAULT NULL COMMENT 'CAPA附件',
  `handling_conclusion` varchar(32) DEFAULT NULL COMMENT '处理结论',
  `verification_result` varchar(32) DEFAULT NULL COMMENT '整改验证结论',
  `verification_content` text COMMENT '整改验证情况',
  `content_version` int NOT NULL DEFAULT 1 COMMENT '处理正文版本',
  `content_hash` char(64) DEFAULT NULL COMMENT '处理正文摘要',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_edhr_deviation_handling` (`tenant_id`, `deviation_id`),
  KEY `idx_mes_edhr_deviation_handling_content` (`tenant_id`, `deviation_id`, `content_version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MES eDHR偏差唯一处理记录';

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_deviation_sequence` (
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `year_month` char(6) NOT NULL COMMENT '上海时区年月YYYYMM',
  `last_value` bigint NOT NULL COMMENT '已分配流水号',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`tenant_id`, `year_month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MES eDHR偏差月度编号序列';

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_deviation_create_request` (
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `idempotency_key` varchar(128) NOT NULL COMMENT '偏差发起幂等键',
  `payload_hash` char(64) NOT NULL COMMENT '规范化请求载荷摘要',
  `deviation_id` bigint DEFAULT NULL COMMENT '已创建偏差ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '保留时间',
  PRIMARY KEY (`tenant_id`, `idempotency_key`),
  UNIQUE KEY `uk_mes_edhr_deviation_request_id` (`tenant_id`, `deviation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MES eDHR偏差发起幂等请求';

DROP PROCEDURE IF EXISTS ensure_mes_edhr_deviation_permissions;
DELIMITER $$
CREATE PROCEDURE ensure_mes_edhr_deviation_permissions()
BEGIN
  IF NOT EXISTS (
      SELECT 1 FROM `system_menu`
      WHERE `id` = 5700 AND `path` = 'pro' AND `type` = 1 AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing MES production management menu 5700';
  END IF;

  IF EXISTS (
      SELECT 1 FROM `system_menu`
      WHERE `id` IN (9008440,9008441,9008443,9008444,9008445,9008446,9008447,9008448,9008449)
        AND NOT (
          (`id` = 9008440 AND `parent_id` = 5700 AND `permission` = 'mes:pro-edhr-deviation:query' AND `type` = 3)
          OR (`id` = 9008441 AND `parent_id` = 5700 AND `permission` = 'mes:pro-edhr-deviation:create' AND `type` = 3)
          OR (`id` = 9008443 AND `parent_id` = 5700 AND `permission` = 'mes:pro-edhr-deviation:handle' AND `type` = 3)
          OR (`id` = 9008444 AND `parent_id` = 5700 AND `permission` = 'mes:pro-edhr-deviation:verify' AND `type` = 3)
          OR (`id` = 9008445 AND `parent_id` = 5700 AND `permission` = 'mes:pro-edhr-deviation:department-confirm' AND `type` = 3)
          OR (`id` = 9008446 AND `parent_id` = 5700 AND `permission` = 'mes:pro-edhr-deviation:qa-close' AND `type` = 3)
          OR (`id` = 9008447 AND `parent_id` = 5700 AND `permission` = 'mes:pro-edhr-deviation:quality-approve' AND `type` = 3)
          OR (`id` = 9008448 AND `parent_id` = 5700 AND `permission` = 'mes:pro-edhr-deviation:critical-management-approve' AND `type` = 3)
          OR (`id` = 9008449 AND `parent_id` = 5700 AND `permission` = 'mes:pro-edhr-deviation:ncr-create' AND `type` = 3)
        )
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Deviation permission menu ID is already used';
  END IF;

  IF EXISTS (
      SELECT 1 FROM `system_menu`
      WHERE `deleted` = b'0'
        AND `permission` IN (
          'mes:pro-edhr-deviation:query', 'mes:pro-edhr-deviation:create',
          'mes:pro-edhr-deviation:handle', 'mes:pro-edhr-deviation:verify',
          'mes:pro-edhr-deviation:department-confirm', 'mes:pro-edhr-deviation:qa-close',
          'mes:pro-edhr-deviation:quality-approve',
          'mes:pro-edhr-deviation:critical-management-approve',
          'mes:pro-edhr-deviation:ncr-create'
        )
        AND `id` NOT IN (9008440,9008441,9008443,9008444,9008445,9008446,9008447,9008448,9008449)
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Deviation permission already belongs to another menu';
  END IF;

  INSERT INTO `system_menu`
    (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  VALUES
    (9008440, '偏差查询', 'mes:pro-edhr-deviation:query', 3, 996, 5700, '', '', '', '', 0, b'0', b'0', b'0', 'edhr-deviation', NOW(), 'edhr-deviation', NOW(), b'0'),
    (9008441, '偏差发起', 'mes:pro-edhr-deviation:create', 3, 997, 5700, '', '', '', '', 0, b'0', b'0', b'0', 'edhr-deviation', NOW(), 'edhr-deviation', NOW(), b'0'),
    (9008443, '偏差处理', 'mes:pro-edhr-deviation:handle', 3, 998, 5700, '', '', '', '', 0, b'0', b'0', b'0', 'edhr-deviation', NOW(), 'edhr-deviation', NOW(), b'0'),
    (9008444, '偏差验证', 'mes:pro-edhr-deviation:verify', 3, 999, 5700, '', '', '', '', 0, b'0', b'0', b'0', 'edhr-deviation', NOW(), 'edhr-deviation', NOW(), b'0'),
    (9008445, '偏差部门负责人确认', 'mes:pro-edhr-deviation:department-confirm', 3, 1000, 5700, '', '', '', '', 0, b'0', b'0', b'0', 'edhr-deviation', NOW(), 'edhr-deviation', NOW(), b'0'),
    (9008446, '偏差QA关闭确认', 'mes:pro-edhr-deviation:qa-close', 3, 1001, 5700, '', '', '', '', 0, b'0', b'0', b'0', 'edhr-deviation', NOW(), 'edhr-deviation', NOW(), b'0'),
    (9008447, '偏差质量负责人批准', 'mes:pro-edhr-deviation:quality-approve', 3, 1002, 5700, '', '', '', '', 0, b'0', b'0', b'0', 'edhr-deviation', NOW(), 'edhr-deviation', NOW(), b'0'),
    (9008448, '关键偏差管理者代表批准', 'mes:pro-edhr-deviation:critical-management-approve', 3, 1003, 5700, '', '', '', '', 0, b'0', b'0', b'0', 'edhr-deviation', NOW(), 'edhr-deviation', NOW(), b'0'),
    (9008449, '关键偏差转不合格评审', 'mes:pro-edhr-deviation:ncr-create', 3, 1004, 5700, '', '', '', '', 0, b'0', b'0', b'0', 'edhr-deviation', NOW(), 'edhr-deviation', NOW(), b'0')
  ON DUPLICATE KEY UPDATE
    `name` = VALUES(`name`), `permission` = VALUES(`permission`), `type` = VALUES(`type`),
    `sort` = VALUES(`sort`), `parent_id` = VALUES(`parent_id`), `status` = VALUES(`status`),
    `visible` = VALUES(`visible`), `keep_alive` = VALUES(`keep_alive`),
    `always_show` = VALUES(`always_show`), `updater` = VALUES(`updater`),
    `update_time` = VALUES(`update_time`), `deleted` = VALUES(`deleted`);
END$$
DELIMITER ;

-- GxP policy operation registration is role based and versioned so later changes
-- add a new version without rewriting historical audit events. The GxP core schema
-- is an explicit migration dependency. Formal policy-version approval is represented
-- by the quality-role electronic signature workflow, not by fabricating an approver user id here.
INSERT INTO `gxp_audit_policy_operation`
  (`tenant_id`, `policy_version`, `operation_id`, `source_type`, `source_locator`,
   `domain`, `subject_type`, `action_type`, `reason_policy`, `signature_policy`,
   `state_policy`, `retention_class`, `test_ids`, `owner`, `applicability`, `active`,
   `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (1, '20260924-edhr-deviation-01', 'edhr.deviation.create', 'SERVICE_METHOD',
   'cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrDeviationServiceImpl#create',
   'EDHR', 'EDHR_DEVIATION', 'CREATE', 'REQUIRED_CATEGORY_AND_TEXT', 'REQUIRED',
   'ABSENT_TO_PRESENT', 'GXP_BATCH_RECORD',
   'P1-AC1,P1-AC5,MesProEdhrDeviationSignatureIntegrationTest', 'ROLE_QA_QUALITY_OWNER',
   'GXP', b'1', 'edhr-deviation', NOW(), 'edhr-deviation', NOW(), b'0')
ON DUPLICATE KEY UPDATE
  `source_type` = VALUES(`source_type`), `source_locator` = VALUES(`source_locator`),
  `domain` = VALUES(`domain`), `subject_type` = VALUES(`subject_type`),
  `action_type` = VALUES(`action_type`), `reason_policy` = VALUES(`reason_policy`),
  `signature_policy` = VALUES(`signature_policy`), `state_policy` = VALUES(`state_policy`),
  `retention_class` = VALUES(`retention_class`), `test_ids` = VALUES(`test_ids`),
  `owner` = VALUES(`owner`), `applicability` = VALUES(`applicability`),
  `active` = VALUES(`active`), `updater` = VALUES(`updater`),
  `update_time` = VALUES(`update_time`), `deleted` = VALUES(`deleted`);

START TRANSACTION;
CALL ensure_mes_edhr_deviation_permissions();
COMMIT;
DROP PROCEDURE IF EXISTS ensure_mes_edhr_deviation_permissions;
