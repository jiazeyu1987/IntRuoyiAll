-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261104_mes_pqc_dynamic_patrol_rounds; type=schema; riskLevel=medium
SET @pqc_cancel_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='mes_pqc_inspection_task' AND column_name='cancel_reason');
SET @pqc_cancel_ddl = IF(@pqc_cancel_exists=0, 'ALTER TABLE mes_pqc_inspection_task ADD COLUMN cancel_reason varchar(500) NULL COMMENT ''巡检轮次撤销原因''', 'SELECT 1');
PREPARE pqc_cancel_stmt FROM @pqc_cancel_ddl;
EXECUTE pqc_cancel_stmt;
DEALLOCATE PREPARE pqc_cancel_stmt;
SET @pqc_cancel_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='mes_pqc_inspection_task' AND column_name='cancelled_by');
SET @pqc_cancel_ddl = IF(@pqc_cancel_exists=0, 'ALTER TABLE mes_pqc_inspection_task ADD COLUMN cancelled_by bigint NULL COMMENT ''撤销操作人''', 'SELECT 1');
PREPARE pqc_cancel_stmt FROM @pqc_cancel_ddl;
EXECUTE pqc_cancel_stmt;
DEALLOCATE PREPARE pqc_cancel_stmt;
SET @pqc_cancel_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='mes_pqc_inspection_task' AND column_name='cancelled_at');
SET @pqc_cancel_ddl = IF(@pqc_cancel_exists=0, 'ALTER TABLE mes_pqc_inspection_task ADD COLUMN cancelled_at datetime NULL COMMENT ''撤销时间''', 'SELECT 1');
PREPARE pqc_cancel_stmt FROM @pqc_cancel_ddl;
EXECUTE pqc_cancel_stmt;
DEALLOCATE PREPARE pqc_cancel_stmt;
