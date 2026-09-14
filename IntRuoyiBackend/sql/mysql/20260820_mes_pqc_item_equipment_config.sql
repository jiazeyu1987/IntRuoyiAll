-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260817_mes_pqc_item_level_task_identity; type=schema; riskLevel=medium
-- PQC item equipment config: tenant-level itemCode -> equipment -> equipment number list.

CREATE TABLE IF NOT EXISTS `mes_pqc_item_equipment_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  `item_code` varchar(64) NOT NULL COMMENT '检验项目编号',
  `item_name_snapshot` varchar(128) DEFAULT NULL COMMENT '检验项目名称快照',
  `equipment_id` bigint NOT NULL COMMENT '设备台账ID',
  `equipment_code` varchar(64) NOT NULL COMMENT '设备编码快照',
  `equipment_name` varchar(128) NOT NULL COMMENT '设备名称快照',
  `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用',
  `default_flag` bit(1) NOT NULL DEFAULT b'0' COMMENT '默认排序标识',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pqc_item_equipment` (`tenant_id`, `item_code`, `equipment_id`, `deleted`),
  KEY `idx_mes_pqc_item_equipment_item` (`tenant_id`, `item_code`, `enabled`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='PQC检验项目设备配置';

CREATE TABLE IF NOT EXISTS `mes_pqc_item_equipment_number_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  `config_id` bigint NOT NULL COMMENT '设备配置ID',
  `item_code` varchar(64) NOT NULL COMMENT '检验项目编号',
  `equipment_id` bigint NOT NULL COMMENT '设备台账ID',
  `equipment_number` varchar(64) NOT NULL COMMENT '设备编号快照',
  `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pqc_item_equipment_number` (`tenant_id`, `item_code`, `equipment_id`, `equipment_number`, `deleted`),
  KEY `idx_mes_pqc_item_equipment_number_config` (`tenant_id`, `config_id`, `enabled`, `sort`),
  KEY `idx_mes_pqc_item_equipment_number_item` (`tenant_id`, `item_code`, `equipment_id`, `enabled`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='PQC检验项目设备编号配置';
