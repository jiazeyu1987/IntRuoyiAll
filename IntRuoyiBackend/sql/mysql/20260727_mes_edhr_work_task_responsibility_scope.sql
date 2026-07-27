-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260718_mes_edhr_work_task_ownership; type=schema; riskLevel=medium
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_mes_edhr_work_task_responsibility_scope;

DELIMITER //
CREATE PROCEDURE ensure_mes_edhr_work_task_responsibility_scope()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_edhr_work_task'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'MES visual fill config requires mes_pro_edhr_work_task';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_edhr_work_task'
      AND column_name = 'responsibility_scope_json'
  ) THEN
    ALTER TABLE `mes_pro_edhr_work_task`
      ADD COLUMN `responsibility_scope_json` json DEFAULT NULL COMMENT 'Frozen assist-row responsibility scope snapshot'
      AFTER `responsibility_source_digest`;
  END IF;
END//
DELIMITER ;

CALL ensure_mes_edhr_work_task_responsibility_scope();

DROP PROCEDURE IF EXISTS ensure_mes_edhr_work_task_responsibility_scope;
