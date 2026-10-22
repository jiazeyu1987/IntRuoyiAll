-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260929_mes_pqc_task_frozen_identity_columns; type=schema; riskLevel=medium
-- New PQC tasks belong to an order/QA process/stage, independently of production processes.
-- Existing task rows and their evidence are preserved; no historical regrouping is performed.
DROP PROCEDURE IF EXISTS migrate_mes_pqc_order_qa_stage_tasks;
DELIMITER $$
CREATE PROCEDURE migrate_mes_pqc_order_qa_stage_tasks()
BEGIN
  DECLARE v_count INT DEFAULT 0;
  ALTER TABLE mes_pqc_inspection_task
    MODIFY COLUMN route_process_id bigint NULL COMMENT '历史生产路线工序；整单PQC任务为空',
    MODIFY COLUMN process_id bigint NULL COMMENT '历史生产工序；整单PQC任务为空';
  ALTER TABLE mes_pqc_process_inspection_aggregate_detail
    MODIFY COLUMN route_process_id bigint NULL COMMENT '历史生产路线工序；整单PQC汇集为空',
    MODIFY COLUMN process_id bigint NULL COMMENT '历史生产工序；整单PQC汇集为空';
  ALTER TABLE mes_pro_process_pool_event_revision
    MODIFY COLUMN route_process_id bigint NULL COMMENT '生产路线工序；整单PQC补正为空',
    MODIFY COLUMN process_id bigint NULL COMMENT '生产工序；整单PQC补正为空';
  SELECT COUNT(*) INTO v_count FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'mes_pqc_inspection_task'
      AND column_name = 'inspection_item_plan_json';
  IF v_count = 0 THEN
    ALTER TABLE mes_pqc_inspection_task ADD COLUMN inspection_item_plan_json json NULL
      COMMENT '整单QA工序阶段的逐项目冻结抽样数量';
  END IF;
  SELECT COUNT(*) INTO v_count FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'mes_pqc_inspection_task'
      AND column_name = 'qa_stage_active_order_id';
  IF v_count = 0 THEN
    ALTER TABLE mes_pqc_inspection_task ADD COLUMN qa_stage_active_order_id bigint
      GENERATED ALWAYS AS (CASE WHEN inspection_item_plan_json IS NOT NULL THEN active_order_id ELSE NULL END) STORED;
  END IF;
  SELECT COUNT(*) INTO v_count FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'mes_pqc_inspection_task'
      AND index_name = 'uk_mes_pqc_order_qa_stage';
  IF v_count = 0 THEN
    ALTER TABLE mes_pqc_inspection_task ADD UNIQUE KEY uk_mes_pqc_order_qa_stage
      (tenant_id, qa_stage_active_order_id, regulation_version_id, qa_process_id,
       inspection_rule_key, business_date, shift_code, round_no, deleted);
  END IF;
END$$
DELIMITER ;
CALL migrate_mes_pqc_order_qa_stage_tasks();
DROP PROCEDURE IF EXISTS migrate_mes_pqc_order_qa_stage_tasks;
