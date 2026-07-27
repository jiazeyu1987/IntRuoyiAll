-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260726_system_codex_test_case_project; type=schema; riskLevel=medium
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_system_codex_test_node_chain;
DELIMITER //
CREATE PROCEDURE ensure_system_codex_test_node_chain()
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
       AND column_name = 'node_chain_name'
  ) THEN
    ALTER TABLE `system_codex_test_case`
      ADD COLUMN `node_chain_name` varchar(128) NULL COMMENT '串行节点串名称' AFTER `project`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
      FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'system_codex_test_case'
       AND column_name = 'node_chain_sort'
  ) THEN
    ALTER TABLE `system_codex_test_case`
      ADD COLUMN `node_chain_sort` int NULL COMMENT '串内节点序号' AFTER `node_chain_name`;
  END IF;

  IF EXISTS (
    SELECT 1
      FROM `system_codex_test_case`
     WHERE `deleted` = b'0'
       AND (
         (`node_chain_name` IS NULL AND `node_chain_sort` IS NOT NULL)
         OR (`node_chain_name` IS NOT NULL AND TRIM(`node_chain_name`) = '')
         OR (`node_chain_name` IS NOT NULL AND (`node_chain_sort` IS NULL OR `node_chain_sort` <= 0))
       )
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid codex test node chain configuration';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM `system_codex_test_case`
     WHERE `deleted` = b'0'
       AND `node_chain_name` IS NOT NULL
       AND (
         `default_execution_mode` <> 'SEQUENTIAL'
         OR `parallel_safe` <> b'0'
       )
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Codex test node chain must be sequential and parallel unsafe';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM `system_codex_test_case`
     WHERE `deleted` = b'0'
       AND `node_chain_name` IS NOT NULL
     GROUP BY `tenant_id`, `node_chain_name`, `node_chain_sort`
    HAVING COUNT(*) > 1
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Duplicate codex test node chain sort';
  END IF;

  IF NOT EXISTS (
    SELECT 1
      FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'system_codex_test_case'
       AND index_name = 'idx_system_codex_test_case_tenant_node_chain'
  ) THEN
    ALTER TABLE `system_codex_test_case`
      ADD KEY `idx_system_codex_test_case_tenant_node_chain`
        (`tenant_id`, `node_chain_name`, `node_chain_sort`, `deleted`);
  END IF;
END//
DELIMITER ;

CALL ensure_system_codex_test_node_chain();

DROP PROCEDURE IF EXISTS ensure_system_codex_test_node_chain;
