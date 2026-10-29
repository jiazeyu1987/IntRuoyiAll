-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260524_showroom_prompt_menu_visibility,20260624_mes_auto_schedule_permission_split,20260711_mes_batch_record_cell_link_rule,20260924_mes_edhr_deviation_management,20260924_mes_edhr_deviation_ncr; type=data; riskLevel=low
-- Repair the 16 observed UTF-8 menu names previously decoded as Windows-1252.
-- Both names use UTF-8 hex to avoid dependence on the SQL client's character set.
-- Match exact menu identity and corrupted bytes; preserve valid/customized names and all permissions.
-- Distinguish ncr-create from deviation-create because sibling menu names must be unique.

BEGIN;

-- 批记录单元格链接查询
UPDATE `system_menu`
SET `name` = CONVERT(UNHEX('E689B9E8AEB0E5BD95E58D95E58583E6A0BCE993BEE68EA5E69FA5E8AFA2') USING utf8mb4),
    `updater` = 'fix-menu-name-utf8',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND `id` = 605071101
  AND `permission` = 'mes:pro-batch-record-cell-link:query'
  AND BINARY `name` = UNHEX('C3A6E280B0C2B9C3A8C2AEC2B0C3A5C2BDE280A2C3A5C28DE280A2C3A5E280A6C692C3A6C2A0C2BCC3A9E2809CC2BEC3A6C5BDC2A5C3A6C5B8C2A5C3A8C2AFC2A2');

-- 批记录单元格链接维护
UPDATE `system_menu`
SET `name` = CONVERT(UNHEX('E689B9E8AEB0E5BD95E58D95E58583E6A0BCE993BEE68EA5E7BBB4E68AA4') USING utf8mb4),
    `updater` = 'fix-menu-name-utf8',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND `id` = 605071102
  AND `permission` = 'mes:pro-batch-record-cell-link:update'
  AND BINARY `name` = UNHEX('C3A6E280B0C2B9C3A8C2AEC2B0C3A5C2BDE280A2C3A5C28DE280A2C3A5E280A6C692C3A6C2A0C2BCC3A9E2809CC2BEC3A6C5BDC2A5C3A7C2BBC2B4C3A6C5A0C2A4');

-- 提示管理
UPDATE `system_menu`
SET `name` = CONVERT(UNHEX('E68F90E7A4BAE7AEA1E79086') USING utf8mb4),
    `updater` = 'fix-menu-name-utf8',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND `id` = 980119
  AND `permission` = ''
  AND `component_name` = 'ShowroomAdminPrompt'
  AND BINARY `name` = UNHEX('C3A6C28FC290C3A7C2A4C2BAC3A7C2AEC2A1C3A7C290E280A0');

-- 自动排产预览
UPDATE `system_menu`
SET `name` = CONVERT(UNHEX('E887AAE58AA8E68E92E4BAA7E9A284E8A788') USING utf8mb4),
    `updater` = 'fix-menu-name-utf8',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND `id` = 900180
  AND `permission` = 'mes:pro-auto-schedule:preview'
  AND BINARY `name` = UNHEX('C3A8E280A1C2AAC3A5C5A0C2A8C3A6C5BDE28099C3A4C2BAC2A7C3A9C2A2E2809EC3A8C2A7CB86');

-- 自动排产发布
UPDATE `system_menu`
SET `name` = CONVERT(UNHEX('E887AAE58AA8E68E92E4BAA7E58F91E5B883') USING utf8mb4),
    `updater` = 'fix-menu-name-utf8',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND `id` = 900181
  AND `permission` = 'mes:pro-auto-schedule:apply'
  AND BINARY `name` = UNHEX('C3A8E280A1C2AAC3A5C5A0C2A8C3A6C5BDE28099C3A4C2BAC2A7C3A5C28FE28098C3A5C2B8C692');

-- 自动排产重排
UPDATE `system_menu`
SET `name` = CONVERT(UNHEX('E887AAE58AA8E68E92E4BAA7E9878DE68E92') USING utf8mb4),
    `updater` = 'fix-menu-name-utf8',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND `id` = 900182
  AND `permission` = 'mes:pro-auto-schedule:replan'
  AND BINARY `name` = UNHEX('C3A8E280A1C2AAC3A5C5A0C2A8C3A6C5BDE28099C3A4C2BAC2A7C3A9E280A1C28DC3A6C5BDE28099');

-- 偏差查询
UPDATE `system_menu`
SET `name` = CONVERT(UNHEX('E5818FE5B7AEE69FA5E8AFA2') USING utf8mb4),
    `updater` = 'fix-menu-name-utf8',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND `id` = 9008440
  AND `permission` = 'mes:pro-edhr-deviation:query'
  AND BINARY `name` = UNHEX('C3A5C281C28FC3A5C2B7C2AEC3A6C5B8C2A5C3A8C2AFC2A2');

-- 偏差发起
UPDATE `system_menu`
SET `name` = CONVERT(UNHEX('E5818FE5B7AEE58F91E8B5B7') USING utf8mb4),
    `updater` = 'fix-menu-name-utf8',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND `id` = 9008441
  AND `permission` = 'mes:pro-edhr-deviation:create'
  AND BINARY `name` = UNHEX('C3A5C281C28FC3A5C2B7C2AEC3A5C28FE28098C3A8C2B5C2B7');

-- 关键偏差转不合格评审
UPDATE `system_menu`
SET `name` = CONVERT(UNHEX('E585B3E994AEE5818FE5B7AEE8BDACE4B88DE59088E6A0BCE8AF84E5AEA1') USING utf8mb4),
    `updater` = 'fix-menu-name-utf8',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND `id` = 9008442
  AND `permission` = 'mes:pro-edhr-nonconformance-review:deviation-create'
  AND BINARY `name` = UNHEX('C3A5E280A6C2B3C3A9E2809DC2AEC3A5C281C28FC3A5C2B7C2AEC3A8C2BDC2ACC3A4C2B8C28DC3A5C290CB86C3A6C2A0C2BCC3A8C2AFE2809EC3A5C2AEC2A1');

-- 偏差处理
UPDATE `system_menu`
SET `name` = CONVERT(UNHEX('E5818FE5B7AEE5A484E79086') USING utf8mb4),
    `updater` = 'fix-menu-name-utf8',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND `id` = 9008443
  AND `permission` = 'mes:pro-edhr-deviation:handle'
  AND BINARY `name` = UNHEX('C3A5C281C28FC3A5C2B7C2AEC3A5C2A4E2809EC3A7C290E280A0');

-- 偏差验证
UPDATE `system_menu`
SET `name` = CONVERT(UNHEX('E5818FE5B7AEE9AA8CE8AF81') USING utf8mb4),
    `updater` = 'fix-menu-name-utf8',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND `id` = 9008444
  AND `permission` = 'mes:pro-edhr-deviation:verify'
  AND BINARY `name` = UNHEX('C3A5C281C28FC3A5C2B7C2AEC3A9C2AAC592C3A8C2AFC281');

-- 偏差部门负责人确认
UPDATE `system_menu`
SET `name` = CONVERT(UNHEX('E5818FE5B7AEE983A8E997A8E8B49FE8B4A3E4BABAE7A1AEE8AEA4') USING utf8mb4),
    `updater` = 'fix-menu-name-utf8',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND `id` = 9008445
  AND `permission` = 'mes:pro-edhr-deviation:department-confirm'
  AND BINARY `name` = UNHEX('C3A5C281C28FC3A5C2B7C2AEC3A9C692C2A8C3A9E28094C2A8C3A8C2B4C5B8C3A8C2B4C2A3C3A4C2BAC2BAC3A7C2A1C2AEC3A8C2AEC2A4');

-- 偏差QA关闭确认
UPDATE `system_menu`
SET `name` = CONVERT(UNHEX('E5818FE5B7AE5141E585B3E997ADE7A1AEE8AEA4') USING utf8mb4),
    `updater` = 'fix-menu-name-utf8',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND `id` = 9008446
  AND `permission` = 'mes:pro-edhr-deviation:qa-close'
  AND BINARY `name` = UNHEX('C3A5C281C28FC3A5C2B7C2AE5141C3A5E280A6C2B3C3A9E28094C2ADC3A7C2A1C2AEC3A8C2AEC2A4');

-- 偏差质量负责人批准
UPDATE `system_menu`
SET `name` = CONVERT(UNHEX('E5818FE5B7AEE8B4A8E9878FE8B49FE8B4A3E4BABAE689B9E58786') USING utf8mb4),
    `updater` = 'fix-menu-name-utf8',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND `id` = 9008447
  AND `permission` = 'mes:pro-edhr-deviation:quality-approve'
  AND BINARY `name` = UNHEX('C3A5C281C28FC3A5C2B7C2AEC3A8C2B4C2A8C3A9E280A1C28FC3A8C2B4C5B8C3A8C2B4C2A3C3A4C2BAC2BAC3A6E280B0C2B9C3A5E280A1E280A0');

-- 关键偏差管理者代表批准
UPDATE `system_menu`
SET `name` = CONVERT(UNHEX('E585B3E994AEE5818FE5B7AEE7AEA1E79086E88085E4BBA3E8A1A8E689B9E58786') USING utf8mb4),
    `updater` = 'fix-menu-name-utf8',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND `id` = 9008448
  AND `permission` = 'mes:pro-edhr-deviation:critical-management-approve'
  AND BINARY `name` = UNHEX('C3A5E280A6C2B3C3A9E2809DC2AEC3A5C281C28FC3A5C2B7C2AEC3A7C2AEC2A1C3A7C290E280A0C3A8E282ACE280A6C3A4C2BBC2A3C3A8C2A1C2A8C3A6E280B0C2B9C3A5E280A1E280A0');

-- 关键偏差创建不合格评审
UPDATE `system_menu`
SET `name` = CONVERT(UNHEX('E585B3E994AEE5818FE5B7AEE5889BE5BBBAE4B88DE59088E6A0BCE8AF84E5AEA1') USING utf8mb4),
    `updater` = 'fix-menu-name-utf8',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND `id` = 9008449
  AND `permission` = 'mes:pro-edhr-deviation:ncr-create'
  AND BINARY `name` = UNHEX('C3A5E280A6C2B3C3A9E2809DC2AEC3A5C281C28FC3A5C2B7C2AEC3A8C2BDC2ACC3A4C2B8C28DC3A5C290CB86C3A6C2A0C2BCC3A8C2AFE2809EC3A5C2AEC2A1');

COMMIT;
