param(
    [string]$TaskDirectory = 'E:\IntRuoyi\doc\tasks\20260807-test-permission-role-differential-sync',
    [string]$OutputPath = 'E:\IntRuoyi\IntRuoyiBackend\sql\mysql\20260807_test_tenant1_all_role_permission_sync.sql'
)

$ErrorActionPreference = 'Stop'

function ConvertTo-SqlString {
    param([AllowNull()]$Value)
    if ($null -eq $Value) {
        return 'NULL'
    }
    $text = ([string]$Value).Replace("'", "''").Replace("`r", ' ').Replace("`n", ' ')
    return "'$text'"
}

function ConvertTo-SqlNullableString {
    param([AllowNull()]$Value)
    if ($null -eq $Value) {
        return 'NULL'
    }
    return ConvertTo-SqlString $Value
}

$local = Get-Content -Raw -Encoding utf8 (Join-Path $TaskDirectory 'all-role-audit-local.json') | ConvertFrom-Json
$test = Get-Content -Raw -Encoding utf8 (Join-Path $TaskDirectory 'all-role-audit-test.json') | ConvertFrom-Json
$roleDefinitions = @(Get-Content -Raw -Encoding utf8 (Join-Path $TaskDirectory 'all-role-source-definitions.json') | ConvertFrom-Json)

if ($roleDefinitions.Count -ne 60) {
    throw "Expected 60 source role definitions, got $($roleDefinitions.Count)"
}
if (@($roleDefinitions | Group-Object code | Where-Object Count -ne 1).Count -ne 0) {
    throw 'Source role codes are not unique'
}

$roleValues = @($roleDefinitions | Sort-Object code | ForEach-Object {
    $categoryCode = ConvertTo-SqlNullableString $_.categoryCode
    $remark = ConvertTo-SqlNullableString $_.remark
    "  ($(ConvertTo-SqlString $_.code), $(ConvertTo-SqlString $_.name), $([int]$_.sort), $categoryCode, $([int]$_.dataScope), $(ConvertTo-SqlString $_.dataScopeDeptIds), $([int]$_.status), $([int]$_.type), $remark)"
}) -join ",`n"

$effectiveRoleMenus = @($local.roleMenus | Where-Object {
    [int]$_.menuStatus -eq 0 -and -not [string]::IsNullOrWhiteSpace([string]$_.permission)
})
$pairGroups = @($effectiveRoleMenus | Group-Object { "$($_.roleCode)`u{001f}$($_.permission)" })
if ($pairGroups.Count -ne 1676) {
    throw "Expected 1676 effective role-permission pairs, got $($pairGroups.Count)"
}

$permissionValues = @($pairGroups | ForEach-Object {
    $representative = $_.Group | Sort-Object `
        @{ Expression = { if ([int]$_.menuType -eq 2) { 0 } elseif ([int]$_.menuType -eq 3) { 1 } else { 2 } } }, `
        @{ Expression = { [long]$_.menuId } } | Select-Object -First 1
    [pscustomobject]@{
        RoleCode = [string]$representative.roleCode
        Permission = [string]$representative.permission
        MenuType = [int]$representative.menuType
        Path = $representative.path
        Component = $representative.component
        ComponentName = $representative.componentName
    }
} | Sort-Object RoleCode, Permission | ForEach-Object {
    "  ($(ConvertTo-SqlString $_.RoleCode), $(ConvertTo-SqlString $_.Permission), $($_.MenuType), $(ConvertTo-SqlNullableString $_.Path), $(ConvertTo-SqlNullableString $_.Component), $(ConvertTo-SqlNullableString $_.ComponentName))"
}) -join ",`n"

$testPermissions = @($test.menus | Where-Object {
    [int]$_.status -eq 0 -and -not [string]::IsNullOrWhiteSpace([string]$_.permission)
} | ForEach-Object permission | Sort-Object -Unique)
$sourceAssignedMenuIds = @($effectiveRoleMenus | ForEach-Object { [long]$_.menuId } | Sort-Object -Unique)
$missingMenus = @($local.menus | Where-Object {
    [int]$_.status -eq 0 -and
    -not [string]::IsNullOrWhiteSpace([string]$_.permission) -and
    $testPermissions -notcontains [string]$_.permission -and
    $sourceAssignedMenuIds -contains [long]$_.id
} | Sort-Object @{ Expression = { [long]$_.id } })
$missingPermissions = @($missingMenus | ForEach-Object permission | Sort-Object -Unique)
if ($missingPermissions.Count -ne 12) {
    throw "Expected 12 missing permissions, got $($missingPermissions.Count)"
}
if ($missingMenus.Count -ne 13) {
    throw "Expected 13 missing menu definitions, got $($missingMenus.Count)"
}

$localMenuById = @{}
foreach ($menu in $local.menus) {
    $localMenuById[[string]$menu.id] = $menu
}
$missingMenuIdSet = @{}
foreach ($menu in $missingMenus) {
    $missingMenuIdSet[[string]$menu.id] = $true
}

$missingMenuValues = @($missingMenus | ForEach-Object {
    $menu = $_
    $usesFormalTeamLeaderParent = [string]$menu.permission -in @(
        'mes:pro-process-pool-team-leader:review',
        'mes:pro-process-pool-team-leader:abnormal',
        'mes:pro-process-pool-team-leader:maintain'
    )
    $parent = if ($usesFormalTeamLeaderParent) {
        $test.menus | Where-Object { [long]$_.id -eq 900436 } | Select-Object -First 1
    } else {
        $localMenuById[[string]$menu.parentId]
    }
    if ($null -eq $parent) {
        throw "Missing local parent menu for source menu $($menu.id)"
    }
    $parentSourceKey = if (-not $usesFormalTeamLeaderParent -and $missingMenuIdSet.ContainsKey([string]$parent.id)) {
        ConvertTo-SqlString "local-menu-$($parent.id)"
    } else {
        'NULL'
    }
    "  ($(ConvertTo-SqlString "local-menu-$($menu.id)"), $(ConvertTo-SqlString $menu.name), $(ConvertTo-SqlString $menu.permission), $([int]$menu.type), $([int]$menu.sort), $parentSourceKey, $([long]$parent.id), $(ConvertTo-SqlString $parent.permission), $([int]$parent.type), $(ConvertTo-SqlNullableString $parent.path), $(ConvertTo-SqlNullableString $parent.component), $(ConvertTo-SqlNullableString $parent.componentName), $(ConvertTo-SqlNullableString $menu.path), $(ConvertTo-SqlNullableString $menu.component), $(ConvertTo-SqlNullableString $menu.componentName), $([int]$menu.status), $([int]$menu.visible))"
}) -join ",`n"

$template = @'
-- release-migration: allowedEnvironments=test; dependsOn=20260728_mes_scheduler_route_flow_list_permission; type=data; riskLevel=high
-- Purpose: align every active tenant-1 local role and effective permission with the test environment by stable keys.
-- source-active-role-count: 60
-- source-role-permission-count: 1676
-- source-missing-permission-count: 12
-- Target-only roles and all user-role bindings must remain unchanged.
-- Other-tenant role-menu rows must remain unchanged.

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS sync_test_tenant1_all_role_permissions;

DELIMITER //
CREATE PROCEDURE sync_test_tenant1_all_role_permissions()
BEGIN
  DECLARE previous_menu_resolution_count int DEFAULT -1;
  DECLARE current_menu_resolution_count int DEFAULT 0;

  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  DROP TEMPORARY TABLE IF EXISTS `tmp_test_tenant1_role_source`;
  CREATE TEMPORARY TABLE `tmp_test_tenant1_role_source` (
    `code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    `name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    `sort` int NOT NULL,
    `category_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
    `data_scope` tinyint NOT NULL,
    `data_scope_dept_ids` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    `status` tinyint NOT NULL,
    `type` tinyint NOT NULL,
    `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
    PRIMARY KEY (`code`)
  );

  INSERT INTO `tmp_test_tenant1_role_source`
    (`code`, `name`, `sort`, `category_code`, `data_scope`, `data_scope_dept_ids`, `status`, `type`, `remark`)
  VALUES
__ROLE_VALUES__;

  IF (SELECT COUNT(*) FROM `tmp_test_tenant1_role_source`) <> 60 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Unexpected tenant-1 source role count';
  END IF;

  IF EXISTS (
    SELECT `role`.`code`
    FROM `system_role` AS `role`
    JOIN `tmp_test_tenant1_role_source` AS `source`
      ON `role`.`code` = `source`.`code`
    WHERE `role`.`tenant_id` = 1 AND `role`.`deleted` = b'0'
    GROUP BY `role`.`code`
    HAVING COUNT(*) > 1
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Duplicate active target role code in tenant 1';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_test_tenant1_role_category_target`;
  CREATE TEMPORARY TABLE `tmp_test_tenant1_role_category_target` (
    `category_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    `category_id` bigint NOT NULL,
    PRIMARY KEY (`category_code`)
  );

  INSERT INTO `tmp_test_tenant1_role_category_target` (`category_code`, `category_id`)
  SELECT `source`.`category_code`, MIN(`category`.`id`)
  FROM (
    SELECT DISTINCT `category_code`
    FROM `tmp_test_tenant1_role_source`
    WHERE `category_code` IS NOT NULL
  ) AS `source`
  JOIN `system_role_category` AS `category`
    ON `category`.`code` = `source`.`category_code`
   AND `category`.`tenant_id` = 1
   AND `category`.`deleted` = b'0'
   AND `category`.`status` = 0
  GROUP BY `source`.`category_code`
  HAVING COUNT(*) = 1;

  IF EXISTS (
    SELECT 1
    FROM `tmp_test_tenant1_role_source` AS `source`
    LEFT JOIN `tmp_test_tenant1_role_category_target` AS `category`
      ON `category`.`category_code` = `source`.`category_code`
    WHERE `source`.`category_code` IS NOT NULL
      AND `category`.`category_id` IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing or duplicate target role category code';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_test_tenant1_missing_menu_source`;
  CREATE TEMPORARY TABLE `tmp_test_tenant1_missing_menu_source` (
    `source_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    `permission` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    `type` tinyint NOT NULL,
    `sort` int NOT NULL,
    `parent_source_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
    `parent_target_id_hint` bigint NOT NULL,
    `parent_permission` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    `parent_type` tinyint NOT NULL,
    `parent_path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
    `parent_component` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
    `parent_component_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
    `path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
    `component` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
    `component_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
    `status` tinyint NOT NULL,
    `visible` tinyint NOT NULL,
    PRIMARY KEY (`source_key`)
  );

  INSERT INTO `tmp_test_tenant1_missing_menu_source`
    (`source_key`, `name`, `permission`, `type`, `sort`, `parent_source_key`, `parent_target_id_hint`,
     `parent_permission`, `parent_type`, `parent_path`, `parent_component`, `parent_component_name`,
     `path`, `component`, `component_name`, `status`, `visible`)
  VALUES
__MISSING_MENU_VALUES__;

  IF (SELECT COUNT(DISTINCT `permission`) FROM `tmp_test_tenant1_missing_menu_source`) <> 12 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Unexpected missing source permission count';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `tmp_test_tenant1_missing_menu_source` AS `source`
    WHERE EXISTS (
      SELECT 1 FROM `system_menu` AS `permission_menu`
      WHERE `permission_menu`.`permission` = `source`.`permission`
        AND `permission_menu`.`deleted` = b'0'
        AND `permission_menu`.`status` = 0
    )
    AND NOT EXISTS (
      SELECT 1 FROM `system_menu` AS `exact_menu`
      WHERE `exact_menu`.`permission` = `source`.`permission`
        AND `exact_menu`.`type` = `source`.`type`
        AND (`exact_menu`.`path` <=> `source`.`path`)
        AND (`exact_menu`.`component` <=> `source`.`component`)
        AND (`exact_menu`.`component_name` <=> `source`.`component_name`)
        AND `exact_menu`.`name` COLLATE utf8mb4_unicode_ci = `source`.`name`
        AND `exact_menu`.`deleted` = b'0'
        AND `exact_menu`.`status` = 0
    )
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Conflicting target menu for source missing permission';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `tmp_test_tenant1_missing_menu_source` AS `source`
    LEFT JOIN `system_menu` AS `parent`
      ON `parent`.`id` = `source`.`parent_target_id_hint`
     AND `parent`.`deleted` = b'0'
     AND `parent`.`status` = 0
     AND `parent`.`type` = `source`.`parent_type`
     AND `parent`.`permission` = `source`.`parent_permission`
     AND (`parent`.`path` <=> `source`.`parent_path`)
     AND (`parent`.`component` <=> `source`.`parent_component`)
     AND (`parent`.`component_name` <=> `source`.`parent_component_name`)
    WHERE `source`.`parent_source_key` IS NULL
      AND `parent`.`id` IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing stable parent contract for source menu';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_test_tenant1_role_permission_source`;
  CREATE TEMPORARY TABLE `tmp_test_tenant1_role_permission_source` (
    `role_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    `permission` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    `source_menu_type` tinyint NOT NULL,
    `source_path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
    `source_component` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
    `source_component_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
    PRIMARY KEY (`role_code`, `permission`)
  );

  INSERT INTO `tmp_test_tenant1_role_permission_source`
    (`role_code`, `permission`, `source_menu_type`, `source_path`, `source_component`, `source_component_name`)
  VALUES
__PERMISSION_VALUES__;

  IF (SELECT COUNT(*) FROM `tmp_test_tenant1_role_permission_source`) <> 1676 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Unexpected source role-permission count';
  END IF;

  START TRANSACTION;

  DROP TEMPORARY TABLE IF EXISTS `tmp_test_tenant1_missing_menu_target`;
  CREATE TEMPORARY TABLE `tmp_test_tenant1_missing_menu_target` (
    `source_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`source_key`)
  );

  WHILE (SELECT COUNT(*) FROM `tmp_test_tenant1_missing_menu_target`) <
        (SELECT COUNT(*) FROM `tmp_test_tenant1_missing_menu_source`) DO
    SET previous_menu_resolution_count = (SELECT COUNT(*) FROM `tmp_test_tenant1_missing_menu_target`);

    DROP TEMPORARY TABLE IF EXISTS `tmp_test_tenant1_missing_menu_parent_snapshot`;
    CREATE TEMPORARY TABLE `tmp_test_tenant1_missing_menu_parent_snapshot`
      LIKE `tmp_test_tenant1_missing_menu_target`;
    INSERT INTO `tmp_test_tenant1_missing_menu_parent_snapshot` (`source_key`, `menu_id`)
    SELECT `source_key`, `menu_id` FROM `tmp_test_tenant1_missing_menu_target`;

    INSERT IGNORE INTO `tmp_test_tenant1_missing_menu_target` (`source_key`, `menu_id`)
    SELECT `source`.`source_key`, MIN(`menu`.`id`)
    FROM `tmp_test_tenant1_missing_menu_source` AS `source`
    LEFT JOIN `tmp_test_tenant1_missing_menu_parent_snapshot` AS `parent_target`
      ON `parent_target`.`source_key` = `source`.`parent_source_key`
    JOIN `system_menu` AS `menu`
      ON `menu`.`permission` = `source`.`permission`
     AND `menu`.`type` = `source`.`type`
     AND (`menu`.`path` <=> `source`.`path`)
     AND (`menu`.`component` <=> `source`.`component`)
     AND (`menu`.`component_name` <=> `source`.`component_name`)
     AND `menu`.`name` COLLATE utf8mb4_unicode_ci = `source`.`name`
     AND `menu`.`parent_id` = COALESCE(`parent_target`.`menu_id`, `source`.`parent_target_id_hint`)
     AND `menu`.`deleted` = b'0'
     AND `menu`.`status` = 0
    WHERE `source`.`parent_source_key` IS NULL OR `parent_target`.`menu_id` IS NOT NULL
    GROUP BY `source`.`source_key`;

    DROP TEMPORARY TABLE IF EXISTS `tmp_test_tenant1_missing_menu_parent_snapshot`;
    CREATE TEMPORARY TABLE `tmp_test_tenant1_missing_menu_parent_snapshot`
      LIKE `tmp_test_tenant1_missing_menu_target`;
    INSERT INTO `tmp_test_tenant1_missing_menu_parent_snapshot` (`source_key`, `menu_id`)
    SELECT `source_key`, `menu_id` FROM `tmp_test_tenant1_missing_menu_target`;

    INSERT INTO `system_menu`
      (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
       `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
    SELECT `source`.`name`, `source`.`permission`, `source`.`type`, `source`.`sort`,
           COALESCE(`parent_target`.`menu_id`, `source`.`parent_target_id_hint`),
           `source`.`path`, '', `source`.`component`, `source`.`component_name`,
           `source`.`status`, CAST(`source`.`visible` AS UNSIGNED), b'1', b'1',
           'test-tenant1-role-permission-sync', NOW(), 'test-tenant1-role-permission-sync', NOW(), b'0'
    FROM `tmp_test_tenant1_missing_menu_source` AS `source`
    LEFT JOIN `tmp_test_tenant1_missing_menu_target` AS `existing_target`
      ON `existing_target`.`source_key` = `source`.`source_key`
    LEFT JOIN `tmp_test_tenant1_missing_menu_parent_snapshot` AS `parent_target`
      ON `parent_target`.`source_key` = `source`.`parent_source_key`
    WHERE `existing_target`.`menu_id` IS NULL
      AND (`source`.`parent_source_key` IS NULL OR `parent_target`.`menu_id` IS NOT NULL)
      AND NOT EXISTS (
        SELECT 1 FROM `system_menu` AS `existing_menu`
        WHERE `existing_menu`.`permission` = `source`.`permission`
          AND `existing_menu`.`type` = `source`.`type`
          AND (`existing_menu`.`path` <=> `source`.`path`)
          AND (`existing_menu`.`component` <=> `source`.`component`)
          AND (`existing_menu`.`component_name` <=> `source`.`component_name`)
          AND `existing_menu`.`name` COLLATE utf8mb4_unicode_ci = `source`.`name`
          AND `existing_menu`.`parent_id` = COALESCE(`parent_target`.`menu_id`, `source`.`parent_target_id_hint`)
          AND `existing_menu`.`deleted` = b'0'
      );

    INSERT IGNORE INTO `tmp_test_tenant1_missing_menu_target` (`source_key`, `menu_id`)
    SELECT `source`.`source_key`, MIN(`menu`.`id`)
    FROM `tmp_test_tenant1_missing_menu_source` AS `source`
    LEFT JOIN `tmp_test_tenant1_missing_menu_parent_snapshot` AS `parent_target`
      ON `parent_target`.`source_key` = `source`.`parent_source_key`
    JOIN `system_menu` AS `menu`
      ON `menu`.`permission` = `source`.`permission`
     AND `menu`.`type` = `source`.`type`
     AND (`menu`.`path` <=> `source`.`path`)
     AND (`menu`.`component` <=> `source`.`component`)
     AND (`menu`.`component_name` <=> `source`.`component_name`)
     AND `menu`.`name` COLLATE utf8mb4_unicode_ci = `source`.`name`
     AND `menu`.`parent_id` = COALESCE(`parent_target`.`menu_id`, `source`.`parent_target_id_hint`)
     AND `menu`.`deleted` = b'0'
     AND `menu`.`status` = 0
    WHERE `source`.`parent_source_key` IS NULL OR `parent_target`.`menu_id` IS NOT NULL
    GROUP BY `source`.`source_key`;

    SET current_menu_resolution_count = (SELECT COUNT(*) FROM `tmp_test_tenant1_missing_menu_target`);
    IF current_menu_resolution_count = previous_menu_resolution_count THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing source permissions after target menu resolution';
    END IF;
  END WHILE;

  INSERT INTO `system_role` (`name`, `code`, `sort`, `category_id`, `data_scope`, `data_scope_dept_ids`, `status`, `type`, `remark`,
     `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT `source`.`name`, `source`.`code`, `source`.`sort`, `category`.`category_id`,
         `source`.`data_scope`, `source`.`data_scope_dept_ids`, `source`.`status`, `source`.`type`, `source`.`remark`,
         'test-tenant1-role-permission-sync', NOW(), 'test-tenant1-role-permission-sync', NOW(), b'0', 1
  FROM `tmp_test_tenant1_role_source` AS `source`
  LEFT JOIN `tmp_test_tenant1_role_category_target` AS `category`
    ON `category`.`category_code` = `source`.`category_code`
  WHERE NOT EXISTS (
    SELECT 1 FROM `system_role` AS `existing`
    WHERE `existing`.`tenant_id` = 1
      AND `existing`.`code` = `source`.`code`
      AND `existing`.`deleted` = b'0'
  );

  UPDATE `system_role` AS `role`
  JOIN `tmp_test_tenant1_role_source` AS `source`
    ON `role`.`code` = `source`.`code`
  LEFT JOIN `tmp_test_tenant1_role_category_target` AS `category`
    ON `category`.`category_code` = `source`.`category_code`
  SET `role`.`name` = `source`.`name`,
      `role`.`sort` = `source`.`sort`,
      `role`.`category_id` = `category`.`category_id`,
      `role`.`data_scope` = `source`.`data_scope`,
      `role`.`data_scope_dept_ids` = `source`.`data_scope_dept_ids`,
      `role`.`status` = `source`.`status`,
      `role`.`type` = `source`.`type`,
      `role`.`remark` = `source`.`remark`,
      `role`.`updater` = 'test-tenant1-role-permission-sync',
      `role`.`update_time` = NOW()
  WHERE `role`.`tenant_id` = 1 AND `role`.`deleted` = b'0';

  DROP TEMPORARY TABLE IF EXISTS `tmp_test_tenant1_role_target`;
  CREATE TEMPORARY TABLE `tmp_test_tenant1_role_target` (
    `role_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    `role_id` bigint NOT NULL,
    PRIMARY KEY (`role_code`),
    UNIQUE KEY (`role_id`)
  );

  INSERT INTO `tmp_test_tenant1_role_target` (`role_code`, `role_id`)
  SELECT `source`.`code`, `role`.`id`
  FROM `tmp_test_tenant1_role_source` AS `source`
  JOIN `system_role` AS `role`
    ON `role`.`code` = `source`.`code`
   AND `role`.`tenant_id` = 1
   AND `role`.`deleted` = b'0';

  IF (SELECT COUNT(*) FROM `tmp_test_tenant1_role_target`) <> 60 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Target role resolution did not produce 60 roles';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_test_tenant1_permission_menu_target`;
  CREATE TEMPORARY TABLE `tmp_test_tenant1_permission_menu_target` (
    `role_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    `permission` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`role_code`, `permission`)
  );

  INSERT INTO `tmp_test_tenant1_permission_menu_target` (`role_code`, `permission`, `menu_id`)
  SELECT `ranked`.`role_code`, `ranked`.`permission`, `ranked`.`menu_id`
  FROM (
    SELECT `desired`.`role_code`, `desired`.`permission`, `menu`.`id` AS `menu_id`,
           ROW_NUMBER() OVER (
             PARTITION BY `desired`.`role_code`, `desired`.`permission`
             ORDER BY
               CASE
                 WHEN `menu`.`type` = `desired`.`source_menu_type`
                  AND (`menu`.`path` <=> `desired`.`source_path`)
                  AND (`menu`.`component` <=> `desired`.`source_component`)
                  AND (`menu`.`component_name` <=> `desired`.`source_component_name`) THEN 0
                 WHEN `menu`.`type` = `desired`.`source_menu_type` THEN 1
                 ELSE 2
               END,
               `menu`.`id`
           ) AS `row_number`
    FROM `tmp_test_tenant1_role_permission_source` AS `desired`
    JOIN `system_menu` AS `menu`
      ON `menu`.`permission` = `desired`.`permission`
     AND `menu`.`deleted` = b'0'
     AND `menu`.`status` = 0
  ) AS `ranked`
  WHERE `ranked`.`row_number` = 1;

  IF (SELECT COUNT(*) FROM `tmp_test_tenant1_permission_menu_target`) <> 1676 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing source permissions after target menu resolution';
  END IF;

  UPDATE `system_role_menu` AS `role_menu`
  JOIN `system_role` AS `role`
    ON `role`.`id` = `role_menu`.`role_id`
   AND `role`.`tenant_id` = `role_menu`.`tenant_id`
  JOIN `tmp_test_tenant1_role_source` AS `source_role`
    ON `role`.`code` = `source_role`.`code`
  JOIN `system_menu` AS `menu`
    ON `menu`.`id` = `role_menu`.`menu_id`
   AND `menu`.`deleted` = b'0'
   AND `menu`.`status` = 0
  LEFT JOIN `tmp_test_tenant1_role_permission_source` AS `desired`
    ON `desired`.`role_code` = `source_role`.`code`
   AND `desired`.`permission` = `menu`.`permission`
  SET `role_menu`.`deleted` = b'1',
      `role_menu`.`updater` = 'test-tenant1-role-permission-sync',
      `role_menu`.`update_time` = NOW()
  WHERE `role`.`tenant_id` = 1
    AND `role_menu`.`tenant_id` = 1
    AND `role_menu`.`deleted` = b'0'
    AND `menu`.`permission` <> ''
    AND `desired`.`permission` IS NULL;

  DROP TEMPORARY TABLE IF EXISTS `tmp_test_tenant1_role_menu_desired`;
  CREATE TEMPORARY TABLE `tmp_test_tenant1_role_menu_desired` (
    `role_id` bigint NOT NULL,
    `role_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    `permission` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`role_id`, `permission`)
  );

  INSERT INTO `tmp_test_tenant1_role_menu_desired` (`role_id`, `role_code`, `permission`, `menu_id`)
  SELECT `role`.`role_id`, `permission_menu`.`role_code`, `permission_menu`.`permission`, `permission_menu`.`menu_id`
  FROM `tmp_test_tenant1_permission_menu_target` AS `permission_menu`
  JOIN `tmp_test_tenant1_role_target` AS `role`
    ON `role`.`role_code` = `permission_menu`.`role_code`;

  DROP TEMPORARY TABLE IF EXISTS `tmp_test_tenant1_role_menu_missing`;
  CREATE TEMPORARY TABLE `tmp_test_tenant1_role_menu_missing` AS
  SELECT `desired`.`role_id`, `desired`.`role_code`, `desired`.`permission`, `desired`.`menu_id`
  FROM `tmp_test_tenant1_role_menu_desired` AS `desired`
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` AS `existing`
    JOIN `system_menu` AS `existing_menu`
      ON `existing_menu`.`id` = `existing`.`menu_id`
     AND `existing_menu`.`deleted` = b'0'
     AND `existing_menu`.`status` = 0
    WHERE `existing`.`role_id` = `desired`.`role_id`
      AND `existing`.`tenant_id` = 1
      AND `existing`.`deleted` = b'0'
      AND `existing_menu`.`permission` = `desired`.`permission`
  );

  DROP TEMPORARY TABLE IF EXISTS `tmp_test_tenant1_role_menu_restore`;
  CREATE TEMPORARY TABLE `tmp_test_tenant1_role_menu_restore` (
    `id` bigint NOT NULL,
    PRIMARY KEY (`id`)
  );

  INSERT INTO `tmp_test_tenant1_role_menu_restore` (`id`)
  SELECT MIN(`existing`.`id`)
  FROM `tmp_test_tenant1_role_menu_missing` AS `desired`
  JOIN `system_role_menu` AS `existing`
    ON `existing`.`role_id` = `desired`.`role_id`
   AND `existing`.`menu_id` = `desired`.`menu_id`
   AND `existing`.`tenant_id` = 1
   AND `existing`.`deleted` = b'1'
  GROUP BY `desired`.`role_id`, `desired`.`menu_id`;

  UPDATE `system_role_menu` AS `role_menu`
  JOIN `tmp_test_tenant1_role_menu_restore` AS `restore`
    ON `restore`.`id` = `role_menu`.`id`
  SET `role_menu`.`deleted` = b'0',
      `role_menu`.`updater` = 'test-tenant1-role-permission-sync',
      `role_menu`.`update_time` = NOW();

  INSERT INTO `system_role_menu`
    (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT `desired`.`role_id`, `desired`.`menu_id`,
         'test-tenant1-role-permission-sync', NOW(), 'test-tenant1-role-permission-sync', NOW(), b'0', 1
  FROM `tmp_test_tenant1_role_menu_missing` AS `desired`
  WHERE NOT EXISTS (
    SELECT 1 FROM `system_role_menu` AS `existing`
    WHERE `existing`.`role_id` = `desired`.`role_id`
      AND `existing`.`menu_id` = `desired`.`menu_id`
      AND `existing`.`tenant_id` = 1
      AND `existing`.`deleted` = b'0'
  );

  DROP TEMPORARY TABLE IF EXISTS `tmp_test_tenant1_role_menu_ancestor`;
  CREATE TEMPORARY TABLE `tmp_test_tenant1_role_menu_ancestor` (
    `role_id` bigint NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`role_id`, `menu_id`)
  );

  INSERT IGNORE INTO `tmp_test_tenant1_role_menu_ancestor` (`role_id`, `menu_id`)
  WITH RECURSIVE `ancestor_tree` AS (
    SELECT `desired`.`role_id`, `parent`.`id` AS `menu_id`, `parent`.`parent_id`, `parent`.`permission`, 1 AS `depth`
    FROM `tmp_test_tenant1_role_menu_desired` AS `desired`
    JOIN `system_menu` AS `menu` ON `menu`.`id` = `desired`.`menu_id`
    JOIN `system_menu` AS `parent`
      ON `parent`.`id` = `menu`.`parent_id`
     AND `parent`.`deleted` = b'0'
     AND `parent`.`status` = 0
    UNION ALL
    SELECT `tree`.`role_id`, `parent`.`id`, `parent`.`parent_id`, `parent`.`permission`, `tree`.`depth` + 1
    FROM `ancestor_tree` AS `tree`
    JOIN `system_menu` AS `parent`
      ON `parent`.`id` = `tree`.`parent_id`
     AND `parent`.`deleted` = b'0'
     AND `parent`.`status` = 0
    WHERE `tree`.`depth` < 20
  )
  SELECT DISTINCT `tree`.`role_id`, `tree`.`menu_id`
  FROM `ancestor_tree` AS `tree`
  WHERE `tree`.`permission` = '' OR `tree`.`permission` IS NULL;

  UPDATE `system_role_menu` AS `role_menu`
  JOIN `tmp_test_tenant1_role_menu_ancestor` AS `ancestor`
    ON `ancestor`.`role_id` = `role_menu`.`role_id`
   AND `ancestor`.`menu_id` = `role_menu`.`menu_id`
  SET `role_menu`.`deleted` = b'0',
      `role_menu`.`updater` = 'test-tenant1-role-permission-sync',
      `role_menu`.`update_time` = NOW()
  WHERE `role_menu`.`tenant_id` = 1 AND `role_menu`.`deleted` = b'1';

  INSERT INTO `system_role_menu`
    (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT `ancestor`.`role_id`, `ancestor`.`menu_id`,
         'test-tenant1-role-permission-sync', NOW(), 'test-tenant1-role-permission-sync', NOW(), b'0', 1
  FROM `tmp_test_tenant1_role_menu_ancestor` AS `ancestor`
  WHERE NOT EXISTS (
    SELECT 1 FROM `system_role_menu` AS `existing`
    WHERE `existing`.`role_id` = `ancestor`.`role_id`
      AND `existing`.`menu_id` = `ancestor`.`menu_id`
      AND `existing`.`tenant_id` = 1
      AND `existing`.`deleted` = b'0'
  );

  IF EXISTS (
    SELECT 1
    FROM `tmp_test_tenant1_role_permission_source` AS `desired`
    JOIN `tmp_test_tenant1_role_target` AS `target_role`
      ON `target_role`.`role_code` = `desired`.`role_code`
    WHERE NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` AS `role_menu`
      JOIN `system_menu` AS `menu`
        ON `menu`.`id` = `role_menu`.`menu_id`
       AND `menu`.`deleted` = b'0'
       AND `menu`.`status` = 0
      WHERE `role_menu`.`role_id` = `target_role`.`role_id`
        AND `role_menu`.`tenant_id` = 1
        AND `role_menu`.`deleted` = b'0'
        AND `menu`.`permission` = `desired`.`permission`
    )
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Target role is missing a source permission after sync';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_role` AS `role`
    JOIN `tmp_test_tenant1_role_source` AS `source_role`
      ON `role`.`code` = `source_role`.`code`
    JOIN `system_role_menu` AS `role_menu`
      ON `role_menu`.`role_id` = `role`.`id`
     AND `role_menu`.`tenant_id` = `role`.`tenant_id`
     AND `role_menu`.`deleted` = b'0'
    JOIN `system_menu` AS `menu`
      ON `menu`.`id` = `role_menu`.`menu_id`
     AND `menu`.`deleted` = b'0'
     AND `menu`.`status` = 0
    LEFT JOIN `tmp_test_tenant1_role_permission_source` AS `desired`
      ON `desired`.`role_code` = `source_role`.`code`
     AND `desired`.`permission` = `menu`.`permission`
    WHERE `role`.`tenant_id` = 1
      AND `role`.`deleted` = b'0'
      AND `menu`.`permission` <> ''
      AND `desired`.`permission` IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Target role retains an extra effective permission after sync';
  END IF;

  COMMIT;

  DROP TEMPORARY TABLE IF EXISTS `tmp_test_tenant1_role_menu_ancestor`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_test_tenant1_role_menu_restore`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_test_tenant1_role_menu_missing`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_test_tenant1_role_menu_desired`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_test_tenant1_permission_menu_target`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_test_tenant1_role_target`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_test_tenant1_missing_menu_parent_snapshot`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_test_tenant1_missing_menu_target`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_test_tenant1_role_permission_source`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_test_tenant1_missing_menu_source`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_test_tenant1_role_category_target`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_test_tenant1_role_source`;
END//
DELIMITER ;

CALL sync_test_tenant1_all_role_permissions();

DROP PROCEDURE IF EXISTS sync_test_tenant1_all_role_permissions;
'@

$sql = $template.Replace('__ROLE_VALUES__', $roleValues)
$sql = $sql.Replace('__MISSING_MENU_VALUES__', $missingMenuValues)
$sql = $sql.Replace('__PERMISSION_VALUES__', $permissionValues)
[System.IO.File]::WriteAllText($OutputPath, $sql, [System.Text.UTF8Encoding]::new($false))

$summary = [ordered]@{
    outputPath = $OutputPath
    sourceRoleCount = $roleDefinitions.Count
    sourceRolePermissionCount = $pairGroups.Count
    missingPermissionCount = $missingPermissions.Count
    missingMenuDefinitionCount = $missingMenus.Count
    sha256 = (Get-FileHash -Algorithm SHA256 $OutputPath).Hash.ToLowerInvariant()
}
$summary | ConvertTo-Json
