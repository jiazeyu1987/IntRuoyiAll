-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260822_mes_edhr_release_final_state_trace; type=schema; riskLevel=low
-- PQC 生产放行保存线下 UDI 受控文件编号引用；历史活跃订单保持 NULL。

DROP PROCEDURE IF EXISTS add_mes_pqc_release_udi_document_no_20260924;
DELIMITER $$
CREATE PROCEDURE add_mes_pqc_release_udi_document_no_20260924()
BEGIN
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.TABLES
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'mes_pro_process_pool_active_order'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing mes_pro_process_pool_active_order prerequisite table';
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'mes_pro_process_pool_active_order'
        AND COLUMN_NAME = 'udi_control_document_no'
  ) THEN
    ALTER TABLE `mes_pro_process_pool_active_order`
      ADD COLUMN `udi_control_document_no` varchar(128) DEFAULT NULL COMMENT 'UDI编号' AFTER `released_at`;
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'mes_pro_process_pool_active_order'
        AND COLUMN_NAME = 'udi_control_document_no'
        AND DATA_TYPE = 'varchar'
        AND CHARACTER_MAXIMUM_LENGTH = 128
        AND IS_NULLABLE = 'YES'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'UDI control document number column has an incompatible definition';
  END IF;
END$$
DELIMITER ;

CALL add_mes_pqc_release_udi_document_no_20260924();
DROP PROCEDURE add_mes_pqc_release_udi_document_no_20260924;
