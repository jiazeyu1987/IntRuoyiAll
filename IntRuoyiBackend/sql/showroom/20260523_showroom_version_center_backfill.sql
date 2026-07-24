-- Showroom version center backfill
-- Idempotent insert of readable bundles for already-published revisions.
-- Rules:
-- - company revisions without authoritative display/company-type snapshot are skipped
-- - company preview linkage remains NULL unless later backfilled with external authoritative evidence
-- - any revision with multiple published media candidates is skipped instead of guessed

INSERT INTO `showroom_version_bundle` (
    `target_type`,
    `target_id`,
    `revision_id`,
    `revision_no`,
    `release_preview_asset_version_id`,
    `narration_zh_version_id`,
    `narration_en_version_id`,
    `copied_from_revision_id`,
    `published_by`,
    `published_at`,
    `creator`,
    `updater`,
    `tenant_id`
)
SELECT
    'COMPANY',
    r.`company_id`,
    r.`id`,
    r.`revision_no`,
    NULL,
    zh.`id`,
    en.`id`,
    NULL,
    r.`approved_by`,
    r.`published_at`,
    'version-center-backfill',
    'version-center-backfill',
    r.`tenant_id`
FROM `showroom_company_revision` r
JOIN (
    SELECT `target_id`, `source_revision_id`, MAX(`id`) AS `id`, COUNT(*) AS `candidate_count`
    FROM `showroom_narration_version`
    WHERE `deleted` = b'0'
      AND `status` = 'PUBLISHED'
      AND `target_type` = 'COMPANY'
      AND `audience_type` = 'PUBLIC'
      AND `language` = 'ZH'
    GROUP BY `target_id`, `source_revision_id`
) zh
    ON zh.`target_id` = r.`company_id`
   AND zh.`source_revision_id` = r.`id`
   AND zh.`candidate_count` = 1
JOIN (
    SELECT `target_id`, `source_revision_id`, MAX(`id`) AS `id`, COUNT(*) AS `candidate_count`
    FROM `showroom_narration_version`
    WHERE `deleted` = b'0'
      AND `status` = 'PUBLISHED'
      AND `target_type` = 'COMPANY'
      AND `audience_type` = 'PUBLIC'
      AND `language` = 'EN'
    GROUP BY `target_id`, `source_revision_id`
) en
    ON en.`target_id` = r.`company_id`
   AND en.`source_revision_id` = r.`id`
   AND en.`candidate_count` = 1
LEFT JOIN `showroom_version_bundle` existing
    ON existing.`deleted` = b'0'
   AND existing.`target_type` = 'COMPANY'
   AND existing.`target_id` = r.`company_id`
   AND existing.`revision_id` = r.`id`
WHERE r.`deleted` = b'0'
  AND r.`status` = 'PUBLISHED'
  AND r.`display_name_snapshot` IS NOT NULL
  AND r.`display_name_snapshot` <> ''
  AND r.`display_name_en_snapshot` IS NOT NULL
  AND r.`display_name_en_snapshot` <> ''
  AND r.`company_type_snapshot` IS NOT NULL
  AND r.`company_type_snapshot` <> ''
  AND existing.`id` IS NULL;

INSERT INTO `showroom_version_bundle` (
    `target_type`,
    `target_id`,
    `revision_id`,
    `revision_no`,
    `release_preview_asset_version_id`,
    `narration_zh_version_id`,
    `narration_en_version_id`,
    `copied_from_revision_id`,
    `published_by`,
    `published_at`,
    `creator`,
    `updater`,
    `tenant_id`
)
SELECT
    'PRODUCT',
    r.`product_id`,
    r.`id`,
    r.`revision_no`,
    preview.`id`,
    zh.`id`,
    en.`id`,
    NULL,
    r.`approved_by`,
    r.`published_at`,
    'version-center-backfill',
    'version-center-backfill',
    r.`tenant_id`
FROM `showroom_product_revision` r
JOIN (
    SELECT `target_id`, `source_revision_id`, MAX(`id`) AS `id`, COUNT(*) AS `candidate_count`
    FROM `showroom_narration_version`
    WHERE `deleted` = b'0'
      AND `status` = 'PUBLISHED'
      AND `target_type` = 'PRODUCT'
      AND `audience_type` = 'PUBLIC'
      AND `language` = 'ZH'
    GROUP BY `target_id`, `source_revision_id`
) zh
    ON zh.`target_id` = r.`product_id`
   AND zh.`source_revision_id` = r.`id`
   AND zh.`candidate_count` = 1
JOIN (
    SELECT `target_id`, `source_revision_id`, MAX(`id`) AS `id`, COUNT(*) AS `candidate_count`
    FROM `showroom_narration_version`
    WHERE `deleted` = b'0'
      AND `status` = 'PUBLISHED'
      AND `target_type` = 'PRODUCT'
      AND `audience_type` = 'PUBLIC'
      AND `language` = 'EN'
    GROUP BY `target_id`, `source_revision_id`
) en
    ON en.`target_id` = r.`product_id`
   AND en.`source_revision_id` = r.`id`
   AND en.`candidate_count` = 1
JOIN (
    SELECT `target_id`, `source_revision_id`, MAX(`id`) AS `id`, COUNT(*) AS `candidate_count`
    FROM `showroom_preview_asset_version`
    WHERE `deleted` = b'0'
      AND `status` = 'PUBLISHED'
      AND `target_type` = 'PRODUCT'
      AND `image_file_id` IS NOT NULL
    GROUP BY `target_id`, `source_revision_id`
) preview
    ON preview.`target_id` = r.`product_id`
   AND preview.`source_revision_id` = r.`id`
   AND preview.`candidate_count` = 1
LEFT JOIN `showroom_version_bundle` existing
    ON existing.`deleted` = b'0'
   AND existing.`target_type` = 'PRODUCT'
   AND existing.`target_id` = r.`product_id`
   AND existing.`revision_id` = r.`id`
WHERE r.`deleted` = b'0'
  AND r.`status` = 'PUBLISHED'
  AND existing.`id` IS NULL;
