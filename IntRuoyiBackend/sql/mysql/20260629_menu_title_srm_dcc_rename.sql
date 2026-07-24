-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260513_dcc_base_schema,20260618_srm_d7_1_code_rule_baseline; type=menu; riskLevel=low
-- Rename existing top-level menu titles from legacy names to SRM and 文控中心.

UPDATE `system_menu`
SET `name` = 'SRM',
    `updater` = '1',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND `id` = 991000
  AND `path` = '/srm'
  AND `name` <> 'SRM';

UPDATE `system_menu`
SET `name` = '文控中心',
    `updater` = '1',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND `id` = 6800
  AND `path` = '/dcc'
  AND `name` <> '文控中心';
