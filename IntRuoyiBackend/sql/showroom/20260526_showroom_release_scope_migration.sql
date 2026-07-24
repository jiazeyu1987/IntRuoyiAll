-- Showroom release scoped lookup migration for existing MySQL databases.
-- Repeatable: safe to run multiple times. It does not infer scope for old release rows.
-- Historical rows that existed before scoped release remain NULL in site_key/stage and are ignored by scoped lookups.

CREATE TABLE IF NOT EXISTS `showroom_public_site_binding` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `site_key` varchar(64) NOT NULL,
  `stage` varchar(16) NOT NULL,
  `tenant_id` bigint NOT NULL,
  `display_name` varchar(128) NOT NULL,
  `enabled` bit(1) NOT NULL DEFAULT b'1',
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_showroom_public_site_stage` (`site_key`, `stage`),
  KEY `idx_showroom_public_site_binding_tenant` (`tenant_id`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='展厅公开站点绑定';

DELIMITER $$

DROP PROCEDURE IF EXISTS `showroom_round12_exec` $$
CREATE PROCEDURE `showroom_round12_exec`(IN p_sql TEXT)
BEGIN
  SET @showroom_round12_sql = p_sql;
  PREPARE showroom_round12_stmt FROM @showroom_round12_sql;
  EXECUTE showroom_round12_stmt;
  DEALLOCATE PREPARE showroom_round12_stmt;
END $$

DROP PROCEDURE IF EXISTS `showroom_round12_require_table` $$
CREATE PROCEDURE `showroom_round12_require_table`(IN p_table_name varchar(64))
BEGIN
  DECLARE v_message varchar(255);
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = p_table_name
  ) THEN
    SET v_message = CONCAT('Missing required showroom table: ', p_table_name);
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_message;
  END IF;
END $$

DROP PROCEDURE IF EXISTS `showroom_round12_add_column` $$
CREATE PROCEDURE `showroom_round12_add_column`(
  IN p_table_name varchar(64),
  IN p_column_name varchar(64),
  IN p_column_definition text,
  IN p_after_column varchar(64)
)
BEGIN
  CALL showroom_round12_require_table(p_table_name);
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = p_table_name
      AND column_name = p_column_name
  ) THEN
    CALL showroom_round12_exec(CONCAT(
      'ALTER TABLE `', p_table_name, '` ADD COLUMN `', p_column_name, '` ',
      p_column_definition,
      IF(p_after_column IS NULL OR p_after_column = '', '', CONCAT(' AFTER `', p_after_column, '`'))
    ));
  END IF;
END $$

DROP PROCEDURE IF EXISTS `showroom_round12_drop_index` $$
CREATE PROCEDURE `showroom_round12_drop_index`(
  IN p_table_name varchar(64),
  IN p_index_name varchar(64)
)
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = p_table_name
      AND index_name = p_index_name
  ) THEN
    CALL showroom_round12_exec(CONCAT('ALTER TABLE `', p_table_name, '` DROP INDEX `', p_index_name, '`'));
  END IF;
END $$

DROP PROCEDURE IF EXISTS `showroom_round12_add_index` $$
CREATE PROCEDURE `showroom_round12_add_index`(
  IN p_table_name varchar(64),
  IN p_index_name varchar(64),
  IN p_index_definition text
)
BEGIN
  CALL showroom_round12_require_table(p_table_name);
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = p_table_name
      AND index_name = p_index_name
  ) THEN
    CALL showroom_round12_exec(CONCAT('ALTER TABLE `', p_table_name, '` ADD ', p_index_definition));
  END IF;
END $$

DELIMITER ;

CALL showroom_round12_add_column('showroom_release', 'site_key', 'varchar(64) DEFAULT NULL', 'release_id');
CALL showroom_round12_add_column('showroom_release', 'stage', 'varchar(16) DEFAULT NULL', 'site_key');

CALL showroom_round12_add_column('showroom_release_source_snapshot', 'site_key', 'varchar(64) DEFAULT NULL', 'release_id');
CALL showroom_round12_add_column('showroom_release_source_snapshot', 'stage', 'varchar(16) DEFAULT NULL', 'site_key');

CALL showroom_round12_add_column('showroom_release_document', 'site_key', 'varchar(64) DEFAULT NULL', 'release_id');
CALL showroom_round12_add_column('showroom_release_document', 'stage', 'varchar(16) DEFAULT NULL', 'site_key');

CALL showroom_round12_add_column('showroom_release_asset', 'site_key', 'varchar(64) DEFAULT NULL', 'id');
CALL showroom_round12_add_column('showroom_release_asset', 'stage', 'varchar(16) DEFAULT NULL', 'site_key');

CALL showroom_round12_add_column('showroom_release_asset_ref', 'site_key', 'varchar(64) DEFAULT NULL', 'release_id');
CALL showroom_round12_add_column('showroom_release_asset_ref', 'stage', 'varchar(16) DEFAULT NULL', 'site_key');

CALL showroom_round12_add_column('showroom_release_pointer', 'site_key', 'varchar(64) DEFAULT NULL', 'id');
CALL showroom_round12_add_column('showroom_release_pointer', 'stage', 'varchar(16) DEFAULT NULL', 'site_key');

CALL showroom_round12_add_column('showroom_release_legacy_projection', 'site_key', 'varchar(64) DEFAULT NULL', 'release_id');
CALL showroom_round12_add_column('showroom_release_legacy_projection', 'stage', 'varchar(16) DEFAULT NULL', 'site_key');

CALL showroom_round12_add_column('showroom_release_tombstone', 'site_key', 'varchar(64) DEFAULT NULL', 'id');
CALL showroom_round12_add_column('showroom_release_tombstone', 'stage', 'varchar(16) DEFAULT NULL', 'site_key');

CALL showroom_round12_drop_index('showroom_release_pointer', 'uk_showroom_release_pointer_key');
CALL showroom_round12_add_index(
  'showroom_release_pointer',
  'uk_showroom_release_pointer_scope',
  'UNIQUE KEY `uk_showroom_release_pointer_scope` (`tenant_id`, `site_key`, `stage`, `pointer_key`)'
);

CALL showroom_round12_drop_index('showroom_release_asset', 'uk_showroom_release_asset');
CALL showroom_round12_add_index(
  'showroom_release_asset',
  'uk_showroom_release_asset',
  'UNIQUE KEY `uk_showroom_release_asset` (`tenant_id`, `site_key`, `stage`, `asset_id`, `content_hash`)'
);

CALL showroom_round12_drop_index('showroom_release_tombstone', 'uk_showroom_release_tombstone');
CALL showroom_round12_add_index(
  'showroom_release_tombstone',
  'uk_showroom_release_tombstone',
  'UNIQUE KEY `uk_showroom_release_tombstone` (`tenant_id`, `site_key`, `stage`, `resource_type`, `resource_key`)'
);

CALL showroom_round12_add_index(
  'showroom_release',
  'idx_showroom_release_scope_status',
  'KEY `idx_showroom_release_scope_status` (`tenant_id`, `site_key`, `stage`, `status`, `published_at`, `id`)'
);

CALL showroom_round12_add_index(
  'showroom_release_source_snapshot',
  'idx_showroom_release_snapshot_scope',
  'KEY `idx_showroom_release_snapshot_scope` (`tenant_id`, `site_key`, `stage`, `release_id`)'
);

CALL showroom_round12_add_index(
  'showroom_release_document',
  'idx_showroom_release_document_scope',
  'KEY `idx_showroom_release_document_scope` (`tenant_id`, `site_key`, `stage`, `release_id`, `document_id`)'
);

CALL showroom_round12_add_index(
  'showroom_release_asset_ref',
  'idx_showroom_release_asset_ref_scope',
  'KEY `idx_showroom_release_asset_ref_scope` (`tenant_id`, `site_key`, `stage`, `release_id`, `document_id`, `asset_id`)'
);

CALL showroom_round12_add_index(
  'showroom_release_asset_ref',
  'idx_showroom_release_asset_ref_asset_scope',
  'KEY `idx_showroom_release_asset_ref_asset_scope` (`tenant_id`, `site_key`, `stage`, `asset_id`, `content_hash`)'
);

CALL showroom_round12_add_index(
  'showroom_release_legacy_projection',
  'idx_showroom_release_legacy_projection_scope',
  'KEY `idx_showroom_release_legacy_projection_scope` (`tenant_id`, `site_key`, `stage`, `release_id`)'
);

CALL showroom_round12_exec(
  'ALTER TABLE `showroom_version_bundle` MODIFY COLUMN `narration_zh_version_id` bigint DEFAULT NULL'
);
CALL showroom_round12_exec(
  'ALTER TABLE `showroom_version_bundle` MODIFY COLUMN `narration_en_version_id` bigint DEFAULT NULL'
);

DROP PROCEDURE IF EXISTS `showroom_round12_add_index`;
DROP PROCEDURE IF EXISTS `showroom_round12_drop_index`;
DROP PROCEDURE IF EXISTS `showroom_round12_add_column`;
DROP PROCEDURE IF EXISTS `showroom_round12_require_table`;
DROP PROCEDURE IF EXISTS `showroom_round12_exec`;
