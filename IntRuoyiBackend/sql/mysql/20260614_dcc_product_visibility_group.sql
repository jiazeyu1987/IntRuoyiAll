-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260513_dcc_base_schema; type=schema; riskLevel=medium
-- DCC product visibility groups for file view matrix product-scoped grants.
-- Safety: additive schema only; no product, user, or controlled-file data is migrated.

DROP PROCEDURE IF EXISTS ensure_dcc_permission_rule_scope_type;

DELIMITER $$

CREATE PROCEDURE ensure_dcc_permission_rule_scope_type()
BEGIN
  DECLARE v_scope_type_column_count INT DEFAULT 0;

  SELECT COUNT(1)
    INTO v_scope_type_column_count
    FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'dcc_file_category_permission_rule'
     AND COLUMN_NAME = 'scope_type';

  IF v_scope_type_column_count = 0 THEN
    ALTER TABLE `dcc_file_category_permission_rule`
      ADD COLUMN `scope_type` varchar(32) NOT NULL DEFAULT 'GLOBAL'
        COMMENT 'Permission scope: GLOBAL or PRODUCT_GROUP';
  ELSE
    SELECT 'dcc_file_category_permission_rule.scope_type already exists' AS migration_status;
  END IF;
END$$

DELIMITER ;

CALL ensure_dcc_permission_rule_scope_type();
DROP PROCEDURE IF EXISTS ensure_dcc_permission_rule_scope_type;

CREATE TABLE IF NOT EXISTS `dcc_product_visibility_group` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Group id',
  `tenant_id` bigint NOT NULL COMMENT 'Tenant id',
  `dept_id` bigint NOT NULL COMMENT 'Owning department id',
  `name` varchar(128) NOT NULL COMMENT 'Product visibility group name',
  `active` bit(1) NOT NULL DEFAULT b'1' COMMENT 'Whether group is active',
  `remark` varchar(500) DEFAULT NULL COMMENT 'Remark',
  `creator` varchar(64) DEFAULT '' COMMENT 'Creator',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `updater` varchar(64) DEFAULT '' COMMENT 'Updater',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'Deleted',
  PRIMARY KEY (`id`),
  KEY `idx_dcc_pvg_dept` (`tenant_id`, `dept_id`, `active`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC product visibility group';

CREATE TABLE IF NOT EXISTS `dcc_product_visibility_group_member` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Member id',
  `tenant_id` bigint NOT NULL COMMENT 'Tenant id',
  `group_id` bigint NOT NULL COMMENT 'Group id',
  `user_id` bigint NOT NULL COMMENT 'User id',
  `active` bit(1) NOT NULL DEFAULT b'1' COMMENT 'Whether member binding is active',
  `remark` varchar(500) DEFAULT NULL COMMENT 'Remark',
  `creator` varchar(64) DEFAULT '' COMMENT 'Creator',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `updater` varchar(64) DEFAULT '' COMMENT 'Updater',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'Deleted',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_pvg_member` (`tenant_id`, `group_id`, `user_id`, `deleted`),
  KEY `idx_dcc_pvgm_user` (`tenant_id`, `user_id`, `active`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC product visibility group member';

CREATE TABLE IF NOT EXISTS `dcc_product_visibility_group_product` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Product binding id',
  `tenant_id` bigint NOT NULL COMMENT 'Tenant id',
  `group_id` bigint NOT NULL COMMENT 'Group id',
  `product_master_id` bigint NOT NULL COMMENT 'MDM product master id',
  `active` bit(1) NOT NULL DEFAULT b'1' COMMENT 'Whether product binding is active',
  `remark` varchar(500) DEFAULT NULL COMMENT 'Remark',
  `creator` varchar(64) DEFAULT '' COMMENT 'Creator',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `updater` varchar(64) DEFAULT '' COMMENT 'Updater',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'Deleted',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_pvg_product` (`tenant_id`, `group_id`, `product_master_id`, `deleted`),
  KEY `idx_dcc_pvgp_product` (`tenant_id`, `product_master_id`, `active`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC product visibility group product';
