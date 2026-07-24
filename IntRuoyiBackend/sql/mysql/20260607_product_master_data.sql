-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=data; riskLevel=medium
CREATE TABLE IF NOT EXISTS `mdm_product` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_code` varchar(64) NOT NULL,
  `dcc_product_code` varchar(14) DEFAULT NULL,
  `name_cn` varchar(255) NOT NULL,
  `name_en` varchar(255) DEFAULT NULL,
  `model_specification` varchar(255) DEFAULT NULL,
  `category` varchar(128) DEFAULT NULL,
  `status` varchar(32) NOT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mdm_product_code` (`tenant_id`, `product_code`),
  UNIQUE KEY `uk_mdm_product_dcc_code` (`tenant_id`, `dcc_product_code`),
  KEY `idx_mdm_product_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Product master data';

CREATE TABLE IF NOT EXISTS `mdm_product_import_batch` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `status` varchar(32) NOT NULL,
  `total_count` int NOT NULL DEFAULT 0,
  `create_count` int NOT NULL DEFAULT 0,
  `update_count` int NOT NULL DEFAULT 0,
  `disable_count` int NOT NULL DEFAULT 0,
  `unchanged_count` int NOT NULL DEFAULT 0,
  `failure_count` int NOT NULL DEFAULT 0,
  `confirmed_at` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_mdm_product_import_batch_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Product master data import batch';

CREATE TABLE IF NOT EXISTS `mdm_product_import_row` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_id` bigint NOT NULL,
  `row_no` int NOT NULL,
  `product_code` varchar(64) DEFAULT NULL,
  `dcc_product_code` varchar(14) DEFAULT NULL,
  `name_cn` varchar(255) DEFAULT NULL,
  `name_en` varchar(255) DEFAULT NULL,
  `model_specification` varchar(255) DEFAULT NULL,
  `category` varchar(128) DEFAULT NULL,
  `current_status` varchar(32) DEFAULT NULL,
  `import_action` varchar(32) NOT NULL,
  `failure_reason` varchar(1000) DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_mdm_product_import_row_batch` (`tenant_id`, `batch_id`, `row_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Product master data import row';

SET @dcc_product_master_column_exists := (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'dcc_controlled_file'
    AND COLUMN_NAME = 'product_master_id'
);
SET @dcc_product_master_add_sql := IF(
  @dcc_product_master_column_exists = 0,
  'ALTER TABLE `dcc_controlled_file` ADD COLUMN `product_master_id` bigint DEFAULT NULL AFTER `file_number`',
  'SELECT 1'
);
PREPARE dcc_product_master_stmt FROM @dcc_product_master_add_sql;
EXECUTE dcc_product_master_stmt;
DEALLOCATE PREPARE dcc_product_master_stmt;

SET @showroom_product_master_column_exists := (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'showroom_product'
    AND COLUMN_NAME = 'product_master_id'
);
SET @showroom_product_master_add_sql := IF(
  @showroom_product_master_column_exists = 0,
  'ALTER TABLE `showroom_product` ADD COLUMN `product_master_id` bigint DEFAULT NULL AFTER `product_code`',
  'SELECT 1'
);
PREPARE showroom_product_master_stmt FROM @showroom_product_master_add_sql;
EXECUTE showroom_product_master_stmt;
DEALLOCATE PREPARE showroom_product_master_stmt;

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 990200, '基础数据', '', 1, 35, 0, '/mdm', 'ep:coin', NULL, NULL,
       0, b'1', b'1', b'1', 'mdm-product', NOW(), 'mdm-product', NOW(), b'0'
WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu` WHERE `deleted` = b'0' AND (`id` = 990200 OR `path` = '/mdm')
);

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 990201, '产品主数据', 'mdm:product:query', 2, 10, 990200, 'product', 'ep:goods', 'mdm/product/index', 'MdmProduct',
       0, b'1', b'1', b'1', 'mdm-product', NOW(), 'mdm-product', NOW(), b'0'
WHERE EXISTS (
  SELECT 1 FROM `system_menu` WHERE `id` = 990200 AND `deleted` = b'0'
)
  AND NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `deleted` = b'0' AND (`id` = 990201 OR `permission` = 'mdm:product:query')
  );

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 990202, '产品主数据新增', 'mdm:product:create', 3, 20, 990201, '', '', '', '',
       0, b'1', b'1', b'1', 'mdm-product', NOW(), 'mdm-product', NOW(), b'0'
WHERE EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 990201 AND `deleted` = b'0')
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `deleted` = b'0' AND `permission` = 'mdm:product:create');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 990203, '产品主数据修改', 'mdm:product:update', 3, 30, 990201, '', '', '', '',
       0, b'1', b'1', b'1', 'mdm-product', NOW(), 'mdm-product', NOW(), b'0'
WHERE EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 990201 AND `deleted` = b'0')
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `deleted` = b'0' AND `permission` = 'mdm:product:update');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 990204, '产品主数据删除', 'mdm:product:delete', 3, 40, 990201, '', '', '', '',
       0, b'1', b'1', b'1', 'mdm-product', NOW(), 'mdm-product', NOW(), b'0'
WHERE EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 990201 AND `deleted` = b'0')
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `deleted` = b'0' AND `permission` = 'mdm:product:delete');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 990205, '产品主数据导入', 'mdm:product:import', 3, 50, 990201, '', '', '', '',
       0, b'1', b'1', b'1', 'mdm-product', NOW(), 'mdm-product', NOW(), b'0'
WHERE EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 990201 AND `deleted` = b'0')
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `deleted` = b'0' AND `permission` = 'mdm:product:import');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 990206, '产品主数据导出', 'mdm:product:export', 3, 60, 990201, '', '', '', '',
       0, b'1', b'1', b'1', 'mdm-product', NOW(), 'mdm-product', NOW(), b'0'
WHERE EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 990201 AND `deleted` = b'0')
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `deleted` = b'0' AND `permission` = 'mdm:product:export');
