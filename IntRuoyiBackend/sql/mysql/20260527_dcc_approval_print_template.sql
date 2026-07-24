-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
CREATE TABLE IF NOT EXISTS `dcc_approval_print_template` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `template_file_id` bigint NOT NULL,
  `template_file_name` varchar(255) NOT NULL,
  `template_file_content_type` varchar(255) DEFAULT NULL,
  `active` tinyint NOT NULL DEFAULT 1,
  `remark` varchar(500) DEFAULT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_dcc_approval_print_template_tenant_active` (`tenant_id`, `active`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC approval print template';

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '模板配置', 'dcc:controlled-file:print-template:manage', 2, 14, 6800, 'controlled-file/print-template', 'ep:printer', 'dcc/controlled-file/print-template/index', 'DccApprovalPrintTemplate', 0, b'0', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu`
  WHERE `permission` = 'dcc:controlled-file:print-template:manage'
     OR `path` = 'controlled-file/print-template'
);
