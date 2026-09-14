-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260724_system_codex_test_management; type=schema; riskLevel=medium
DROP PROCEDURE IF EXISTS ensure_system_codex_test_execution_case_progress;
DELIMITER $$
CREATE PROCEDURE ensure_system_codex_test_execution_case_progress()
BEGIN
  IF NOT EXISTS (
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'system_codex_test_execution_case'
        AND COLUMN_NAME = 'progress_phase'
  ) THEN
    ALTER TABLE `system_codex_test_execution_case`
      ADD COLUMN `progress_phase` varchar(32) NULL COMMENT '运行监控阶段：METHOD/CHECKPOINT/DONE' AFTER `failure_reason`;
  END IF;

  IF NOT EXISTS (
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'system_codex_test_execution_case'
        AND COLUMN_NAME = 'current_method_sort'
  ) THEN
    ALTER TABLE `system_codex_test_execution_case`
      ADD COLUMN `current_method_sort` int NULL COMMENT '当前执行的测试方法项序号' AFTER `progress_phase`;
  END IF;

  IF NOT EXISTS (
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'system_codex_test_execution_case'
        AND COLUMN_NAME = 'current_checkpoint_sort'
  ) THEN
    ALTER TABLE `system_codex_test_execution_case`
      ADD COLUMN `current_checkpoint_sort` int NULL COMMENT '当前验证的测试目标项序号' AFTER `current_method_sort`;
  END IF;

  IF NOT EXISTS (
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'system_codex_test_execution_case'
        AND COLUMN_NAME = 'progress_message'
  ) THEN
    ALTER TABLE `system_codex_test_execution_case`
      ADD COLUMN `progress_message` varchar(512) NULL COMMENT '运行监控进度说明' AFTER `current_checkpoint_sort`;
  END IF;
END$$
DELIMITER ;

CALL ensure_system_codex_test_execution_case_progress();

DROP PROCEDURE IF EXISTS ensure_system_codex_test_execution_case_progress;
