-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260726_system_codex_smart_scheduling_test_items,20260726_dcc_codex_test_items_seed; type=schema; riskLevel=medium
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_system_codex_test_case_project;
DELIMITER //
CREATE PROCEDURE ensure_system_codex_test_case_project()
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
      FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'system_codex_test_case'
       AND column_name = 'project'
  ) THEN
    ALTER TABLE `system_codex_test_case`
      ADD COLUMN `project` varchar(16) NULL COMMENT '所属项目：智能排产/文控/批记录' AFTER `name`;
  END IF;

  UPDATE `system_codex_test_case`
     SET `project` = CASE
       WHEN `name` LIKE '%文控%'
         OR LOWER(CONCAT_WS(CHAR(10), `name`, `method_text`, `test_data_text`)) LIKE '%/dcc/%'
         OR LOWER(CONCAT_WS(CHAR(10), `name`, `method_text`, `test_data_text`)) LIKE '%controlled-file%'
         OR LOWER(CONCAT_WS(CHAR(10), `name`, `method_text`, `test_data_text`)) LIKE '%dcc-%'
         THEN '文控'
       WHEN `name` LIKE '%批记录%'
         OR `name` LIKE '%记录本%'
         OR LOWER(CONCAT_WS(CHAR(10), `name`, `method_text`, `test_data_text`)) LIKE '%edhr%'
         OR LOWER(CONCAT_WS(CHAR(10), `name`, `method_text`, `test_data_text`)) LIKE '%batch-record%'
         OR LOWER(CONCAT_WS(CHAR(10), `name`, `method_text`, `test_data_text`)) LIKE '%recordbook%'
         THEN '批记录'
       WHEN `name` LIKE '%排产%'
         OR LOWER(CONCAT_WS(CHAR(10), `name`, `method_text`, `test_data_text`)) LIKE '%smart-scheduling%'
         OR LOWER(CONCAT_WS(CHAR(10), `name`, `method_text`, `test_data_text`)) LIKE '%scheduler%'
         OR `method_text` LIKE '%排产%'
         THEN '智能排产'
       ELSE `project`
     END
   WHERE `deleted` = b'0'
     AND (
       `project` IS NULL
       OR TRIM(`project`) = ''
       OR `project` NOT IN ('智能排产', '文控', '批记录')
     );

  IF EXISTS (
    SELECT 1
      FROM `system_codex_test_case`
     WHERE `deleted` = b'0'
       AND (
         `project` IS NULL
         OR TRIM(`project`) = ''
         OR `project` NOT IN ('智能排产', '文控', '批记录')
       )
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Unclassified codex test case project';
  END IF;

  ALTER TABLE `system_codex_test_case`
    MODIFY COLUMN `project` varchar(16) NOT NULL COMMENT '所属项目：智能排产/文控/批记录' AFTER `name`;

  IF NOT EXISTS (
    SELECT 1
      FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'system_codex_test_case'
       AND index_name = 'idx_system_codex_test_case_tenant_project'
  ) THEN
    ALTER TABLE `system_codex_test_case`
      ADD KEY `idx_system_codex_test_case_tenant_project` (`tenant_id`, `project`, `deleted`);
  END IF;
END//
DELIMITER ;

CALL ensure_system_codex_test_case_project();

DROP PROCEDURE IF EXISTS ensure_system_codex_test_case_project;
