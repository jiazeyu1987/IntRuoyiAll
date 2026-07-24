-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260512_bpm_base_schema; type=data; riskLevel=low
-- 修复 BPM 流程分类乱码，并补齐批记录升版审批流程分类。

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

UPDATE `bpm_category`
SET `name` = '办公审批',
    `description` = CASE
        WHEN `description` IS NULL OR `description` = '' OR `description` REGEXP '^[?]+$'
            THEN 'OA 示例流程分类'
        ELSE `description`
    END,
    `updater` = '20260714-bpm-category-fix',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND `code` = 'OA'
  AND (`name` IS NULL OR `name` = '' OR `name` REGEXP '^[?]+$');

INSERT INTO `bpm_category` (
    `name`, `code`, `description`, `status`, `sort`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
)
SELECT '办公审批', 'OA', 'OA 示例流程分类', 0, 10,
       '20260714-bpm-category-fix', NOW(), '20260714-bpm-category-fix', NOW(), b'0', tenant.`id`
FROM `system_tenant` tenant
WHERE tenant.`deleted` = b'0'
  AND NOT EXISTS (
      SELECT 1
      FROM `bpm_category` category
      WHERE category.`tenant_id` = tenant.`id`
        AND category.`code` = 'OA'
        AND category.`deleted` = b'0'
  );

UPDATE `bpm_category`
SET `name` = '批记录',
    `description` = CASE
        WHEN `description` IS NULL OR `description` = '' OR `description` REGEXP '^[?]+$'
            THEN '批记录升版审批流程分类'
        ELSE `description`
    END,
    `updater` = '20260714-bpm-category-fix',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND `code` = 'BATCH_RECORD'
  AND (`name` IS NULL OR `name` = '' OR `name` REGEXP '^[?]+$');

INSERT INTO `bpm_category` (
    `name`, `code`, `description`, `status`, `sort`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
)
SELECT '批记录', 'BATCH_RECORD', '批记录升版审批流程分类', 0, 20,
       '20260714-bpm-category-fix', NOW(), '20260714-bpm-category-fix', NOW(), b'0', tenant.`id`
FROM `system_tenant` tenant
WHERE tenant.`deleted` = b'0'
  AND NOT EXISTS (
      SELECT 1
      FROM `bpm_category` category
      WHERE category.`tenant_id` = tenant.`id`
        AND category.`code` = 'BATCH_RECORD'
        AND category.`deleted` = b'0'
  );
