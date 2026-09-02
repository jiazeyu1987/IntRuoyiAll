-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260710_dcc_product_catalog_database; type=schema; riskLevel=medium
-- DCC 产品目录、项目代码、注册证的明确关联关系。
-- Rollback: 先导出 dcc_data_relation，再 DROP TABLE `dcc_data_relation`。

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `dcc_data_relation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `product_catalog_id` bigint NOT NULL COMMENT 'DCC 产品目录 ID',
  `project_code_id` bigint NOT NULL COMMENT 'DCC 项目代码 ID',
  `registration_certificate_id` bigint NOT NULL COMMENT '注册证主档 ID',
  `relation_status` varchar(32) NOT NULL DEFAULT 'CONFIRMED' COMMENT '关联状态',
  `relation_source` varchar(32) NOT NULL DEFAULT 'MANUAL' COMMENT '关联来源',
  `relation_remark` varchar(512) DEFAULT NULL COMMENT '关联说明',
  `confirmed_by` bigint DEFAULT NULL COMMENT '确认人',
  `confirmed_time` datetime DEFAULT NULL COMMENT '确认时间',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_data_relation_identity` (`tenant_id`, `product_catalog_id`, `project_code_id`, `registration_certificate_id`, `deleted`),
  KEY `idx_dcc_data_relation_project_code` (`tenant_id`, `project_code_id`, `deleted`),
  KEY `idx_dcc_data_relation_registration_certificate` (`tenant_id`, `registration_certificate_id`, `deleted`),
  KEY `idx_dcc_data_relation_catalog` (`tenant_id`, `product_catalog_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC 产品目录三方明确关联';
