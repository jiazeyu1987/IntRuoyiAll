-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260621_srm_phase45_simulated_execution; type=schema; riskLevel=medium
-- SRM T6 NAS locator snapshot tables and dynamic route menu.

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `srm_nas_locator_refresh_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'NAS索引刷新任务编号',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `status` varchar(16) NOT NULL COMMENT '刷新状态',
  `scope_share` varchar(255) NOT NULL COMMENT '受保护共享 UNC 路径',
  `root_path` varchar(255) NOT NULL COMMENT '刷新起始相对路径',
  `started_time` datetime NOT NULL COMMENT '开始时间',
  `finished_time` datetime DEFAULT NULL COMMENT '结束时间',
  `directory_count` bigint NOT NULL DEFAULT 0 COMMENT '目录数量',
  `file_count` bigint NOT NULL DEFAULT 0 COMMENT '文件数量',
  `error_message` varchar(1000) DEFAULT NULL COMMENT '失败原因',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_srm_nas_locator_refresh_task_tenant_status` (`tenant_id`, `status`, `deleted`),
  KEY `idx_srm_nas_locator_refresh_task_tenant_finish` (`tenant_id`, `finished_time`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SRM NAS定位刷新任务';

CREATE TABLE IF NOT EXISTS `srm_nas_locator_entry` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'NAS索引条目编号',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `refresh_task_id` bigint NOT NULL COMMENT '所属刷新任务编号',
  `entry_type` varchar(16) NOT NULL COMMENT '条目类型',
  `name` varchar(255) NOT NULL COMMENT '条目名称',
  `path` varchar(1000) NOT NULL COMMENT '完整相对路径',
  `path_hash` char(64) NOT NULL COMMENT '完整相对路径哈希',
  `parent_path` varchar(1000) NOT NULL COMMENT '父级相对路径',
  `size` bigint NOT NULL DEFAULT 0 COMMENT '文件大小，目录固定为0',
  `modified_at` bigint DEFAULT NULL COMMENT '最后修改时间戳（毫秒）',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_srm_nas_locator_entry_task_type_path_hash` (`refresh_task_id`, `entry_type`, `path_hash`),
  KEY `idx_srm_nas_locator_entry_tenant_task_type` (`tenant_id`, `refresh_task_id`, `entry_type`, `deleted`),
  KEY `idx_srm_nas_locator_entry_tenant_name` (`tenant_id`, `name`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SRM NAS定位索引条目';

DROP PROCEDURE IF EXISTS ensure_srm_t6_nas_locator;

DELIMITER $$
CREATE PROCEDURE ensure_srm_t6_nas_locator()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `id` = 991000
      AND `path` = '/srm'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = '缺少 SRM 基础菜单，禁止安装 T6 NAS定位 菜单';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_tenant_package`
    WHERE `deleted` = b'0'
      AND `menu_ids` IS NOT NULL
      AND `menu_ids` <> ''
      AND JSON_VALID(`menu_ids`) = 0
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON';
  END IF;

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991100, 'NAS定位', '', 2, 100, 991000, 'nas-locator', 'ep:folder-opened', 'srm/nas-locator/index', 'SrmNasLocator', 0, b'1', b'1', b'1', 'srm-t6', NOW(), 'srm-t6', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 991100);

  UPDATE `system_menu`
  SET `name` = 'NAS定位',
      `permission` = '',
      `path` = 'nas-locator',
      `icon` = 'ep:folder-opened',
      `component` = 'srm/nas-locator/index',
      `component_name` = 'SrmNasLocator',
      `updater` = 'srm-t6',
      `update_time` = NOW()
  WHERE `id` = 991100;

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991101, 'NAS定位查询', 'srm:nas-locator:query', 3, 1, 991100, '', '', '', '', 0, b'1', b'1', b'1', 'srm-t6', NOW(), 'srm-t6', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 991101);

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991102, 'NAS定位刷新', 'srm:nas-locator:refresh', 3, 2, 991100, '', '', '', '', 0, b'1', b'1', b'1', 'srm-t6', NOW(), 'srm-t6', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 991102);

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991103, 'NAS定位下载', 'srm:nas-locator:download', 3, 3, 991100, '', '', '', '', 0, b'1', b'1', b'1', 'srm-t6', NOW(), 'srm-t6', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 991103);

  UPDATE `system_menu`
  SET `name` = CASE `id`
      WHEN 991101 THEN 'NAS定位查询'
      WHEN 991102 THEN 'NAS定位刷新'
      WHEN 991103 THEN 'NAS定位下载'
      ELSE `name`
    END,
    `permission` = CASE `id`
      WHEN 991101 THEN 'srm:nas-locator:query'
      WHEN 991102 THEN 'srm:nas-locator:refresh'
      WHEN 991103 THEN 'srm:nas-locator:download'
      ELSE `permission`
    END,
    `updater` = 'srm-t6',
    `update_time` = NOW()
  WHERE `id` IN (991101, 991102, 991103);

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `id` = 991100
      AND `component` = 'srm/nas-locator/index'
      AND `component_name` = 'SrmNasLocator'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing SRM nas-locator route menu for get-permission-info';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_srm_t6_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_srm_t6_menu_ids` AS
  SELECT `id`
  FROM `system_menu`
  WHERE `deleted` = b'0'
    AND `id` IN (991100, 991101, 991102, 991103);

  DROP TEMPORARY TABLE IF EXISTS `tmp_srm_t6_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_srm_t6_package_menu_ids` (
    `package_id` bigint NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`package_id`, `menu_id`)
  );

  INSERT INTO `tmp_srm_t6_package_menu_ids` (`package_id`, `menu_id`)
  SELECT DISTINCT
    `package`.`id`,
    CAST(`existing_menu`.`menu_id` AS UNSIGNED)
  FROM `system_tenant_package` AS `package`
  JOIN JSON_TABLE(
    `package`.`menu_ids`,
    '$[*]' COLUMNS (`menu_id` bigint PATH '$')
  ) AS `existing_menu`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`)
  UNION
  SELECT
    `package`.`id`,
    `menu`.`id`
  FROM `system_tenant_package` AS `package`
  CROSS JOIN `tmp_srm_t6_menu_ids` AS `menu`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`)
    AND JSON_CONTAINS(`package`.`menu_ids`, CAST('991000' AS JSON), '$');

  UPDATE `system_tenant_package` AS `package`
  JOIN (
    SELECT DISTINCT
      `package_id`,
      JSON_ARRAYAGG(`menu_id`) OVER (
        PARTITION BY `package_id`
        ORDER BY `menu_id`
        ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
      ) AS `menu_ids`
    FROM `tmp_srm_t6_package_menu_ids`
  ) AS `merged`
    ON `merged`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = `merged`.`menu_ids`,
      `package`.`updater` = 'srm-t6',
      `package`.`update_time` = NOW();

  INSERT INTO `system_role_menu`
  (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT
    `role`.`id`,
    `menu`.`id`,
    'srm-t6',
    NOW(),
    'srm-t6',
    NOW(),
    b'0',
    `role`.`tenant_id`
  FROM `system_role` AS `role`
  CROSS JOIN `tmp_srm_t6_menu_ids` AS `menu`
  LEFT JOIN `system_tenant` AS `tenant`
    ON `tenant`.`id` = `role`.`tenant_id`
   AND `tenant`.`deleted` = b'0'
  LEFT JOIN `system_tenant_package` AS `package`
    ON `package`.`id` = `tenant`.`package_id`
   AND `package`.`deleted` = b'0'
   AND JSON_VALID(`package`.`menu_ids`)
  WHERE `role`.`deleted` = b'0'
    AND (
      `role`.`code` = 'super_admin'
      OR (
        `role`.`code` = 'tenant_admin'
        AND `package`.`id` IS NOT NULL
        AND JSON_CONTAINS(`package`.`menu_ids`, CAST(`menu`.`id` AS JSON), '$')
      )
    )
    AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` AS `existing`
      WHERE `existing`.`role_id` = `role`.`id`
        AND `existing`.`menu_id` = `menu`.`id`
        AND `existing`.`deleted` = b'0'
    );

  DROP TEMPORARY TABLE IF EXISTS `tmp_srm_t6_package_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_srm_t6_menu_ids`;
END$$
DELIMITER ;

CALL ensure_srm_t6_nas_locator();
DROP PROCEDURE IF EXISTS ensure_srm_t6_nas_locator;
