-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260802_mes_pqc_inspection_task; type=schema; riskLevel=medium
-- Freeze source versions independently of patrol tasks. No historical order/task backfill.
SET @pqc_source_column_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'mes_pro_process_pool_active_order'
      AND column_name = 'pqc_qa_source_version_ids_json');
SET @pqc_source_ddl = IF(@pqc_source_column_exists = 0,
    'ALTER TABLE mes_pro_process_pool_active_order ADD COLUMN pqc_qa_source_version_ids_json JSON NULL COMMENT ''冻结的产品及通用PQC规程版本ID''',
    'SELECT 1');
PREPARE pqc_source_stmt FROM @pqc_source_ddl;
EXECUTE pqc_source_stmt;
DEALLOCATE PREPARE pqc_source_stmt;
