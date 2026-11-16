-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260606_showroom_hall_product_canvas_layout; type=schema; riskLevel=medium
CREATE TABLE IF NOT EXISTS `showroom_award` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Award ID',
    `award_code` varchar(64) NOT NULL COMMENT 'Award code, e.g. AWARD-001',
    `current_revision_id` bigint DEFAULT NULL COMMENT 'Current published revision ID',
    `current_revision_no` int DEFAULT NULL COMMENT 'Current published revision number',
    `incomplete_flag` bit(1) NOT NULL DEFAULT b'0' COMMENT 'Whether required generated fields are incomplete',
    `status` varchar(32) NOT NULL COMMENT 'Award status',
    `creator` varchar(64) DEFAULT '' COMMENT 'Creator',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    `updater` varchar(64) DEFAULT '' COMMENT 'Updater',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'Deleted flag',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT 'Tenant ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_showroom_award_code` (`tenant_id`, `award_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Showroom award master';

CREATE TABLE IF NOT EXISTS `showroom_award_revision` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Award revision ID',
    `award_id` bigint NOT NULL COMMENT 'Award ID',
    `revision_no` int NOT NULL COMMENT 'Revision number',
    `status` varchar(32) NOT NULL COMMENT 'Revision status',
    `award_code_snapshot` varchar(64) NOT NULL COMMENT 'Award code snapshot',
    `name_cn` varchar(255) NOT NULL COMMENT 'Chinese award name',
    `name_en` varchar(255) DEFAULT NULL COMMENT 'English award name',
    `description_zh` text COMMENT 'Chinese narration text',
    `description_en` text COMMENT 'English narration text',
    `issuer` varchar(255) DEFAULT NULL COMMENT 'Issuer',
    `award_date_text` varchar(255) DEFAULT NULL COMMENT 'Award date or term text',
    `cover_image` text COMMENT 'Cover image admin URL',
    `submitted_by` bigint DEFAULT NULL COMMENT 'Submitted by',
    `approved_by` bigint DEFAULT NULL COMMENT 'Approved by',
    `published_at` datetime DEFAULT NULL COMMENT 'Published time',
    `creator` varchar(64) DEFAULT '' COMMENT 'Creator',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    `updater` varchar(64) DEFAULT '' COMMENT 'Updater',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'Deleted flag',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT 'Tenant ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_showroom_award_revision_no` (`tenant_id`, `award_id`, `revision_no`),
    KEY `idx_showroom_award_revision_award` (`tenant_id`, `award_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Showroom award revision';

CREATE TABLE IF NOT EXISTS `showroom_hall_item` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Hall item ID',
    `hall_id` bigint NOT NULL COMMENT 'Hall ID',
    `item_type` varchar(32) NOT NULL COMMENT 'PRODUCT or AWARD',
    `item_id` bigint NOT NULL COMMENT 'Product or award ID',
    `display_order` int NOT NULL COMMENT 'Display order',
    `layout_x` decimal(8,6) DEFAULT NULL COMMENT 'Normalized canvas rectangle x',
    `layout_y` decimal(8,6) DEFAULT NULL COMMENT 'Normalized canvas rectangle y',
    `layout_width` decimal(8,6) DEFAULT NULL COMMENT 'Normalized canvas rectangle width',
    `layout_height` decimal(8,6) DEFAULT NULL COMMENT 'Normalized canvas rectangle height',
    `creator` varchar(64) DEFAULT '' COMMENT 'Creator',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    `updater` varchar(64) DEFAULT '' COMMENT 'Updater',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'Deleted flag',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT 'Tenant ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_showroom_hall_item` (`tenant_id`, `hall_id`, `item_type`, `item_id`),
    KEY `idx_showroom_hall_item_order` (`tenant_id`, `hall_id`, `display_order`, `id`),
    KEY `idx_showroom_hall_item_item` (`tenant_id`, `item_type`, `item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Showroom hall mixed display item';

INSERT INTO `showroom_hall_item` (
    `hall_id`, `item_type`, `item_id`, `display_order`, `layout_x`, `layout_y`, `layout_width`, `layout_height`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
)
SELECT hp.`hall_id`, 'PRODUCT', hp.`product_id`, hp.`display_order`, hp.`layout_x`, hp.`layout_y`,
       hp.`layout_width`, hp.`layout_height`, hp.`creator`, hp.`create_time`, hp.`updater`, hp.`update_time`,
       hp.`deleted`, hp.`tenant_id`
FROM `showroom_hall_product` hp
LEFT JOIN `showroom_hall_item` hi
       ON hi.`tenant_id` = hp.`tenant_id`
      AND hi.`hall_id` = hp.`hall_id`
      AND hi.`item_type` = 'PRODUCT'
      AND hi.`item_id` = hp.`product_id`
WHERE hp.`deleted` = b'0'
  AND hi.`id` IS NULL;
