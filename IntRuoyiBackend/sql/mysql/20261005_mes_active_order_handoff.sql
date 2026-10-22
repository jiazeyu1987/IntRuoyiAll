-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260815_system_notify_message_business_key,20260811_mes_process_pool_pqc_repeat_review_constraint; type=schema; riskLevel=medium
-- Reviewed execution/backup belongs to the root task. No business assignment, policy activation or old-cycle notification backfill.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS migrate_mes_active_order_handoff;
DELIMITER $$
CREATE PROCEDURE migrate_mes_active_order_handoff()
BEGIN
  DECLARE v_count INT DEFAULT 0;
  DECLARE v_columns VARCHAR(255);
  SELECT COUNT(*) INTO v_count FROM (
    SELECT INDEX_NAME FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='system_notify_message' AND NON_UNIQUE=0
    GROUP BY INDEX_NAME HAVING GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',')='tenant_id,business_key' AND COUNT(SUB_PART)=0
  ) platform_identity;
  IF v_count<>1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Required platform notify tenant,business identity missing or ambiguous'; END IF;
  IF NOT EXISTS(SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='mes_pro_process_pool_submission_review') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Formal submission review table missing';
  END IF;
  IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='mes_pro_process_pool_submission_review' AND COLUMN_NAME='review_round') THEN
    ALTER TABLE mes_pro_process_pool_submission_review ADD review_round INT NOT NULL DEFAULT 0 COMMENT 'Immutable formal review round, existing history numbered by id';
    -- Preserve every historical row and its decision; only add deterministic round metadata.
    UPDATE mes_pro_process_pool_submission_review r JOIN (
      SELECT id,ROW_NUMBER() OVER(PARTITION BY tenant_id,event_id ORDER BY id)-1 AS formal_round FROM mes_pro_process_pool_submission_review
    ) numbered ON numbered.id=r.id SET r.review_round=numbered.formal_round;
  ELSEIF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='mes_pro_process_pool_submission_review' AND COLUMN_NAME='review_round' AND DATA_TYPE='int' AND IS_NULLABLE='NO') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Incompatible formal review_round column';
  END IF;
  IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='mes_pro_process_pool_submission_review' AND COLUMN_NAME='source_revision_id') THEN
    ALTER TABLE mes_pro_process_pool_submission_review ADD source_revision_id BIGINT NULL COMMENT 'Exact signed correction revision, initial submission NULL';
  END IF;
  IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='mes_pro_process_pool_submission_review' AND COLUMN_NAME='superseded_review_id') THEN
    ALTER TABLE mes_pro_process_pool_submission_review ADD superseded_review_id BIGINT NULL COMMENT 'Original rejected review preserved';
  END IF;
  IF EXISTS(SELECT 1 FROM mes_pro_process_pool_submission_review WHERE review_round<0) OR EXISTS(
      SELECT 1 FROM mes_pro_process_pool_submission_review GROUP BY tenant_id,event_id,review_round,deleted HAVING COUNT(*)>1) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Invalid or duplicate formal review round blocks migration';
  END IF;
  IF EXISTS(SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='mes_pro_process_pool_submission_review' AND INDEX_NAME='uk_mes_pp_submission_review_production_terminal') THEN
    SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX),MIN(NON_UNIQUE) INTO v_columns,v_count FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='mes_pro_process_pool_submission_review' AND INDEX_NAME='uk_mes_pp_submission_review_production_terminal';
    IF v_columns<>'tenant_id,production_terminal_event_id' OR v_count<>0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Unexpected production terminal index contract'; END IF;
    ALTER TABLE mes_pro_process_pool_submission_review DROP INDEX uk_mes_pp_submission_review_production_terminal;
  END IF;
  IF EXISTS(SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='mes_pro_process_pool_submission_review' AND INDEX_NAME IN('uk_mes_pp_submission_review_event','uk_mes_pp_submission_review_event_terminal')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Prerequisite repeat-review constraint migration not applied';
  END IF;
  IF NOT EXISTS(SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='mes_pro_process_pool_submission_review' AND INDEX_NAME='uk_mes_pp_submission_review_round') THEN
    ALTER TABLE mes_pro_process_pool_submission_review ADD UNIQUE KEY uk_mes_pp_submission_review_round(tenant_id,event_id,review_round,deleted);
  ELSE
    SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX),MIN(NON_UNIQUE) INTO v_columns,v_count FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='mes_pro_process_pool_submission_review' AND INDEX_NAME='uk_mes_pp_submission_review_round';
    IF v_columns<>'tenant_id,event_id,review_round,deleted' OR v_count<>0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Unexpected formal review round index contract'; END IF;
  END IF;
END$$
DELIMITER ;
CALL migrate_mes_active_order_handoff();
DROP PROCEDURE migrate_mes_active_order_handoff;

-- Independent audit ledger: intentionally no FK to orders/events that a formal reset may physically remove.
CREATE TABLE IF NOT EXISTS mes_active_order_handoff_task (
 id BIGINT NOT NULL,tenant_id BIGINT NOT NULL,active_order_id BIGINT NOT NULL,work_order_id BIGINT NOT NULL,route_process_id BIGINT NULL,
 task_type VARCHAR(40) NOT NULL,source_type VARCHAR(40) NOT NULL,source_id BIGINT NOT NULL,round_id BIGINT NOT NULL,
 candidate_user_snapshot TEXT NOT NULL,responsibility_snapshot_json LONGTEXT NOT NULL,initiated_by BIGINT NOT NULL,
 notification_only BIT(1) NOT NULL,status VARCHAR(16) NOT NULL,action_url VARCHAR(1000) NOT NULL,reason TEXT NOT NULL,
 row_version INT NOT NULL DEFAULT 0,completed_by BIGINT NULL,completion_source_id BIGINT NULL,completed_at DATETIME NULL,
 creator VARCHAR(64) NULL,updater VARCHAR(64) NULL,create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,deleted BIT(1) NOT NULL DEFAULT b'0',
 PRIMARY KEY(id),UNIQUE KEY uk_mes_handoff_cycle_source(tenant_id,active_order_id,task_type,source_type,source_id,round_id),
 KEY idx_mes_handoff_cycle_status(tenant_id,active_order_id,status),KEY idx_mes_handoff_source(tenant_id,source_type,source_id,round_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Frozen active-cycle formal handoff ledger';
CREATE TABLE IF NOT EXISTS mes_active_order_handoff_delivery (
 id BIGINT NOT NULL AUTO_INCREMENT,tenant_id BIGINT NOT NULL,handoff_task_id BIGINT NOT NULL,user_id BIGINT NOT NULL,
 business_key VARCHAR(255) NOT NULL,template_params_json LONGTEXT NOT NULL,status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
 attempt_count INT NOT NULL DEFAULT 0,row_version INT NOT NULL DEFAULT 0,last_attempt_at DATETIME NULL,sent_at DATETIME NULL,
 system_message_id BIGINT NULL,last_error_summary VARCHAR(512) NULL,
 creator VARCHAR(64) NULL,updater VARCHAR(64) NULL,create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,deleted BIT(1) NOT NULL DEFAULT b'0',
 PRIMARY KEY(id),UNIQUE KEY uk_mes_handoff_recipient(tenant_id,handoff_task_id,user_id),
 UNIQUE KEY uk_mes_handoff_delivery_business(tenant_id,business_key),KEY idx_mes_handoff_delivery_status(tenant_id,status,last_attempt_at,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Handoff transactional intent and recoverable delivery receipts';

DROP PROCEDURE IF EXISTS seed_mes_active_order_handoff_template;
DELIMITER $$
CREATE PROCEDURE seed_mes_active_order_handoff_template()
BEGIN
  DECLARE v_count INT DEFAULT 0;
  SELECT COUNT(*) INTO v_count FROM system_notify_template WHERE code='MES_ACTIVE_ORDER_HANDOFF';
  IF v_count>1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Ambiguous active-order handoff template'; END IF;
  IF v_count=0 THEN
    INSERT INTO system_notify_template(name,code,type,nickname,content,params,status,remark,creator,create_time,updater,update_time,deleted)
    VALUES('活跃订单正式交接','MES_ACTIVE_ORDER_HANDOFF',2,'MES系统','活跃订单{activeOrderId}：{taskName}。原因：{reason}。入口：{actionUrl}',
      '["activeOrderId","taskName","actionUrl","reason","handoffTaskId","handoffType"]',0,'正式冻结周期交接；不回填历史；不替代放行任务合同','1',NOW(),'1',NOW(),b'0');
  ELSEIF (SELECT COUNT(*) FROM system_notify_template WHERE code='MES_ACTIVE_ORDER_HANDOFF' AND deleted=b'0' AND status=0 AND JSON_VALID(params)
      AND JSON_LENGTH(params)=6 AND JSON_CONTAINS(params,JSON_ARRAY('activeOrderId','taskName','actionUrl','reason','handoffTaskId','handoffType')))<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Existing handoff template requires explicit configuration review';
  END IF;
END$$
DELIMITER ;
CALL seed_mes_active_order_handoff_template();
DROP PROCEDURE seed_mes_active_order_handoff_template;
