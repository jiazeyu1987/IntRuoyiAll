-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- eDHR 最终归档工作任务契约
-- Fail fast: 归档责任规则必须显式维护，本迁移不创建默认管理员或兜底责任人。

DROP PROCEDURE IF EXISTS ensure_mes_edhr_archive_work_task_table;
DROP PROCEDURE IF EXISTS ensure_mes_edhr_archive_work_task_column;
DROP PROCEDURE IF EXISTS ensure_mes_edhr_archive_work_task_index;
DROP PROCEDURE IF EXISTS drop_mes_edhr_archive_work_task_index;
DROP PROCEDURE IF EXISTS ensure_mes_edhr_archive_work_task_scope_backfill;

DELIMITER $$

CREATE PROCEDURE ensure_mes_edhr_archive_work_task_table(IN p_table_name varchar(128))
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table_name
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing required eDHR work task table for final archive migration';
  END IF;
END$$

CREATE PROCEDURE ensure_mes_edhr_archive_work_task_column(
  IN p_table_name varchar(128),
  IN p_column_name varchar(128),
  IN p_column_definition text
)
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table_name
      AND COLUMN_NAME = p_column_name
  ) THEN
    SET @edhr_archive_work_task_sql = CONCAT(
      'ALTER TABLE `', p_table_name, '` ADD COLUMN `', p_column_name, '` ', p_column_definition
    );
    PREPARE stmt FROM @edhr_archive_work_task_sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$

CREATE PROCEDURE ensure_mes_edhr_archive_work_task_index(
  IN p_table_name varchar(128),
  IN p_index_name varchar(128),
  IN p_index_definition text
)
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table_name
      AND INDEX_NAME = p_index_name
  ) THEN
    SET @edhr_archive_work_task_sql = CONCAT(
      'ALTER TABLE `', p_table_name, '` ADD ', p_index_definition
    );
    PREPARE stmt FROM @edhr_archive_work_task_sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$

CREATE PROCEDURE drop_mes_edhr_archive_work_task_index(
  IN p_table_name varchar(128),
  IN p_index_name varchar(128)
)
BEGIN
  IF EXISTS (
    SELECT 1
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table_name
      AND INDEX_NAME = p_index_name
  ) THEN
    SET @edhr_archive_work_task_sql = CONCAT(
      'ALTER TABLE `', p_table_name, '` DROP INDEX `', p_index_name, '`'
    );
    PREPARE stmt FROM @edhr_archive_work_task_sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$

CREATE PROCEDURE ensure_mes_edhr_archive_work_task_scope_backfill()
BEGIN
  IF EXISTS (
    SELECT 1
    FROM `mes_pro_edhr_work_task`
    WHERE `deleted` = b'0'
      AND `business_scope_type` = 'BATCH_TASK'
      AND `business_scope_id` = 0
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'eDHR work task business_scope_id backfill failed; cannot create final archive scope index';
  END IF;
END$$

DELIMITER ;

CALL ensure_mes_edhr_archive_work_task_table('mes_pro_edhr_work_task_assignment_rule');
CALL ensure_mes_edhr_archive_work_task_table('mes_pro_edhr_work_task');
CALL ensure_mes_edhr_archive_work_task_table('system_notify_template');

-- Applied column definition: `scope_type` varchar(32) NOT NULL DEFAULT 'ROUTE_PROCESS'
CALL ensure_mes_edhr_archive_work_task_column(
  'mes_pro_edhr_work_task_assignment_rule',
  'scope_type',
  'varchar(32) NOT NULL DEFAULT ''ROUTE_PROCESS'' COMMENT ''规则作用域：ROUTE_PROCESS/ROUTE'' AFTER `route_process_id`'
);
-- Applied column definition: `scope_id` bigint DEFAULT NULL
CALL ensure_mes_edhr_archive_work_task_column(
  'mes_pro_edhr_work_task_assignment_rule',
  'scope_id',
  'bigint DEFAULT NULL COMMENT ''规则作用域ID；ROUTE_PROCESS为路线工序ID，ROUTE为工艺路线ID'' AFTER `scope_type`'
);

ALTER TABLE `mes_pro_edhr_work_task_assignment_rule`
  MODIFY COLUMN `route_process_id` bigint DEFAULT NULL COMMENT '路线工序ID；归档任务按 ROUTE 作用域时为空';

UPDATE `mes_pro_edhr_work_task_assignment_rule`
SET `scope_type` = 'ROUTE_PROCESS',
    `scope_id` = `route_process_id`
WHERE `deleted` = b'0'
  AND (`scope_id` IS NULL OR `scope_type` = '');

-- 归档责任规则由业务显式维护：`scope_type` = 'ROUTE' 且 `task_type` = 'ARCHIVE'。
CALL drop_mes_edhr_archive_work_task_index(
  'mes_pro_edhr_work_task_assignment_rule',
  'uk_mes_pro_edhr_work_task_rule'
);
CALL ensure_mes_edhr_archive_work_task_index(
  'mes_pro_edhr_work_task_assignment_rule',
  'uk_mes_pro_edhr_work_task_rule_scope',
  'UNIQUE KEY `uk_mes_pro_edhr_work_task_rule_scope` (`tenant_id`, `scope_type`, `scope_id`, `task_type`, `deleted`)'
);
CALL ensure_mes_edhr_archive_work_task_index(
  'mes_pro_edhr_work_task_assignment_rule',
  'idx_mes_pro_edhr_work_task_rule_scope_user',
  'KEY `idx_mes_pro_edhr_work_task_rule_scope_user` (`tenant_id`, `scope_type`, `scope_id`, `assignee_user_id`, `enabled`)'
);

-- Applied column definition: `business_scope_type` varchar(32) NOT NULL DEFAULT 'BATCH_TASK'
CALL ensure_mes_edhr_archive_work_task_column(
  'mes_pro_edhr_work_task',
  'business_scope_type',
  'varchar(32) NOT NULL DEFAULT ''BATCH_TASK'' COMMENT ''业务作用域：BATCH_TASK/BATCH_ARCHIVE'' AFTER `batch_task_id`'
);
-- Applied column definition: `business_scope_id` bigint NOT NULL DEFAULT 0
CALL ensure_mes_edhr_archive_work_task_column(
  'mes_pro_edhr_work_task',
  'business_scope_id',
  'bigint NOT NULL DEFAULT 0 COMMENT ''业务作用域ID；BATCH_TASK为批次工序任务ID，BATCH_ARCHIVE为批次执行ID'' AFTER `business_scope_type`'
);

ALTER TABLE `mes_pro_edhr_work_task`
  MODIFY COLUMN `batch_task_id` bigint DEFAULT NULL COMMENT '批次工序任务ID；最终归档任务为空',
  MODIFY COLUMN `route_process_id` bigint DEFAULT NULL COMMENT '路线工序ID；最终归档任务按批次级处理时为空';

UPDATE `mes_pro_edhr_work_task`
SET `business_scope_type` = 'BATCH_TASK',
    `business_scope_id` = `batch_task_id`
WHERE `deleted` = b'0'
  AND `business_scope_type` = 'BATCH_TASK'
  AND `business_scope_id` = 0
  AND `batch_task_id` IS NOT NULL;

CALL ensure_mes_edhr_archive_work_task_scope_backfill();

CALL drop_mes_edhr_archive_work_task_index('mes_pro_edhr_work_task', 'uk_mes_pro_edhr_work_task_active');
CALL drop_mes_edhr_archive_work_task_index('mes_pro_edhr_work_task', 'uk_mes_pro_edhr_work_task_active_cell');
CALL ensure_mes_edhr_archive_work_task_index(
  'mes_pro_edhr_work_task',
  'idx_mes_pro_edhr_work_task_active_scope',
  'KEY `idx_mes_pro_edhr_work_task_active_scope` (`tenant_id`, `business_scope_type`, `business_scope_id`, `task_type`, `status`, `signature_cell_key`, `deleted`)'
);
CALL ensure_mes_edhr_archive_work_task_index(
  'mes_pro_edhr_work_task',
  'idx_mes_pro_edhr_work_task_business_scope',
  'KEY `idx_mes_pro_edhr_work_task_business_scope` (`tenant_id`, `business_scope_type`, `business_scope_id`, `task_type`, `status`)'
);

INSERT INTO `system_notify_template`
(`name`, `code`, `type`, `nickname`, `content`, `params`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 'eDHR最终归档任务通知', 'MES_EDHR_ARCHIVE_TASK_ASSIGNED', 2, 'eDHR任务中心',
       '工作到你了：请完成工单{workOrderCode}批次{batchCode}的最终归档。入口：{actionUrl}',
       '["workOrderCode","batchCode","processName","actionUrl","workTaskId"]', 0, 'eDHR最终归档工作任务',
       'edhr-final-archive-work-task', NOW(), 'edhr-final-archive-work-task', NOW(), b'0'
WHERE NOT EXISTS (
  SELECT 1
  FROM `system_notify_template`
  WHERE `code` = 'MES_EDHR_ARCHIVE_TASK_ASSIGNED'
    AND `deleted` = b'0'
);

DROP PROCEDURE IF EXISTS ensure_mes_edhr_archive_work_task_table;
DROP PROCEDURE IF EXISTS ensure_mes_edhr_archive_work_task_column;
DROP PROCEDURE IF EXISTS ensure_mes_edhr_archive_work_task_index;
DROP PROCEDURE IF EXISTS drop_mes_edhr_archive_work_task_index;
DROP PROCEDURE IF EXISTS ensure_mes_edhr_archive_work_task_scope_backfill;
