-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- DCC electronic signature management patch.
-- Adds DCC signature authorization persistence and the DCC电子签名管理 menu entry.

CREATE TABLE IF NOT EXISTS `dcc_electronic_signature_authorization` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `electronic_signature_enabled` tinyint NOT NULL DEFAULT 1,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_esign_authorization_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC electronic signature authorization';

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6815, 'DCC电子签名管理', 'dcc:controlled-file:signature:manage', 2, 12, 6800, 'controlled-file/signatures', 'ep:management', 'dcc/controlled-file/signatures/index', 'DccControlledFileSignatures', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 6815 OR `path` = 'controlled-file/signatures');
