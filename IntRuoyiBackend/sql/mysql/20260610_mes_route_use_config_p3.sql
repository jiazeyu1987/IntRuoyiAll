-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=config; riskLevel=medium
-- MES 工艺路线用途配置 P3：排产配置 / 批处理配置分离

CREATE TABLE IF NOT EXISTS `mes_pro_route_use_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `route_id` bigint NOT NULL COMMENT '工艺路线ID',
  `use_type` varchar(16) NOT NULL COMMENT '用途类型：SCHEDULE/BATCH',
  `config_version` varchar(64) DEFAULT NULL COMMENT '用途配置版本',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_route_use_config` (`tenant_id`,`route_id`,`use_type`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 工艺路线用途配置';

CREATE TABLE IF NOT EXISTS `mes_pro_route_use_process_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `route_use_config_id` bigint NOT NULL COMMENT '用途配置ID',
  `route_id` bigint NOT NULL COMMENT '工艺路线ID',
  `route_process_id` bigint NOT NULL COMMENT '工艺路线工序ID',
  `use_type` varchar(16) NOT NULL COMMENT '用途类型：SCHEDULE/BATCH',
  `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '当前用途下是否启用',
  `production_quantity_factor` decimal(18,6) NOT NULL DEFAULT 1.000000 COMMENT '生产数量系数，工序计划数量=成品数量*生产数量系数',
  `batch_record_report_id` varchar(64) DEFAULT NULL COMMENT '当前用途下默认批记录报表ID，仅批处理用途有效',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_route_use_process_config` (`tenant_id`,`route_process_id`,`use_type`,`deleted`),
  KEY `idx_mes_pro_route_use_process_config_route_use` (`tenant_id`,`route_use_config_id`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 工艺路线工序用途配置';
