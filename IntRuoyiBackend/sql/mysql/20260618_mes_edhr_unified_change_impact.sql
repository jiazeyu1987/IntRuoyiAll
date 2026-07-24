-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260612_mes_edhr_record_change_menu; type=schema; riskLevel=medium
-- eDHR commercial unified change request and impact analysis contract for form, DHR, and recordbook templates.
-- This migration is idempotent and fail-fast for invalid tenant package menu JSON.

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_unified_change_request` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `change_code` varchar(64) NOT NULL COMMENT '变更编号',
  `controlled_object_type` varchar(64) NOT NULL COMMENT '受控对象类型',
  `controlled_object_id` varchar(128) NOT NULL COMMENT '受控对象ID',
  `controlled_object_code` varchar(128) NOT NULL COMMENT '受控对象编号',
  `current_version` varchar(64) NOT NULL COMMENT '当前版本',
  `target_version` varchar(64) NOT NULL COMMENT '目标版本',
  `change_type` varchar(64) NOT NULL COMMENT '变更类型',
  `change_status` varchar(32) NOT NULL COMMENT '变更状态',
  `risk_level` varchar(32) NOT NULL COMMENT '风险等级',
  `reason_category` varchar(64) DEFAULT NULL COMMENT '原因分类',
  `reason` varchar(500) NOT NULL COMMENT '变更原因',
  `diff_snapshot_json` longtext NOT NULL COMMENT '差异快照JSON',
  `impact_summary_json` longtext NOT NULL COMMENT '影响范围摘要JSON',
  `impact_recalculated_at` datetime NOT NULL COMMENT '影响范围复算时间',
  `impact_recalculation_hash` char(64) NOT NULL COMMENT '影响范围复算摘要',
  `requested_by` bigint DEFAULT NULL COMMENT '创建人',
  `requested_at` datetime NOT NULL COMMENT '创建时间',
  `submitted_by` bigint DEFAULT NULL COMMENT '提交人',
  `submitted_at` datetime DEFAULT NULL COMMENT '提交时间',
  `approved_by` bigint DEFAULT NULL COMMENT '审批人',
  `approved_at` datetime DEFAULT NULL COMMENT '审批时间',
  `approval_opinion` varchar(500) DEFAULT NULL COMMENT '审批意见',
  `approval_signoff_evidence_hash` char(64) DEFAULT NULL COMMENT '审批签核证据摘要',
  `effect_requested_by` bigint DEFAULT NULL COMMENT '生效申请人',
  `effect_requested_at` datetime DEFAULT NULL COMMENT '生效申请时间',
  `effect_signoff_evidence_hash` char(64) DEFAULT NULL COMMENT '生效签核证据摘要',
  `idempotency_key` varchar(128) NOT NULL COMMENT '创建幂等键',
  `evidence_hash` char(64) NOT NULL COMMENT '变更证据摘要',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_edhr_unified_change_idempotency` (`tenant_id`, `controlled_object_type`, `controlled_object_id`, `change_type`, `idempotency_key`, `deleted`),
  KEY `idx_mes_pro_edhr_unified_change_object` (`tenant_id`, `controlled_object_type`, `controlled_object_id`, `change_status`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES eDHR 统一变更申请';

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_unified_change_impact` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `change_request_id` bigint NOT NULL COMMENT '统一变更申请ID',
  `impact_type` varchar(64) NOT NULL COMMENT '影响类型',
  `impact_object_type` varchar(64) NOT NULL COMMENT '影响对象类型',
  `impact_object_id` varchar(128) NOT NULL COMMENT '影响对象ID',
  `impact_object_code` varchar(128) DEFAULT NULL COMMENT '影响对象编号',
  `risk_level` varchar(32) NOT NULL COMMENT '风险等级',
  `responsibility_module` varchar(64) NOT NULL COMMENT '责任模块',
  `requires_training` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否需要培训',
  `requires_revalidation` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否需要再验证',
  `requires_release_recheck` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否需要放行复检',
  `impact_detail` varchar(1000) NOT NULL COMMENT '影响明细',
  `next_action` varchar(500) NOT NULL COMMENT '后续动作',
  `evidence_hash` char(64) NOT NULL COMMENT '影响证据摘要',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_mes_pro_edhr_unified_change_impact_request` (`tenant_id`, `change_request_id`, `risk_level`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES eDHR 统一变更影响范围';

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_unified_change_event` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `change_request_id` bigint NOT NULL COMMENT '统一变更申请ID',
  `event_type` varchar(64) NOT NULL COMMENT '事件类型',
  `from_status` varchar(32) DEFAULT NULL COMMENT '原状态',
  `to_status` varchar(32) NOT NULL COMMENT '目标状态',
  `actor_user_id` bigint DEFAULT NULL COMMENT '操作人',
  `reason` varchar(500) DEFAULT NULL COMMENT '原因',
  `signoff_evidence_hash` char(64) DEFAULT NULL COMMENT '签核证据摘要',
  `event_snapshot_json` longtext NOT NULL COMMENT '事件快照JSON',
  `evidence_hash` char(64) NOT NULL COMMENT '事件证据摘要',
  `occurred_at` datetime NOT NULL COMMENT '事件发生时间',
  `idempotency_key` varchar(128) NOT NULL COMMENT '事件幂等键',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_edhr_unified_change_event_idempotency` (`tenant_id`, `change_request_id`, `event_type`, `idempotency_key`, `deleted`),
  KEY `idx_mes_pro_edhr_unified_change_event_request` (`tenant_id`, `change_request_id`, `occurred_at`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES eDHR 统一变更事件';

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900293, 'eDHR统一变更', 'mes:pro-edhr-change:unified-query', 2, 293, 900220,
       '/mes/pro/feedback/edhr-unified-change', 'ep:operation', 'mes/pro/edhr-unified-change/UnifiedChangePage', 'MesProEdhrUnifiedChange', 0, b'1', b'0', b'1', 'edhr-unified-change', NOW(), 'edhr-unified-change', NOW(), b'0'
WHERE EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900220 AND `deleted` = b'0')
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900293 OR `permission` = 'mes:pro-edhr-change:unified-query');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900294, 'eDHR统一变更创建', 'mes:pro-edhr-change:unified-create', 3, 1, 900293,
       '', '', '', '', 0, b'1', b'0', b'1', 'edhr-unified-change', NOW(), 'edhr-unified-change', NOW(), b'0'
WHERE EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900293 AND `deleted` = b'0')
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900294 OR `permission` = 'mes:pro-edhr-change:unified-create');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900295, 'eDHR统一变更提交', 'mes:pro-edhr-change:unified-submit', 3, 2, 900293,
       '', '', '', '', 0, b'1', b'0', b'1', 'edhr-unified-change', NOW(), 'edhr-unified-change', NOW(), b'0'
WHERE EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900293 AND `deleted` = b'0')
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900295 OR `permission` = 'mes:pro-edhr-change:unified-submit');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900296, 'eDHR统一变更审批', 'mes:pro-edhr-change:unified-approve', 3, 3, 900293,
       '', '', '', '', 0, b'1', b'0', b'1', 'edhr-unified-change', NOW(), 'edhr-unified-change', NOW(), b'0'
WHERE EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900293 AND `deleted` = b'0')
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900296 OR `permission` = 'mes:pro-edhr-change:unified-approve');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900297, 'eDHR统一变更生效申请', 'mes:pro-edhr-change:unified-effect', 3, 4, 900293,
       '', '', '', '', 0, b'1', b'0', b'1', 'edhr-unified-change', NOW(), 'edhr-unified-change', NOW(), b'0'
WHERE EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900293 AND `deleted` = b'0')
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900297 OR `permission` = 'mes:pro-edhr-change:unified-effect');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900298, 'eDHR统一变更影响范围', 'mes:pro-edhr-change:impact-query', 3, 5, 900293,
       '', '', '', '', 0, b'1', b'0', b'1', 'edhr-unified-change', NOW(), 'edhr-unified-change', NOW(), b'0'
WHERE EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900293 AND `deleted` = b'0')
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900298 OR `permission` = 'mes:pro-edhr-change:impact-query');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900299, 'eDHR统一变更事件', 'mes:pro-edhr-change:event-query', 3, 6, 900293,
       '', '', '', '', 0, b'1', b'0', b'1', 'edhr-unified-change', NOW(), 'edhr-unified-change', NOW(), b'0'
WHERE EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900293 AND `deleted` = b'0')
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900299 OR `permission` = 'mes:pro-edhr-change:event-query');

DROP PROCEDURE IF EXISTS ensure_mes_edhr_unified_change_menus;
DELIMITER $$
CREATE PROCEDURE ensure_mes_edhr_unified_change_menus()
BEGIN
  IF EXISTS (
      SELECT 1 FROM `system_tenant_package` AS `package`
      WHERE `package`.`deleted` = b'0'
        AND NOT JSON_VALID(`package`.`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot merge eDHR unified change menus';
  END IF;

  IF (SELECT COUNT(*) FROM `system_menu` WHERE `deleted` = b'0' AND `id` IN (900293, 900294, 900295, 900296, 900297, 900298, 900299)) <> 7 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR unified change system_menu rows; cannot merge tenant package menu_ids';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_unified_change_target_packages`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_unified_change_target_packages` AS
  SELECT `package`.`id` AS `package_id`
  FROM `system_tenant_package` AS `package`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`)
    AND JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('900220' AS JSON), '$');

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_unified_change_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_unified_change_menu_ids` AS
  SELECT `id`
  FROM `system_menu`
  WHERE `id` IN (900293, 900294, 900295, 900296, 900297, 900298, 900299)
    AND `deleted` = b'0';

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_unified_change_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_unified_change_package_menu_ids` (
    `package_id` bigint NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`package_id`, `menu_id`)
  );

  INSERT INTO `tmp_mes_edhr_unified_change_package_menu_ids` (`package_id`, `menu_id`)
  SELECT DISTINCT
    `target_package`.`package_id`,
    CAST(`existing_menu`.`menu_id` AS UNSIGNED)
  FROM `tmp_mes_edhr_unified_change_target_packages` AS `target_package`
  JOIN `system_tenant_package` AS `package`
    ON `package`.`id` = `target_package`.`package_id`
   AND `package`.`deleted` = b'0'
   AND JSON_VALID(`package`.`menu_ids`)
  JOIN JSON_TABLE(
    `package`.`menu_ids`,
    '$[*]' COLUMNS (`menu_id` bigint PATH '$')
  ) AS `existing_menu`;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_unified_change_missing_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_unified_change_missing_package_menu_ids` AS
  SELECT
    `target_package`.`package_id`,
    `menu`.`id`
  FROM `tmp_mes_edhr_unified_change_target_packages` AS `target_package`
  CROSS JOIN `tmp_mes_edhr_unified_change_menu_ids` AS `menu`
  WHERE NOT EXISTS (
    SELECT 1
    FROM `tmp_mes_edhr_unified_change_package_menu_ids` AS `existing`
    WHERE `existing`.`package_id` = `target_package`.`package_id`
      AND `existing`.`menu_id` = `menu`.`id`
  );

  INSERT INTO `tmp_mes_edhr_unified_change_package_menu_ids` (`package_id`, `menu_id`)
  SELECT `package_id`, `id`
  FROM `tmp_mes_edhr_unified_change_missing_package_menu_ids`;

  UPDATE `system_tenant_package` AS `package`
  JOIN (
    SELECT
      `ordered`.`package_id`,
      JSON_ARRAYAGG(`ordered`.`menu_id`) AS `menu_ids`
    FROM (
      SELECT `package_id`, `menu_id`
      FROM `tmp_mes_edhr_unified_change_package_menu_ids`
      ORDER BY `package_id`, `menu_id`
    ) AS `ordered`
    GROUP BY `ordered`.`package_id`
  ) AS `merged`
    ON `merged`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = `merged`.`menu_ids`,
      `package`.`updater` = 'edhr-unified-change',
      `package`.`update_time` = NOW();

  INSERT INTO `system_role_menu`
  (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT
    `role`.`id`,
    `menu`.`id`,
    'edhr-unified-change',
    NOW(),
    'edhr-unified-change',
    NOW(),
    b'0',
    `role`.`tenant_id`
  FROM `system_role` AS `role`
  JOIN `system_tenant` AS `tenant`
    ON `tenant`.`id` = `role`.`tenant_id`
   AND `tenant`.`deleted` = b'0'
  JOIN `tmp_mes_edhr_unified_change_target_packages` AS `target_package`
    ON `target_package`.`package_id` = `tenant`.`package_id`
  JOIN `system_menu` AS `menu`
    ON `menu`.`id` IN (900293, 900294, 900295, 900296, 900297, 900298, 900299)
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

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_unified_change_missing_package_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_unified_change_package_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_unified_change_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_unified_change_target_packages`;
END$$
DELIMITER ;

CALL ensure_mes_edhr_unified_change_menus();

DROP PROCEDURE IF EXISTS ensure_mes_edhr_unified_change_menus;
