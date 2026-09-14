-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260815_system_notify_message_business_key,20260907_dcc_publication_impact_assessment; type=schema; riskLevel=medium
-- Idempotent notification delivery ledger for new DCC publication follow-up batches only.

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `dcc_publication_notification_delivery` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `batch_id` BIGINT NOT NULL COMMENT '发布后续批次 ID',
  `candidate_id` BIGINT NOT NULL COMMENT '通知候选 ID',
  `user_id` BIGINT NOT NULL COMMENT '收件用户 ID',
  `business_key` VARCHAR(255) NOT NULL COMMENT '平台消息幂等业务键',
  `template_code` VARCHAR(64) NOT NULL COMMENT '通知模板',
  `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SENT/FAILED',
  `attempt_count` INT NOT NULL DEFAULT 0 COMMENT '发送尝试次数',
  `last_attempt_at` DATETIME NULL COMMENT '最近尝试时间',
  `sent_at` DATETIME NULL COMMENT '发送成功时间',
  `system_message_id` BIGINT NULL COMMENT '平台站内信 ID',
  `last_error_summary` VARCHAR(512) NULL COMMENT '脱敏错误摘要',
  `row_version` INT NOT NULL DEFAULT 0 COMMENT 'CAS 版本',
  `creation_token` VARCHAR(36) NOT NULL COMMENT '并发幂等创建令牌',
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator` VARCHAR(64) NULL,
  `updater` VARCHAR(64) NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_pub_delivery_candidate` (`tenant_id`, `batch_id`, `candidate_id`, `deleted`),
  UNIQUE KEY `uk_dcc_pub_delivery_business_key` (`tenant_id`, `business_key`, `deleted`),
  KEY `idx_dcc_pub_delivery_status` (`tenant_id`, `batch_id`, `status`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC 发布通知投递';

CREATE TABLE IF NOT EXISTS `dcc_publication_notification_audit` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `delivery_id` BIGINT NOT NULL COMMENT '通知投递 ID',
  `batch_id` BIGINT NOT NULL COMMENT '发布后续批次 ID',
  `action_type` VARCHAR(32) NOT NULL COMMENT 'MATERIALIZE/ATTEMPT/SENT/FAILED/RETRY',
  `actor_id` BIGINT NULL COMMENT '操作人',
  `reason` VARCHAR(1000) NULL COMMENT '操作原因',
  `status_before` VARCHAR(16) NULL,
  `status_after` VARCHAR(16) NOT NULL,
  `attempt_count` INT NOT NULL,
  `system_message_id` BIGINT NULL,
  `error_summary` VARCHAR(512) NULL,
  `row_version_before` INT NOT NULL,
  `row_version_after` INT NOT NULL,
  `occurred_at` DATETIME NOT NULL,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator` VARCHAR(64) NULL,
  `updater` VARCHAR(64) NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_dcc_pub_notification_audit_delivery` (`tenant_id`, `delivery_id`, `id`, `deleted`),
  KEY `idx_dcc_pub_notification_audit_batch` (`tenant_id`, `batch_id`, `id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC 发布通知不可变审计';

DROP PROCEDURE IF EXISTS ensure_dcc_publication_notification_p3;
DELIMITER $$
CREATE PROCEDURE ensure_dcc_publication_notification_p3()
BEGIN
  DECLARE v_parent_count INT DEFAULT 0;
  DECLARE v_source_count INT DEFAULT 0;
  DECLARE v_source_menu_id BIGINT DEFAULT NULL;
  DECLARE v_template_count INT DEFAULT 0;
  DECLARE v_approve_menu_count INT DEFAULT 0;
  DECLARE v_approve_menu_id BIGINT DEFAULT NULL;

  SELECT COUNT(*) INTO v_parent_count FROM `system_menu` WHERE `id` = 6800 AND `deleted` = b'0';
  IF v_parent_count <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing or duplicate DCC parent menu 6800';
  END IF;

  SELECT COUNT(*), MAX(`id`) INTO v_source_count, v_source_menu_id
  FROM `system_menu` WHERE `path` = 'controlled-file/categories' AND `deleted` = b'0';
  IF v_source_count <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing or duplicate DCC category-management source menu';
  END IF;
  SELECT COUNT(*), MAX(`id`) INTO v_approve_menu_count, v_approve_menu_id
  FROM `system_menu` WHERE `permission` = 'dcc:controlled-file:approve' AND `deleted` = b'0' AND `status` = 0;
  IF v_approve_menu_count <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing or duplicate active DCC approve permission menu';
  END IF;
  IF EXISTS (
      SELECT 1 FROM `system_role`
      WHERE `code` = 'doc_control' AND `deleted` = b'0' AND `status` = 0
      GROUP BY `tenant_id` HAVING COUNT(*) > 1
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Duplicate active doc_control role in tenant';
  END IF;
  IF EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 6830 AND `path` <> 'controlled-file/publication-followup') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Menu id 6830 is occupied by another business';
  END IF;
  IF EXISTS (SELECT 1 FROM `system_menu` WHERE `path` = 'controlled-file/publication-followup' AND `id` <> 6830 AND `deleted` = b'0') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'DCC publication-followup path is occupied by another menu';
  END IF;
  IF EXISTS (SELECT 1 FROM `system_tenant_package` WHERE `deleted` = b'0' AND JSON_VALID(`menu_ids`) = 0) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON';
  END IF;

  SELECT COUNT(*) INTO v_template_count
  FROM `system_notify_template` WHERE `code` = 'dcc_publication_released' AND `deleted` = b'0';
  IF v_template_count > 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Duplicate DCC publication notification template';
  END IF;
  IF v_template_count = 0 THEN
    INSERT INTO `system_notify_template`
      (`name`, `code`, `type`, `nickname`, `content`, `params`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
    VALUES ('DCC发布后续通知', 'dcc_publication_released', 2, 'DCC系统',
            '受控文件 {fileNumber} {versionNo} 已正式发布，请查看发布后续。',
            '["fileNumber","versionNo","followupUrl"]', 0, 'DCC 新大版本发布后续通知',
            '1', NOW(), '1', NOW(), b'0');
  ELSE
    UPDATE `system_notify_template`
    SET `name` = 'DCC发布后续通知', `type` = 2, `nickname` = 'DCC系统',
        `content` = '受控文件 {fileNumber} {versionNo} 已正式发布，请查看发布后续。',
        `params` = '["fileNumber","versionNo","followupUrl"]', `status` = 0,
        `remark` = 'DCC 新大版本发布后续通知', `updater` = '1', `update_time` = NOW()
    WHERE `code` = 'dcc_publication_released' AND `deleted` = b'0';
  END IF;

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 6830, '发布后续', 'dcc:controlled-file:publication-followup:manage', 2, 65, 6800,
         'controlled-file/publication-followup', 'ep:bell', 'dcc/controlled-file/publication-followup/index',
         'DccControlledFilePublicationFollowup', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 6830);

  UPDATE `system_menu`
  SET `name` = '发布后续', `permission` = 'dcc:controlled-file:publication-followup:manage',
      `parent_id` = 6800, `path` = 'controlled-file/publication-followup', `icon` = 'ep:bell',
      `component` = 'dcc/controlled-file/publication-followup/index',
      `component_name` = 'DccControlledFilePublicationFollowup', `status` = 0, `visible` = b'1',
      `keep_alive` = b'1', `always_show` = b'1', `updater` = '1', `update_time` = NOW()
  WHERE `id` = 6830 AND `deleted` = b'0';

  INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `updater`, `tenant_id`)
  SELECT target_role.`id`, 6830, '1', '1', target_role.`tenant_id`
  FROM `system_role` target_role
  JOIN `system_role_menu` approval_role_menu
    ON approval_role_menu.`role_id` = target_role.`id`
   AND approval_role_menu.`tenant_id` = target_role.`tenant_id`
   AND approval_role_menu.`menu_id` = v_approve_menu_id
   AND approval_role_menu.`deleted` = b'0'
  JOIN `system_menu` approval_menu
    ON approval_menu.`id` = approval_role_menu.`menu_id`
   AND approval_menu.`permission` = 'dcc:controlled-file:approve'
   AND approval_menu.`deleted` = b'0'
  WHERE target_role.`code` = 'doc_control' AND target_role.`status` = 0 AND target_role.`deleted` = b'0'
    AND NOT EXISTS (SELECT 1 FROM `system_role_menu` existing WHERE existing.`role_id` = target_role.`id` AND existing.`menu_id` = 6830 AND existing.`tenant_id` = target_role.`tenant_id` AND existing.`deleted` = b'0');

  UPDATE `system_tenant_package`
  SET `menu_ids` = JSON_ARRAY_APPEND(CAST(`menu_ids` AS JSON), '$', 6830),
      `updater` = '1', `update_time` = NOW()
  WHERE `deleted` = b'0'
    AND JSON_CONTAINS(CAST(`menu_ids` AS JSON), CAST(v_source_menu_id AS CHAR), '$') = 1
    AND JSON_CONTAINS(CAST(`menu_ids` AS JSON), '6830', '$') = 0;

  IF NOT EXISTS (
      SELECT 1 FROM `system_notify_template`
      WHERE `code` = 'dcc_publication_released' AND `nickname` = 'DCC系统'
        AND `content` = '受控文件 {fileNumber} {versionNo} 已正式发布，请查看发布后续。'
        AND `params` = '["fileNumber","versionNo","followupUrl"]' AND `status` = 0 AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'DCC publication notification template verification failed';
  END IF;
  IF NOT EXISTS (
      SELECT 1 FROM `system_menu`
      WHERE `id` = 6830 AND `parent_id` = 6800 AND `path` = 'controlled-file/publication-followup'
        AND `component` = 'dcc/controlled-file/publication-followup/index'
        AND `component_name` = 'DccControlledFilePublicationFollowup'
        AND `permission` = 'dcc:controlled-file:publication-followup:manage'
        AND `status` = 0 AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'DCC publication-followup menu verification failed';
  END IF;
  IF EXISTS (
      SELECT 1 FROM `system_role` target_role
      JOIN `system_role_menu` approval_role_menu
        ON approval_role_menu.`role_id` = target_role.`id`
       AND approval_role_menu.`tenant_id` = target_role.`tenant_id`
       AND approval_role_menu.`menu_id` = v_approve_menu_id
       AND approval_role_menu.`deleted` = b'0'
      LEFT JOIN `system_role_menu` target_role_menu
        ON target_role_menu.`role_id` = target_role.`id`
       AND target_role_menu.`tenant_id` = target_role.`tenant_id`
       AND target_role_menu.`menu_id` = 6830
       AND target_role_menu.`deleted` = b'0'
      WHERE target_role.`code` = 'doc_control' AND target_role.`status` = 0
        AND target_role.`deleted` = b'0' AND target_role_menu.`id` IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'DCC publication-followup role binding verification failed';
  END IF;
END $$
DELIMITER ;

CALL ensure_dcc_publication_notification_p3();
DROP PROCEDURE IF EXISTS ensure_dcc_publication_notification_p3;
