-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260513_dcc_base_schema; type=data; riskLevel=medium
-- DCC 基础数据迁入全局基础数据子菜单

BEGIN;

SET @menu_name_basic_data := CONVERT(UNHEX('E59FBAE7A180E695B0E68DAE') USING utf8mb4);
SET @menu_name_project_code := CONVERT(UNHEX('444343E9A1B9E79BAEE4BBA3E7A081') USING utf8mb4);
SET @menu_name_product_catalog := CONVERT(UNHEX('444343E4BAA7E59381E79BAEE5BD95') USING utf8mb4);

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 990200, '基础数据', '', 1, 35, 0, '/mdm', 'ep:coin', NULL, NULL,
       0, b'1', b'1', b'1', 'dcc-basic-data', NOW(), 'dcc-basic-data', NOW(), b'0'
WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu` WHERE `deleted` = b'0' AND (`id` = 990200 OR `path` = '/mdm')
);

UPDATE `system_menu`
SET `name` = @menu_name_basic_data,
    `updater` = 'dcc-basic-data',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND (`id` = 990200 OR `path` = '/mdm');

SET @dcc_global_basic_data_menu_id := (
    SELECT `id`
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND (`id` = 990200 OR `path` = '/mdm')
    LIMIT 1
);

UPDATE `system_menu`
SET `name` = @menu_name_project_code,
    `parent_id` = @dcc_global_basic_data_menu_id,
    `path` = 'project-code',
    `icon` = 'ep:data-analysis',
    `component` = 'dcc/controlled-file/basic-data/project-code/index',
    `component_name` = 'DccProjectCodeBasicDataPage',
    `permission` = 'dcc:project-code:query',
    `type` = 2,
    `sort` = 20,
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `updater` = 'dcc-basic-data',
    `update_time` = NOW()
WHERE @dcc_global_basic_data_menu_id IS NOT NULL
  AND (`id` = 990210 OR `permission` = 'dcc:project-code:query')
  AND `deleted` = b'0';

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 990216, @menu_name_product_catalog, '', 2, 21, @dcc_global_basic_data_menu_id, 'product-catalog', 'ep:goods',
       'dcc/controlled-file/basic-data/product-catalog/index', 'DccProductCatalogBasicDataPage',
       0, b'1', b'1', b'1', 'dcc-basic-data', NOW(), 'dcc-basic-data', NOW(), b'0'
WHERE @dcc_global_basic_data_menu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM `system_menu`
      WHERE `deleted` = b'0'
        AND (`id` = 990216 OR (`parent_id` = @dcc_global_basic_data_menu_id AND `path` = 'product-catalog'))
  );

UPDATE `system_menu`
SET `name` = @menu_name_product_catalog,
    `permission` = '',
    `parent_id` = @dcc_global_basic_data_menu_id,
    `path` = 'product-catalog',
    `icon` = 'ep:goods',
    `component` = 'dcc/controlled-file/basic-data/product-catalog/index',
    `component_name` = 'DccProductCatalogBasicDataPage',
    `type` = 2,
    `sort` = 21,
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `updater` = 'dcc-basic-data',
    `update_time` = NOW()
WHERE @dcc_global_basic_data_menu_id IS NOT NULL
  AND `id` = 990216
  AND `deleted` = b'0';

SET @dcc_project_code_menu_id := (
    SELECT `id`
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND (`id` = 990210 OR (`parent_id` = @dcc_global_basic_data_menu_id AND `path` = 'project-code'))
    LIMIT 1
);

UPDATE `system_menu`
SET `parent_id` = @dcc_project_code_menu_id,
    `updater` = 'dcc-basic-data',
    `update_time` = NOW()
WHERE @dcc_project_code_menu_id IS NOT NULL
  AND `permission` IN (
      'dcc:project-code:create',
      'dcc:project-code:update',
      'dcc:project-code:delete',
      'dcc:project-code:import',
      'dcc:project-code:export'
  )
  AND `deleted` = b'0';

INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `updater`, `tenant_id`)
SELECT DISTINCT src.`role_id`, target_menu.`id`, src.`creator`, src.`updater`, src.`tenant_id`
FROM `system_role_menu` src
JOIN `system_menu` source_menu
  ON source_menu.`path` = 'controlled-file/categories'
 AND source_menu.`deleted` = b'0'
JOIN `system_menu` target_menu
  ON (
      target_menu.`id` = @dcc_global_basic_data_menu_id
      OR target_menu.`path` IN ('project-code', 'product-catalog')
      OR target_menu.`permission` IN (
          'dcc:project-code:create',
          'dcc:project-code:update',
          'dcc:project-code:delete',
          'dcc:project-code:import',
          'dcc:project-code:export'
      )
  )
 AND target_menu.`deleted` = b'0'
WHERE src.`menu_id` = source_menu.`id`
  AND src.`deleted` = b'0'
  AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` existing
      WHERE existing.`role_id` = src.`role_id`
        AND existing.`menu_id` = target_menu.`id`
        AND existing.`tenant_id` = src.`tenant_id`
        AND existing.`deleted` = b'0'
  );

COMMIT;
