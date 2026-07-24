-- Showroom tenant isolation migration.
-- Strategy:
-- 1. Existing historical showroom mutable rows with tenant_id = 0 are explicitly assigned to tenant 1.
-- 2. Test tenant 122 data must be seeded/imported with tenant_id = 122 by deployment scripts or admin APIs.
-- 3. Runtime code must not copy tenant 1 rows or read tenant_id = 0 as a fallback.

UPDATE `showroom_company` SET `tenant_id` = 1 WHERE `tenant_id` = 0;
UPDATE `showroom_company_revision` SET `tenant_id` = 1 WHERE `tenant_id` = 0;
UPDATE `showroom_product` SET `tenant_id` = 1 WHERE `tenant_id` = 0;
UPDATE `showroom_product_revision` SET `tenant_id` = 1 WHERE `tenant_id` = 0;
UPDATE `showroom_hall` SET `tenant_id` = 1 WHERE `tenant_id` = 0;
UPDATE `showroom_hall_product` SET `tenant_id` = 1 WHERE `tenant_id` = 0;
UPDATE `showroom_product_revision_relation` SET `tenant_id` = 1 WHERE `tenant_id` = 0;
UPDATE `showroom_product_cover_batch_task` SET `tenant_id` = 1 WHERE `tenant_id` = 0;
UPDATE `showroom_product_cover_batch_task_item` SET `tenant_id` = 1 WHERE `tenant_id` = 0;
UPDATE `showroom_change_request` SET `tenant_id` = 1 WHERE `tenant_id` = 0;
UPDATE `showroom_change_request_item` SET `tenant_id` = 1 WHERE `tenant_id` = 0;
UPDATE `showroom_change_request_signature` SET `tenant_id` = 1 WHERE `tenant_id` = 0;
UPDATE `showroom_version_audit` SET `tenant_id` = 1 WHERE `tenant_id` = 0;
UPDATE `showroom_field_assignment` SET `tenant_id` = 1 WHERE `tenant_id` = 0;
UPDATE `showroom_product_comment` SET `tenant_id` = 1 WHERE `tenant_id` = 0;
UPDATE `showroom_narration_version` SET `tenant_id` = 1 WHERE `tenant_id` = 0;
UPDATE `showroom_preview_asset_version` SET `tenant_id` = 1 WHERE `tenant_id` = 0;
UPDATE `showroom_version_bundle` SET `tenant_id` = 1 WHERE `tenant_id` = 0;

DROP PROCEDURE IF EXISTS showroom_drop_index_if_exists;
DELIMITER //
CREATE PROCEDURE showroom_drop_index_if_exists(
    IN p_table_name varchar(128),
    IN p_index_name varchar(128)
)
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = p_table_name
          AND index_name = p_index_name
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table_name, '` DROP INDEX `', p_index_name, '`');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//
DELIMITER ;

CALL showroom_drop_index_if_exists('showroom_company', 'uk_showroom_company_code');
ALTER TABLE `showroom_company`
    ADD UNIQUE KEY `uk_showroom_company_code` (`tenant_id`, `company_code`);

CALL showroom_drop_index_if_exists('showroom_company_revision', 'uk_showroom_company_revision_no');
ALTER TABLE `showroom_company_revision`
    ADD UNIQUE KEY `uk_showroom_company_revision_no` (`tenant_id`, `company_id`, `revision_no`);

CALL showroom_drop_index_if_exists('showroom_product', 'uk_showroom_product_code');
ALTER TABLE `showroom_product`
    ADD UNIQUE KEY `uk_showroom_product_code` (`tenant_id`, `product_code`);

CALL showroom_drop_index_if_exists('showroom_product_revision', 'uk_showroom_product_revision_no');
ALTER TABLE `showroom_product_revision`
    ADD UNIQUE KEY `uk_showroom_product_revision_no` (`tenant_id`, `product_id`, `revision_no`);

CALL showroom_drop_index_if_exists('showroom_hall', 'uk_showroom_hall_code');
ALTER TABLE `showroom_hall`
    ADD UNIQUE KEY `uk_showroom_hall_code` (`tenant_id`, `hall_code`);

CALL showroom_drop_index_if_exists('showroom_hall_product', 'uk_showroom_hall_product');
ALTER TABLE `showroom_hall_product`
    ADD UNIQUE KEY `uk_showroom_hall_product` (`tenant_id`, `hall_id`, `product_id`);

CALL showroom_drop_index_if_exists('showroom_product_revision_relation', 'uk_showroom_product_revision_relation');
ALTER TABLE `showroom_product_revision_relation`
    ADD UNIQUE KEY `uk_showroom_product_revision_relation`
        (`tenant_id`, `product_revision_id`, `related_product_id`, `relation_type`);

CALL showroom_drop_index_if_exists('showroom_product_cover_batch_task_item', 'uk_showroom_cover_batch_task_item');
ALTER TABLE `showroom_product_cover_batch_task_item`
    ADD UNIQUE KEY `uk_showroom_cover_batch_task_item` (`tenant_id`, `task_id`, `product_id`);

CALL showroom_drop_index_if_exists('showroom_narration_version', 'uk_showroom_narration_version_no');
ALTER TABLE `showroom_narration_version`
    ADD UNIQUE KEY `uk_showroom_narration_version_no`
        (`tenant_id`, `target_type`, `target_id`, `audience_type`, `language`, `version_no`);

CALL showroom_drop_index_if_exists('showroom_preview_asset_version', 'uk_showroom_preview_asset_version_no');
ALTER TABLE `showroom_preview_asset_version`
    ADD UNIQUE KEY `uk_showroom_preview_asset_version_no`
        (`tenant_id`, `target_type`, `target_id`, `version_no`);

CALL showroom_drop_index_if_exists('showroom_version_bundle', 'uk_showroom_version_bundle_revision');
CALL showroom_drop_index_if_exists('showroom_version_bundle', 'uk_showroom_version_bundle_no');
ALTER TABLE `showroom_version_bundle`
    ADD UNIQUE KEY `uk_showroom_version_bundle_revision`
        (`tenant_id`, `target_type`, `target_id`, `revision_id`),
    ADD UNIQUE KEY `uk_showroom_version_bundle_no`
        (`tenant_id`, `target_type`, `target_id`, `revision_no`);

DROP PROCEDURE IF EXISTS showroom_drop_index_if_exists;
