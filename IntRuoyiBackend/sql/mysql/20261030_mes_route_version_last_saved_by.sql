-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260715_mes_route_version_lifecycle; type=schema; riskLevel=low
-- 独立保留最后成功保存草稿的用户；历史记录不从 updater、提交人或发布人推断。
SET @last_saved_by_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_route_version'
      AND COLUMN_NAME = 'last_saved_by'
);
SET @last_saved_by_sql = IF(@last_saved_by_exists = 0,
    'ALTER TABLE `mes_pro_route_version` ADD COLUMN `last_saved_by` bigint NULL COMMENT ''最后保存草稿的用户编号'' AFTER `source_route_version_id`',
    'SELECT ''last_saved_by already exists''');
PREPARE last_saved_by_statement FROM @last_saved_by_sql;
EXECUTE last_saved_by_statement;
DEALLOCATE PREPARE last_saved_by_statement;
