-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260814_mes_c015_route_dcc_qa_reconciliation_schema; type=schema; riskLevel=medium
-- MES QA：通用检验规程产品绑定与一线 PQC 组合规程读取基础

DROP PROCEDURE IF EXISTS migrate_mes_qa_regulation_active_dcc_owner_scope;

DELIMITER //
CREATE PROCEDURE migrate_mes_qa_regulation_active_dcc_owner_scope()
BEGIN
    IF EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'mes_qa_inspection_regulation'
          AND INDEX_NAME = 'uk_mes_qa_regulation_dcc_project'
    ) THEN
        ALTER TABLE `mes_qa_inspection_regulation` DROP INDEX `uk_mes_qa_regulation_dcc_project`;
    END IF;

    IF EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'mes_qa_inspection_regulation'
          AND INDEX_NAME = 'uk_mes_qa_regulation_active_dcc'
    ) THEN
        ALTER TABLE `mes_qa_inspection_regulation` DROP INDEX `uk_mes_qa_regulation_active_dcc`;
    END IF;

    IF EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'mes_qa_inspection_regulation'
          AND COLUMN_NAME = 'active_dcc_project_code_id'
    ) THEN
        ALTER TABLE `mes_qa_inspection_regulation` DROP COLUMN `active_dcc_project_code_id`;
    END IF;

    ALTER TABLE `mes_qa_inspection_regulation`
        ADD COLUMN `active_dcc_project_code_id` bigint GENERATED ALWAYS AS (
            CASE WHEN `deleted` = b'0' AND `owner_module` = 'MES_QA'
                 THEN `dcc_project_code_id` ELSE NULL END
        ) STORED COMMENT '启用产品QA规程DCC唯一键' AFTER `current_version_id`;

    ALTER TABLE `mes_qa_inspection_regulation`
        ADD UNIQUE KEY `uk_mes_qa_regulation_active_dcc`
            (`tenant_id`, `active_dcc_project_code_id`);
END//
DELIMITER ;

CALL migrate_mes_qa_regulation_active_dcc_owner_scope();

DROP PROCEDURE IF EXISTS migrate_mes_qa_regulation_active_dcc_owner_scope;

CREATE TABLE IF NOT EXISTS `mes_qa_common_regulation_product_binding` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `product_id` bigint NOT NULL COMMENT '产品ID',
    `dcc_project_code_id` bigint NOT NULL COMMENT 'DCC项目代码ID',
    `common_regulation_set_id` bigint DEFAULT NULL COMMENT '通用检验规程套ID',
    `common_regulation_set_version_id` bigint DEFAULT NULL COMMENT '通用检验规程套版本ID',
    `regulation_id` bigint NOT NULL COMMENT '通用QA规程ID',
    `regulation_version_id` bigint NOT NULL COMMENT '通用QA规程版本ID',
    `scope_code` varchar(64) NOT NULL COMMENT '绑定范围：COMMON_PACKAGING',
    `binding_status` varchar(32) NOT NULL COMMENT '绑定状态：ENABLED/DISABLED',
    `active_binding_key` varchar(64) GENERATED ALWAYS AS (
        CASE WHEN `deleted` = b'0' AND `binding_status` = 'ENABLED' THEN CAST(`product_id` AS CHAR) ELSE NULL END
    ) STORED COMMENT '启用绑定唯一键',
    `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    `idempotency_key` varchar(128) DEFAULT NULL COMMENT '幂等键',
    `change_reason` varchar(512) DEFAULT NULL COMMENT '变更原因',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mes_qa_common_binding_active_product` (`tenant_id`, `active_binding_key`),
    KEY `idx_mes_qa_common_binding_product_status` (`tenant_id`, `product_id`, `binding_status`),
    KEY `idx_mes_qa_common_binding_set_version_status` (`tenant_id`, `common_regulation_set_version_id`, `binding_status`),
    KEY `idx_mes_qa_common_binding_version_status` (`tenant_id`, `regulation_version_id`, `binding_status`),
    KEY `idx_mes_qa_common_binding_dcc_version` (`tenant_id`, `dcc_project_code_id`, `regulation_id`, `regulation_version_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES QA 通用检验规程产品绑定';

DROP PROCEDURE IF EXISTS migrate_mes_qa_common_regulation_set_schema;

DELIMITER //
CREATE PROCEDURE migrate_mes_qa_common_regulation_set_schema()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'mes_qa_common_regulation_product_binding'
          AND COLUMN_NAME = 'common_regulation_set_id'
    ) THEN
        ALTER TABLE `mes_qa_common_regulation_product_binding`
            ADD COLUMN `common_regulation_set_id` bigint DEFAULT NULL COMMENT '通用检验规程套ID' AFTER `dcc_project_code_id`;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'mes_qa_common_regulation_product_binding'
          AND COLUMN_NAME = 'common_regulation_set_version_id'
    ) THEN
        ALTER TABLE `mes_qa_common_regulation_product_binding`
            ADD COLUMN `common_regulation_set_version_id` bigint DEFAULT NULL COMMENT '通用检验规程套版本ID' AFTER `common_regulation_set_id`;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'mes_qa_common_regulation_product_binding'
          AND INDEX_NAME = 'idx_mes_qa_common_binding_set_version_status'
    ) THEN
        ALTER TABLE `mes_qa_common_regulation_product_binding`
            ADD KEY `idx_mes_qa_common_binding_set_version_status`
                (`tenant_id`, `common_regulation_set_version_id`, `binding_status`);
    END IF;
END//
DELIMITER ;

CALL migrate_mes_qa_common_regulation_set_schema();

DROP PROCEDURE IF EXISTS migrate_mes_qa_common_regulation_set_schema;

CREATE TABLE IF NOT EXISTS `mes_qa_common_regulation_set` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `set_code` varchar(64) NOT NULL COMMENT '通用规程套编号',
    `set_name` varchar(255) NOT NULL COMMENT '通用规程套名称',
    `set_status` varchar(32) NOT NULL COMMENT '套状态：ENABLED/DISABLED',
    `current_version_id` bigint DEFAULT NULL COMMENT '当前发布套版本ID',
    `remark` varchar(512) DEFAULT NULL COMMENT '备注',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mes_qa_common_set_code` (`tenant_id`, `set_code`, `deleted`),
    KEY `idx_mes_qa_common_set_status` (`tenant_id`, `set_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES QA 通用检验规程套';

CREATE TABLE IF NOT EXISTS `mes_qa_common_regulation_set_version` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `set_id` bigint NOT NULL COMMENT '通用检验规程套ID',
    `version_no` varchar(64) NOT NULL COMMENT '套版本号',
    `lifecycle_status` varchar(32) NOT NULL COMMENT '版本状态：DRAFT/PUBLISHED/RETIRED',
    `effective_date` date DEFAULT NULL COMMENT '生效日期',
    `published_at` datetime DEFAULT NULL COMMENT '发布时间',
    `retired_at` datetime DEFAULT NULL COMMENT '作废时间',
    `remark` varchar(512) DEFAULT NULL COMMENT '备注',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mes_qa_common_set_version_no` (`tenant_id`, `set_id`, `version_no`, `deleted`),
    KEY `idx_mes_qa_common_set_version_status` (`tenant_id`, `set_id`, `lifecycle_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES QA 通用检验规程套版本';

CREATE TABLE IF NOT EXISTS `mes_qa_common_regulation_set_version_member` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `set_version_id` bigint NOT NULL COMMENT '通用检验规程套版本ID',
    `regulation_id` bigint NOT NULL COMMENT '通用检验规程ID',
    `regulation_version_id` bigint NOT NULL COMMENT '通用检验规程版本ID',
    `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
    `member_role` varchar(64) DEFAULT NULL COMMENT '成员角色：如初包装/大中包装',
    `remark` varchar(512) DEFAULT NULL COMMENT '备注',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mes_qa_common_set_version_member`
        (`tenant_id`, `set_version_id`, `regulation_version_id`, `deleted`),
    KEY `idx_mes_qa_common_set_member_regulation_version`
        (`tenant_id`, `regulation_version_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES QA 通用检验规程套版本成员';
