-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
CREATE TABLE IF NOT EXISTS `showroom_product_revision_attachment` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `product_id` bigint NOT NULL,
    `product_revision_id` bigint NOT NULL,
    `asset_type` varchar(16) NOT NULL,
    `file_id` bigint NOT NULL,
    `original_name` varchar(255) NOT NULL,
    `mime_type` varchar(128) NOT NULL,
    `file_size` bigint NOT NULL,
    `display_order` int NOT NULL DEFAULT 0,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_showroom_product_revision_attachment_file` (`tenant_id`, `product_revision_id`, `file_id`),
    KEY `idx_showroom_product_revision_attachment_revision` (`tenant_id`, `product_revision_id`, `display_order`, `id`),
    KEY `idx_showroom_product_revision_attachment_product` (`tenant_id`, `product_id`, `product_revision_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Showroom product revision attachment snapshot';
