-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260513_dcc_base_schema; type=data; riskLevel=low
-- Merge DCC view and preview into one read permission concept.
-- Safe to run repeatedly: only upgrades legacy read rules and retires the obsolete preview menu row.

UPDATE `dcc_directory_access_rule`
SET `can_query` = b'1',
    `can_preview` = b'1',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND (`can_query` = b'1' OR `can_preview` = b'1')
  AND (`can_query` <> b'1' OR `can_preview` <> b'1');

UPDATE `system_menu`
SET `deleted` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE `permission` = 'dcc:controlled-file:preview'
  AND `deleted` = b'0';
