-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260608_edhr_batch_execution_schema; type=schema; riskLevel=medium
CREATE TABLE IF NOT EXISTS `mes_pro_edhr_nonconformance_review` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `review_code` varchar(64) NOT NULL COMMENT '不合格评审单号',
  `source_type` varchar(32) NOT NULL COMMENT '来源类型：PQC_SUBMISSION/PQC_RELEASE',
  `source_id` bigint DEFAULT NULL COMMENT '来源记录ID',
  `batch_execution_id` bigint NOT NULL COMMENT 'eDHR批次执行ID',
  `batch_execution_code` varchar(64) DEFAULT NULL COMMENT 'eDHR批次执行编码',
  `work_order_id` bigint DEFAULT NULL COMMENT '工单ID',
  `work_order_code` varchar(64) DEFAULT NULL COMMENT '工单编码快照',
  `batch_code` varchar(128) DEFAULT NULL COMMENT '批次号',
  `previous_batch_status` int DEFAULT NULL COMMENT '冻结前批次状态',
  `review_status` varchar(32) NOT NULL DEFAULT 'pending_review' COMMENT '评审状态：pending_review/closed',
  `nonconformance_reason` varchar(500) NOT NULL COMMENT '不合格原因',
  `review_material_url` varchar(1000) DEFAULT NULL COMMENT '评审材料URL',
  `review_opinion` varchar(1000) DEFAULT NULL COMMENT '评审意见',
  `qa_signature` varchar(255) DEFAULT NULL COMMENT 'QA签名',
  `qa_user_id` bigint DEFAULT NULL COMMENT 'QA用户ID',
  `frozen_at` datetime NOT NULL COMMENT '冻结时间',
  `closed_at` datetime DEFAULT NULL COMMENT '关闭时间',
  `unfrozen_at` datetime DEFAULT NULL COMMENT '解冻时间',
  `voided_at` datetime DEFAULT NULL COMMENT '作废时间',
  `disposition` varchar(32) DEFAULT NULL COMMENT '处置结论：concession_release/rework/void',
  `trace_snapshot_json` longtext DEFAULT NULL COMMENT '追溯快照JSON',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_edhr_ncr_code` (`tenant_id`, `review_code`),
  KEY `idx_mes_edhr_ncr_batch` (`tenant_id`, `batch_execution_id`, `review_status`),
  KEY `idx_mes_edhr_ncr_work_order` (`tenant_id`, `work_order_id`, `review_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MES eDHR不合格评审单';

DROP PROCEDURE IF EXISTS ensure_mes_edhr_nonconformance_review_menu;
DELIMITER $$
CREATE PROCEDURE ensure_mes_edhr_nonconformance_review_menu()
BEGIN
  IF NOT EXISTS (
      SELECT 1 FROM `system_menu`
      WHERE `id` = 5700 AND `path` = 'pro' AND `type` = 1 AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing MES production management menu 5700';
  END IF;

  IF EXISTS (
      SELECT 1 FROM `system_menu`
      WHERE `id` = 9008300
        AND NOT (`parent_id` = 5700
          AND `path` = 'feedback/edhr-nonconformance-review'
          AND `component` = 'mes/pro/edhr-nonconformance/NonconformanceReviewPage'
          AND `type` = 2)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'system_menu id 9008300 is already used by another menu';
  END IF;

  IF EXISTS (
      SELECT 1 FROM `system_menu`
      WHERE `path` = 'feedback/edhr-nonconformance-review'
        AND `deleted` = b'0'
        AND `id` <> 9008300
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'eDHR nonconformance review path already exists with a different id';
  END IF;

  IF EXISTS (
      SELECT 1 FROM `system_menu`
      WHERE `id` = 9008301
        AND NOT (`parent_id` = 9008300
          AND `permission` = 'mes:pro-edhr-nonconformance-review:query'
          AND `type` = 3)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'system_menu id 9008301 is already used by another menu';
  END IF;

  IF EXISTS (
      SELECT 1 FROM `system_menu`
      WHERE `id` = 9008302
        AND NOT (`parent_id` = 9008300
          AND `permission` = 'mes:pro-edhr-nonconformance-review:create'
          AND `type` = 3)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'system_menu id 9008302 is already used by another menu';
  END IF;

  IF EXISTS (
      SELECT 1 FROM `system_menu`
      WHERE `id` = 9008303
        AND NOT (`parent_id` = 9008300
          AND `permission` = 'mes:pro-edhr-nonconformance-review:dispose'
          AND `type` = 3)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'system_menu id 9008303 is already used by another menu';
  END IF;

  IF EXISTS (
      SELECT 1 FROM `system_menu`
      WHERE `deleted` = b'0'
        AND `permission` = 'mes:pro-edhr-nonconformance-review:query'
        AND `id` NOT IN (9008300, 9008301)
  ) OR EXISTS (
      SELECT 1 FROM `system_menu`
      WHERE `deleted` = b'0'
        AND `permission` = 'mes:pro-edhr-nonconformance-review:create'
        AND `id` <> 9008302
  ) OR EXISTS (
      SELECT 1 FROM `system_menu`
      WHERE `deleted` = b'0'
        AND `permission` = 'mes:pro-edhr-nonconformance-review:dispose'
        AND `id` <> 9008303
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'eDHR nonconformance review permission already belongs to another menu';
  END IF;

  INSERT INTO `system_menu`
    (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  VALUES
    (9008300, 'eDHR不合格评审', 'mes:pro-edhr-nonconformance-review:query', 2, 996, 5700, 'feedback/edhr-nonconformance-review', 'ep:warning-filled', 'mes/pro/edhr-nonconformance/NonconformanceReviewPage', 'MesProFeedbackEdhrNonconformanceReview', 0, b'1', b'1', b'1', 'edhr-nonconformance-review', NOW(), 'edhr-nonconformance-review', NOW(), b'0')
  ON DUPLICATE KEY UPDATE
    `name` = VALUES(`name`), `permission` = VALUES(`permission`), `type` = VALUES(`type`),
    `sort` = VALUES(`sort`), `parent_id` = VALUES(`parent_id`), `path` = VALUES(`path`),
    `icon` = VALUES(`icon`), `component` = VALUES(`component`),
    `component_name` = VALUES(`component_name`), `status` = VALUES(`status`),
    `visible` = VALUES(`visible`), `keep_alive` = VALUES(`keep_alive`),
    `always_show` = VALUES(`always_show`), `updater` = VALUES(`updater`),
    `update_time` = VALUES(`update_time`), `deleted` = VALUES(`deleted`);

  INSERT INTO `system_menu`
    (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  VALUES
    (9008301, 'eDHR不合格评审查询', 'mes:pro-edhr-nonconformance-review:query', 3, 1, 9008300, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-nonconformance-review', NOW(), 'edhr-nonconformance-review', NOW(), b'0'),
    (9008302, 'eDHR不合格评审创建', 'mes:pro-edhr-nonconformance-review:create', 3, 2, 9008300, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-nonconformance-review', NOW(), 'edhr-nonconformance-review', NOW(), b'0'),
    (9008303, 'eDHR不合格评审处置', 'mes:pro-edhr-nonconformance-review:dispose', 3, 3, 9008300, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-nonconformance-review', NOW(), 'edhr-nonconformance-review', NOW(), b'0')
  ON DUPLICATE KEY UPDATE
    `name` = VALUES(`name`), `permission` = VALUES(`permission`), `type` = VALUES(`type`),
    `sort` = VALUES(`sort`), `parent_id` = VALUES(`parent_id`), `path` = VALUES(`path`),
    `icon` = VALUES(`icon`), `component` = VALUES(`component`),
    `component_name` = VALUES(`component_name`), `status` = VALUES(`status`),
    `visible` = VALUES(`visible`), `keep_alive` = VALUES(`keep_alive`),
    `always_show` = VALUES(`always_show`), `updater` = VALUES(`updater`),
    `update_time` = VALUES(`update_time`), `deleted` = VALUES(`deleted`);
END$$
DELIMITER ;

START TRANSACTION;
CALL ensure_mes_edhr_nonconformance_review_menu();
COMMIT;
DROP PROCEDURE IF EXISTS ensure_mes_edhr_nonconformance_review_menu;
