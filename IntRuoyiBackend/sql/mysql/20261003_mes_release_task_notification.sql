-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260815_system_notify_message_business_key; type=schema; riskLevel=medium
-- PROPOSAL ONLY: execution requires separate authorization. No historical task/message backfill.
-- New release ASSIGNED recipient ledger; the existing manager template is never rewritten.
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS preflight_mes_release_task_notification;
DELIMITER $$
CREATE PROCEDURE preflight_mes_release_task_notification()
BEGIN
  DECLARE v_unique_count INT DEFAULT 0;
  SELECT COUNT(*) INTO v_unique_count
  FROM (
    SELECT INDEX_NAME
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='system_notify_message' AND NON_UNIQUE=0
    GROUP BY INDEX_NAME
    HAVING GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',')='tenant_id,business_key'
       AND COUNT(SUB_PART)=0
  ) AS platform_identity;
  IF v_unique_count<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Required platform unique tenant_id,business_key identity is missing or ambiguous';
  END IF;
END$$
DELIMITER ;
CALL preflight_mes_release_task_notification();
DROP PROCEDURE preflight_mes_release_task_notification;

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_release_task_notify_delivery` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `work_task_id` BIGINT NOT NULL,
  `event_type` VARCHAR(32) NOT NULL COMMENT '本合同仅 ASSIGNED',
  `user_id` BIGINT NOT NULL,
  `business_key` VARCHAR(255) NOT NULL,
  `template_code` VARCHAR(128) NOT NULL,
  `template_params_json` LONGTEXT NOT NULL COMMENT '从正式任务冻结的字符串参数',
  `initiated_by` BIGINT NOT NULL COMMENT '申请人或正式 PQC 决策人',
  `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SENT/FAILED',
  `attempt_count` INT NOT NULL DEFAULT 0,
  `row_version` INT NOT NULL DEFAULT 0,
  `last_attempt_at` DATETIME NULL,
  `sent_at` DATETIME NULL,
  `system_message_id` BIGINT NULL,
  `last_error_summary` VARCHAR(512) NULL COMMENT '仅异常类型及业务码，不存原始异常载荷',
  `creator` VARCHAR(64) NULL,
  `updater` VARCHAR(64) NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` BIT(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_release_notify_recipient` (`tenant_id`,`work_task_id`,`event_type`,`user_id`),
  UNIQUE KEY `uk_mes_release_notify_business` (`tenant_id`,`business_key`),
  KEY `idx_mes_release_notify_pending` (`tenant_id`,`status`,`last_attempt_at`,`id`),
  KEY `idx_mes_release_notify_task` (`tenant_id`,`work_task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='正式放行任务候选站内信投递';

DROP PROCEDURE IF EXISTS seed_mes_release_task_notification_template;
DELIMITER $$
CREATE PROCEDURE seed_mes_release_task_notification_template()
BEGIN
  DECLARE v_count INT DEFAULT 0;
  DECLARE v_valid INT DEFAULT 0;
  SELECT COUNT(*) INTO v_count FROM `system_notify_template`
    WHERE `code`='MES_EDHR_PQC_PRODUCTION_RELEASE_TASK_ASSIGNED';
  IF v_count>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Ambiguous PQC release notification template identity';
  END IF;
  IF v_count=0 THEN
    INSERT INTO `system_notify_template`
      (`name`,`code`,`type`,`nickname`,`content`,`params`,`status`,`remark`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
    VALUES ('PQC生产放行待办通知','MES_EDHR_PQC_PRODUCTION_RELEASE_TASK_ASSIGNED',2,'MES系统',
      '工单{workOrderCode}批次{batchCode}已申请PQC生产放行，请处理任务{workTaskId}。入口：{actionUrl}',
      '["workOrderCode","batchCode","processName","actionUrl","workTaskId","reason"]',0,
      '新正式任务冻结候选通知；历史任务不回填','1',NOW(),'1',NOW(),b'0');
  ELSE
    SELECT COUNT(*) INTO v_valid FROM `system_notify_template`
      WHERE `code`='MES_EDHR_PQC_PRODUCTION_RELEASE_TASK_ASSIGNED' AND `deleted`=b'0'
        AND `status`=0 AND JSON_VALID(`params`)
        AND JSON_LENGTH(`params`)=6
        AND JSON_CONTAINS(`params`,JSON_ARRAY('workOrderCode','batchCode','processName','actionUrl','workTaskId','reason'));
    IF v_valid<>1 THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Existing PQC template requires explicit configuration review; migration will not overwrite it';
    END IF;
  END IF;
END$$
DELIMITER ;
CALL seed_mes_release_task_notification_template();
DROP PROCEDURE seed_mes_release_task_notification_template;
