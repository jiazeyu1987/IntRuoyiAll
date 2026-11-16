-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260512_mes_base_schema; type=schema; riskLevel=low
-- 基线表：20260512_mes_base_schema.sql 中的 mes_pro_process。
-- 仅新增可空字段，不回填工序或改写历史订单、路线快照。
-- 回滚：先回退读取该列的应用并导出绑定数据，再由 DBA 按授权删除新增列。
SET NAMES utf8mb4;

DELIMITER $$
CREATE PROCEDURE migrate_mes_process_sop_20261028()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_process'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'mes_pro_process prerequisite missing';
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_process' AND COLUMN_NAME = 'sop_file_url'
  ) THEN
    ALTER TABLE mes_pro_process ADD COLUMN sop_file_url VARCHAR(2048) NULL DEFAULT NULL COMMENT '工序SOP文件地址' AFTER attention;
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_process' AND COLUMN_NAME = 'sop_file_url'
      AND DATA_TYPE = 'varchar' AND CHARACTER_MAXIMUM_LENGTH = 2048
      AND IS_NULLABLE = 'YES' AND COLUMN_DEFAULT IS NULL AND EXTRA = ''
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'mes_pro_process.sop_file_url schema mismatch';
  END IF;
END$$
DELIMITER ;
CALL migrate_mes_process_sop_20261028();
DROP PROCEDURE migrate_mes_process_sop_20261028;
