-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260520_dcc_signature_menu_restore,20260526_edhr_approval_archive_schema_contract,20260528_signature_governance_menu; type=menu; riskLevel=medium
-- 统一电子签名一级菜单。前置条件：旧文件签名菜单 6815、批记录签名菜单 900026 以及 signature-governance 权限项已存在或将由历史迁移创建。

SET NAMES utf8mb4;

SET @unified_signature_menu_id := 900218;
SET @unified_signature_overview_menu_id := 900410;
SET @unified_signature_file_menu_id := 900411;
SET @unified_signature_batch_menu_id := 900412;
SET @unified_signature_authorization_menu_id := 900413;
SET @unified_signature_retention_menu_id := 900414;
SET @unified_signature_periodic_review_menu_id := 900415;
SET @unified_signature_csv_package_menu_id := 900416;
SET @unified_signature_policy_menu_id := 900417;

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT @unified_signature_menu_id, '电子签名', 'signature-governance:policy:query', 2, 68, 0,
       '/signature-governance', 'ep:edit-pen', 'signature-governance/index', 'SignatureGovernanceWorkbench',
       0, b'1', b'1', b'1', 'unified-signature', NOW(), 'unified-signature', NOW(), b'0'
WHERE NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND (`id` = @unified_signature_menu_id OR `path` = '/signature-governance')
);

UPDATE `system_menu`
SET `name` = '电子签名',
    `type` = 2,
    `sort` = 68,
    `parent_id` = 0,
    `path` = '/signature-governance',
    `icon` = 'ep:edit-pen',
    `component` = 'signature-governance/index',
    `component_name` = 'SignatureGovernanceWorkbench',
    `visible` = b'1',
    `updater` = 'unified-signature',
    `update_time` = NOW()
WHERE `id` = @unified_signature_menu_id
  AND `deleted` = b'0';

DROP PROCEDURE IF EXISTS `_unified_signature_menu_require_parent`;
DELIMITER //
CREATE PROCEDURE `_unified_signature_menu_require_parent`()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM `system_menu`
        WHERE `deleted` = b'0'
          AND `id` = @unified_signature_menu_id
          AND `path` = '/signature-governance'
          AND `component` = 'signature-governance/index'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Missing unified electronic signature menu 900218 /signature-governance';
    END IF;
END//
DELIMITER ;
CALL `_unified_signature_menu_require_parent`();
DROP PROCEDURE `_unified_signature_menu_require_parent`;

DROP PROCEDURE IF EXISTS `_unified_signature_menu_require_children`;
DELIMITER //
CREATE PROCEDURE `_unified_signature_menu_require_children`()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM `system_menu`
        WHERE `deleted` = b'0'
          AND (
              (`id` = @unified_signature_overview_menu_id AND (`name` <> '总览' OR `path` <> 'overview' OR COALESCE(`component`, '') <> 'signature-governance/index' OR COALESCE(`component_name`, '') <> 'SignatureGovernanceOverview'))
              OR (`id` = @unified_signature_file_menu_id AND (`name` <> '文件签名记录' OR `path` <> 'file-signatures' OR COALESCE(`component`, '') <> 'signature-governance/index' OR COALESCE(`component_name`, '') <> 'SignatureGovernanceFileSignatures'))
              OR (`id` = @unified_signature_batch_menu_id AND (`name` <> '批记录签名记录' OR `path` <> 'batch-signatures' OR COALESCE(`component`, '') <> 'signature-governance/index' OR COALESCE(`component_name`, '') <> 'SignatureGovernanceBatchSignatures'))
              OR (`id` = @unified_signature_authorization_menu_id AND (`name` <> '用户授权' OR `path` <> 'authorizations' OR COALESCE(`component`, '') <> 'signature-governance/index' OR COALESCE(`component_name`, '') <> 'SignatureGovernanceAuthorizations'))
              OR (`id` = @unified_signature_retention_menu_id AND (`name` <> '长期留存' OR `path` <> 'retention' OR COALESCE(`component`, '') <> 'signature-governance/index' OR COALESCE(`component_name`, '') <> 'SignatureGovernanceRetention'))
              OR (`id` = @unified_signature_periodic_review_menu_id AND (`name` <> '周期复核' OR `path` <> 'periodic-review' OR COALESCE(`component`, '') <> 'signature-governance/index' OR COALESCE(`component_name`, '') <> 'SignatureGovernancePeriodicReview'))
              OR (`id` = @unified_signature_csv_package_menu_id AND (`name` <> 'CSV质量包' OR `path` <> 'csv-package' OR COALESCE(`component`, '') <> 'signature-governance/index' OR COALESCE(`component_name`, '') <> 'SignatureGovernanceCsvPackage'))
              OR (`id` = @unified_signature_policy_menu_id AND (`name` <> '统一策略' OR `path` <> 'policy' OR COALESCE(`component`, '') <> 'signature-governance/index' OR COALESCE(`component_name`, '') <> 'SignatureGovernancePolicy'))
          )
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Menu ids 900410-900417 are already used by another menu; cannot create unified electronic signature child menus';
    END IF;
END//
DELIMITER ;
CALL `_unified_signature_menu_require_children`();
DROP PROCEDURE `_unified_signature_menu_require_children`;

DROP TEMPORARY TABLE IF EXISTS `tmp_unified_signature_child_menu_ids`;
CREATE TEMPORARY TABLE `tmp_unified_signature_child_menu_ids` (
    `menu_id` BIGINT NOT NULL,
    PRIMARY KEY (`menu_id`)
);

INSERT INTO `tmp_unified_signature_child_menu_ids` (`menu_id`)
VALUES
    (900411),
    (900412),
    (900413),
    (900414),
    (900415),
    (900416),
    (900417);

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT child_def.`id`, child_def.`name`, child_def.`permission`, child_def.`type`, child_def.`sort`, child_def.`parent_id`,
       child_def.`path`, child_def.`icon`, child_def.`component`, child_def.`component_name`, child_def.`status`,
       child_def.`visible`, child_def.`keep_alive`, child_def.`always_show`, child_def.`creator`, NOW(),
       child_def.`updater`, NOW(), child_def.`deleted`
FROM (
    SELECT 900411 AS `id`, '文件签名记录' AS `name`, 'dcc:controlled-file:signature:manage' AS `permission`, 2 AS `type`,
           1 AS `sort`, @unified_signature_menu_id AS `parent_id`, 'file-signatures' AS `path`, 'ep:document-checked' AS `icon`,
           'signature-governance/index' AS `component`, 'SignatureGovernanceFileSignatures' AS `component_name`,
           0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`,
           'unified-signature' AS `creator`, 'unified-signature' AS `updater`, b'0' AS `deleted`
    UNION ALL
    SELECT 900412 AS `id`, '批记录签名记录' AS `name`, 'mes:pro-batch-record-execution:signature-query' AS `permission`, 2 AS `type`,
           2 AS `sort`, @unified_signature_menu_id AS `parent_id`, 'batch-signatures' AS `path`, 'ep:list' AS `icon`,
           'signature-governance/index' AS `component`, 'SignatureGovernanceBatchSignatures' AS `component_name`,
           0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`,
           'unified-signature' AS `creator`, 'unified-signature' AS `updater`, b'0' AS `deleted`
    UNION ALL
    SELECT 900413 AS `id`, '用户授权' AS `name`, 'dcc:controlled-file:signature:manage' AS `permission`, 2 AS `type`,
           3 AS `sort`, @unified_signature_menu_id AS `parent_id`, 'authorizations' AS `path`, 'ep:user' AS `icon`,
           'signature-governance/index' AS `component`, 'SignatureGovernanceAuthorizations' AS `component_name`,
           0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`,
           'unified-signature' AS `creator`, 'unified-signature' AS `updater`, b'0' AS `deleted`
    UNION ALL
    SELECT 900414 AS `id`, '长期留存' AS `name`, 'signature-governance:retention:query' AS `permission`, 2 AS `type`,
           4 AS `sort`, @unified_signature_menu_id AS `parent_id`, 'retention' AS `path`, 'ep:collection' AS `icon`,
           'signature-governance/index' AS `component`, 'SignatureGovernanceRetention' AS `component_name`,
           0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`,
           'unified-signature' AS `creator`, 'unified-signature' AS `updater`, b'0' AS `deleted`
    UNION ALL
    SELECT 900415 AS `id`, '周期复核' AS `name`, 'signature-governance:periodic-review:query' AS `permission`, 2 AS `type`,
           5 AS `sort`, @unified_signature_menu_id AS `parent_id`, 'periodic-review' AS `path`, 'ep:refresh' AS `icon`,
           'signature-governance/index' AS `component`, 'SignatureGovernancePeriodicReview' AS `component_name`,
           0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`,
           'unified-signature' AS `creator`, 'unified-signature' AS `updater`, b'0' AS `deleted`
    UNION ALL
    SELECT 900416 AS `id`, 'CSV质量包' AS `name`, 'signature-governance:csv-package:query' AS `permission`, 2 AS `type`,
           6 AS `sort`, @unified_signature_menu_id AS `parent_id`, 'csv-package' AS `path`, 'ep:document-copy' AS `icon`,
           'signature-governance/index' AS `component`, 'SignatureGovernanceCsvPackage' AS `component_name`,
           0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`,
           'unified-signature' AS `creator`, 'unified-signature' AS `updater`, b'0' AS `deleted`
    UNION ALL
    SELECT 900417 AS `id`, '统一策略' AS `name`, 'signature-governance:policy:query' AS `permission`, 2 AS `type`,
           7 AS `sort`, @unified_signature_menu_id AS `parent_id`, 'policy' AS `path`, 'ep:setting' AS `icon`,
           'signature-governance/index' AS `component`, 'SignatureGovernancePolicy' AS `component_name`,
           0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`,
           'unified-signature' AS `creator`, 'unified-signature' AS `updater`, b'0' AS `deleted`
) AS child_def
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `permission` = VALUES(`permission`),
  `type` = VALUES(`type`),
  `sort` = VALUES(`sort`),
  `parent_id` = VALUES(`parent_id`),
  `path` = VALUES(`path`),
  `icon` = VALUES(`icon`),
  `component` = VALUES(`component`),
  `component_name` = VALUES(`component_name`),
  `status` = VALUES(`status`),
  `visible` = VALUES(`visible`),
  `keep_alive` = VALUES(`keep_alive`),
  `always_show` = VALUES(`always_show`),
  `updater` = VALUES(`updater`),
  `update_time` = VALUES(`update_time`),
  `deleted` = VALUES(`deleted`);

UPDATE `system_menu`
SET `visible` = b'0',
    `deleted` = b'1',
    `updater` = 'unified-signature',
    `update_time` = NOW()
WHERE `id` = @unified_signature_overview_menu_id
  AND `deleted` = b'0'
  AND `name` = '总览'
  AND `path` = 'overview'
  AND COALESCE(`component`, '') = 'signature-governance/index'
  AND COALESCE(`component_name`, '') = 'SignatureGovernanceOverview';

UPDATE `system_menu`
SET `name` = '文件签名记录',
    `type` = 3,
    `sort` = 1,
    `parent_id` = @unified_signature_file_menu_id,
    `path` = '',
    `icon` = '',
    `component` = '',
    `component_name` = '',
    `visible` = b'1',
    `updater` = 'unified-signature',
    `update_time` = NOW()
WHERE `id` = 6815
  AND `permission` = 'dcc:controlled-file:signature:manage'
  AND `deleted` = b'0';

UPDATE `system_menu`
SET `name` = '批记录签名记录',
    `type` = 3,
    `sort` = 1,
    `parent_id` = @unified_signature_batch_menu_id,
    `path` = '',
    `icon` = '',
    `component` = '',
    `component_name` = '',
    `visible` = b'1',
    `updater` = 'unified-signature',
    `update_time` = NOW()
WHERE `id` = 900026
  AND `permission` = 'mes:pro-batch-record-execution:signature-query'
  AND `deleted` = b'0';

UPDATE `system_menu`
SET `parent_id` = CASE
        WHEN `permission` IN ('signature-governance:policy:query', 'signature-governance:policy:manage') THEN @unified_signature_policy_menu_id
        WHEN `permission` IN ('signature-governance:retention:query', 'signature-governance:retention:manage') THEN @unified_signature_retention_menu_id
        WHEN `permission` IN ('signature-governance:periodic-review:query', 'signature-governance:periodic-review:manage') THEN @unified_signature_periodic_review_menu_id
        WHEN `permission` IN ('signature-governance:csv-package:query', 'signature-governance:csv-package:manage') THEN @unified_signature_csv_package_menu_id
        ELSE `parent_id`
    END,
    `updater` = 'unified-signature',
    `update_time` = NOW()
WHERE `permission` IN (
    'signature-governance:policy:query',
    'signature-governance:policy:manage',
    'signature-governance:retention:query',
    'signature-governance:retention:manage',
    'signature-governance:periodic-review:query',
    'signature-governance:periodic-review:manage',
    'signature-governance:csv-package:query',
    'signature-governance:csv-package:manage'
)
  AND `deleted` = b'0'
  AND `type` = 3
  AND `id` <> @unified_signature_menu_id
  AND `id` NOT IN (
      @unified_signature_overview_menu_id,
      @unified_signature_file_menu_id,
      @unified_signature_batch_menu_id,
      @unified_signature_authorization_menu_id,
      @unified_signature_retention_menu_id,
      @unified_signature_periodic_review_menu_id,
      @unified_signature_csv_package_menu_id,
      @unified_signature_policy_menu_id
  );

INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT
    src.`role_id`,
    child.`menu_id`,
    'unified-signature',
    NOW(),
    'unified-signature',
    NOW(),
    b'0',
    src.`tenant_id`
FROM `system_role_menu` src
CROSS JOIN `tmp_unified_signature_child_menu_ids` child
WHERE src.`deleted` = b'0'
  AND src.`menu_id` = @unified_signature_menu_id
  AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` existing
      WHERE existing.`role_id` = src.`role_id`
        AND existing.`menu_id` = child.`menu_id`
        AND existing.`tenant_id` = src.`tenant_id`
        AND existing.`deleted` = b'0'
  );

UPDATE `system_role_menu`
SET `deleted` = b'1',
    `updater` = 'unified-signature',
    `update_time` = NOW()
WHERE `menu_id` = @unified_signature_overview_menu_id
  AND `deleted` = b'0';

DROP TEMPORARY TABLE IF EXISTS `tmp_unified_signature_child_menu_ids`;
