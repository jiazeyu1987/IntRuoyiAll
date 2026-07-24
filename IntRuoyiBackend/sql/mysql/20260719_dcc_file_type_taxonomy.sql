-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260513_dcc_base_schema; type=schema; riskLevel=medium
-- DCC file type taxonomy configuration and basic-data menu entry.

CREATE TABLE IF NOT EXISTS `dcc_file_type_taxonomy` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `parent_id` BIGINT NOT NULL DEFAULT 0,
  `level_no` TINYINT NOT NULL,
  `code` VARCHAR(64) NOT NULL,
  `name` VARCHAR(128) NOT NULL,
  `active` TINYINT NOT NULL DEFAULT 1,
  `sort` INT NOT NULL DEFAULT 0,
  `remark` VARCHAR(255) NULL,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `creator` VARCHAR(64) NULL,
  `updater` VARCHAR(64) NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_file_type_taxonomy_tenant_code_deleted` (`tenant_id`, `code`, `deleted`),
  UNIQUE KEY `uk_dcc_file_type_taxonomy_sibling_name_deleted` (`tenant_id`, `parent_id`, `name`, `deleted`),
  KEY `idx_dcc_file_type_taxonomy_parent` (`tenant_id`, `parent_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='DCC file type five-level taxonomy';

SET @dcc_file_category_taxonomy_column_sql := (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'dcc_file_category'
              AND COLUMN_NAME = 'file_type_taxonomy_id'
        ),
        'SELECT ''dcc_file_category.file_type_taxonomy_id already exists'' AS migration_message',
        'ALTER TABLE `dcc_file_category` ADD COLUMN `file_type_taxonomy_id` BIGINT NULL COMMENT ''Bound DCC file type taxonomy leaf id'' AFTER `lifecycle_stage`'
    )
);
PREPARE dcc_file_category_taxonomy_column_stmt FROM @dcc_file_category_taxonomy_column_sql;
EXECUTE dcc_file_category_taxonomy_column_stmt;
DEALLOCATE PREPARE dcc_file_category_taxonomy_column_stmt;

SET @dcc_controlled_file_taxonomy_column_sql := (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'dcc_controlled_file'
              AND COLUMN_NAME = 'file_type_taxonomy_id'
        ),
        'SELECT ''dcc_controlled_file.file_type_taxonomy_id already exists'' AS migration_message',
        'ALTER TABLE `dcc_controlled_file` ADD COLUMN `file_type_taxonomy_id` BIGINT NULL COMMENT ''Selected DCC file type taxonomy leaf id'' AFTER `project_code_recognized_time`'
    )
);
PREPARE dcc_controlled_file_taxonomy_column_stmt FROM @dcc_controlled_file_taxonomy_column_sql;
EXECUTE dcc_controlled_file_taxonomy_column_stmt;
DEALLOCATE PREPARE dcc_controlled_file_taxonomy_column_stmt;

SET @dcc_recognition_record_taxonomy_column_sql := (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'dcc_controlled_file_recognition_record'
              AND COLUMN_NAME = 'file_type_taxonomy_id'
        ),
        'SELECT ''dcc_controlled_file_recognition_record.file_type_taxonomy_id already exists'' AS migration_message',
        'ALTER TABLE `dcc_controlled_file_recognition_record` ADD COLUMN `file_type_taxonomy_id` BIGINT NULL COMMENT ''Recognized DCC file type taxonomy leaf id'' AFTER `failure_message`'
    )
);
PREPARE dcc_recognition_record_taxonomy_column_stmt FROM @dcc_recognition_record_taxonomy_column_sql;
EXECUTE dcc_recognition_record_taxonomy_column_stmt;
DEALLOCATE PREPARE dcc_recognition_record_taxonomy_column_stmt;

SET @dcc_file_category_taxonomy_index_sql := (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'dcc_file_category'
              AND INDEX_NAME = 'idx_dcc_file_category_taxonomy'
        ),
        'SELECT ''idx_dcc_file_category_taxonomy already exists'' AS migration_message',
        'CREATE INDEX `idx_dcc_file_category_taxonomy` ON `dcc_file_category` (`tenant_id`, `file_type_taxonomy_id`, `deleted`)'
    )
);
PREPARE dcc_file_category_taxonomy_index_stmt FROM @dcc_file_category_taxonomy_index_sql;
EXECUTE dcc_file_category_taxonomy_index_stmt;
DEALLOCATE PREPARE dcc_file_category_taxonomy_index_stmt;

SET @dcc_controlled_file_taxonomy_index_sql := (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'dcc_controlled_file'
              AND INDEX_NAME = 'idx_dcc_controlled_file_taxonomy'
        ),
        'SELECT ''idx_dcc_controlled_file_taxonomy already exists'' AS migration_message',
        'CREATE INDEX `idx_dcc_controlled_file_taxonomy` ON `dcc_controlled_file` (`tenant_id`, `file_type_taxonomy_id`, `deleted`)'
    )
);
PREPARE dcc_controlled_file_taxonomy_index_stmt FROM @dcc_controlled_file_taxonomy_index_sql;
EXECUTE dcc_controlled_file_taxonomy_index_stmt;
DEALLOCATE PREPARE dcc_controlled_file_taxonomy_index_stmt;

SET @dcc_recognition_record_taxonomy_index_sql := (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'dcc_controlled_file_recognition_record'
              AND INDEX_NAME = 'idx_dcc_recognition_record_taxonomy'
        ),
        'SELECT ''idx_dcc_recognition_record_taxonomy already exists'' AS migration_message',
        'CREATE INDEX `idx_dcc_recognition_record_taxonomy` ON `dcc_controlled_file_recognition_record` (`tenant_id`, `file_type_taxonomy_id`, `deleted`)'
    )
);
PREPARE dcc_recognition_record_taxonomy_index_stmt FROM @dcc_recognition_record_taxonomy_index_sql;
EXECUTE dcc_recognition_record_taxonomy_index_stmt;
DEALLOCATE PREPARE dcc_recognition_record_taxonomy_index_stmt;

SET @menu_name_basic_data := (CONVERT(UNHEX('E59FBAE7A180E695B0E68DAE') USING utf8mb4) COLLATE utf8mb4_unicode_ci);
SET @menu_name_file_type_taxonomy := (CONVERT(UNHEX('444343E69687E4BBB6E58886E7B1BB') USING utf8mb4) COLLATE utf8mb4_unicode_ci);

SET @dcc_global_basic_data_menu_id := (
    SELECT `id`
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND (`id` = 990200 OR `path` = '/mdm' OR `name` = @menu_name_basic_data)
    ORDER BY CASE WHEN `id` = 990200 THEN 0 ELSE 1 END
    LIMIT 1
);

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 990230, @menu_name_file_type_taxonomy, 'dcc:controlled-file:category:manage', 2, 22,
       @dcc_global_basic_data_menu_id, 'file-type-taxonomy', 'ep:collection-tag',
       'dcc/controlled-file/basic-data/file-type-taxonomy/index', 'DccFileTypeTaxonomyBasicDataPage',
       0, b'1', b'1', b'1', 'dcc-file-type-taxonomy', NOW(), 'dcc-file-type-taxonomy', NOW(), b'0'
WHERE @dcc_global_basic_data_menu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM `system_menu`
      WHERE `deleted` = b'0'
        AND (`id` = 990230 OR (`parent_id` = @dcc_global_basic_data_menu_id AND `path` = 'file-type-taxonomy'))
  );

UPDATE `system_menu`
SET `name` = @menu_name_file_type_taxonomy,
    `permission` = 'dcc:controlled-file:category:manage',
    `parent_id` = @dcc_global_basic_data_menu_id,
    `path` = 'file-type-taxonomy',
    `icon` = 'ep:collection-tag',
    `component` = 'dcc/controlled-file/basic-data/file-type-taxonomy/index',
    `component_name` = 'DccFileTypeTaxonomyBasicDataPage',
    `type` = 2,
    `sort` = 22,
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `updater` = 'dcc-file-type-taxonomy',
    `update_time` = NOW()
WHERE @dcc_global_basic_data_menu_id IS NOT NULL
  AND (`id` = 990230 OR (`parent_id` = @dcc_global_basic_data_menu_id AND `path` = 'file-type-taxonomy'))
  AND `deleted` = b'0';

INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `updater`, `tenant_id`)
SELECT DISTINCT src.`role_id`, target_menu.`id`, src.`creator`, src.`updater`, src.`tenant_id`
FROM `system_role_menu` src
JOIN `system_menu` source_menu
  ON source_menu.`path` = 'controlled-file/categories'
 AND source_menu.`deleted` = b'0'
JOIN `system_menu` target_menu
  ON target_menu.`id` = 990230
 AND target_menu.`deleted` = b'0'
WHERE src.`menu_id` = source_menu.`id`
  AND src.`deleted` = b'0'
  AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` existing
      WHERE existing.`role_id` = src.`role_id`
        AND existing.`menu_id` = target_menu.`id`
        AND existing.`tenant_id` = src.`tenant_id`
        AND existing.`deleted` = b'0'
  );
