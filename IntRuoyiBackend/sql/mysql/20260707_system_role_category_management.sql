-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- 权限角色分类管理
-- 说明：
-- 1. 新增租户级角色分类表 system_role_category。
-- 2. system_role 增加 category_id。
-- 3. 自动归类历史角色；无法按规则匹配时直接失败并输出清单，不写入未分类。

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `system_role_category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色分类ID',
  `name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色分类名称',
  `code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色分类标识',
  `sort` int NOT NULL COMMENT '显示顺序',
  `status` tinyint NOT NULL COMMENT '状态（0正常 1停用）',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_role_category_tenant_code` (`tenant_id`, `code`, `deleted`) USING BTREE,
  KEY `idx_role_category_tenant_sort` (`tenant_id`, `sort`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '角色分类表';

SET @category_column_missing := (
  SELECT COUNT(*) = 0
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'system_role'
    AND COLUMN_NAME = 'category_id'
);
SET @add_category_column_sql := IF(
  @category_column_missing,
  'ALTER TABLE `system_role` ADD COLUMN `category_id` bigint NULL COMMENT ''角色分类编号'' AFTER `sort`',
  'SELECT 1'
);
PREPARE add_category_column_stmt FROM @add_category_column_sql;
EXECUTE add_category_column_stmt;
DEALLOCATE PREPARE add_category_column_stmt;

SET @category_index_missing := (
  SELECT COUNT(*) = 0
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'system_role'
    AND INDEX_NAME = 'idx_role_category_id'
);
SET @add_category_index_sql := IF(
  @category_index_missing,
  'ALTER TABLE `system_role` ADD KEY `idx_role_category_id` (`category_id`) USING BTREE',
  'SELECT 1'
);
PREPARE add_category_index_stmt FROM @add_category_index_sql;
EXECUTE add_category_index_stmt;
DEALLOCATE PREPARE add_category_index_stmt;

DROP PROCEDURE IF EXISTS migrate_system_role_category_20260707;
DELIMITER //
CREATE PROCEDURE migrate_system_role_category_20260707()
BEGIN
  DECLARE unmatched_roles TEXT DEFAULT NULL;
  DECLARE unmatched_role_message VARCHAR(128) DEFAULT '角色分类迁移存在未匹配历史角色，请查看上一条结果集 unmatched_roles';

  INSERT INTO `system_role_category` (`name`, `code`, `sort`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT defaults.category_name, defaults.category_code, defaults.category_sort, 0, defaults.category_remark,
         '20260707-role-category', NOW(), '20260707-role-category', NOW(), b'0', tenants.tenant_id
  FROM (SELECT DISTINCT `tenant_id` FROM `system_role` WHERE `deleted` = b'0') tenants
  JOIN (
    SELECT '展厅' AS category_name, 'showroom' AS category_code, 10 AS category_sort, '展厅权限角色' AS category_remark
    UNION ALL SELECT '批记录', 'batch-record', 20, '批记录权限角色'
    UNION ALL SELECT '排产', 'scheduling', 30, '排产权限角色'
    UNION ALL SELECT '文控', 'dcc', 40, '文控权限角色'
    UNION ALL SELECT 'SRM', 'srm', 50, 'SRM 权限角色'
    UNION ALL SELECT '菜单', 'menu', 60, '菜单权限角色'
  ) defaults
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_role_category` existing
    WHERE existing.`tenant_id` = tenants.`tenant_id`
      AND existing.`code` = defaults.category_code
      AND existing.`deleted` = b'0'
  );

  UPDATE `system_role` role
  JOIN `system_role_category` category
    ON category.`tenant_id` = role.`tenant_id`
   AND category.`deleted` = b'0'
   AND category.`code` = CASE
      WHEN LOWER(CONCAT_WS(' ', role.`code`, role.`name`, IFNULL(role.`remark`, ''))) LIKE '%showroom%'
        OR CONCAT_WS(' ', role.`code`, role.`name`, IFNULL(role.`remark`, '')) LIKE '%展厅%'
        THEN 'showroom'
      WHEN LOWER(CONCAT_WS(' ', role.`code`, role.`name`, IFNULL(role.`remark`, ''))) LIKE '%edhr%'
        OR LOWER(CONCAT_WS(' ', role.`code`, role.`name`, IFNULL(role.`remark`, ''))) LIKE '%batch%'
        OR LOWER(CONCAT_WS(' ', role.`code`, role.`name`, IFNULL(role.`remark`, ''))) LIKE '%pressure_pump%'
        OR CONCAT_WS(' ', role.`code`, role.`name`, IFNULL(role.`remark`, '')) LIKE '%批记录%'
        THEN 'batch-record'
      WHEN LOWER(CONCAT_WS(' ', role.`code`, role.`name`, IFNULL(role.`remark`, ''))) LIKE '%schedule%'
        OR LOWER(CONCAT_WS(' ', role.`code`, role.`name`, IFNULL(role.`remark`, ''))) LIKE '%scheduler%'
        OR LOWER(role.`code`) LIKE 'post_release_mes_smoke_%'
        OR CONCAT_WS(' ', role.`code`, role.`name`, IFNULL(role.`remark`, '')) LIKE '%排产%'
        THEN 'scheduling'
      WHEN LOWER(CONCAT_WS(' ', role.`code`, role.`name`, IFNULL(role.`remark`, ''))) LIKE '%dcc%'
        OR LOWER(role.`code`) IN ('doc_control', 'wenkong', 'wenkong_download')
        OR CONCAT_WS(' ', role.`code`, role.`name`, IFNULL(role.`remark`, '')) LIKE '%文控%'
        OR CONCAT_WS(' ', role.`code`, role.`name`, IFNULL(role.`remark`, '')) LIKE '%体系工程师%'
        THEN 'dcc'
      WHEN LOWER(CONCAT_WS(' ', role.`code`, role.`name`, IFNULL(role.`remark`, ''))) LIKE '%srm%'
        THEN 'srm'
      WHEN LOWER(CONCAT_WS(' ', role.`code`, role.`name`, IFNULL(role.`remark`, ''))) LIKE '%menu%'
        OR LOWER(role.`code`) IN ('super_admin', 'common', 'tenant_admin', 'crm_admin', 'test-dp', 'approval_center_entry', 'approval_admin')
        OR LOWER(CONCAT_WS(' ', role.`code`, role.`name`, IFNULL(role.`remark`, ''))) LIKE '%system:role%'
        OR LOWER(CONCAT_WS(' ', role.`code`, role.`name`, IFNULL(role.`remark`, ''))) LIKE '%system:permission%'
        OR CONCAT_WS(' ', role.`code`, role.`name`, IFNULL(role.`remark`, '')) LIKE '%菜单%'
        THEN 'menu'
      ELSE NULL
    END
  SET role.`category_id` = category.`id`,
      role.`updater` = '20260707-role-category',
      role.`update_time` = NOW()
  WHERE role.`deleted` = b'0'
    AND role.`category_id` IS NULL;

  SELECT GROUP_CONCAT(CONCAT('tenant=', `tenant_id`, ',id=', `id`, ',name=', `name`, ',code=', `code`) ORDER BY `tenant_id`, `id` SEPARATOR '; ')
    INTO unmatched_roles
  FROM `system_role`
  WHERE `deleted` = b'0'
    AND `category_id` IS NULL;

  IF unmatched_roles IS NOT NULL THEN
    SELECT unmatched_roles AS unmatched_roles;
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = unmatched_role_message;
  END IF;

  INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT '角色分类查询', 'system:role-category:query', 3, 6, 900183, '', '', '', NULL, 0, b'1', b'1', b'1', '20260707-role-category', NOW(), '20260707-role-category', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'system:role-category:query' AND `deleted` = b'0');

  INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT '角色分类新增', 'system:role-category:create', 3, 7, 900183, '', '', '', NULL, 0, b'1', b'1', b'1', '20260707-role-category', NOW(), '20260707-role-category', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'system:role-category:create' AND `deleted` = b'0');

  INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT '角色分类修改', 'system:role-category:update', 3, 8, 900183, '', '', '', NULL, 0, b'1', b'1', b'1', '20260707-role-category', NOW(), '20260707-role-category', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'system:role-category:update' AND `deleted` = b'0');

  INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT '角色分类删除', 'system:role-category:delete', 3, 9, 900183, '', '', '', NULL, 0, b'1', b'1', b'1', '20260707-role-category', NOW(), '20260707-role-category', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'system:role-category:delete' AND `deleted` = b'0');
END//
DELIMITER ;

CALL migrate_system_role_category_20260707();
DROP PROCEDURE IF EXISTS migrate_system_role_category_20260707;
