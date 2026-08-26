-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260707_mes_batch_record_extra_form_slots,20260822_mes_edhr_batch_traceability; type=schema; riskLevel=medium
-- Flow 8: store the formal source snapshot that release-material tasks were created against.
-- Existing tasks intentionally remain NULL and therefore stay blocked until a formal rematerialization path exists.

DROP PROCEDURE IF EXISTS ensure_mes_edhr_material_task_source_witness;
DELIMITER $$
CREATE PROCEDURE ensure_mes_edhr_material_task_source_witness()
BEGIN
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.TABLES
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'mes_pro_edhr_batch_execution_task'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing mes_pro_edhr_batch_execution_task; apply eDHR batch execution schema first';
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'mes_pro_edhr_batch_execution_task'
        AND COLUMN_NAME = 'material_source_snapshot_hash'
  ) THEN
    ALTER TABLE mes_pro_edhr_batch_execution_task
      ADD COLUMN material_source_snapshot_hash char(64) DEFAULT NULL
      COMMENT 'Flow7 formal source snapshot witnessed for four-material release gate'
      AFTER route_binding_snapshot_hash;
  END IF;
END$$
DELIMITER ;
CALL ensure_mes_edhr_material_task_source_witness();
DROP PROCEDURE IF EXISTS ensure_mes_edhr_material_task_source_witness;
