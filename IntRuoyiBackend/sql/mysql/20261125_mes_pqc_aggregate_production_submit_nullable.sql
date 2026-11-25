-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260805_mes_pqc_process_inspection_aggregate_runtime_closure,20260808_mes_pqc_record_production_submit_nullable; type=schema; riskLevel=medium; requiresTargetPreflight=true
SET SESSION lock_wait_timeout = 10;
DROP PROCEDURE IF EXISTS migrate_pqc_aggregate_submit_nullable_20261125;
DELIMITER $$
CREATE PROCEDURE migrate_pqc_aggregate_submit_nullable_20261125()
BEGIN
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='mes_pro_process_pool_pqc_record'
        AND COLUMN_NAME='production_submit_event_id' AND COLUMN_TYPE='bigint'
        AND IS_NULLABLE='YES' AND COLUMN_DEFAULT IS NULL) <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='PQC_SOURCE_PRODUCTION_SUBMIT_CONTRACT_MISMATCH';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='mes_pqc_process_inspection_aggregate_detail'
        AND COLUMN_NAME='production_submit_event_id' AND COLUMN_TYPE='bigint'
        AND COLUMN_DEFAULT IS NULL AND EXTRA='') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='PQC_AGGREGATE_PRODUCTION_SUBMIT_CONTRACT_MISMATCH';
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='mes_pqc_process_inspection_aggregate_detail'
        AND COLUMN_NAME='production_submit_event_id' AND IS_NULLABLE='NO') THEN
    ALTER TABLE mes_pqc_process_inspection_aggregate_detail
      MODIFY COLUMN production_submit_event_id bigint NULL COMMENT '可选绑定的生产提交事件ID',
      ALGORITHM=INPLACE, LOCK=NONE;
  END IF;
END$$
DELIMITER ;
CALL migrate_pqc_aggregate_submit_nullable_20261125();
DROP PROCEDURE migrate_pqc_aggregate_submit_nullable_20261125;
