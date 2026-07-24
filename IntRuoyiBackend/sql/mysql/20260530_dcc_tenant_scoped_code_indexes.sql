-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
DROP PROCEDURE IF EXISTS intruoyi_drop_dcc_index_if_exists;
DELIMITER $$
CREATE PROCEDURE intruoyi_drop_dcc_index_if_exists(
  IN target_table varchar(64),
  IN target_index varchar(64),
  IN ddl_sql text
)
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = target_table
      AND index_name = target_index
  ) THEN
    SET @ddl = ddl_sql;
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;

DROP PROCEDURE IF EXISTS intruoyi_add_dcc_index_if_missing;
DELIMITER $$
CREATE PROCEDURE intruoyi_add_dcc_index_if_missing(
  IN target_table varchar(64),
  IN target_index varchar(64),
  IN ddl_sql text
)
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = target_table
      AND index_name = target_index
  ) THEN
    SET @ddl = ddl_sql;
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;

CALL intruoyi_drop_dcc_index_if_exists(
  'dcc_file_category',
  'uk_dcc_file_category_code',
  'ALTER TABLE `dcc_file_category` DROP INDEX `uk_dcc_file_category_code`'
);

CALL intruoyi_add_dcc_index_if_missing(
  'dcc_file_category',
  'uk_dcc_file_category_tenant_code',
  'ALTER TABLE `dcc_file_category` ADD UNIQUE KEY `uk_dcc_file_category_tenant_code` (`tenant_id`, `code`)'
);

CALL intruoyi_drop_dcc_index_if_exists(
  'dcc_approval_position',
  'uk_dcc_approval_position_code',
  'ALTER TABLE `dcc_approval_position` DROP INDEX `uk_dcc_approval_position_code`'
);

CALL intruoyi_add_dcc_index_if_missing(
  'dcc_approval_position',
  'uk_dcc_approval_position_tenant_code',
  'ALTER TABLE `dcc_approval_position` ADD UNIQUE KEY `uk_dcc_approval_position_tenant_code` (`tenant_id`, `code`)'
);

DROP PROCEDURE IF EXISTS intruoyi_add_dcc_index_if_missing;
DROP PROCEDURE IF EXISTS intruoyi_drop_dcc_index_if_exists;
