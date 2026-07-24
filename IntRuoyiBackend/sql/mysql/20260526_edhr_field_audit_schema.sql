-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- eDHR field-level append-only audit schema.
-- Missing prerequisites fail fast; historical audit rows are never rewritten.

DROP PROCEDURE IF EXISTS ensure_edhr_field_audit_schema;
DELIMITER $$
CREATE PROCEDURE ensure_edhr_field_audit_schema()
BEGIN
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.TABLES
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'mes_pro_batch_record_execution'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing mes_pro_batch_record_execution; apply base eDHR schema first';
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM information_schema.TABLES
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'mes_pro_batch_record_execution_signature'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing mes_pro_batch_record_execution_signature; apply eDHR signature schema first';
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM information_schema.TABLES
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'mes_pro_batch_record_execution_archive'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing mes_pro_batch_record_execution_archive; apply sql/mysql/20260525_edhr_archive_schema.sql first';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_batch_record_execution' AND COLUMN_NAME = 'cell_values_hash') THEN
    ALTER TABLE `mes_pro_batch_record_execution` ADD COLUMN `cell_values_hash` char(64) DEFAULT NULL COMMENT 'Current canonical cell_values_json SHA-256' AFTER `cell_values_json`;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_batch_record_execution' AND COLUMN_NAME = 'field_audit_revision') THEN
    ALTER TABLE `mes_pro_batch_record_execution` ADD COLUMN `field_audit_revision` bigint DEFAULT NULL COMMENT 'Field audit revision' AFTER `cell_values_hash`;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_batch_record_execution' AND COLUMN_NAME = 'field_audit_head_hash') THEN
    ALTER TABLE `mes_pro_batch_record_execution` ADD COLUMN `field_audit_head_hash` char(64) DEFAULT NULL COMMENT 'Field audit head hash' AFTER `field_audit_revision`;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_batch_record_execution' AND COLUMN_NAME = 'field_audit_last_batch_id') THEN
    ALTER TABLE `mes_pro_batch_record_execution` ADD COLUMN `field_audit_last_batch_id` bigint DEFAULT NULL COMMENT 'Last field audit batch ID' AFTER `field_audit_head_hash`;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_batch_record_execution_signature' AND COLUMN_NAME = 'reason_category') THEN
    ALTER TABLE `mes_pro_batch_record_execution_signature` ADD COLUMN `reason_category` varchar(64) DEFAULT NULL COMMENT 'Field change reason category' AFTER `actor_name`;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_batch_record_execution_signature' AND COLUMN_NAME = 'audit_batch_id') THEN
    ALTER TABLE `mes_pro_batch_record_execution_signature` ADD COLUMN `audit_batch_id` bigint DEFAULT NULL COMMENT 'Field audit batch ID' AFTER `reason_category`;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_batch_record_execution_signature' AND COLUMN_NAME = 'signature_challenge_hash') THEN
    ALTER TABLE `mes_pro_batch_record_execution_signature` ADD COLUMN `signature_challenge_hash` char(64) DEFAULT NULL COMMENT 'Field change signature challenge hash' AFTER `audit_batch_id`;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_batch_record_execution_signature' AND COLUMN_NAME = 'field_audit_revision') THEN
    ALTER TABLE `mes_pro_batch_record_execution_signature` ADD COLUMN `field_audit_revision` bigint DEFAULT NULL COMMENT 'Bound field audit revision' AFTER `signature_challenge_hash`;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_batch_record_execution_signature' AND COLUMN_NAME = 'field_audit_head_hash') THEN
    ALTER TABLE `mes_pro_batch_record_execution_signature` ADD COLUMN `field_audit_head_hash` char(64) DEFAULT NULL COMMENT 'Bound field audit head hash' AFTER `field_audit_revision`;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_batch_record_execution_signature' AND COLUMN_NAME = 'cell_values_hash') THEN
    ALTER TABLE `mes_pro_batch_record_execution_signature` ADD COLUMN `cell_values_hash` char(64) DEFAULT NULL COMMENT 'Bound cell values hash' AFTER `field_audit_head_hash`;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_batch_record_execution_archive' AND COLUMN_NAME = 'field_audit_revision') THEN
    ALTER TABLE `mes_pro_batch_record_execution_archive` ADD COLUMN `field_audit_revision` bigint DEFAULT NULL COMMENT 'Bound field audit revision' AFTER `cell_values_hash`;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_batch_record_execution_archive' AND COLUMN_NAME = 'field_audit_head_hash') THEN
    ALTER TABLE `mes_pro_batch_record_execution_archive` ADD COLUMN `field_audit_head_hash` char(64) DEFAULT NULL COMMENT 'Bound field audit head hash' AFTER `field_audit_revision`;
  END IF;

  CREATE TABLE IF NOT EXISTS `mes_pro_batch_record_execution_field_audit_batch` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `execution_id` bigint NOT NULL COMMENT 'eDHR execution ID',
    `idempotency_key` varchar(64) NOT NULL COMMENT 'Request idempotency key',
    `request_hash` char(64) NOT NULL COMMENT 'Canonical request hash without password plaintext',
    `action_type` varchar(32) NOT NULL COMMENT 'FIELD_CHANGE or BASELINE_ANCHOR',
    `reason_category` varchar(64) NOT NULL COMMENT 'Reason category',
    `reason_text` varchar(500) NOT NULL COMMENT 'Reason text',
    `field_count` int NOT NULL COMMENT 'Changed field count',
    `actor_id` bigint NOT NULL COMMENT 'Actor ID',
    `actor_name` varchar(64) NOT NULL COMMENT 'Actor name snapshot',
    `signature_id` bigint NOT NULL COMMENT 'FIELD_CHANGE signature ID',
    `signature_challenge_hash` char(64) NOT NULL COMMENT 'Pre-sign challenge hash',
    `signature_projection_hash` char(64) NOT NULL COMMENT 'Persisted signature projection hash',
    `base_cell_values_hash` char(64) NOT NULL COMMENT 'Request base cell values hash',
    `before_cell_values_hash` char(64) NOT NULL COMMENT 'Before transaction cell values hash',
    `after_cell_values_hash` char(64) NOT NULL COMMENT 'After transaction cell values hash',
    `base_field_audit_revision` bigint NOT NULL COMMENT 'Request base field audit revision',
    `before_field_audit_revision` bigint NOT NULL COMMENT 'Before transaction field audit revision',
    `after_field_audit_revision` bigint NOT NULL COMMENT 'After transaction field audit revision',
    `base_field_audit_head_hash` char(64) NOT NULL COMMENT 'Request base field audit head hash',
    `previous_head_hash` char(64) NOT NULL COMMENT 'Previous head hash',
    `new_head_hash` char(64) NOT NULL COMMENT 'New head hash',
    `hash_verification_json` text NOT NULL COMMENT 'Hash verification summary',
    `changed_at` datetime NOT NULL COMMENT 'Server change time',
    `client_ip` varchar(64) DEFAULT NULL COMMENT 'Client IP',
    `user_agent` varchar(512) DEFAULT NULL COMMENT 'User-Agent',
    `creator` varchar(64) DEFAULT '' COMMENT 'Creator',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    `updater` varchar(64) DEFAULT '' COMMENT 'Updater',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'Deleted flag',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT 'Tenant ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_field_audit_batch_idempotency` (`tenant_id`, `execution_id`, `idempotency_key`),
    UNIQUE KEY `uk_field_audit_batch_signature` (`tenant_id`, `signature_id`),
    KEY `idx_field_audit_batch_execution_revision` (`tenant_id`, `execution_id`, `after_field_audit_revision`),
    KEY `idx_field_audit_batch_execution_time` (`tenant_id`, `execution_id`, `changed_at`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES eDHR field audit batch';

  CREATE TABLE IF NOT EXISTS `mes_pro_batch_record_execution_field_audit_item` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `audit_batch_id` bigint NOT NULL COMMENT 'Field audit batch ID',
    `execution_id` bigint NOT NULL COMMENT 'eDHR execution ID',
    `field_audit_revision` bigint NOT NULL COMMENT 'Revision after this item',
    `batch_item_index` int NOT NULL COMMENT 'Item index in batch, starts from 1',
    `field_path` varchar(512) NOT NULL COMMENT 'Field path',
    `field_key` varchar(128) NOT NULL COMMENT 'Field key',
    `field_label` varchar(255) NOT NULL COMMENT 'Field label snapshot',
    `row_index` int NOT NULL COMMENT 'Row index',
    `column_index` int NOT NULL COMMENT 'Column index',
    `component` varchar(64) DEFAULT NULL COMMENT 'Component snapshot',
    `value_type` varchar(32) NOT NULL COMMENT 'Typed JSON value type',
    `old_value_json` longtext NOT NULL COMMENT 'Canonical old typed value JSON',
    `old_value_display` varchar(1000) NOT NULL COMMENT 'Old display value',
    `old_value_hash` char(64) NOT NULL COMMENT 'Old value hash',
    `new_value_json` longtext NOT NULL COMMENT 'Canonical new typed value JSON',
    `new_value_display` varchar(1000) NOT NULL COMMENT 'New display value',
    `new_value_hash` char(64) NOT NULL COMMENT 'New value hash',
    `reason_category` varchar(64) NOT NULL COMMENT 'Reason category',
    `reason_text` varchar(500) NOT NULL COMMENT 'Reason text',
    `actor_id` bigint NOT NULL COMMENT 'Actor ID',
    `actor_name` varchar(64) NOT NULL COMMENT 'Actor name snapshot',
    `signature_id` bigint NOT NULL COMMENT 'FIELD_CHANGE signature ID',
    `signature_projection_hash` char(64) NOT NULL COMMENT 'Signature projection hash',
    `previous_hash` char(64) NOT NULL COMMENT 'Previous item hash',
    `audit_hash` char(64) NOT NULL COMMENT 'This item hash',
    `before_cell_values_hash` char(64) NOT NULL COMMENT 'Batch before cell values hash',
    `after_cell_values_hash` char(64) NOT NULL COMMENT 'Batch after cell values hash',
    `execution_snapshot_hash` char(64) NOT NULL COMMENT 'Execution snapshot hash',
    `changed_at` datetime NOT NULL COMMENT 'Server change time',
    `creator` varchar(64) DEFAULT '' COMMENT 'Creator',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    `updater` varchar(64) DEFAULT '' COMMENT 'Updater',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'Deleted flag',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT 'Tenant ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_field_audit_item_revision` (`tenant_id`, `execution_id`, `field_audit_revision`),
    UNIQUE KEY `uk_field_audit_item_hash` (`tenant_id`, `audit_hash`),
    KEY `idx_field_audit_item_field` (`tenant_id`, `execution_id`, `field_path`, `field_key`, `changed_at`),
    KEY `idx_field_audit_item_actor_time` (`tenant_id`, `actor_id`, `changed_at`),
    KEY `idx_field_audit_item_batch` (`tenant_id`, `audit_batch_id`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES eDHR field audit item';

  INSERT INTO `system_menu`
    (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 900027, 'eDHR字段审计保存', 'mes:pro-batch-record-execution:field-audit-update', 3, 271, 900002, '', '', '', '', 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-batch-record-execution:field-audit-update');
  INSERT INTO `system_menu`
    (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 900028, 'eDHR字段审计查询', 'mes:pro-batch-record-execution:field-audit-query', 3, 272, 900002, '', '', '', '', 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-batch-record-execution:field-audit-query');
  INSERT INTO `system_menu`
    (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 900029, 'eDHR字段审计校验', 'mes:pro-batch-record-execution:field-audit-verify', 3, 273, 900002, '', '', '', '', 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-batch-record-execution:field-audit-verify');
  INSERT INTO `system_menu`
    (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 900030, 'eDHR字段审计导出', 'mes:pro-batch-record-execution:field-audit-export', 3, 274, 900002, '', '', '', '', 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-batch-record-execution:field-audit-export');
END$$
DELIMITER ;
CALL ensure_edhr_field_audit_schema();
DROP PROCEDURE IF EXISTS ensure_edhr_field_audit_schema;

DROP PROCEDURE IF EXISTS ensure_edhr_field_audit_tenant_package_menus;
DELIMITER $$
CREATE PROCEDURE ensure_edhr_field_audit_tenant_package_menus()
BEGIN
  IF EXISTS (
      SELECT 1
      FROM `system_tenant_package` AS `package`
      WHERE `package`.`deleted` = b'0'
        AND NOT JSON_VALID(`package`.`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot merge eDHR field audit permissions';
  END IF;

  IF EXISTS (
      SELECT 1
      FROM (
          SELECT `permission`
          FROM `system_menu`
          WHERE `deleted` = b'0'
            AND `permission` IN (
                'mes:pro-batch-record-execution:field-audit-update',
                'mes:pro-batch-record-execution:field-audit-query',
                'mes:pro-batch-record-execution:field-audit-verify',
                'mes:pro-batch-record-execution:field-audit-export'
            )
          GROUP BY `permission`
          HAVING COUNT(*) > 1
      ) AS `duplicate_permission`
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Duplicate eDHR field audit system_menu permissions; cannot merge tenant package menu_ids';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_field_audit_permission_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_edhr_field_audit_permission_menu_ids` (
      `permission` varchar(128) NOT NULL,
      `id` BIGINT NOT NULL,
      PRIMARY KEY (`permission`),
      UNIQUE KEY `uk_tmp_edhr_field_audit_permission_menu_id` (`id`)
  );

  INSERT INTO `tmp_edhr_field_audit_permission_menu_ids` (`permission`, `id`)
  SELECT
      `permission`,
      MIN(`id`) AS `id`
  FROM `system_menu`
  WHERE `deleted` = b'0'
    AND `permission` IN (
        'mes:pro-batch-record-execution:field-audit-update',
        'mes:pro-batch-record-execution:field-audit-query',
        'mes:pro-batch-record-execution:field-audit-verify',
        'mes:pro-batch-record-execution:field-audit-export'
    )
  GROUP BY `permission`;

  IF (SELECT COUNT(DISTINCT `permission`) FROM `tmp_edhr_field_audit_permission_menu_ids`) <> 4 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR field audit system_menu permissions; cannot merge tenant package menu_ids';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_field_audit_permission_target_packages`;
  CREATE TEMPORARY TABLE `tmp_edhr_field_audit_permission_target_packages` AS
  SELECT DISTINCT
      `package`.`id` AS `package_id`
  FROM `system_tenant_package` AS `package`
  INNER JOIN JSON_TABLE(
      `package`.`menu_ids`,
      '$[*]' COLUMNS (`menu_id` BIGINT PATH '$')
  ) AS `existing_menu`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`)
    AND `existing_menu`.`menu_id` IN (5700, 900002);

  DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_field_audit_permission_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_edhr_field_audit_permission_package_menu_ids` (
      `package_id` BIGINT NOT NULL,
      `menu_id` BIGINT NOT NULL,
      PRIMARY KEY (`package_id`, `menu_id`)
  );

  INSERT IGNORE INTO `tmp_edhr_field_audit_permission_package_menu_ids` (`package_id`, `menu_id`)
  SELECT DISTINCT
      `target`.`package_id`,
      CAST(`existing_menu`.`menu_id` AS UNSIGNED) AS `menu_id`
  FROM `tmp_edhr_field_audit_permission_target_packages` AS `target`
  INNER JOIN `system_tenant_package` AS `package`
          ON `package`.`id` = `target`.`package_id`
         AND `package`.`deleted` = b'0'
         AND JSON_VALID(`package`.`menu_ids`)
  INNER JOIN JSON_TABLE(
      `package`.`menu_ids`,
      '$[*]' COLUMNS (`menu_id` BIGINT PATH '$')
  ) AS `existing_menu`;

  INSERT IGNORE INTO `tmp_edhr_field_audit_permission_package_menu_ids` (`package_id`, `menu_id`)
  SELECT DISTINCT
      `target`.`package_id`,
      `permission_menu`.`id` AS `menu_id`
  FROM `tmp_edhr_field_audit_permission_target_packages` AS `target`
  CROSS JOIN `tmp_edhr_field_audit_permission_menu_ids` AS `permission_menu`;

  DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_field_audit_permission_package_menu_json`;
  CREATE TEMPORARY TABLE `tmp_edhr_field_audit_permission_package_menu_json` AS
  SELECT
      `package_id`,
      JSON_ARRAYAGG(`menu_id`) AS `menu_ids`
  FROM (
      SELECT `package_id`, `menu_id`
      FROM `tmp_edhr_field_audit_permission_package_menu_ids`
      ORDER BY `package_id`, `menu_id`
  ) AS `merged_menu_ids`
  GROUP BY `package_id`;

  UPDATE `system_tenant_package` AS `package`
  INNER JOIN `tmp_edhr_field_audit_permission_package_menu_json` AS `merged`
          ON `merged`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = `merged`.`menu_ids`,
      `package`.`updater` = 'edhr-field-audit-permission',
      `package`.`update_time` = NOW()
  WHERE `package`.`deleted` = b'0';

  DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_field_audit_permission_target_roles`;
  CREATE TEMPORARY TABLE `tmp_edhr_field_audit_permission_target_roles` AS
  SELECT DISTINCT
      `tenant`.`id` AS `tenant_id`,
      `role`.`id` AS `role_id`
  FROM `system_tenant` AS `tenant`
  INNER JOIN `tmp_edhr_field_audit_permission_target_packages` AS `target_package`
          ON `target_package`.`package_id` = `tenant`.`package_id`
  INNER JOIN `system_role` AS `role`
          ON `role`.`tenant_id` = `tenant`.`id`
         AND `role`.`code` = 'tenant_admin'
         AND `role`.`deleted` = b'0'
  WHERE `tenant`.`deleted` = b'0'
    AND `tenant`.`package_id` <> 0;

  UPDATE `system_role_menu` AS `role_menu`
  INNER JOIN `tmp_edhr_field_audit_permission_target_roles` AS `target_role`
          ON `target_role`.`tenant_id` = `role_menu`.`tenant_id`
         AND `target_role`.`role_id` = `role_menu`.`role_id`
  INNER JOIN `tmp_edhr_field_audit_permission_menu_ids` AS `permission_menu`
          ON `permission_menu`.`id` = `role_menu`.`menu_id`
  SET `role_menu`.`deleted` = b'0',
      `role_menu`.`updater` = 'edhr-field-audit-permission',
      `role_menu`.`update_time` = NOW()
  WHERE `role_menu`.`deleted` = b'1';

  INSERT INTO `system_role_menu`
  (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT
      `target_role`.`role_id`,
      `permission_menu`.`id`,
      'edhr-field-audit-permission',
      NOW(),
      'edhr-field-audit-permission',
      NOW(),
      b'0',
      `target_role`.`tenant_id`
  FROM `tmp_edhr_field_audit_permission_target_roles` AS `target_role`
  CROSS JOIN `tmp_edhr_field_audit_permission_menu_ids` AS `permission_menu`
  WHERE NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` AS `existing`
      WHERE `existing`.`tenant_id` = `target_role`.`tenant_id`
        AND `existing`.`role_id` = `target_role`.`role_id`
        AND `existing`.`menu_id` = `permission_menu`.`id`
        AND `existing`.`deleted` = b'0'
  );

  DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_field_audit_permission_target_roles`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_field_audit_permission_package_menu_json`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_field_audit_permission_package_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_field_audit_permission_target_packages`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_field_audit_permission_menu_ids`;
END$$
DELIMITER ;

CALL ensure_edhr_field_audit_tenant_package_menus();

DROP PROCEDURE IF EXISTS ensure_edhr_field_audit_tenant_package_menus;

DROP TRIGGER IF EXISTS `trg_field_audit_batch_no_update`;
DROP TRIGGER IF EXISTS `trg_field_audit_batch_no_delete`;
DROP TRIGGER IF EXISTS `trg_field_audit_item_no_update`;
DROP TRIGGER IF EXISTS `trg_field_audit_item_no_delete`;
DELIMITER $$
CREATE TRIGGER `trg_field_audit_batch_no_update`
BEFORE UPDATE ON `mes_pro_batch_record_execution_field_audit_batch`
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'eDHR field audit batch is append-only';
END$$
CREATE TRIGGER `trg_field_audit_batch_no_delete`
BEFORE DELETE ON `mes_pro_batch_record_execution_field_audit_batch`
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'eDHR field audit batch is append-only';
END$$
CREATE TRIGGER `trg_field_audit_item_no_update`
BEFORE UPDATE ON `mes_pro_batch_record_execution_field_audit_item`
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'eDHR field audit item is append-only';
END$$
CREATE TRIGGER `trg_field_audit_item_no_delete`
BEFORE DELETE ON `mes_pro_batch_record_execution_field_audit_item`
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'eDHR field audit item is append-only';
END$$
DELIMITER ;
