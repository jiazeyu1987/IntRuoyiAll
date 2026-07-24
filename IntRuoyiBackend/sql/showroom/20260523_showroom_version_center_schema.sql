-- Showroom version center schema extension
-- 1. Persist authoritative company title/type snapshots on company revisions
-- 2. Persist readable/publishable version bundles for history/detail/republish

ALTER TABLE `showroom_company_revision`
    ADD COLUMN IF NOT EXISTS `display_name_snapshot` varchar(255) DEFAULT NULL AFTER `honors_awards_en`,
    ADD COLUMN IF NOT EXISTS `display_name_en_snapshot` varchar(255) DEFAULT NULL AFTER `display_name_snapshot`,
    ADD COLUMN IF NOT EXISTS `company_type_snapshot` varchar(32) DEFAULT NULL AFTER `display_name_en_snapshot`;

CREATE TABLE IF NOT EXISTS `showroom_version_bundle` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `target_type` varchar(32) NOT NULL,
    `target_id` bigint NOT NULL,
    `revision_id` bigint NOT NULL,
    `revision_no` int NOT NULL,
    `release_preview_asset_version_id` bigint DEFAULT NULL,
    `narration_zh_version_id` bigint DEFAULT NULL,
    `narration_en_version_id` bigint DEFAULT NULL,
    `copied_from_revision_id` bigint DEFAULT NULL,
    `published_by` bigint DEFAULT NULL,
    `published_at` datetime DEFAULT NULL,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_showroom_version_bundle_revision` (`tenant_id`, `target_type`, `target_id`, `revision_id`),
    UNIQUE KEY `uk_showroom_version_bundle_no` (`tenant_id`, `target_type`, `target_id`, `revision_no`),
    KEY `idx_showroom_version_bundle_published_at` (`target_type`, `target_id`, `published_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Showroom version center readable bundle';
