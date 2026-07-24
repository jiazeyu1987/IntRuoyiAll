-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260626_showroom_keyword_bu_seed_runtime; type=data; riskLevel=medium
-- Normalize current showroom product BU values. 空 BU 保持为空；only current product revisions are updated.

DROP TEMPORARY TABLE IF EXISTS `tmp_showroom_product_current_bu_classified`;

CREATE TEMPORARY TABLE `tmp_showroom_product_current_bu_classified` AS
SELECT
    `revision`.`id` AS `revision_id`,
    `revision`.`product_id`,
    `revision`.`tenant_id`,
    `revision`.`pipeline_layout`,
    `revision`.`pipeline_layout_en`,
    CASE
        WHEN CONCAT(COALESCE(`revision`.`pipeline_layout`, ''), ' ', COALESCE(`revision`.`pipeline_layout_en`, '')) LIKE '%应为心血管BU%' THEN '心血管BU'
        WHEN CONCAT(COALESCE(`revision`.`pipeline_layout`, ''), ' ', COALESCE(`revision`.`pipeline_layout_en`, '')) LIKE '%应为外周血管BU%' THEN '外周血管BU'
        WHEN CONCAT(COALESCE(`revision`.`pipeline_layout`, ''), ' ', COALESCE(`revision`.`pipeline_layout_en`, '')) LIKE '%应为结构心BU%' THEN '结构心BU'
        WHEN CONCAT(COALESCE(`revision`.`pipeline_layout`, ''), ' ', COALESCE(`revision`.`pipeline_layout_en`, '')) LIKE '%应为神经血管BU%' THEN '神经血管BU'
        WHEN CONCAT(COALESCE(`revision`.`pipeline_layout`, ''), ' ', COALESCE(`revision`.`pipeline_layout_en`, '')) LIKE '%应为心脏电生理BU%' THEN '心脏电生理BU'
        WHEN CONCAT(COALESCE(`revision`.`pipeline_layout`, ''), ' ', COALESCE(`revision`.`pipeline_layout_en`, '')) LIKE '%应为非血管BU%' THEN '非血管BU'
        WHEN CONCAT(COALESCE(`revision`.`pipeline_layout`, ''), ' ', COALESCE(`revision`.`pipeline_layout_en`, '')) LIKE '%Cardiac Electrophysiology%' THEN '心脏电生理BU'
        WHEN CONCAT(COALESCE(`revision`.`pipeline_layout`, ''), ' ', COALESCE(`revision`.`pipeline_layout_en`, '')) LIKE '%心脏电生理%' THEN '心脏电生理BU'
        WHEN CONCAT(COALESCE(`revision`.`pipeline_layout`, ''), ' ', COALESCE(`revision`.`pipeline_layout_en`, '')) LIKE '%Neurovascular%' THEN '神经血管BU'
        WHEN CONCAT(COALESCE(`revision`.`pipeline_layout`, ''), ' ', COALESCE(`revision`.`pipeline_layout_en`, '')) LIKE '%神经血管%' THEN '神经血管BU'
        WHEN CONCAT(COALESCE(`revision`.`pipeline_layout`, ''), ' ', COALESCE(`revision`.`pipeline_layout_en`, '')) LIKE '%Cardiovascular%' THEN '心血管BU'
        WHEN CONCAT(COALESCE(`revision`.`pipeline_layout`, ''), ' ', COALESCE(`revision`.`pipeline_layout_en`, '')) LIKE '%心血管%' THEN '心血管BU'
        WHEN CONCAT(COALESCE(`revision`.`pipeline_layout`, ''), ' ', COALESCE(`revision`.`pipeline_layout_en`, '')) LIKE '%Structural Heart%' THEN '结构心BU'
        WHEN CONCAT(COALESCE(`revision`.`pipeline_layout`, ''), ' ', COALESCE(`revision`.`pipeline_layout_en`, '')) LIKE '%结构心%' THEN '结构心BU'
        WHEN CONCAT(COALESCE(`revision`.`pipeline_layout`, ''), ' ', COALESCE(`revision`.`pipeline_layout_en`, '')) LIKE '%结构BU%' THEN '结构心BU'
        WHEN CONCAT(COALESCE(`revision`.`pipeline_layout`, ''), ' ', COALESCE(`revision`.`pipeline_layout_en`, '')) LIKE '%Peripheral Vascular%' THEN '外周血管BU'
        WHEN CONCAT(COALESCE(`revision`.`pipeline_layout`, ''), ' ', COALESCE(`revision`.`pipeline_layout_en`, '')) LIKE '%Peripheral Vessel%' THEN '外周血管BU'
        WHEN CONCAT(COALESCE(`revision`.`pipeline_layout`, ''), ' ', COALESCE(`revision`.`pipeline_layout_en`, '')) LIKE '%外周血管%' THEN '外周血管BU'
        WHEN CONCAT(COALESCE(`revision`.`pipeline_layout`, ''), ' ', COALESCE(`revision`.`pipeline_layout_en`, '')) LIKE '%Non-vascular%' THEN '非血管BU'
        WHEN CONCAT(COALESCE(`revision`.`pipeline_layout`, ''), ' ', COALESCE(`revision`.`pipeline_layout_en`, '')) LIKE '%Non vascular%' THEN '非血管BU'
        WHEN CONCAT(COALESCE(`revision`.`pipeline_layout`, ''), ' ', COALESCE(`revision`.`pipeline_layout_en`, '')) LIKE '%非血管%' THEN '非血管BU'
        ELSE NULL
    END AS `target_zh`,
    CASE
        WHEN CONCAT(COALESCE(`revision`.`pipeline_layout`, ''), ' ', COALESCE(`revision`.`pipeline_layout_en`, '')) LIKE '%应为心血管BU%' THEN 'Cardiovascular BU'
        WHEN CONCAT(COALESCE(`revision`.`pipeline_layout`, ''), ' ', COALESCE(`revision`.`pipeline_layout_en`, '')) LIKE '%应为外周血管BU%' THEN 'Peripheral Vascular BU'
        WHEN CONCAT(COALESCE(`revision`.`pipeline_layout`, ''), ' ', COALESCE(`revision`.`pipeline_layout_en`, '')) LIKE '%应为结构心BU%' THEN 'Structural Heart BU'
        WHEN CONCAT(COALESCE(`revision`.`pipeline_layout`, ''), ' ', COALESCE(`revision`.`pipeline_layout_en`, '')) LIKE '%应为神经血管BU%' THEN 'Neurovascular BU'
        WHEN CONCAT(COALESCE(`revision`.`pipeline_layout`, ''), ' ', COALESCE(`revision`.`pipeline_layout_en`, '')) LIKE '%应为心脏电生理BU%' THEN 'Cardiac Electrophysiology BU'
        WHEN CONCAT(COALESCE(`revision`.`pipeline_layout`, ''), ' ', COALESCE(`revision`.`pipeline_layout_en`, '')) LIKE '%应为非血管BU%' THEN 'Non-vascular BU'
        WHEN CONCAT(COALESCE(`revision`.`pipeline_layout`, ''), ' ', COALESCE(`revision`.`pipeline_layout_en`, '')) LIKE '%Cardiac Electrophysiology%' THEN 'Cardiac Electrophysiology BU'
        WHEN CONCAT(COALESCE(`revision`.`pipeline_layout`, ''), ' ', COALESCE(`revision`.`pipeline_layout_en`, '')) LIKE '%心脏电生理%' THEN 'Cardiac Electrophysiology BU'
        WHEN CONCAT(COALESCE(`revision`.`pipeline_layout`, ''), ' ', COALESCE(`revision`.`pipeline_layout_en`, '')) LIKE '%Neurovascular%' THEN 'Neurovascular BU'
        WHEN CONCAT(COALESCE(`revision`.`pipeline_layout`, ''), ' ', COALESCE(`revision`.`pipeline_layout_en`, '')) LIKE '%神经血管%' THEN 'Neurovascular BU'
        WHEN CONCAT(COALESCE(`revision`.`pipeline_layout`, ''), ' ', COALESCE(`revision`.`pipeline_layout_en`, '')) LIKE '%Cardiovascular%' THEN 'Cardiovascular BU'
        WHEN CONCAT(COALESCE(`revision`.`pipeline_layout`, ''), ' ', COALESCE(`revision`.`pipeline_layout_en`, '')) LIKE '%心血管%' THEN 'Cardiovascular BU'
        WHEN CONCAT(COALESCE(`revision`.`pipeline_layout`, ''), ' ', COALESCE(`revision`.`pipeline_layout_en`, '')) LIKE '%Structural Heart%' THEN 'Structural Heart BU'
        WHEN CONCAT(COALESCE(`revision`.`pipeline_layout`, ''), ' ', COALESCE(`revision`.`pipeline_layout_en`, '')) LIKE '%结构心%' THEN 'Structural Heart BU'
        WHEN CONCAT(COALESCE(`revision`.`pipeline_layout`, ''), ' ', COALESCE(`revision`.`pipeline_layout_en`, '')) LIKE '%结构BU%' THEN 'Structural Heart BU'
        WHEN CONCAT(COALESCE(`revision`.`pipeline_layout`, ''), ' ', COALESCE(`revision`.`pipeline_layout_en`, '')) LIKE '%Peripheral Vascular%' THEN 'Peripheral Vascular BU'
        WHEN CONCAT(COALESCE(`revision`.`pipeline_layout`, ''), ' ', COALESCE(`revision`.`pipeline_layout_en`, '')) LIKE '%Peripheral Vessel%' THEN 'Peripheral Vascular BU'
        WHEN CONCAT(COALESCE(`revision`.`pipeline_layout`, ''), ' ', COALESCE(`revision`.`pipeline_layout_en`, '')) LIKE '%外周血管%' THEN 'Peripheral Vascular BU'
        WHEN CONCAT(COALESCE(`revision`.`pipeline_layout`, ''), ' ', COALESCE(`revision`.`pipeline_layout_en`, '')) LIKE '%Non-vascular%' THEN 'Non-vascular BU'
        WHEN CONCAT(COALESCE(`revision`.`pipeline_layout`, ''), ' ', COALESCE(`revision`.`pipeline_layout_en`, '')) LIKE '%Non vascular%' THEN 'Non-vascular BU'
        WHEN CONCAT(COALESCE(`revision`.`pipeline_layout`, ''), ' ', COALESCE(`revision`.`pipeline_layout_en`, '')) LIKE '%非血管%' THEN 'Non-vascular BU'
        ELSE NULL
    END AS `target_en`
FROM `showroom_product_revision` AS `revision`
JOIN `showroom_product` AS `product`
    ON `product`.`current_revision_id` = `revision`.`id`
WHERE `product`.`deleted` = b'0'
  AND `revision`.`deleted` = b'0'
  AND (NULLIF(TRIM(COALESCE(`revision`.`pipeline_layout`, '')), '') IS NOT NULL OR NULLIF(TRIM(COALESCE(`revision`.`pipeline_layout_en`, '')), '') IS NOT NULL)
  AND NOT (
      `revision`.`tenant_id` = 0
      AND NULLIF(TRIM(COALESCE(`revision`.`pipeline_layout`, '')), '') IS NULL
      AND TRIM(COALESCE(`revision`.`pipeline_layout_en`, '')) = 'Null value probe'
  );

DROP TEMPORARY TABLE IF EXISTS `tmp_showroom_product_current_bu_unknown_guard`;

CREATE TEMPORARY TABLE `tmp_showroom_product_current_bu_unknown_guard` (
    `must_be_empty` int NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Unrecognized non-empty showroom product BU value: this insert intentionally fails if unknown rows exist.
INSERT INTO `tmp_showroom_product_current_bu_unknown_guard` (`must_be_empty`)
SELECT NULL
FROM `tmp_showroom_product_current_bu_classified` AS `classified`
WHERE `classified`.`target_zh` IS NULL;

CREATE TABLE IF NOT EXISTS `showroom_product_current_bu_backup_20260626` (
    `revision_id` bigint NOT NULL,
    `product_id` bigint NOT NULL,
    `tenant_id` bigint NOT NULL,
    `pipeline_layout` text NULL,
    `pipeline_layout_en` text NULL,
    `backup_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`revision_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `showroom_product_current_bu_backup_20260626` (
    `revision_id`,
    `product_id`,
    `tenant_id`,
    `pipeline_layout`,
    `pipeline_layout_en`
)
SELECT
    `classified`.`revision_id`,
    `classified`.`product_id`,
    `classified`.`tenant_id`,
    `classified`.`pipeline_layout`,
    `classified`.`pipeline_layout_en`
FROM `tmp_showroom_product_current_bu_classified` AS `classified`
WHERE NOT EXISTS (
    SELECT 1
    FROM `showroom_product_current_bu_backup_20260626` AS `backup`
    WHERE `backup`.`revision_id` = `classified`.`revision_id`
);

UPDATE `showroom_product_revision` AS `revision`
JOIN `showroom_product_current_bu_backup_20260626` AS `backup`
    ON `backup`.`revision_id` = `revision`.`id`
JOIN `tmp_showroom_product_current_bu_classified` AS `classified`
    ON `classified`.`revision_id` = `revision`.`id`
SET
    `revision`.`pipeline_layout` = `classified`.`target_zh`,
    `revision`.`pipeline_layout_en` = `classified`.`target_en`,
    `revision`.`updater` = 'showroom-current-bu-normalization',
    `revision`.`update_time` = CURRENT_TIMESTAMP
WHERE `classified`.`target_zh` IS NOT NULL
  AND (
      COALESCE(`revision`.`pipeline_layout`, '') COLLATE utf8mb4_0900_ai_ci <> `classified`.`target_zh` COLLATE utf8mb4_0900_ai_ci
      OR COALESCE(`revision`.`pipeline_layout_en`, '') COLLATE utf8mb4_0900_ai_ci <> `classified`.`target_en` COLLATE utf8mb4_0900_ai_ci
  );

DROP TEMPORARY TABLE IF EXISTS `tmp_showroom_product_current_bu_classified`;
DROP TEMPORARY TABLE IF EXISTS `tmp_showroom_product_current_bu_unknown_guard`;
