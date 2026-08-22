-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260618_mes_edhr_release_transaction_lifecycle; type=schema; riskLevel=medium
-- Flow 10 final decision ledger. Additive only: no historical status rewrite and no ERP status update.

DROP PROCEDURE IF EXISTS ensure_mes_edhr_release_final_state_columns;
DELIMITER $$
CREATE PROCEDURE ensure_mes_edhr_release_final_state_columns()
BEGIN
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
       WHERE TABLE_SCHEMA = DATABASE()
         AND TABLE_NAME = 'mes_pro_edhr_release_transaction'
         AND COLUMN_NAME = 'release_decision_id'
  ) THEN
    ALTER TABLE mes_pro_edhr_release_transaction
      ADD COLUMN release_decision_id bigint DEFAULT NULL COMMENT '最终放行决定ID' AFTER release_status;
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
       WHERE TABLE_SCHEMA = DATABASE()
         AND TABLE_NAME = 'mes_pro_edhr_release_transaction'
         AND COLUMN_NAME = 'finalization_payload_hash'
  ) THEN
    ALTER TABLE mes_pro_edhr_release_transaction
      ADD COLUMN finalization_payload_hash char(64) DEFAULT NULL COMMENT '最终化规范载荷摘要' AFTER release_decision_id;
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
       WHERE TABLE_SCHEMA = DATABASE()
         AND TABLE_NAME = 'mes_pro_process_pool_active_order'
         AND COLUMN_NAME = 'release_decision_id'
  ) THEN
    ALTER TABLE mes_pro_process_pool_active_order
      ADD COLUMN release_decision_id bigint DEFAULT NULL COMMENT '最终放行决定ID';
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
       WHERE TABLE_SCHEMA = DATABASE()
         AND TABLE_NAME = 'mes_pro_process_pool_active_order'
         AND COLUMN_NAME = 'released_by'
  ) THEN
    ALTER TABLE mes_pro_process_pool_active_order
      ADD COLUMN released_by bigint DEFAULT NULL COMMENT '最终放行联动操作人';
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
       WHERE TABLE_SCHEMA = DATABASE()
         AND TABLE_NAME = 'mes_pro_process_pool_active_order'
         AND COLUMN_NAME = 'released_at'
  ) THEN
    ALTER TABLE mes_pro_process_pool_active_order
      ADD COLUMN released_at datetime DEFAULT NULL COMMENT '最终放行联动时间';
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
       WHERE TABLE_SCHEMA = DATABASE()
         AND TABLE_NAME = 'mes_pro_work_order'
         AND COLUMN_NAME = 'release_decision_id'
  ) THEN
    ALTER TABLE mes_pro_work_order
      ADD COLUMN release_decision_id bigint DEFAULT NULL COMMENT '最终放行决定ID';
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
       WHERE TABLE_SCHEMA = DATABASE()
         AND TABLE_NAME = 'mes_pro_work_order'
         AND COLUMN_NAME = 'released_by'
  ) THEN
    ALTER TABLE mes_pro_work_order
      ADD COLUMN released_by bigint DEFAULT NULL COMMENT '最终放行联动操作人';
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
       WHERE TABLE_SCHEMA = DATABASE()
         AND TABLE_NAME = 'mes_pro_work_order'
         AND COLUMN_NAME = 'released_at'
  ) THEN
    ALTER TABLE mes_pro_work_order
      ADD COLUMN released_at datetime DEFAULT NULL COMMENT '最终放行联动时间';
  END IF;
END$$
DELIMITER ;

CALL ensure_mes_edhr_release_final_state_columns();
DROP PROCEDURE IF EXISTS ensure_mes_edhr_release_final_state_columns;

CREATE TABLE IF NOT EXISTS mes_pro_edhr_release_decision (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  release_transaction_id bigint NOT NULL COMMENT '放行事务ID',
  release_application_id bigint DEFAULT NULL COMMENT '放行申请ID（存在时关联）',
  batch_execution_id bigint NOT NULL COMMENT '已存在批次执行ID',
  work_order_id bigint DEFAULT NULL COMMENT '生产工单ID',
  active_order_id bigint DEFAULT NULL COMMENT '活跃订单ID（活跃订单来源）',
  pick_list_binding_id varchar(128) DEFAULT NULL COMMENT '正式领料绑定ID（活跃订单来源）',
  completion_event_id varchar(128) DEFAULT NULL COMMENT '流程4完成事件ID',
  completion_backfill_receipt_id varchar(128) DEFAULT NULL COMMENT '流程4回填成功回执ID',
  origin varchar(32) DEFAULT NULL COMMENT '来源：ACTIVE_ORDER/MANUAL/SCHEDULED/PQC_INDEPENDENT',
  entry_type varchar(32) DEFAULT NULL COMMENT '入口类型，必须与origin一致',
  source_relation varchar(512) DEFAULT NULL COMMENT '流程7正式来源关系',
  source_snapshot_hash char(64) DEFAULT NULL COMMENT '来源快照摘要',
  material_gate_receipt_id varchar(128) DEFAULT NULL COMMENT '流程8四材料门禁回执ID',
  material_gate_snapshot_hash char(64) DEFAULT NULL COMMENT '四材料manifest摘要',
  material_gate_version int DEFAULT NULL COMMENT '四材料manifest版本',
  decision_status varchar(32) NOT NULL COMMENT 'RELEASED/REJECTED/WITHDRAWN',
  idempotency_key varchar(128) NOT NULL COMMENT '业务幂等键',
  payload_hash char(64) NOT NULL COMMENT '最终化载荷摘要',
  actor_user_id bigint NOT NULL COMMENT '决定人',
  signoff_evidence_hash char(64) DEFAULT NULL COMMENT '签核证据摘要',
  approval_opinion varchar(500) DEFAULT NULL COMMENT '审批意见',
  decision_reason varchar(500) DEFAULT NULL COMMENT '驳回或撤回原因',
  audit_snapshot_json longtext NOT NULL COMMENT '来源与状态审计快照',
  decided_at datetime NOT NULL COMMENT '决定时间',
  version int NOT NULL DEFAULT 1 COMMENT '版本号',
  creator varchar(64) DEFAULT '',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater varchar(64) DEFAULT '',
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted bit(1) NOT NULL DEFAULT b'0',
  tenant_id bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_mes_edhr_release_decision_status (
      tenant_id, release_transaction_id, decision_status, deleted),
  UNIQUE KEY uk_mes_edhr_release_decision_transaction (
      tenant_id, release_transaction_id, deleted),
  UNIQUE KEY uk_mes_edhr_release_decision_idempotency (
      tenant_id, release_transaction_id, decision_status, idempotency_key, deleted),
  KEY idx_mes_edhr_release_decision_batch (
      tenant_id, batch_execution_id, decision_status, decided_at, deleted),
  KEY idx_mes_edhr_release_decision_active_order (
      tenant_id, active_order_id, decided_at, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES eDHR 最终放行决定账本';

DELIMITER $$
CREATE PROCEDURE ensure_mes_edhr_release_decision_transaction_index()
BEGIN
  DECLARE duplicate_count BIGINT DEFAULT 0;
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.STATISTICS
       WHERE TABLE_SCHEMA = DATABASE()
         AND TABLE_NAME = 'mes_pro_edhr_release_decision'
         AND INDEX_NAME = 'uk_mes_edhr_release_decision_transaction'
  ) THEN
    SELECT COUNT(*) INTO duplicate_count
      FROM (
        SELECT tenant_id, release_transaction_id, deleted
          FROM mes_pro_edhr_release_decision
         GROUP BY tenant_id, release_transaction_id, deleted
        HAVING COUNT(*) > 1
      ) duplicate_rows;
    IF duplicate_count > 0 THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'duplicate terminal release decisions require historical reconciliation before Flow 10 migration';
    END IF;
    ALTER TABLE mes_pro_edhr_release_decision
      ADD UNIQUE KEY uk_mes_edhr_release_decision_transaction
        (tenant_id, release_transaction_id, deleted);
  END IF;
END$$
DELIMITER ;

CALL ensure_mes_edhr_release_decision_transaction_index();
DROP PROCEDURE IF EXISTS ensure_mes_edhr_release_decision_transaction_index;
