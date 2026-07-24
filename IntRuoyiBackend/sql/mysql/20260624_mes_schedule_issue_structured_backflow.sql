-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260610_mes_schedule_order_p1; type=schema; riskLevel=medium
-- MES 智能排产：生产异常结构化回流字段。

SELECT COUNT(*) INTO @mes_schedule_issue_status_column_count
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'mes_pro_schedule_issue'
  AND column_name = 'status';
SET @mes_schedule_issue_status_sql = IF(
  @mes_schedule_issue_status_column_count = 0,
  'ALTER TABLE `mes_pro_schedule_issue` ADD COLUMN `status` varchar(32) NOT NULL DEFAULT ''OPEN'' COMMENT ''issue status'' AFTER `resolved`',
  'SELECT ''mes_pro_schedule_issue.status already exists'' AS migration_status'
);
PREPARE mes_schedule_issue_status_stmt FROM @mes_schedule_issue_status_sql;
EXECUTE mes_schedule_issue_status_stmt;
DEALLOCATE PREPARE mes_schedule_issue_status_stmt;

SELECT COUNT(*) INTO @mes_schedule_issue_source_type_column_count
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'mes_pro_schedule_issue'
  AND column_name = 'source_type';
SET @mes_schedule_issue_source_type_sql = IF(
  @mes_schedule_issue_source_type_column_count = 0,
  'ALTER TABLE `mes_pro_schedule_issue` ADD COLUMN `source_type` varchar(64) DEFAULT NULL COMMENT ''source document type'' AFTER `status`',
  'SELECT ''mes_pro_schedule_issue.source_type already exists'' AS migration_status'
);
PREPARE mes_schedule_issue_source_type_stmt FROM @mes_schedule_issue_source_type_sql;
EXECUTE mes_schedule_issue_source_type_stmt;
DEALLOCATE PREPARE mes_schedule_issue_source_type_stmt;

SELECT COUNT(*) INTO @mes_schedule_issue_source_id_column_count
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'mes_pro_schedule_issue'
  AND column_name = 'source_id';
SET @mes_schedule_issue_source_id_sql = IF(
  @mes_schedule_issue_source_id_column_count = 0,
  'ALTER TABLE `mes_pro_schedule_issue` ADD COLUMN `source_id` bigint DEFAULT NULL COMMENT ''source document id'' AFTER `source_type`',
  'SELECT ''mes_pro_schedule_issue.source_id already exists'' AS migration_status'
);
PREPARE mes_schedule_issue_source_id_stmt FROM @mes_schedule_issue_source_id_sql;
EXECUTE mes_schedule_issue_source_id_stmt;
DEALLOCATE PREPARE mes_schedule_issue_source_id_stmt;

SELECT COUNT(*) INTO @mes_schedule_issue_resolution_reason_column_count
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'mes_pro_schedule_issue'
  AND column_name = 'resolution_reason';
SET @mes_schedule_issue_resolution_reason_sql = IF(
  @mes_schedule_issue_resolution_reason_column_count = 0,
  'ALTER TABLE `mes_pro_schedule_issue` ADD COLUMN `resolution_reason` varchar(500) DEFAULT NULL COMMENT ''resolution reason'' AFTER `source_id`',
  'SELECT ''mes_pro_schedule_issue.resolution_reason already exists'' AS migration_status'
);
PREPARE mes_schedule_issue_resolution_reason_stmt FROM @mes_schedule_issue_resolution_reason_sql;
EXECUTE mes_schedule_issue_resolution_reason_stmt;
DEALLOCATE PREPARE mes_schedule_issue_resolution_reason_stmt;

SELECT COUNT(*) INTO @mes_schedule_issue_resolved_by_column_count
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'mes_pro_schedule_issue'
  AND column_name = 'resolved_by';
SET @mes_schedule_issue_resolved_by_sql = IF(
  @mes_schedule_issue_resolved_by_column_count = 0,
  'ALTER TABLE `mes_pro_schedule_issue` ADD COLUMN `resolved_by` bigint DEFAULT NULL COMMENT ''resolved user id'' AFTER `resolution_reason`',
  'SELECT ''mes_pro_schedule_issue.resolved_by already exists'' AS migration_status'
);
PREPARE mes_schedule_issue_resolved_by_stmt FROM @mes_schedule_issue_resolved_by_sql;
EXECUTE mes_schedule_issue_resolved_by_stmt;
DEALLOCATE PREPARE mes_schedule_issue_resolved_by_stmt;

SELECT COUNT(*) INTO @mes_schedule_issue_resolved_at_column_count
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'mes_pro_schedule_issue'
  AND column_name = 'resolved_at';
SET @mes_schedule_issue_resolved_at_sql = IF(
  @mes_schedule_issue_resolved_at_column_count = 0,
  'ALTER TABLE `mes_pro_schedule_issue` ADD COLUMN `resolved_at` datetime DEFAULT NULL COMMENT ''resolved time'' AFTER `resolved_by`',
  'SELECT ''mes_pro_schedule_issue.resolved_at already exists'' AS migration_status'
);
PREPARE mes_schedule_issue_resolved_at_stmt FROM @mes_schedule_issue_resolved_at_sql;
EXECUTE mes_schedule_issue_resolved_at_stmt;
DEALLOCATE PREPARE mes_schedule_issue_resolved_at_stmt;

UPDATE `mes_pro_schedule_issue`
SET `status` = CASE WHEN `resolved` = b'1' THEN 'RESOLVED' ELSE 'OPEN' END
WHERE `status` IS NULL OR `status` = '';

SELECT COUNT(*) INTO @mes_schedule_issue_status_index_count
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'mes_pro_schedule_issue'
  AND index_name = 'idx_mes_pro_schedule_issue_status';
SET @mes_schedule_issue_status_index_sql = IF(
  @mes_schedule_issue_status_index_count = 0,
  'ALTER TABLE `mes_pro_schedule_issue` ADD KEY `idx_mes_pro_schedule_issue_status` (`status`)',
  'SELECT ''idx_mes_pro_schedule_issue_status already exists'' AS migration_status'
);
PREPARE mes_schedule_issue_status_index_stmt FROM @mes_schedule_issue_status_index_sql;
EXECUTE mes_schedule_issue_status_index_stmt;
DEALLOCATE PREPARE mes_schedule_issue_status_index_stmt;

SELECT COUNT(*) INTO @mes_schedule_issue_source_index_count
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'mes_pro_schedule_issue'
  AND index_name = 'idx_mes_pro_schedule_issue_source';
SET @mes_schedule_issue_source_index_sql = IF(
  @mes_schedule_issue_source_index_count = 0,
  'ALTER TABLE `mes_pro_schedule_issue` ADD KEY `idx_mes_pro_schedule_issue_source` (`source_type`, `source_id`)',
  'SELECT ''idx_mes_pro_schedule_issue_source already exists'' AS migration_status'
);
PREPARE mes_schedule_issue_source_index_stmt FROM @mes_schedule_issue_source_index_sql;
EXECUTE mes_schedule_issue_source_index_stmt;
DEALLOCATE PREPARE mes_schedule_issue_source_index_stmt;
