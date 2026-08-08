-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260803_mes_process_pool_pqc_structured_binding; type=schema; riskLevel=medium
-- One-line PQC can be submitted from active order + route process + QA regulation without a production-submit event.

DROP PROCEDURE IF EXISTS relax_mes_pqc_record_submit_event_if_required;
DELIMITER $$
CREATE PROCEDURE relax_mes_pqc_record_submit_event_if_required()
BEGIN
  IF EXISTS (
    SELECT 1
      FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_pqc_record'
       AND column_name = 'production_submit_event_id'
       AND is_nullable = 'NO'
  ) THEN
    ALTER TABLE `mes_pro_process_pool_pqc_record`
      MODIFY COLUMN `production_submit_event_id` bigint NULL COMMENT '可选绑定的生产提交事件ID';
  END IF;
END$$
DELIMITER ;

CALL relax_mes_pqc_record_submit_event_if_required();

DROP PROCEDURE IF EXISTS relax_mes_pqc_record_submit_event_if_required;
