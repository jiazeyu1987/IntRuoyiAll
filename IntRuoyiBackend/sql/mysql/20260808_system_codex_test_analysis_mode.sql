-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260727_system_codex_test_node_chain,20260726_system_codex_test_run_monitor_progress; type=schema; riskLevel=medium
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_system_codex_test_analysis_mode;
DELIMITER $$
CREATE PROCEDURE ensure_system_codex_test_analysis_mode()
BEGIN
  IF NOT EXISTS (
      SELECT 1
        FROM information_schema.tables
       WHERE table_schema = DATABASE()
         AND table_name = 'system_codex_test_case'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing system_codex_test_case table';
  END IF;

  IF NOT EXISTS (
      SELECT 1
        FROM information_schema.tables
       WHERE table_schema = DATABASE()
         AND table_name = 'system_codex_test_execution_case'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing system_codex_test_execution_case table';
  END IF;

  IF NOT EXISTS (
      SELECT 1
        FROM information_schema.columns
       WHERE table_schema = DATABASE()
         AND table_name = 'system_codex_test_case'
         AND column_name = 'analysis_mode'
  ) THEN
    ALTER TABLE `system_codex_test_case`
      ADD COLUMN `analysis_mode` varchar(32) NOT NULL DEFAULT 'PLAYWRIGHT_E2E'
      COMMENT '分析模式：PLAYWRIGHT_E2E/CODE_READONLY' AFTER `test_data_text`;
  END IF;

  UPDATE `system_codex_test_case`
     SET `analysis_mode` = 'PLAYWRIGHT_E2E'
   WHERE `analysis_mode` IS NULL OR TRIM(`analysis_mode`) = '';

  IF EXISTS (
      SELECT 1
        FROM `system_codex_test_case`
       WHERE `analysis_mode` NOT IN ('PLAYWRIGHT_E2E', 'CODE_READONLY')
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid system_codex_test_case.analysis_mode';
  END IF;

  ALTER TABLE `system_codex_test_case`
    MODIFY COLUMN `analysis_mode` varchar(32) NOT NULL DEFAULT 'PLAYWRIGHT_E2E'
    COMMENT '分析模式：PLAYWRIGHT_E2E/CODE_READONLY' AFTER `test_data_text`;

  IF NOT EXISTS (
      SELECT 1
        FROM information_schema.columns
       WHERE table_schema = DATABASE()
         AND table_name = 'system_codex_test_execution_case'
         AND column_name = 'analysis_mode_snapshot'
  ) THEN
    ALTER TABLE `system_codex_test_execution_case`
      ADD COLUMN `analysis_mode_snapshot` varchar(32) NOT NULL DEFAULT 'PLAYWRIGHT_E2E'
      COMMENT '执行快照分析模式：PLAYWRIGHT_E2E/CODE_READONLY' AFTER `test_data_text_snapshot`;
  END IF;

  UPDATE `system_codex_test_execution_case`
     SET `analysis_mode_snapshot` = 'PLAYWRIGHT_E2E'
   WHERE `analysis_mode_snapshot` IS NULL OR TRIM(`analysis_mode_snapshot`) = '';

  IF EXISTS (
      SELECT 1
        FROM `system_codex_test_execution_case`
       WHERE `analysis_mode_snapshot` NOT IN ('PLAYWRIGHT_E2E', 'CODE_READONLY')
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid system_codex_test_execution_case.analysis_mode_snapshot';
  END IF;

  ALTER TABLE `system_codex_test_execution_case`
    MODIFY COLUMN `analysis_mode_snapshot` varchar(32) NOT NULL DEFAULT 'PLAYWRIGHT_E2E'
    COMMENT '执行快照分析模式：PLAYWRIGHT_E2E/CODE_READONLY' AFTER `test_data_text_snapshot`;
END$$
DELIMITER ;

CALL ensure_system_codex_test_analysis_mode();

DROP PROCEDURE IF EXISTS ensure_system_codex_test_analysis_mode;
