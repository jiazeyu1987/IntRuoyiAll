-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260513_dcc_base_schema; type=data; riskLevel=medium
-- 角色管理三分改名与导航重组

BEGIN;

UPDATE `system_menu`
SET `name` = '角色管理',
    `permission` = '',
    `type` = 1,
    `sort` = 2,
    `parent_id` = 1,
    `path` = 'role',
    `icon` = 'ep:user',
    `component` = '',
    `component_name` = NULL,
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE `id` = 101
  AND `deleted` = b'0';

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900183, '权限角色', '', 2, 1, 101, 'permission-role', 'ep:user', 'system/role/index', 'SystemRole', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 900183
       OR (`parent_id` = 101 AND `path` = 'permission-role')
);

UPDATE `system_menu`
SET `name` = '权限角色',
    `permission` = '',
    `type` = 2,
    `sort` = 1,
    `parent_id` = 101,
    `path` = 'permission-role',
    `icon` = 'ep:user',
    `component` = 'system/role/index',
    `component_name` = 'SystemRole',
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE `id` = 900183
  AND `deleted` = b'0';

UPDATE `system_menu`
SET `name` = '组织角色',
    `permission` = '',
    `type` = 2,
    `sort` = 2,
    `parent_id` = 101,
    `path` = 'organization-role',
    `icon` = 'fa:address-book-o',
    `component` = 'system/post/index',
    `component_name` = 'SystemPost',
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE `id` = 104
  AND `deleted` = b'0';

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6804, '审批角色', 'dcc:controlled-file:position:manage', 2, 3, 101, 'approval-role', 'ep:user', 'dcc/controlled-file/positions/index', 'DccControlledFilePositions', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 6804
       OR `permission` = 'dcc:controlled-file:position:manage'
);

UPDATE `system_menu`
SET `name` = '审批角色',
    `permission` = 'dcc:controlled-file:position:manage',
    `type` = 2,
    `sort` = 3,
    `parent_id` = 101,
    `path` = 'approval-role',
    `icon` = 'ep:user',
    `component` = 'dcc/controlled-file/positions/index',
    `component_name` = 'DccControlledFilePositions',
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE `id` = 6804
   OR (`permission` = 'dcc:controlled-file:position:manage' AND `deleted` = b'0');

UPDATE `system_menu`
SET `parent_id` = 900183,
    `updater` = '1',
    `update_time` = NOW()
WHERE `id` IN (1008, 1009, 1010, 1011, 1012, 1063, 1064, 1065)
  AND `deleted` = b'0';

INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `updater`, `tenant_id`)
SELECT DISTINCT src.`role_id`, 900183, src.`creator`, src.`updater`, src.`tenant_id`
FROM `system_role_menu` src
WHERE src.`deleted` = b'0'
  AND src.`menu_id` IN (101, 1008, 1009, 1010, 1011, 1012, 1063, 1064, 1065)
  AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` existing
      WHERE existing.`role_id` = src.`role_id`
        AND existing.`menu_id` = 900183
        AND existing.`tenant_id` = src.`tenant_id`
        AND existing.`deleted` = b'0'
  );

INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `updater`, `tenant_id`)
SELECT DISTINCT src.`role_id`, 104, src.`creator`, src.`updater`, src.`tenant_id`
FROM `system_role_menu` src
WHERE src.`deleted` = b'0'
  AND src.`menu_id` IN (104, 1021, 1022, 1023, 1024, 1025)
  AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` existing
      WHERE existing.`role_id` = src.`role_id`
        AND existing.`menu_id` = 104
        AND existing.`tenant_id` = src.`tenant_id`
        AND existing.`deleted` = b'0'
  );

INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `updater`, `tenant_id`)
SELECT DISTINCT src.`role_id`, 101, src.`creator`, src.`updater`, src.`tenant_id`
FROM `system_role_menu` src
WHERE src.`deleted` = b'0'
  AND src.`menu_id` IN (900183, 104, 1021, 1022, 1023, 1024, 1025, 6804)
  AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` existing
      WHERE existing.`role_id` = src.`role_id`
        AND existing.`menu_id` = 101
        AND existing.`tenant_id` = src.`tenant_id`
        AND existing.`deleted` = b'0'
  );

UPDATE `system_tenant_package`
SET `menu_ids` = JSON_ARRAY_APPEND(`menu_ids`, '$', CAST('101' AS JSON)),
    `updater` = '1',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND (
      JSON_CONTAINS(`menu_ids`, CAST('104' AS JSON), '$') = 1
      OR JSON_CONTAINS(`menu_ids`, CAST('6804' AS JSON), '$') = 1
      OR JSON_CONTAINS(`menu_ids`, CAST('900183' AS JSON), '$') = 1
  )
  AND JSON_CONTAINS(`menu_ids`, CAST('101' AS JSON), '$') = 0;

UPDATE `system_tenant_package`
SET `menu_ids` = JSON_ARRAY_APPEND(`menu_ids`, '$', CAST('900183' AS JSON)),
    `updater` = '1',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND JSON_CONTAINS(`menu_ids`, CAST('101' AS JSON), '$') = 1
  AND JSON_CONTAINS(`menu_ids`, CAST('900183' AS JSON), '$') = 0;

UPDATE `system_tenant_package`
SET `menu_ids` = JSON_ARRAY_APPEND(`menu_ids`, '$', CAST('104' AS JSON)),
    `updater` = '1',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND JSON_CONTAINS(`menu_ids`, CAST('101' AS JSON), '$') = 1
  AND JSON_CONTAINS(`menu_ids`, CAST('104' AS JSON), '$') = 0;

UPDATE `system_tenant_package`
SET `menu_ids` = JSON_ARRAY_APPEND(`menu_ids`, '$', CAST('6804' AS JSON)),
    `updater` = '1',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND JSON_CONTAINS(`menu_ids`, CAST('101' AS JSON), '$') = 1
  AND JSON_CONTAINS(`menu_ids`, CAST('6804' AS JSON), '$') = 0;

COMMIT;
