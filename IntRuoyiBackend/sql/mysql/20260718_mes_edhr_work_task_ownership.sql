-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260718_system_entitlement_management; type=schema; riskLevel=medium
-- eDHR work task ownership markers for dynamic filler entitlement transfer.
-- Fail fast: this migration only adds traceability columns and indexes; it does not backfill owners,
-- create roles, bind users, or grant static menus.

DROP PROCEDURE IF EXISTS ensure_mes_edhr_work_task_ownership_table;
DROP PROCEDURE IF EXISTS ensure_mes_edhr_work_task_ownership_column;
DROP PROCEDURE IF EXISTS ensure_mes_edhr_work_task_ownership_index;

DELIMITER $$

CREATE PROCEDURE ensure_mes_edhr_work_task_ownership_table(IN p_table_name varchar(128))
BEGIN
  IF NOT EXISTS (
    SELECT 1
      FROM INFORMATION_SCHEMA.TABLES
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = p_table_name
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'mes_pro_edhr_work_task is missing; cannot apply ownership marker migration';
  END IF;
END$$

CREATE PROCEDURE ensure_mes_edhr_work_task_ownership_column(
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
    SET @edhr_work_task_ownership_sql = CONCAT(
      'ALTER TABLE `', p_table_name, '` ADD COLUMN `', p_column_name, '` ', p_column_definition
    );
    PREPARE stmt FROM @edhr_work_task_ownership_sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$

CREATE PROCEDURE ensure_mes_edhr_work_task_ownership_index(
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
    SET @edhr_work_task_ownership_sql = CONCAT(
      'ALTER TABLE `', p_table_name, '` ADD ', p_index_definition
    );
    PREPARE stmt FROM @edhr_work_task_ownership_sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$

DELIMITER ;

CALL ensure_mes_edhr_work_task_ownership_table('mes_pro_edhr_work_task');

CALL ensure_mes_edhr_work_task_ownership_column(
  'mes_pro_edhr_work_task',
  'responsibility_source_type',
  'varchar(64) DEFAULT NULL COMMENT ''责任来源类型：EDHR_PROCESS_FORM_FILLER 等'' AFTER `source_user_id`'
);

CALL ensure_mes_edhr_work_task_ownership_column(
  'mes_pro_edhr_work_task',
  'responsibility_source_key',
  'varchar(255) DEFAULT NULL COMMENT ''责任来源稳定键：ROUTE/FORM/WORK_TASK'' AFTER `responsibility_source_type`'
);

CALL ensure_mes_edhr_work_task_ownership_column(
  'mes_pro_edhr_work_task',
  'responsibility_source_version',
  'varchar(64) DEFAULT NULL COMMENT ''责任来源版本号快照'' AFTER `responsibility_source_key`'
);

CALL ensure_mes_edhr_work_task_ownership_column(
  'mes_pro_edhr_work_task',
  'responsibility_source_digest',
  'varchar(1000) DEFAULT NULL COMMENT ''责任来源候选人摘要'' AFTER `responsibility_source_version`'
);

CALL ensure_mes_edhr_work_task_ownership_column(
  'mes_pro_edhr_work_task',
  'ownership_locked',
  'bit(1) NOT NULL DEFAULT b''0'' COMMENT ''所有权是否锁定，锁定后禁止配置自动换人'' AFTER `responsibility_source_digest`'
);

CALL ensure_mes_edhr_work_task_ownership_column(
  'mes_pro_edhr_work_task',
  'ownership_last_transferred_at',
  'datetime DEFAULT NULL COMMENT ''最近一次自动换权时间'' AFTER `ownership_locked`'
);

CALL ensure_mes_edhr_work_task_ownership_column(
  'mes_pro_edhr_work_task',
  'ownership_last_transferred_by',
  'bigint DEFAULT NULL COMMENT ''最近一次自动换权操作人用户ID'' AFTER `ownership_last_transferred_at`'
);

CALL ensure_mes_edhr_work_task_ownership_index(
  'mes_pro_edhr_work_task',
  'idx_mes_pro_edhr_work_task_resp_source',
  'KEY `idx_mes_pro_edhr_work_task_resp_source` (`tenant_id`, `responsibility_source_type`, `responsibility_source_key`, `status`, `deleted`)'
);

DROP PROCEDURE IF EXISTS ensure_mes_edhr_work_task_ownership_table;
DROP PROCEDURE IF EXISTS ensure_mes_edhr_work_task_ownership_column;
DROP PROCEDURE IF EXISTS ensure_mes_edhr_work_task_ownership_index;
