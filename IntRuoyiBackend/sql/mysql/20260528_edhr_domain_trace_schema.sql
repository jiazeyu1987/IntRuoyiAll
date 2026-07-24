-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- eDHR domain trace schema.
-- Missing prerequisites fail fast; generated trace snapshots and items are append-only evidence.

DROP PROCEDURE IF EXISTS ensure_edhr_domain_trace_schema;
DELIMITER $$
CREATE PROCEDURE ensure_edhr_domain_trace_schema()
BEGIN
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.TABLES
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'mes_pro_batch_record_execution'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing mes_pro_batch_record_execution; apply eDHR execution schema first';
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
        AND TABLE_NAME = 'mes_pro_batch_record_approval_snapshot'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing mes_pro_batch_record_approval_snapshot; apply eDHR approval schema first';
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM information_schema.TABLES
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'mes_pro_batch_record_execution_archive'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing mes_pro_batch_record_execution_archive; apply eDHR archive schema first';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_batch_record_execution' AND COLUMN_NAME = 'domain_trace_snapshot_id') THEN
    ALTER TABLE `mes_pro_batch_record_execution` ADD COLUMN `domain_trace_snapshot_id` bigint DEFAULT NULL COMMENT 'Latest eDHR domain trace snapshot ID' AFTER `closed_at`;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_batch_record_execution' AND COLUMN_NAME = 'domain_trace_hash') THEN
    ALTER TABLE `mes_pro_batch_record_execution` ADD COLUMN `domain_trace_hash` char(64) DEFAULT NULL COMMENT 'Latest canonical eDHR domain trace hash' AFTER `domain_trace_snapshot_id`;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_batch_record_execution' AND COLUMN_NAME = 'domain_trace_status') THEN
    ALTER TABLE `mes_pro_batch_record_execution` ADD COLUMN `domain_trace_status` varchar(32) DEFAULT NULL COMMENT 'Latest domain trace status: VERIFIED or BLOCKED' AFTER `domain_trace_hash`;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_batch_record_execution' AND COLUMN_NAME = 'domain_trace_verified_at') THEN
    ALTER TABLE `mes_pro_batch_record_execution` ADD COLUMN `domain_trace_verified_at` datetime DEFAULT NULL COMMENT 'Latest domain trace verification time' AFTER `domain_trace_status`;
  END IF;

  CREATE TABLE IF NOT EXISTS `mes_pro_batch_record_domain_trace_snapshot` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `execution_id` bigint NOT NULL COMMENT 'eDHR execution ID',
    `snapshot_version` varchar(32) NOT NULL COMMENT 'Domain trace snapshot version',
    `snapshot_json` longtext NOT NULL COMMENT 'Canonical domain trace snapshot JSON',
    `snapshot_hash` char(64) NOT NULL COMMENT 'Canonical domain trace snapshot SHA-256',
    `completeness_status` varchar(32) NOT NULL COMMENT 'VERIFIED or BLOCKED',
    `blocker_count` int NOT NULL COMMENT 'Current blocker count',
    `verified_by` bigint DEFAULT NULL COMMENT 'Verifier user ID',
    `verified_at` datetime NOT NULL COMMENT 'Verification time',
    `creator` varchar(64) DEFAULT '' COMMENT 'Creator',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    `updater` varchar(64) DEFAULT '' COMMENT 'Updater',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'Deleted flag',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT 'Tenant ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_domain_trace_snapshot_hash` (`tenant_id`, `execution_id`, `snapshot_hash`),
    KEY `idx_domain_trace_snapshot_execution` (`tenant_id`, `execution_id`, `verified_at`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES eDHR domain trace snapshot';

  CREATE TABLE IF NOT EXISTS `mes_pro_batch_record_domain_trace_item` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `snapshot_id` bigint NOT NULL COMMENT 'Domain trace snapshot ID',
    `execution_id` bigint NOT NULL COMMENT 'eDHR execution ID',
    `item_type` varchar(64) NOT NULL COMMENT 'Trace item type',
    `item_key` varchar(128) NOT NULL COMMENT 'Trace item key',
    `item_name` varchar(255) DEFAULT NULL COMMENT 'Trace item display name',
    `source_table` varchar(128) DEFAULT NULL COMMENT 'Source table or logical source',
    `source_id` bigint DEFAULT NULL COMMENT 'Source row ID',
    `source_code` varchar(128) DEFAULT NULL COMMENT 'Source business code',
    `source_version` varchar(64) DEFAULT NULL COMMENT 'Source version',
    `snapshot_json` longtext DEFAULT NULL COMMENT 'Canonical item snapshot JSON',
    `snapshot_hash` char(64) DEFAULT NULL COMMENT 'Canonical item snapshot SHA-256',
    `required_flag` bit(1) NOT NULL DEFAULT b'1' COMMENT 'Whether this trace item is required',
    `status` varchar(32) NOT NULL COMMENT 'VERIFIED or BLOCKED',
    `blocker_code` varchar(128) DEFAULT NULL COMMENT 'Blocker code',
    `blocker_message` varchar(500) DEFAULT NULL COMMENT 'Blocker message',
    `blocker_reason` varchar(500) DEFAULT NULL COMMENT 'Blocker reason shown to frontend',
    `creator` varchar(64) DEFAULT '' COMMENT 'Creator',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    `updater` varchar(64) DEFAULT '' COMMENT 'Updater',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'Deleted flag',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT 'Tenant ID',
    PRIMARY KEY (`id`),
    KEY `idx_domain_trace_item_execution_type` (`tenant_id`, `execution_id`, `item_type`, `status`),
    KEY `idx_domain_trace_item_source` (`tenant_id`, `source_table`, `source_id`),
    KEY `idx_domain_trace_item_snapshot` (`tenant_id`, `snapshot_id`, `item_type`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES eDHR domain trace item';

  INSERT INTO `system_menu`
    (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 900031, 'eDHR主数据追溯查询', 'mes:pro-batch-record-execution:domain-trace-query', 3, 275, 900002, '', '', '', '', 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-batch-record-execution:domain-trace-query');
  INSERT INTO `system_menu`
    (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 900032, 'eDHR主数据追溯校验', 'mes:pro-batch-record-execution:domain-trace-verify', 3, 276, 900002, '', '', '', '', 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-batch-record-execution:domain-trace-verify');
END$$
DELIMITER ;
CALL ensure_edhr_domain_trace_schema();
DROP PROCEDURE IF EXISTS ensure_edhr_domain_trace_schema;

DROP TRIGGER IF EXISTS `trg_domain_trace_snapshot_no_update`;
DROP TRIGGER IF EXISTS `trg_domain_trace_snapshot_no_delete`;
DROP TRIGGER IF EXISTS `trg_domain_trace_item_no_update`;
DROP TRIGGER IF EXISTS `trg_domain_trace_item_no_delete`;

DELIMITER $$
CREATE TRIGGER `trg_domain_trace_snapshot_no_update`
BEFORE UPDATE ON `mes_pro_batch_record_domain_trace_snapshot`
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000'
    SET MESSAGE_TEXT = 'eDHR domain trace snapshots are append-only';
END$$
CREATE TRIGGER `trg_domain_trace_snapshot_no_delete`
BEFORE DELETE ON `mes_pro_batch_record_domain_trace_snapshot`
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000'
    SET MESSAGE_TEXT = 'eDHR domain trace snapshots are append-only';
END$$
CREATE TRIGGER `trg_domain_trace_item_no_update`
BEFORE UPDATE ON `mes_pro_batch_record_domain_trace_item`
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000'
    SET MESSAGE_TEXT = 'eDHR domain trace items are append-only';
END$$
CREATE TRIGGER `trg_domain_trace_item_no_delete`
BEFORE DELETE ON `mes_pro_batch_record_domain_trace_item`
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000'
    SET MESSAGE_TEXT = 'eDHR domain trace items are append-only';
END$$
DELIMITER ;

DROP PROCEDURE IF EXISTS ensure_edhr_domain_trace_tenant_package_menus;
DELIMITER $$
CREATE PROCEDURE ensure_edhr_domain_trace_tenant_package_menus()
BEGIN
  IF EXISTS (
      SELECT 1
      FROM `system_tenant_package` AS `package`
      WHERE `package`.`deleted` = b'0'
        AND NOT JSON_VALID(`package`.`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot merge eDHR domain trace permissions';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_domain_trace_permission_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_edhr_domain_trace_permission_menu_ids` (
      `permission` varchar(128) NOT NULL,
      `id` BIGINT NOT NULL,
      PRIMARY KEY (`permission`),
      UNIQUE KEY `uk_tmp_edhr_domain_trace_permission_menu_id` (`id`)
  );

  INSERT INTO `tmp_edhr_domain_trace_permission_menu_ids` (`permission`, `id`)
  SELECT `permission`, MIN(`id`) AS `id`
  FROM `system_menu`
  WHERE `deleted` = b'0'
    AND `permission` IN (
        'mes:pro-batch-record-execution:domain-trace-query',
        'mes:pro-batch-record-execution:domain-trace-verify'
    )
  GROUP BY `permission`;

  IF (SELECT COUNT(DISTINCT `permission`) FROM `tmp_edhr_domain_trace_permission_menu_ids`) <> 2 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR domain trace system_menu permissions; cannot merge tenant package menu_ids';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_domain_trace_permission_target_packages`;
  CREATE TEMPORARY TABLE `tmp_edhr_domain_trace_permission_target_packages` AS
  SELECT DISTINCT `package`.`id` AS `package_id`
  FROM `system_tenant_package` AS `package`
  INNER JOIN JSON_TABLE(
      `package`.`menu_ids`,
      '$[*]' COLUMNS (`menu_id` BIGINT PATH '$')
  ) AS `existing_menu`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`)
    AND `existing_menu`.`menu_id` IN (5700, 900002);

  DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_domain_trace_permission_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_edhr_domain_trace_permission_package_menu_ids` (
      `package_id` BIGINT NOT NULL,
      `menu_id` BIGINT NOT NULL,
      PRIMARY KEY (`package_id`, `menu_id`)
  );

  INSERT IGNORE INTO `tmp_edhr_domain_trace_permission_package_menu_ids` (`package_id`, `menu_id`)
  SELECT `target_package`.`package_id`, `existing_menu`.`menu_id`
  FROM `tmp_edhr_domain_trace_permission_target_packages` AS `target_package`
  INNER JOIN `system_tenant_package` AS `package`
      ON `package`.`id` = `target_package`.`package_id`
  INNER JOIN JSON_TABLE(
      `package`.`menu_ids`,
      '$[*]' COLUMNS (`menu_id` BIGINT PATH '$')
  ) AS `existing_menu`;

  INSERT IGNORE INTO `tmp_edhr_domain_trace_permission_package_menu_ids` (`package_id`, `menu_id`)
  SELECT `target_package`.`package_id`, `permission_menu`.`id`
  FROM `tmp_edhr_domain_trace_permission_target_packages` AS `target_package`
  CROSS JOIN `tmp_edhr_domain_trace_permission_menu_ids` AS `permission_menu`;

  DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_domain_trace_permission_package_menu_json`;
  CREATE TEMPORARY TABLE `tmp_edhr_domain_trace_permission_package_menu_json` AS
  SELECT
      `package_id`,
      JSON_ARRAYAGG(`menu_id`) AS `menu_ids`
  FROM (
      SELECT `package_id`, `menu_id`
      FROM `tmp_edhr_domain_trace_permission_package_menu_ids`
      ORDER BY `package_id`, `menu_id`
  ) AS `ordered_menu`
  GROUP BY `package_id`;

  UPDATE `system_tenant_package` AS `package`
  INNER JOIN `tmp_edhr_domain_trace_permission_package_menu_json` AS `merged`
      ON `merged`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = `merged`.`menu_ids`,
      `package`.`updater` = 'edhr-domain-trace-permission',
      `package`.`update_time` = NOW()
  WHERE `package`.`deleted` = b'0';

  DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_domain_trace_permission_target_roles`;
  CREATE TEMPORARY TABLE `tmp_edhr_domain_trace_permission_target_roles` AS
  SELECT DISTINCT
      `tenant`.`id` AS `tenant_id`,
      `role`.`id` AS `role_id`
  FROM `system_tenant` AS `tenant`
  INNER JOIN `tmp_edhr_domain_trace_permission_target_packages` AS `target_package`
      ON `target_package`.`package_id` = `tenant`.`package_id`
  INNER JOIN `system_role` AS `role`
      ON `role`.`tenant_id` = `tenant`.`id`
     AND `role`.`code` = 'tenant_admin'
     AND `role`.`deleted` = b'0'
  WHERE `tenant`.`deleted` = b'0';

  UPDATE `system_role_menu` AS `role_menu`
  INNER JOIN `tmp_edhr_domain_trace_permission_target_roles` AS `target_role`
      ON `target_role`.`tenant_id` = `role_menu`.`tenant_id`
     AND `target_role`.`role_id` = `role_menu`.`role_id`
  INNER JOIN `tmp_edhr_domain_trace_permission_menu_ids` AS `permission_menu`
      ON `permission_menu`.`id` = `role_menu`.`menu_id`
  SET `role_menu`.`deleted` = b'0',
      `role_menu`.`updater` = 'edhr-domain-trace-permission',
      `role_menu`.`update_time` = NOW()
  WHERE `role_menu`.`deleted` = b'1';

  INSERT INTO `system_role_menu`
    (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT
      `target_role`.`role_id`,
      `permission_menu`.`id`,
      'edhr-domain-trace-permission',
      NOW(),
      'edhr-domain-trace-permission',
      NOW(),
      b'0',
      `target_role`.`tenant_id`
  FROM `tmp_edhr_domain_trace_permission_target_roles` AS `target_role`
  CROSS JOIN `tmp_edhr_domain_trace_permission_menu_ids` AS `permission_menu`
  WHERE NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` AS `existing`
      WHERE `existing`.`tenant_id` = `target_role`.`tenant_id`
        AND `existing`.`role_id` = `target_role`.`role_id`
        AND `existing`.`menu_id` = `permission_menu`.`id`
        AND `existing`.`deleted` = b'0'
  );
END$$
DELIMITER ;
CALL ensure_edhr_domain_trace_tenant_package_menus();
DROP PROCEDURE IF EXISTS ensure_edhr_domain_trace_tenant_package_menus;
