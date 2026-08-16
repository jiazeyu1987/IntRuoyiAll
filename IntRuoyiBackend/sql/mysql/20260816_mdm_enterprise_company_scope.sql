-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260607_product_master_data; type=schema; riskLevel=medium
-- Recovery: MySQL DDL auto-commits; on failure retain the pre-migration backup, inspect every named table and constraint, reconcile the partial schema, then rerun this idempotent migration.
-- Rollback before business use: drop mdm_role_company_scope, mdm_user_company_scope, then mdm_enterprise after verifying that no downstream object references them.
-- Rollback after business use: destructive table removal is forbidden; restore the pre-migration backup or deliver an approved forward migration.
-- Unique-key policy: business keys remain permanently reserved after soft deletion because deleted is intentionally excluded from all three unique keys; T00-B must reject reuse rather than infer it.
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS assert_mdm_enterprise_company_scope_contract;
DELIMITER $$
CREATE PROCEDURE assert_mdm_enterprise_company_scope_contract(IN require_complete BOOLEAN)
BEGIN
  DECLARE present_table_count int DEFAULT 0;

  SELECT COUNT(*)
    INTO present_table_count
  FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME IN ('mdm_enterprise', 'mdm_user_company_scope', 'mdm_role_company_scope');

  IF require_complete AND present_table_count <> 3 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'MDM enterprise/company scope table contract is incomplete';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME IN ('mdm_enterprise', 'mdm_user_company_scope', 'mdm_role_company_scope')
      AND (TABLE_TYPE <> 'BASE TABLE' OR ENGINE <> 'InnoDB')
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'MDM enterprise/company scope table type or engine mismatch';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM (
      SELECT 'mdm_enterprise' AS table_name, 'id' AS column_name, 1 AS ordinal_position,
             'bigint' AS column_type, 'NO' AS is_nullable, '<null>' AS column_default,
             'auto_increment' AS extra_requirement
      UNION ALL SELECT 'mdm_enterprise', 'enterprise_code', 2, 'varchar(64)', 'NO', '<null>', ''
      UNION ALL SELECT 'mdm_enterprise', 'name', 3, 'varchar(255)', 'NO', '<null>', ''
      UNION ALL SELECT 'mdm_enterprise', 'type', 4, 'varchar(32)', 'NO', '<null>', ''
      UNION ALL SELECT 'mdm_enterprise', 'status', 5, 'varchar(32)', 'NO', '<null>', ''
      UNION ALL SELECT 'mdm_enterprise', 'revision', 6, 'int', 'NO', '1', ''
      UNION ALL SELECT 'mdm_enterprise', 'creator', 7, 'varchar(64)', 'YES', '', ''
      UNION ALL SELECT 'mdm_enterprise', 'create_time', 8, 'datetime', 'NO', 'current_timestamp', ''
      UNION ALL SELECT 'mdm_enterprise', 'updater', 9, 'varchar(64)', 'YES', '', ''
      UNION ALL SELECT 'mdm_enterprise', 'update_time', 10, 'datetime', 'NO', 'current_timestamp',
                       'on update current_timestamp'
      UNION ALL SELECT 'mdm_enterprise', 'deleted', 11, 'bit(1)', 'NO', 'b''0''', ''
      UNION ALL SELECT 'mdm_enterprise', 'tenant_id', 12, 'bigint', 'NO', '<null>', ''
      UNION ALL SELECT 'mdm_user_company_scope', 'id', 1, 'bigint', 'NO', '<null>', 'auto_increment'
      UNION ALL SELECT 'mdm_user_company_scope', 'user_id', 2, 'bigint', 'NO', '<null>', ''
      UNION ALL SELECT 'mdm_user_company_scope', 'company_id', 3, 'bigint', 'NO', '<null>', ''
      UNION ALL SELECT 'mdm_user_company_scope', 'status', 4, 'varchar(32)', 'NO', '<null>', ''
      UNION ALL SELECT 'mdm_user_company_scope', 'revision', 5, 'int', 'NO', '1', ''
      UNION ALL SELECT 'mdm_user_company_scope', 'creator', 6, 'varchar(64)', 'YES', '', ''
      UNION ALL SELECT 'mdm_user_company_scope', 'create_time', 7, 'datetime', 'NO', 'current_timestamp', ''
      UNION ALL SELECT 'mdm_user_company_scope', 'updater', 8, 'varchar(64)', 'YES', '', ''
      UNION ALL SELECT 'mdm_user_company_scope', 'update_time', 9, 'datetime', 'NO', 'current_timestamp',
                       'on update current_timestamp'
      UNION ALL SELECT 'mdm_user_company_scope', 'deleted', 10, 'bit(1)', 'NO', 'b''0''', ''
      UNION ALL SELECT 'mdm_user_company_scope', 'tenant_id', 11, 'bigint', 'NO', '<null>', ''
      UNION ALL SELECT 'mdm_role_company_scope', 'id', 1, 'bigint', 'NO', '<null>', 'auto_increment'
      UNION ALL SELECT 'mdm_role_company_scope', 'role_id', 2, 'bigint', 'NO', '<null>', ''
      UNION ALL SELECT 'mdm_role_company_scope', 'company_id', 3, 'bigint', 'NO', '<null>', ''
      UNION ALL SELECT 'mdm_role_company_scope', 'status', 4, 'varchar(32)', 'NO', '<null>', ''
      UNION ALL SELECT 'mdm_role_company_scope', 'revision', 5, 'int', 'NO', '1', ''
      UNION ALL SELECT 'mdm_role_company_scope', 'creator', 6, 'varchar(64)', 'YES', '', ''
      UNION ALL SELECT 'mdm_role_company_scope', 'create_time', 7, 'datetime', 'NO', 'current_timestamp', ''
      UNION ALL SELECT 'mdm_role_company_scope', 'updater', 8, 'varchar(64)', 'YES', '', ''
      UNION ALL SELECT 'mdm_role_company_scope', 'update_time', 9, 'datetime', 'NO', 'current_timestamp',
                       'on update current_timestamp'
      UNION ALL SELECT 'mdm_role_company_scope', 'deleted', 10, 'bit(1)', 'NO', 'b''0''', ''
      UNION ALL SELECT 'mdm_role_company_scope', 'tenant_id', 11, 'bigint', 'NO', '<null>', ''
    ) AS expected_column
    JOIN information_schema.TABLES AS present_table
      ON present_table.TABLE_SCHEMA = DATABASE()
     AND present_table.TABLE_NAME = expected_column.table_name
    LEFT JOIN information_schema.COLUMNS AS actual_column
      ON actual_column.TABLE_SCHEMA = DATABASE()
     AND actual_column.TABLE_NAME = expected_column.table_name
     AND actual_column.COLUMN_NAME = expected_column.column_name
    WHERE actual_column.COLUMN_NAME IS NULL
       OR actual_column.ORDINAL_POSITION <> expected_column.ordinal_position
       OR LOWER(actual_column.COLUMN_TYPE) <> expected_column.column_type
       OR actual_column.IS_NULLABLE <> expected_column.is_nullable
       OR COALESCE(LOWER(CAST(actual_column.COLUMN_DEFAULT AS CHAR)), '<null>')
            <> expected_column.column_default
       OR (expected_column.extra_requirement = 'auto_increment'
           AND LOWER(actual_column.EXTRA) <> 'auto_increment')
       OR (expected_column.extra_requirement = 'on update current_timestamp'
           AND LOWER(actual_column.EXTRA) NOT LIKE '%on update current_timestamp%')
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'MDM enterprise/company scope column contract mismatch';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM (
      SELECT 'mdm_enterprise' AS table_name, 'uk_mdm_enterprise_tenant_code' AS index_name,
             2 AS column_count, 'tenant_id,enterprise_code' AS column_names
      UNION ALL SELECT 'mdm_user_company_scope', 'uk_mdm_user_company_scope_tenant_user_company',
                       3, 'tenant_id,user_id,company_id'
      UNION ALL SELECT 'mdm_role_company_scope', 'uk_mdm_role_company_scope_tenant_role_company',
                       3, 'tenant_id,role_id,company_id'
    ) AS expected_index
    JOIN information_schema.TABLES AS present_table
      ON present_table.TABLE_SCHEMA = DATABASE()
     AND present_table.TABLE_NAME = expected_index.table_name
    LEFT JOIN (
      SELECT TABLE_NAME, INDEX_NAME, MAX(NON_UNIQUE) AS NON_UNIQUE, COUNT(*) AS column_count,
             GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') AS column_names,
             SUM(SUB_PART IS NOT NULL) AS prefix_column_count
      FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA = DATABASE()
        AND INDEX_NAME IN (
          'uk_mdm_enterprise_tenant_code',
          'uk_mdm_user_company_scope_tenant_user_company',
          'uk_mdm_role_company_scope_tenant_role_company'
        )
      GROUP BY TABLE_NAME, INDEX_NAME
    ) AS actual_index
      ON actual_index.TABLE_NAME = expected_index.table_name
     AND actual_index.INDEX_NAME = expected_index.index_name
    WHERE actual_index.INDEX_NAME IS NULL
       OR actual_index.NON_UNIQUE <> 0
       OR actual_index.column_count <> expected_index.column_count
       OR actual_index.column_names <> expected_index.column_names
       OR actual_index.prefix_column_count <> 0
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'MDM enterprise/company scope unique index contract mismatch';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM information_schema.TABLES AS enterprise_table
    WHERE enterprise_table.TABLE_SCHEMA = DATABASE()
      AND enterprise_table.TABLE_NAME = 'mdm_enterprise'
      AND NOT EXISTS (
        SELECT 1
        FROM information_schema.TABLE_CONSTRAINTS AS table_constraint
        JOIN information_schema.CHECK_CONSTRAINTS AS check_constraint
          ON check_constraint.CONSTRAINT_SCHEMA = table_constraint.CONSTRAINT_SCHEMA
         AND check_constraint.CONSTRAINT_NAME = table_constraint.CONSTRAINT_NAME
        WHERE table_constraint.CONSTRAINT_SCHEMA = DATABASE()
          AND table_constraint.TABLE_NAME = 'mdm_enterprise'
          AND table_constraint.CONSTRAINT_NAME = 'chk_mdm_enterprise_type'
          AND table_constraint.CONSTRAINT_TYPE = 'CHECK'
          AND LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
                REPLACE(check_constraint.CHECK_CLAUSE, CHAR(92), ''), '`', ''), ' ', ''), '(', ''), ')', ''),
                '_utf8mb4', ''))
              = 'typein''owned_company'',''entrusted_party'''
      )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'MDM enterprise type check constraint mismatch';
  END IF;
END$$
DELIMITER ;

CALL assert_mdm_enterprise_company_scope_contract(FALSE);

CREATE TABLE IF NOT EXISTS `mdm_enterprise` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Enterprise ID',
  `enterprise_code` varchar(64) NOT NULL COMMENT 'Stable enterprise code',
  `name` varchar(255) NOT NULL COMMENT 'Enterprise name',
  `type` varchar(32) NOT NULL COMMENT 'Enterprise type',
  `status` varchar(32) NOT NULL COMMENT 'Enable status',
  `revision` int NOT NULL DEFAULT 1 COMMENT 'Optimistic revision',
  `creator` varchar(64) DEFAULT '' COMMENT 'Creator',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
  `updater` varchar(64) DEFAULT '' COMMENT 'Updater',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'Logical deletion flag',
  `tenant_id` bigint NOT NULL COMMENT 'Tenant ID',
  PRIMARY KEY (`id`),
  CONSTRAINT `chk_mdm_enterprise_type` CHECK (`type` IN ('OWNED_COMPANY', 'ENTRUSTED_PARTY')),
  UNIQUE KEY `uk_mdm_enterprise_tenant_code` (`tenant_id`, `enterprise_code`),
  KEY `idx_mdm_enterprise_tenant_type_status` (`tenant_id`, `type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MDM enterprise master';

CREATE TABLE IF NOT EXISTS `mdm_user_company_scope` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Mapping ID',
  `user_id` bigint NOT NULL COMMENT 'System user ID',
  `company_id` bigint NOT NULL COMMENT 'MDM owned company enterprise ID',
  `status` varchar(32) NOT NULL COMMENT 'Enable status',
  `revision` int NOT NULL DEFAULT 1 COMMENT 'Optimistic revision',
  `creator` varchar(64) DEFAULT '' COMMENT 'Creator',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
  `updater` varchar(64) DEFAULT '' COMMENT 'Updater',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'Logical deletion flag',
  `tenant_id` bigint NOT NULL COMMENT 'Tenant ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mdm_user_company_scope_tenant_user_company` (`tenant_id`, `user_id`, `company_id`),
  KEY `idx_mdm_user_company_scope_tenant_company_status` (`tenant_id`, `company_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MDM user company scope';

CREATE TABLE IF NOT EXISTS `mdm_role_company_scope` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Mapping ID',
  `role_id` bigint NOT NULL COMMENT 'System role ID',
  `company_id` bigint NOT NULL COMMENT 'MDM owned company enterprise ID',
  `status` varchar(32) NOT NULL COMMENT 'Enable status',
  `revision` int NOT NULL DEFAULT 1 COMMENT 'Optimistic revision',
  `creator` varchar(64) DEFAULT '' COMMENT 'Creator',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
  `updater` varchar(64) DEFAULT '' COMMENT 'Updater',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'Logical deletion flag',
  `tenant_id` bigint NOT NULL COMMENT 'Tenant ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mdm_role_company_scope_tenant_role_company` (`tenant_id`, `role_id`, `company_id`),
  KEY `idx_mdm_role_company_scope_tenant_company_status` (`tenant_id`, `company_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MDM role company scope';

CALL assert_mdm_enterprise_company_scope_contract(TRUE);
DROP PROCEDURE IF EXISTS assert_mdm_enterprise_company_scope_contract;
