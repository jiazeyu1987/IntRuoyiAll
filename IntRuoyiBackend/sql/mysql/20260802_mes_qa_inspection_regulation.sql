-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260802_mes_process_pool_active_order_process_snapshot; type=schema; riskLevel=medium
-- MES M3：QA 检验规程正式所有权、发布版本和检验项目模型

CREATE TABLE IF NOT EXISTS `mes_qa_inspection_regulation` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `product_id` bigint NOT NULL COMMENT '产品ID',
    `route_id` bigint NOT NULL COMMENT '工艺路线ID',
    `route_version_id` bigint NOT NULL COMMENT '工艺路线版本ID',
    `route_process_id` bigint NOT NULL COMMENT '工艺路线工序ID',
    `process_id` bigint NOT NULL COMMENT '工序ID',
    `owner_module` varchar(32) NOT NULL COMMENT '所有权模块',
    `regulation_code` varchar(64) NOT NULL COMMENT '规程编码',
    `regulation_name` varchar(128) NOT NULL COMMENT '规程名称',
    `lifecycle_status` varchar(32) NOT NULL COMMENT '生命周期：DRAFT/PUBLISHED/RETIRED',
    `current_version_id` bigint DEFAULT NULL COMMENT '当前发布版本ID',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mes_qa_regulation_route_process` (`tenant_id`, `product_id`, `route_id`, `route_version_id`, `route_process_id`, `deleted`),
    UNIQUE KEY `uk_mes_qa_regulation_code` (`tenant_id`, `regulation_code`, `deleted`),
    KEY `idx_mes_qa_regulation_current_version` (`tenant_id`, `current_version_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES QA 检验规程';

CREATE TABLE IF NOT EXISTS `mes_qa_inspection_regulation_version` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `regulation_id` bigint NOT NULL COMMENT 'QA检验规程ID',
    `version_no` varchar(32) NOT NULL COMMENT '版本号',
    `lifecycle_status` varchar(32) NOT NULL COMMENT '生命周期：DRAFT/PUBLISHED/RETIRED',
    `published_at` datetime DEFAULT NULL COMMENT '发布时间',
    `retired_at` datetime DEFAULT NULL COMMENT '作废时间',
    `snapshot_json` longtext NOT NULL COMMENT '发布快照JSON',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mes_qa_regulation_version` (`tenant_id`, `regulation_id`, `version_no`, `deleted`),
    KEY `idx_mes_qa_regulation_version_status` (`tenant_id`, `regulation_id`, `lifecycle_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES QA 检验规程版本';

CREATE TABLE IF NOT EXISTS `mes_qa_inspection_regulation_item` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `regulation_version_id` bigint NOT NULL COMMENT 'QA检验规程版本ID',
    `inspection_type` varchar(32) NOT NULL COMMENT '检验类型：FIRST/PATROL/FINAL',
    `item_code` varchar(64) NOT NULL COMMENT '检验项目编码',
    `item_name` varchar(128) NOT NULL COMMENT '检验项目名称',
    `inspection_method` varchar(512) NOT NULL COMMENT '检验方法',
    `standard_text` varchar(1024) NOT NULL COMMENT '合格标准',
    `result_type` varchar(32) NOT NULL COMMENT '结果类型',
    `first_inspection_quantity` int DEFAULT NULL COMMENT '首检数量',
    `patrol_inspection_ratio` decimal(18,6) DEFAULT NULL COMMENT '巡检数量系数',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mes_qa_regulation_item` (`tenant_id`, `regulation_version_id`, `inspection_type`, `item_code`, `deleted`),
    KEY `idx_mes_qa_regulation_item_version` (`tenant_id`, `regulation_version_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES QA 检验规程项目';
