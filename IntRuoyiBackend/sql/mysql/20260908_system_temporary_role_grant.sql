-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260830_system_user_lifecycle_deactivation; type=schema; riskLevel=medium
-- 设计边界：临时角色授权独立于长期 system_user_role，必须有有效期、审批、撤销/过期和使用审计。

SET NAMES utf8mb4;
START TRANSACTION;

CREATE TABLE IF NOT EXISTS `system_temporary_role_grant` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '授权记录编号',
  `user_id` bigint NOT NULL COMMENT '被授权用户编号',
  `role_id` bigint NOT NULL COMMENT '临时角色编号',
  `reason` varchar(500) NOT NULL COMMENT '申请原因',
  `status` varchar(32) NOT NULL COMMENT '状态：PENDING/ACTIVE/REVOKED/EXPIRED',
  `apply_time` datetime NOT NULL COMMENT '申请时间',
  `applicant_user_id` bigint DEFAULT NULL COMMENT '申请人编号',
  `applicant_username` varchar(64) DEFAULT NULL COMMENT '申请人',
  `approve_time` datetime DEFAULT NULL COMMENT '审批时间',
  `approver_user_id` bigint DEFAULT NULL COMMENT '审批人编号',
  `approver_username` varchar(64) DEFAULT NULL COMMENT '审批人',
  `effective_time` datetime DEFAULT NULL COMMENT '生效时间',
  `expire_time` datetime NOT NULL COMMENT '有效截止时间',
  `revoke_time` datetime DEFAULT NULL COMMENT '撤销或过期时间',
  `revoker_user_id` bigint DEFAULT NULL COMMENT '撤销人编号',
  `revoker_username` varchar(64) DEFAULT NULL COMMENT '撤销人',
  `revoke_reason` varchar(500) DEFAULT NULL COMMENT '撤销或过期原因',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_system_temp_role_grant_user_status` (`tenant_id`, `user_id`, `status`, `expire_time`),
  KEY `idx_system_temp_role_grant_role_status` (`tenant_id`, `role_id`, `status`, `expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统临时角色授权';

CREATE TABLE IF NOT EXISTS `system_temporary_role_grant_audit` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '审计编号',
  `grant_id` bigint NOT NULL COMMENT '授权记录编号',
  `event_type` varchar(64) NOT NULL COMMENT '事件：APPLY/APPROVE/REVOKE/EXPIRE/USE',
  `user_id` bigint NOT NULL COMMENT '被授权用户编号',
  `role_id` bigint NOT NULL COMMENT '角色编号',
  `permission_code` varchar(150) DEFAULT NULL COMMENT '使用时命中的权限标识',
  `operator_user_id` bigint DEFAULT NULL COMMENT '操作人编号',
  `operator_username` varchar(64) DEFAULT NULL COMMENT '操作人',
  `message` varchar(500) DEFAULT NULL COMMENT '事件说明',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_system_temp_role_grant_audit_grant` (`tenant_id`, `grant_id`, `create_time`),
  KEY `idx_system_temp_role_grant_audit_use` (`tenant_id`, `user_id`, `permission_code`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统临时角色授权审计';

INSERT INTO `infra_job`
  (`name`, `status`, `handler_name`, `handler_param`, `cron_expression`,
   `retry_count`, `retry_interval`, `monitor_timeout`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '每分钟回收过期临时角色授权', 1, 'temporaryRoleGrantExpireJob', '{}', '0 * * * * ?',
       3, 60, 0, '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (
    SELECT 1 FROM `infra_job` WHERE `handler_name` = 'temporaryRoleGrantExpireJob'
);

UPDATE `infra_job`
   SET `name` = '每分钟回收过期临时角色授权',
       `status` = 1,
       `handler_name` = 'temporaryRoleGrantExpireJob',
       `handler_param` = '{}',
       `cron_expression` = '0 * * * * ?',
       `retry_count` = 3,
       `retry_interval` = 60,
       `monitor_timeout` = 0,
       `updater` = '1',
       `update_time` = NOW(),
       `deleted` = b'0'
 WHERE `handler_name` = 'temporaryRoleGrantExpireJob';


DROP TEMPORARY TABLE IF EXISTS `tmp_system_temporary_role_grant_menu`;
CREATE TEMPORARY TABLE `tmp_system_temporary_role_grant_menu` (
  `id` bigint NOT NULL PRIMARY KEY,
  `name` varchar(64) NOT NULL,
  `permission` varchar(150) NOT NULL,
  `type` tinyint NOT NULL,
  `sort` int NOT NULL,
  `parent_id` bigint NOT NULL,
  `path` varchar(200) NOT NULL,
  `component` varchar(255) DEFAULT NULL,
  `component_name` varchar(255) DEFAULT NULL
) ENGINE=Memory;

INSERT INTO `tmp_system_temporary_role_grant_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `component`, `component_name`)
VALUES
  (901200, '临时角色授权', 'system:temporary-role-grant:query', 2, 120, 1, 'temporary-role-grant', 'system/temporary-role-grant/index', 'SystemTemporaryRoleGrant'),
  (901201, '新增临时角色授权', 'system:temporary-role-grant:create', 3, 1, 901200, '', NULL, NULL),
  (901202, '审批临时角色授权', 'system:temporary-role-grant:approve', 3, 2, 901200, '', NULL, NULL),
  (901203, '撤销临时角色授权', 'system:temporary-role-grant:revoke', 3, 3, 901200, '', NULL, NULL),
  (901204, '查看临时角色授权审计', 'system:temporary-role-grant:query', 3, 4, 901200, '', NULL, NULL);

INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT `id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, 'ep:timer', `component`, `component_name`, 0, b'1', b'1', b'1', 'temporary-role-grant', NOW(), 'temporary-role-grant', NOW(), b'0'
  FROM `tmp_system_temporary_role_grant_menu` AS `src`
 WHERE NOT EXISTS (SELECT 1 FROM `system_menu` AS `menu` WHERE `menu`.`id` = `src`.`id`);

UPDATE `system_menu` AS `menu`
  JOIN `tmp_system_temporary_role_grant_menu` AS `src` ON `src`.`id` = `menu`.`id`
   SET `menu`.`name` = `src`.`name`,
       `menu`.`permission` = `src`.`permission`,
       `menu`.`type` = `src`.`type`,
       `menu`.`sort` = `src`.`sort`,
       `menu`.`parent_id` = `src`.`parent_id`,
       `menu`.`path` = `src`.`path`,
       `menu`.`component` = `src`.`component`,
       `menu`.`component_name` = `src`.`component_name`,
       `menu`.`status` = 0,
       `menu`.`visible` = b'1',
       `menu`.`deleted` = b'0',
       `menu`.`updater` = 'temporary-role-grant',
       `menu`.`update_time` = NOW();

INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT `role`.`id`, `menu`.`id`, 'temporary-role-grant', NOW(), 'temporary-role-grant', NOW(), b'0', `role`.`tenant_id`
  FROM `system_role` AS `role`
  JOIN `tmp_system_temporary_role_grant_menu` AS `menu`
 WHERE `role`.`code` IN ('super_admin', 'tenant_admin')
   AND `role`.`status` = 0
   AND `role`.`deleted` = b'0'
   AND NOT EXISTS (
     SELECT 1 FROM `system_role_menu` AS `existing`
      WHERE `existing`.`role_id` = `role`.`id`
        AND `existing`.`tenant_id` = `role`.`tenant_id`
        AND `existing`.`menu_id` = `menu`.`id`
   );

UPDATE `system_role_menu` AS `role_menu`
  JOIN `system_role` AS `role` ON `role`.`id` = `role_menu`.`role_id` AND `role`.`tenant_id` = `role_menu`.`tenant_id`
  JOIN `tmp_system_temporary_role_grant_menu` AS `menu` ON `menu`.`id` = `role_menu`.`menu_id`
   SET `role_menu`.`deleted` = b'0',
       `role_menu`.`updater` = 'temporary-role-grant',
       `role_menu`.`update_time` = NOW()
 WHERE `role`.`code` IN ('super_admin', 'tenant_admin');

DROP TEMPORARY TABLE IF EXISTS `tmp_system_temporary_role_grant_menu`;

COMMIT;
