-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260808_mes_active_order_release_application; type=schema; riskLevel=medium
-- 活跃订单资料文件归属。该表不依赖、也不写入 P2 正式批次数据。

CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_active_order_dossier_file` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `active_order_id` bigint NOT NULL COMMENT '活跃订单ID',
    `application_id` bigint DEFAULT NULL COMMENT 'PQC生产放行申请ID，可为空',
    `category_key` varchar(64) NOT NULL COMMENT '资料类型',
    `file_id` bigint NOT NULL COMMENT 'infra文件ID',
    `file_url` varchar(1024) NOT NULL COMMENT '文件访问地址',
    `storage_config_id` bigint NOT NULL COMMENT '文件存储配置ID',
    `storage_path` varchar(512) NOT NULL COMMENT '文件存储路径',
    `file_name` varchar(255) NOT NULL COMMENT '文件名',
    `content_type` varchar(128) NOT NULL COMMENT '文件MIME类型',
    `file_size` bigint NOT NULL COMMENT '文件大小',
    `sha256` char(64) NOT NULL COMMENT '文件SHA-256',
    `operator_id` bigint NOT NULL COMMENT '上传人用户ID',
    `operator_name` varchar(64) NOT NULL COMMENT '上传人名称',
    `operated_at` datetime NOT NULL COMMENT '上传时间',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mes_pp_active_order_dossier_file_file` (`tenant_id`, `file_id`, `deleted`),
    KEY `idx_mes_pp_active_order_dossier_file_owner` (`tenant_id`, `active_order_id`, `deleted`),
    KEY `idx_mes_pp_active_order_dossier_file_category` (`tenant_id`, `active_order_id`, `category_key`, `deleted`),
    KEY `idx_mes_pp_active_order_dossier_file_application` (`tenant_id`, `application_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES活跃订单资料文件归属';
