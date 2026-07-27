-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260612_mes_process_use_route_tabs; type=schema; riskLevel=medium
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_process_form_permission_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `route_process_id` bigint NOT NULL COMMENT '工艺路线工序编号',
  `batch_record_report_id` varchar(128) NOT NULL COMMENT '批记录表单编号',
  `batch_record_definition_id` bigint DEFAULT NULL COMMENT '批记录定义ID',
  `batch_record_version_id` bigint DEFAULT NULL COMMENT '批记录版本ID',
  `rule_type` varchar(32) NOT NULL COMMENT '规则类型：FILL/SIGNATURE',
  `scope_key` varchar(64) NOT NULL DEFAULT 'ALL' COMMENT '责任范围标识，ALL 或辅助行 rowKey',
  `signature_cell_key` varchar(128) NOT NULL DEFAULT '' COMMENT '签名单元格 Key，填写规则为空',
  `signature_role` varchar(32) DEFAULT NULL COMMENT '签名角色：APPROVAL/APPROVE/REVIEW',
  `candidate_source_type` varchar(32) NOT NULL COMMENT '候选来源类型：USER/USERS/ROLE/DEPT/DEPT_LEADER',
  `candidate_source_ids` varchar(1000) NOT NULL COMMENT '候选来源 ID 列表，逗号分隔',
  `completion_policy` varchar(32) NOT NULL COMMENT '完成策略：ANY_ONE/ALL',
  `due_minutes` int NOT NULL COMMENT '处理时限，单位分钟',
  `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用',
  `fillable_scope_json` json DEFAULT NULL COMMENT '精确可填写范围 JSON',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_edhr_process_form_rule` (`tenant_id`, `route_process_id`, `batch_record_report_id`, `batch_record_version_id`, `rule_type`, `scope_key`, `signature_cell_key`, `deleted`),
  KEY `idx_mes_pro_edhr_process_form_rule_route_report` (`tenant_id`, `route_process_id`, `batch_record_report_id`, `batch_record_version_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='eDHR 工序表单权限规则';

DROP PROCEDURE IF EXISTS ensure_mes_edhr_process_form_permission_rule_permissions;

DELIMITER //
CREATE PROCEDURE ensure_mes_edhr_process_form_permission_rule_permissions()
BEGIN
  IF EXISTS (
    SELECT 1
    FROM `system_tenant_package`
    WHERE `deleted` = b'0'
      AND NOT JSON_VALID(`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot merge eDHR process form permission rule menus';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 900221
      AND `permission` = 'mes:pro-batch-record-route:query'
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing process batch record route menu 900221; cannot append process form permission rule buttons';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` IN (900363, 900364)
      AND `deleted` = b'0'
      AND `permission` NOT IN (
        'mes:pro-edhr-process-form-permission-rule:query',
        'mes:pro-edhr-process-form-permission-rule:update'
      )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Menu id 900363 or 900364 is already used by another permission; cannot create process form permission rule buttons';
  END IF;

  INSERT INTO `system_menu` (
    `id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`,
    `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
  ) VALUES
  (
    900363, '工序表单权限查询', 'mes:pro-edhr-process-form-permission-rule:query', 3, 20, 900221,
    '', '', '', '', 0, b'1', b'1', b'1', 'edhr-process-form-permission', NOW(), 'edhr-process-form-permission', NOW(), b'0'
  ),
  (
    900364, '工序表单权限维护', 'mes:pro-edhr-process-form-permission-rule:update', 3, 21, 900221,
    '', '', '', '', 0, b'1', b'1', b'1', 'edhr-process-form-permission', NOW(), 'edhr-process-form-permission', NOW(), b'0'
  )
  ON DUPLICATE KEY UPDATE
    `name` = VALUES(`name`),
    `permission` = VALUES(`permission`),
    `type` = VALUES(`type`),
    `sort` = VALUES(`sort`),
    `parent_id` = VALUES(`parent_id`),
    `path` = VALUES(`path`),
    `icon` = VALUES(`icon`),
    `component` = VALUES(`component`),
    `component_name` = VALUES(`component_name`),
    `status` = VALUES(`status`),
    `visible` = VALUES(`visible`),
    `keep_alive` = VALUES(`keep_alive`),
    `always_show` = VALUES(`always_show`),
    `updater` = VALUES(`updater`),
    `update_time` = VALUES(`update_time`),
    `deleted` = VALUES(`deleted`);

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_process_form_permission_packages`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_process_form_permission_packages` AS
  SELECT DISTINCT `package`.`id` AS `package_id`
  FROM `system_tenant_package` AS `package`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`)
    AND JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('900221' AS JSON), '$');

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_process_form_permission_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_process_form_permission_package_menu_ids` (
    `package_id` bigint NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`package_id`, `menu_id`)
  );

  INSERT IGNORE INTO `tmp_mes_edhr_process_form_permission_package_menu_ids` (`package_id`, `menu_id`)
  SELECT
    `target_package`.`package_id`,
    CAST(`existing_menu`.`menu_id` AS UNSIGNED)
  FROM `tmp_mes_edhr_process_form_permission_packages` AS `target_package`
  JOIN `system_tenant_package` AS `package`
    ON `package`.`id` = `target_package`.`package_id`
   AND `package`.`deleted` = b'0'
   AND JSON_VALID(`package`.`menu_ids`)
  JOIN JSON_TABLE(
    `package`.`menu_ids`,
    '$[*]' COLUMNS (`menu_id` bigint PATH '$')
  ) AS `existing_menu`;

  INSERT IGNORE INTO `tmp_mes_edhr_process_form_permission_package_menu_ids` (`package_id`, `menu_id`)
  SELECT
    `target_package`.`package_id`,
    `menu`.`id`
  FROM `tmp_mes_edhr_process_form_permission_packages` AS `target_package`
  JOIN `system_menu` AS `menu`
    ON `menu`.`id` IN (900363, 900364)
   AND `menu`.`deleted` = b'0';

  UPDATE `system_tenant_package` AS `package`
  JOIN (
    SELECT DISTINCT
      `package_id`,
      JSON_ARRAYAGG(`menu_id`) OVER (
        PARTITION BY `package_id`
        ORDER BY `menu_id`
        ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
      ) AS `menu_ids`
    FROM `tmp_mes_edhr_process_form_permission_package_menu_ids`
  ) AS `merged`
    ON `merged`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = `merged`.`menu_ids`,
      `package`.`updater` = 'edhr-process-form-permission',
      `package`.`update_time` = NOW();

  INSERT INTO `system_role_menu` (
    `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    `role`.`id`,
    `menu`.`id`,
    'edhr-process-form-permission',
    NOW(),
    'edhr-process-form-permission',
    NOW(),
    b'0',
    `role`.`tenant_id`
  FROM `system_role` AS `role`
  JOIN `system_tenant` AS `tenant`
    ON `tenant`.`id` = `role`.`tenant_id`
   AND `tenant`.`deleted` = b'0'
  JOIN `tmp_mes_edhr_process_form_permission_packages` AS `target_package`
    ON `target_package`.`package_id` = `tenant`.`package_id`
  JOIN `system_tenant_package` AS `package`
    ON `package`.`id` = `tenant`.`package_id`
   AND `package`.`deleted` = b'0'
   AND JSON_VALID(`package`.`menu_ids`)
  JOIN `system_role_menu` AS `route_role_menu`
    ON `route_role_menu`.`role_id` = `role`.`id`
   AND `route_role_menu`.`tenant_id` = `role`.`tenant_id`
   AND `route_role_menu`.`menu_id` = 900221
   AND `route_role_menu`.`deleted` = b'0'
  JOIN `system_menu` AS `menu`
    ON `menu`.`id` IN (900363, 900364)
   AND `menu`.`deleted` = b'0'
  WHERE `role`.`deleted` = b'0'
    AND JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST(CONCAT('', `menu`.`id`) AS JSON), '$')
    AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` AS `role_menu`
      WHERE `role_menu`.`role_id` = `role`.`id`
        AND `role_menu`.`menu_id` = `menu`.`id`
        AND `role_menu`.`tenant_id` = `role`.`tenant_id`
        AND `role_menu`.`deleted` = b'0'
    );

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_process_form_permission_package_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_process_form_permission_packages`;
END//
DELIMITER ;

CALL ensure_mes_edhr_process_form_permission_rule_permissions();

DROP PROCEDURE IF EXISTS ensure_mes_edhr_process_form_permission_rule_permissions;
