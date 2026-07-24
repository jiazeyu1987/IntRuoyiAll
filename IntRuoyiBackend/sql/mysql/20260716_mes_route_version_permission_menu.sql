-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260715_mes_route_version_lifecycle; type=data; riskLevel=medium
-- MES 工艺路线版本：补齐候选版本工作区权限菜单、角色授权和租户套餐可见性。

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(5730, '工艺路线版本查询', 'mes:pro-route:version-query', 3, 10, 5720, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(5731, '工艺路线版本创建', 'mes:pro-route:version-create', 3, 11, 5720, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(5732, '工艺路线版本提交', 'mes:pro-route:version-submit', 3, 12, 5720, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(5733, '工艺路线版本取消', 'mes:pro-route:version-cancel', 3, 13, 5720, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(5734, '工艺路线版本发布', 'mes:pro-route:version-publish', 3, 14, 5720, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `permission` = VALUES(`permission`),
  `type` = VALUES(`type`),
  `sort` = VALUES(`sort`),
  `parent_id` = VALUES(`parent_id`),
  `status` = VALUES(`status`),
  `visible` = VALUES(`visible`),
  `deleted` = b'0',
  `updater` = '1',
  `update_time` = NOW();

INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT
       rm.`role_id`,
       target_menu.`menu_id`,
       '1', NOW(), '1', NOW(), b'0', rm.`tenant_id`
FROM `system_role_menu` rm
JOIN (
  SELECT 5721 AS `source_menu_id`, 5730 AS `menu_id`
  UNION ALL SELECT 5722, 5731
  UNION ALL SELECT 5723, 5732
  UNION ALL SELECT 5723, 5733
  UNION ALL SELECT 5723, 5734
) target_menu
  ON target_menu.`source_menu_id` = rm.`menu_id`
WHERE rm.`deleted` = b'0'
  AND rm.`menu_id` IN (5721, 5722, 5723)
  AND NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` existing
    WHERE existing.`role_id` = rm.`role_id`
      AND existing.`tenant_id` = rm.`tenant_id`
      AND existing.`menu_id` = target_menu.`menu_id`
      AND existing.`deleted` = b'0'
  );

DROP TEMPORARY TABLE IF EXISTS `tmp_mes_route_version_package_menu_ids`;
CREATE TEMPORARY TABLE `tmp_mes_route_version_package_menu_ids` (
  `package_id` bigint NOT NULL,
  `menu_id` bigint NOT NULL,
  PRIMARY KEY (`package_id`, `menu_id`)
);

INSERT IGNORE INTO `tmp_mes_route_version_package_menu_ids` (`package_id`, `menu_id`)
SELECT
  `package`.`id`,
  CAST(`existing_menu`.`menu_id` AS UNSIGNED)
FROM `system_tenant_package` AS `package`
JOIN JSON_TABLE(
  `package`.`menu_ids`,
  '$[*]' COLUMNS (`menu_id` bigint PATH '$')
) AS `existing_menu`
WHERE `package`.`deleted` = b'0'
  AND JSON_VALID(`package`.`menu_ids`)
  AND JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('5720' AS JSON), '$');

INSERT IGNORE INTO `tmp_mes_route_version_package_menu_ids` (`package_id`, `menu_id`)
SELECT
  `package`.`id`,
  `menu`.`id`
FROM `system_tenant_package` AS `package`
CROSS JOIN (
  SELECT 5730 AS `id`
  UNION ALL SELECT 5731
  UNION ALL SELECT 5732
  UNION ALL SELECT 5733
  UNION ALL SELECT 5734
) AS `menu`
WHERE `package`.`deleted` = b'0'
  AND JSON_VALID(`package`.`menu_ids`)
  AND JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('5720' AS JSON), '$');

UPDATE `system_tenant_package` AS `package`
JOIN (
  SELECT DISTINCT
    `package_id`,
    JSON_ARRAYAGG(`menu_id`) OVER (
      PARTITION BY `package_id`
      ORDER BY `menu_id`
      ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
    ) AS `menu_ids`
  FROM `tmp_mes_route_version_package_menu_ids`
) AS `merged`
  ON `merged`.`package_id` = `package`.`id`
SET `package`.`menu_ids` = `merged`.`menu_ids`,
    `package`.`updater` = '1',
    `package`.`update_time` = NOW();

DROP TEMPORARY TABLE IF EXISTS `tmp_mes_route_version_package_menu_ids`;
