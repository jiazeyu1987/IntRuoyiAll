-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260920_dcc_project_product_create_approval; type=permission; riskLevel=low
-- 实习用户时间维护模块：角色、入口和按钮权限集中在本迁移中，便于后续整块切割。

SET NAMES utf8mb4;
START TRANSACTION;

CREATE TABLE IF NOT EXISTS `intern_user_time_maintenance_audit` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '审计编号',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户编号',
  `target_type` varchar(64) NOT NULL COMMENT '维护对象类型',
  `target_id` bigint NOT NULL COMMENT '维护对象编号',
  `target_name` varchar(256) DEFAULT NULL COMMENT '维护对象名称',
  `field_name` varchar(64) NOT NULL COMMENT '维护字段',
  `old_time` datetime DEFAULT NULL COMMENT '原时间',
  `new_time` datetime NOT NULL COMMENT '新时间',
  `operator_user_id` bigint DEFAULT NULL COMMENT '操作人',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_intern_user_time_audit_target` (`target_type`, `target_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='实习用户时间维护审计表';

INSERT INTO `system_role`
  (`id`, `name`, `code`, `sort`, `category_id`, `data_scope`, `data_scope_dept_ids`, `status`,
   `type`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
  (991220, '实习用户', 'intern_user', 991220, NULL, 1, '', 0,
   2, '实习用户时间维护模块角色，集中承载上传时间、升版时间、作废时间等业务时间维护能力。',
   'intern-user-module', NOW(), 'intern-user-module', NOW(), b'0', 1)
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `code` = VALUES(`code`),
  `sort` = VALUES(`sort`),
  `data_scope` = VALUES(`data_scope`),
  `data_scope_dept_ids` = VALUES(`data_scope_dept_ids`),
  `status` = VALUES(`status`),
  `type` = VALUES(`type`),
  `remark` = VALUES(`remark`),
  `updater` = VALUES(`updater`),
  `update_time` = VALUES(`update_time`),
  `deleted` = VALUES(`deleted`);

INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
   `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`,
   `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (991210, '实习用户修改上传时间', 'intern-user:time-maintenance:file-upload-time:update',
   3, 3, 1090, '', '', '', '',
   0, b'1', b'1', b'0', 'intern-user-module', NOW(),
   'intern-user-module', NOW(), b'0'),
  (991211, '实习用户查看上传时间修改审计', 'intern-user:time-maintenance:file-upload-time-audit:query',
   3, 4, 1090, '', '', '', '',
   0, b'1', b'1', b'0', 'intern-user-module', NOW(),
   'intern-user-module', NOW(), b'0'),
  (991212, '实习用户修改升版时间', 'intern-user:time-maintenance:dcc-published-time:update',
   3, 5, 6807, '', '', '', '',
   0, b'1', b'1', b'0', 'intern-user-module', NOW(),
   'intern-user-module', NOW(), b'0'),
  (991213, '实习用户查看升版时间修改审计', 'intern-user:time-maintenance:dcc-published-time-audit:query',
   3, 6, 6807, '', '', '', '',
   0, b'1', b'1', b'0', 'intern-user-module', NOW(),
   'intern-user-module', NOW(), b'0'),
  (991214, '实习用户修改作废时间', 'intern-user:time-maintenance:dcc-obsoleted-time:update',
   3, 7, 6807, '', '', '', '',
   0, b'1', b'1', b'0', 'intern-user-module', NOW(),
   'intern-user-module', NOW(), b'0'),
  (991215, '实习用户查看作废时间修改审计', 'intern-user:time-maintenance:dcc-obsoleted-time-audit:query',
   3, 8, 6807, '', '', '', '',
   0, b'1', b'1', b'0', 'intern-user-module', NOW(),
   'intern-user-module', NOW(), b'0')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `permission` = VALUES(`permission`),
  `type` = VALUES(`type`),
  `sort` = VALUES(`sort`),
  `parent_id` = VALUES(`parent_id`),
  `status` = VALUES(`status`),
  `visible` = VALUES(`visible`),
  `keep_alive` = VALUES(`keep_alive`),
  `always_show` = VALUES(`always_show`),
  `updater` = VALUES(`updater`),
  `update_time` = VALUES(`update_time`),
  `deleted` = VALUES(`deleted`);

INSERT INTO `system_role_menu`
  (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT `role`.`id`, `menu`.`id`, 'intern-user-module', NOW(),
       'intern-user-module', NOW(), b'0', `role`.`tenant_id`
FROM `system_role` AS `role`
JOIN `system_menu` AS `menu`
  ON `menu`.`id` IN (2, 1243, 1090, 1091, 6800, 6807, 991210, 991211, 991212, 991213, 991214, 991215)
 AND `menu`.`status` = 0
 AND `menu`.`deleted` = b'0'
WHERE `role`.`code` = 'intern_user'
  AND `role`.`status` = 0
  AND `role`.`deleted` = b'0'
  AND NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` AS `existing`
    WHERE `existing`.`tenant_id` = `role`.`tenant_id`
      AND `existing`.`role_id` = `role`.`id`
      AND `existing`.`menu_id` = `menu`.`id`
      AND `existing`.`deleted` = b'0'
  );

COMMIT;
