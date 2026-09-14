-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260812_mes_pqc_dcc_qa_c00_schema; type=schema; riskLevel=medium
-- Active-order admission locks production context only. DCC/QA context is resolved by the independent PQC flow.
-- Rollback precondition: stop active-order writes and verify all three snapshot columns contain no NULL values.

DROP PROCEDURE IF EXISTS migrate_mes_active_order_qa_decoupling;
DELIMITER $$
CREATE PROCEDURE migrate_mes_active_order_qa_decoupling()
BEGIN
  IF NOT EXISTS (
    SELECT 1
      FROM information_schema.tables
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_active_order'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'mes_pro_process_pool_active_order table is required';
  END IF;

  IF (
    SELECT COUNT(1)
      FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_active_order'
       AND column_name IN ('dcc_project_code_id', 'qa_regulation_id', 'qa_regulation_version_id')
  ) <> 3 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'active-order DCC/QA snapshot columns are incomplete';
  END IF;

  ALTER TABLE `mes_pro_process_pool_active_order`
    MODIFY COLUMN `dcc_project_code_id` bigint DEFAULT NULL COMMENT 'PQC上下文DCC项目代码快照；进入PQC前可为空',
    MODIFY COLUMN `qa_regulation_id` bigint DEFAULT NULL COMMENT 'PQC上下文QA规程快照；进入PQC前可为空',
    MODIFY COLUMN `qa_regulation_version_id` bigint DEFAULT NULL COMMENT 'PQC上下文QA发布版本快照；进入PQC前可为空';
END$$
DELIMITER ;

CALL migrate_mes_active_order_qa_decoupling();

DROP PROCEDURE IF EXISTS migrate_mes_active_order_qa_decoupling;
