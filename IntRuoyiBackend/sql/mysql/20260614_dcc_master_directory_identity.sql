-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- Keep DCC logical document chains distinct per directory.
-- Local folder imports can contain the same file name in different folders; those files must not replace each other.

SELECT COUNT(*)
INTO @dcc_master_directory_id_column_count
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'dcc_controlled_file_master'
  AND COLUMN_NAME = 'directory_id';

SET @dcc_master_directory_id_sql = IF(
  @dcc_master_directory_id_column_count = 0,
  'ALTER TABLE `dcc_controlled_file_master` ADD COLUMN `directory_id` bigint DEFAULT NULL COMMENT ''DCC directory for this logical document chain'' AFTER `category_id`',
  'SELECT ''dcc_controlled_file_master.directory_id already exists'' AS migration_status'
);

PREPARE dcc_master_directory_id_stmt FROM @dcc_master_directory_id_sql;
EXECUTE dcc_master_directory_id_stmt;
DEALLOCATE PREPARE dcc_master_directory_id_stmt;

UPDATE `dcc_controlled_file_master` master_record
LEFT JOIN `dcc_controlled_file` current_file
  ON current_file.`id` = master_record.`current_active_controlled_file_id`
LEFT JOIN (
  SELECT `master_id`,
         MAX(CASE WHEN `deleted` = 0 THEN `directory_id` ELSE NULL END) AS `active_directory_id`,
         MAX(`directory_id`) AS `any_directory_id`
  FROM `dcc_controlled_file`
  WHERE `master_id` IS NOT NULL
  GROUP BY `master_id`
) controlled_file
  ON controlled_file.`master_id` = master_record.`id`
SET master_record.`directory_id` = COALESCE(
  current_file.`directory_id`,
  controlled_file.`active_directory_id`,
  controlled_file.`any_directory_id`
)
WHERE master_record.`directory_id` IS NULL
  AND COALESCE(current_file.`directory_id`, controlled_file.`active_directory_id`,
               controlled_file.`any_directory_id`) IS NOT NULL;

SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX)
INTO @dcc_master_chain_index_columns
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'dcc_controlled_file_master'
  AND INDEX_NAME = 'uk_dcc_controlled_file_master_chain';

SET @dcc_master_drop_old_index_sql = IF(
  @dcc_master_chain_index_columns = 'category_id,file_name',
  'ALTER TABLE `dcc_controlled_file_master` DROP INDEX `uk_dcc_controlled_file_master_chain`',
  'SELECT ''dcc_controlled_file_master chain index already includes directory_id'' AS migration_status'
);

PREPARE dcc_master_drop_old_index_stmt FROM @dcc_master_drop_old_index_sql;
EXECUTE dcc_master_drop_old_index_stmt;
DEALLOCATE PREPARE dcc_master_drop_old_index_stmt;

SELECT COUNT(*)
INTO @dcc_master_directory_chain_index_count
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'dcc_controlled_file_master'
  AND INDEX_NAME = 'uk_dcc_controlled_file_master_chain'
  AND COLUMN_NAME = 'directory_id';

SET @dcc_master_add_directory_index_sql = IF(
  @dcc_master_directory_chain_index_count = 0,
  'ALTER TABLE `dcc_controlled_file_master` ADD UNIQUE KEY `uk_dcc_controlled_file_master_chain` (`category_id`, `directory_id`, `file_name`)',
  'SELECT ''dcc_controlled_file_master directory chain index already exists'' AS migration_status'
);

PREPARE dcc_master_add_directory_index_stmt FROM @dcc_master_add_directory_index_sql;
EXECUTE dcc_master_add_directory_index_stmt;
DEALLOCATE PREPARE dcc_master_add_directory_index_stmt;
