-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260618_mes_edhr_release_precheck_engine; type=schema; riskLevel=medium
-- eDHR commercial release transaction lifecycle: submit, approve, reject, withdraw, and event trace.
-- This migration is idempotent and fail-fast for invalid tenant package menu JSON.

DROP PROCEDURE IF EXISTS ensure_mes_edhr_release_transaction_columns;
DELIMITER $$
CREATE PROCEDURE ensure_mes_edhr_release_transaction_columns()
BEGIN
  IF NOT EXISTS (
      SELECT 1 FROM `information_schema`.`COLUMNS`
      WHERE `TABLE_SCHEMA` = DATABASE()
        AND `TABLE_NAME` = 'mes_pro_edhr_release_transaction'
        AND `COLUMN_NAME` = 'submit_idempotency_key'
  ) THEN
    ALTER TABLE `mes_pro_edhr_release_transaction`
      ADD COLUMN `submit_idempotency_key` varchar(128) DEFAULT NULL COMMENT '提交幂等键' AFTER `precheck_snapshot_json`;
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM `information_schema`.`COLUMNS`
      WHERE `TABLE_SCHEMA` = DATABASE()
        AND `TABLE_NAME` = 'mes_pro_edhr_release_transaction'
        AND `COLUMN_NAME` = 'submitted_by'
  ) THEN
    ALTER TABLE `mes_pro_edhr_release_transaction`
      ADD COLUMN `submitted_by` bigint DEFAULT NULL COMMENT '提交人' AFTER `submit_idempotency_key`;
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM `information_schema`.`COLUMNS`
      WHERE `TABLE_SCHEMA` = DATABASE()
        AND `TABLE_NAME` = 'mes_pro_edhr_release_transaction'
        AND `COLUMN_NAME` = 'submitted_at'
  ) THEN
    ALTER TABLE `mes_pro_edhr_release_transaction`
      ADD COLUMN `submitted_at` datetime DEFAULT NULL COMMENT '提交时间' AFTER `submitted_by`;
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM `information_schema`.`COLUMNS`
      WHERE `TABLE_SCHEMA` = DATABASE()
        AND `TABLE_NAME` = 'mes_pro_edhr_release_transaction'
        AND `COLUMN_NAME` = 'approval_idempotency_key'
  ) THEN
    ALTER TABLE `mes_pro_edhr_release_transaction`
      ADD COLUMN `approval_idempotency_key` varchar(128) DEFAULT NULL COMMENT '批准幂等键' AFTER `submitted_at`;
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM `information_schema`.`COLUMNS`
      WHERE `TABLE_SCHEMA` = DATABASE()
        AND `TABLE_NAME` = 'mes_pro_edhr_release_transaction'
        AND `COLUMN_NAME` = 'approved_by'
  ) THEN
    ALTER TABLE `mes_pro_edhr_release_transaction`
      ADD COLUMN `approved_by` bigint DEFAULT NULL COMMENT '批准人' AFTER `approval_idempotency_key`;
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM `information_schema`.`COLUMNS`
      WHERE `TABLE_SCHEMA` = DATABASE()
        AND `TABLE_NAME` = 'mes_pro_edhr_release_transaction'
        AND `COLUMN_NAME` = 'approved_at'
  ) THEN
    ALTER TABLE `mes_pro_edhr_release_transaction`
      ADD COLUMN `approved_at` datetime DEFAULT NULL COMMENT '批准时间' AFTER `approved_by`;
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM `information_schema`.`COLUMNS`
      WHERE `TABLE_SCHEMA` = DATABASE()
        AND `TABLE_NAME` = 'mes_pro_edhr_release_transaction'
        AND `COLUMN_NAME` = 'approval_signoff_evidence_hash'
  ) THEN
    ALTER TABLE `mes_pro_edhr_release_transaction`
      ADD COLUMN `approval_signoff_evidence_hash` char(64) DEFAULT NULL COMMENT '批准签核证据摘要' AFTER `approved_at`;
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM `information_schema`.`COLUMNS`
      WHERE `TABLE_SCHEMA` = DATABASE()
        AND `TABLE_NAME` = 'mes_pro_edhr_release_transaction'
        AND `COLUMN_NAME` = 'approval_opinion'
  ) THEN
    ALTER TABLE `mes_pro_edhr_release_transaction`
      ADD COLUMN `approval_opinion` varchar(500) DEFAULT NULL COMMENT '审批意见' AFTER `approval_signoff_evidence_hash`;
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM `information_schema`.`COLUMNS`
      WHERE `TABLE_SCHEMA` = DATABASE()
        AND `TABLE_NAME` = 'mes_pro_edhr_release_transaction'
        AND `COLUMN_NAME` = 'rejected_by'
  ) THEN
    ALTER TABLE `mes_pro_edhr_release_transaction`
      ADD COLUMN `rejected_by` bigint DEFAULT NULL COMMENT '驳回人' AFTER `approval_opinion`;
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM `information_schema`.`COLUMNS`
      WHERE `TABLE_SCHEMA` = DATABASE()
        AND `TABLE_NAME` = 'mes_pro_edhr_release_transaction'
        AND `COLUMN_NAME` = 'rejected_at'
  ) THEN
    ALTER TABLE `mes_pro_edhr_release_transaction`
      ADD COLUMN `rejected_at` datetime DEFAULT NULL COMMENT '驳回时间' AFTER `rejected_by`;
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM `information_schema`.`COLUMNS`
      WHERE `TABLE_SCHEMA` = DATABASE()
        AND `TABLE_NAME` = 'mes_pro_edhr_release_transaction'
        AND `COLUMN_NAME` = 'reject_reason'
  ) THEN
    ALTER TABLE `mes_pro_edhr_release_transaction`
      ADD COLUMN `reject_reason` varchar(500) DEFAULT NULL COMMENT '驳回原因' AFTER `rejected_at`;
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM `information_schema`.`COLUMNS`
      WHERE `TABLE_SCHEMA` = DATABASE()
        AND `TABLE_NAME` = 'mes_pro_edhr_release_transaction'
        AND `COLUMN_NAME` = 'withdrawn_by'
  ) THEN
    ALTER TABLE `mes_pro_edhr_release_transaction`
      ADD COLUMN `withdrawn_by` bigint DEFAULT NULL COMMENT '撤回人' AFTER `reject_reason`;
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM `information_schema`.`COLUMNS`
      WHERE `TABLE_SCHEMA` = DATABASE()
        AND `TABLE_NAME` = 'mes_pro_edhr_release_transaction'
        AND `COLUMN_NAME` = 'withdrawn_at'
  ) THEN
    ALTER TABLE `mes_pro_edhr_release_transaction`
      ADD COLUMN `withdrawn_at` datetime DEFAULT NULL COMMENT '撤回时间' AFTER `withdrawn_by`;
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM `information_schema`.`COLUMNS`
      WHERE `TABLE_SCHEMA` = DATABASE()
        AND `TABLE_NAME` = 'mes_pro_edhr_release_transaction'
        AND `COLUMN_NAME` = 'withdraw_reason'
  ) THEN
    ALTER TABLE `mes_pro_edhr_release_transaction`
      ADD COLUMN `withdraw_reason` varchar(500) DEFAULT NULL COMMENT '撤回原因' AFTER `withdrawn_at`;
  END IF;
END$$
DELIMITER ;

CALL ensure_mes_edhr_release_transaction_columns();

DROP PROCEDURE IF EXISTS ensure_mes_edhr_release_transaction_columns;

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_release_transaction_event` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `release_transaction_id` bigint NOT NULL COMMENT '放行事务ID',
  `event_type` varchar(32) NOT NULL COMMENT '事件类型',
  `from_status` varchar(32) NOT NULL COMMENT '原状态',
  `to_status` varchar(32) NOT NULL COMMENT '目标状态',
  `actor_user_id` bigint DEFAULT NULL COMMENT '操作人',
  `reason` varchar(500) DEFAULT NULL COMMENT '原因',
  `opinion` varchar(500) DEFAULT NULL COMMENT '审批意见',
  `idempotency_key` varchar(128) NOT NULL COMMENT '幂等键',
  `signoff_evidence_hash` char(64) DEFAULT NULL COMMENT '签核证据摘要',
  `event_snapshot_json` longtext DEFAULT NULL COMMENT '事件快照JSON',
  `evidence_hash` char(64) NOT NULL COMMENT '事件证据摘要',
  `occurred_at` datetime NOT NULL COMMENT '事件发生时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_edhr_release_event_idempotency` (`tenant_id`, `release_transaction_id`, `event_type`, `idempotency_key`, `deleted`),
  KEY `idx_mes_pro_edhr_release_event_transaction` (`tenant_id`, `release_transaction_id`, `occurred_at`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES eDHR 放行事务事件';

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900353, 'eDHR放行驳回', 'mes:pro-edhr-release:reject', 3, 6, 900260,
       '', '', '', '', 0, b'1', b'1', b'1', 'edhr-release', NOW(), 'edhr-release', NOW(), b'0'
WHERE EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900260 AND `deleted` = b'0')
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900353 OR `permission` = 'mes:pro-edhr-release:reject');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900354, 'eDHR放行撤回', 'mes:pro-edhr-release:withdraw', 3, 7, 900260,
       '', '', '', '', 0, b'1', b'1', b'1', 'edhr-release', NOW(), 'edhr-release', NOW(), b'0'
WHERE EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900260 AND `deleted` = b'0')
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900354 OR `permission` = 'mes:pro-edhr-release:withdraw');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900355, 'eDHR放行事务事件查询', 'mes:pro-edhr-release:event-query', 3, 8, 900260,
       '', '', '', '', 0, b'1', b'1', b'1', 'edhr-release', NOW(), 'edhr-release', NOW(), b'0'
WHERE EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900260 AND `deleted` = b'0')
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900355 OR `permission` = 'mes:pro-edhr-release:event-query');

DROP PROCEDURE IF EXISTS ensure_mes_edhr_release_transaction_menus;
DELIMITER $$
CREATE PROCEDURE ensure_mes_edhr_release_transaction_menus()
BEGIN
  IF EXISTS (
      SELECT 1 FROM `system_tenant_package` AS `package`
      WHERE `package`.`deleted` = b'0'
        AND NOT JSON_VALID(`package`.`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot merge eDHR release transaction menus';
  END IF;

  IF (SELECT COUNT(*) FROM `system_menu` WHERE `deleted` = b'0' AND `id` IN (900263, 900264, 900353, 900354, 900355)) <> 5 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR release transaction system_menu rows; cannot merge tenant package menu_ids';
  END IF;

  IF (SELECT COUNT(*) FROM `system_menu`
      WHERE `deleted` = b'0'
        AND `permission` IN (
          'mes:pro-edhr-release:submit',
          'mes:pro-edhr-release:approve',
          'mes:pro-edhr-release:reject',
          'mes:pro-edhr-release:withdraw',
          'mes:pro-edhr-release:event-query'
        )) <> 5 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR release transaction permission rows; cannot merge tenant package menu_ids';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_release_transaction_target_packages`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_release_transaction_target_packages` AS
  SELECT `package`.`id` AS `package_id`
  FROM `system_tenant_package` AS `package`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`)
    AND JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('900260' AS JSON), '$');

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_release_transaction_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_release_transaction_menu_ids` AS
  SELECT `id`
  FROM `system_menu`
  WHERE `id` IN (900263, 900264, 900353, 900354, 900355)
    AND `deleted` = b'0';

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_release_transaction_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_release_transaction_package_menu_ids` (
    `package_id` bigint NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`package_id`, `menu_id`)
  );

  INSERT INTO `tmp_mes_edhr_release_transaction_package_menu_ids` (`package_id`, `menu_id`)
  SELECT DISTINCT
    `target_package`.`package_id`,
    CAST(`existing_menu`.`menu_id` AS UNSIGNED)
  FROM `tmp_mes_edhr_release_transaction_target_packages` AS `target_package`
  JOIN `system_tenant_package` AS `package`
    ON `package`.`id` = `target_package`.`package_id`
   AND `package`.`deleted` = b'0'
   AND JSON_VALID(`package`.`menu_ids`)
  JOIN JSON_TABLE(
    `package`.`menu_ids`,
    '$[*]' COLUMNS (`menu_id` bigint PATH '$')
  ) AS `existing_menu`;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_release_transaction_missing_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_release_transaction_missing_package_menu_ids` AS
  SELECT
    `target_package`.`package_id`,
    `menu`.`id`
  FROM `tmp_mes_edhr_release_transaction_target_packages` AS `target_package`
  CROSS JOIN `tmp_mes_edhr_release_transaction_menu_ids` AS `menu`
  WHERE NOT EXISTS (
    SELECT 1
    FROM `tmp_mes_edhr_release_transaction_package_menu_ids` AS `existing`
    WHERE `existing`.`package_id` = `target_package`.`package_id`
      AND `existing`.`menu_id` = `menu`.`id`
  );

  INSERT INTO `tmp_mes_edhr_release_transaction_package_menu_ids` (`package_id`, `menu_id`)
  SELECT `package_id`, `id`
  FROM `tmp_mes_edhr_release_transaction_missing_package_menu_ids`;

  UPDATE `system_tenant_package` AS `package`
  JOIN (
    SELECT
      `ordered`.`package_id`,
      JSON_ARRAYAGG(`ordered`.`menu_id`) AS `menu_ids`
    FROM (
      SELECT `package_id`, `menu_id`
      FROM `tmp_mes_edhr_release_transaction_package_menu_ids`
      ORDER BY `package_id`, `menu_id`
    ) AS `ordered`
    GROUP BY `ordered`.`package_id`
  ) AS `merged`
    ON `merged`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = `merged`.`menu_ids`,
      `package`.`updater` = 'edhr-release',
      `package`.`update_time` = NOW();

  INSERT INTO `system_role_menu`
  (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT
    `role`.`id`,
    `menu`.`id`,
    'edhr-release',
    NOW(),
    'edhr-release',
    NOW(),
    b'0',
    `role`.`tenant_id`
  FROM `system_role` AS `role`
  JOIN `system_tenant` AS `tenant`
    ON `tenant`.`id` = `role`.`tenant_id`
   AND `tenant`.`deleted` = b'0'
  JOIN `tmp_mes_edhr_release_transaction_target_packages` AS `target_package`
    ON `target_package`.`package_id` = `tenant`.`package_id`
  JOIN `system_menu` AS `menu`
    ON `menu`.`id` IN (900263, 900264, 900353, 900354, 900355)
   AND `menu`.`deleted` = b'0'
  WHERE `role`.`deleted` = b'0'
    AND `role`.`code` = 'tenant_admin'
    AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` AS `existing`
      WHERE `existing`.`role_id` = `role`.`id`
        AND `existing`.`menu_id` = `menu`.`id`
        AND `existing`.`tenant_id` = `role`.`tenant_id`
        AND `existing`.`deleted` = b'0'
    );

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_release_transaction_missing_package_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_release_transaction_package_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_release_transaction_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_release_transaction_target_packages`;
END$$
DELIMITER ;

CALL ensure_mes_edhr_release_transaction_menus();

DROP PROCEDURE IF EXISTS ensure_mes_edhr_release_transaction_menus;
