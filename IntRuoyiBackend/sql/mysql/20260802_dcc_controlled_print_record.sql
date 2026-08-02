-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260513_dcc_base_schema; type=schema,menu; riskLevel=medium
-- DCC controlled print record and menu permission. Idempotent and non-destructive.

CREATE TABLE IF NOT EXISTS `dcc_controlled_file_print_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `controlled_file_id` BIGINT NOT NULL,
  `file_number` VARCHAR(64) NOT NULL,
  `version_no` VARCHAR(64) NOT NULL,
  `print_no` VARCHAR(64) NOT NULL,
  `purpose` VARCHAR(255) NOT NULL,
  `copies` INT NOT NULL,
  `receiving_department` VARCHAR(128) NOT NULL,
  `use_location` VARCHAR(128) NOT NULL,
  `print_user_id` BIGINT NOT NULL,
  `print_user_name` VARCHAR(128) DEFAULT NULL,
  `print_time` DATETIME NOT NULL,
  `approval_status` VARCHAR(32) NOT NULL,
  `approval_user_id` BIGINT DEFAULT NULL,
  `approval_user_name` VARCHAR(128) DEFAULT NULL,
  `approval_time` DATETIME DEFAULT NULL,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `creator` VARCHAR(64) DEFAULT NULL,
  `updater` VARCHAR(64) DEFAULT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_controlled_print_no` (`tenant_id`, `print_no`, `deleted`),
  KEY `idx_dcc_controlled_print_file` (`tenant_id`, `controlled_file_id`, `print_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC controlled file print record';

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6813,
       CONVERT(UNHEX('444343E58F97E68EA7E68993E58DB0') USING utf8mb4),
       'dcc:controlled-file:print',
       3,
       4,
       6807,
       '',
       '',
       '',
       '',
       0,
       b'1',
       b'1',
       b'1',
       '1',
       NOW(),
       '1',
       NOW(),
       b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'dcc:controlled-file:print');
