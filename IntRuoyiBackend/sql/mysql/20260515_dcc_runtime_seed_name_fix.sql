-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=seed; riskLevel=low
-- Fix DCC runtime / E2E seed names that were already persisted as question marks.
-- The original Chinese labels are irreversibly lost in the current database rows,
-- so these replacements restore readable names based on the stable code semantics.

UPDATE `dcc_file_directory`
SET `name` = 'DCC运行时根目录'
WHERE `deleted` = b'0'
  AND `code` = 'DCC_RUNTIME_ROOT'
  AND (`name` IS NULL OR `name` LIKE '%?%');

UPDATE `dcc_file_directory`
SET `name` = '运行时PDF目录'
WHERE `deleted` = b'0'
  AND `code` = 'DCC_RUNTIME_PDF'
  AND (`name` IS NULL OR `name` LIKE '%?%');

UPDATE `dcc_file_category`
SET `name` = '运行时文件类别'
WHERE `deleted` = b'0'
  AND `code` = 'DCC_RUNTIME_CATEGORY'
  AND (`name` IS NULL OR `name` LIKE '%?%');
