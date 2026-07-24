-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=menu; riskLevel=low
-- 电子签名治理台菜单权限种子。
-- 前置条件：已执行 20260520_dcc_signature_menu_restore.sql，存在 DCC 电子签名管理菜单 6815。

SET @signature_governance_parent_menu_id := (
    SELECT `id`
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `id` = 6815
      AND `permission` = 'dcc:controlled-file:signature:manage'
    LIMIT 1
);

DROP PROCEDURE IF EXISTS `_signature_governance_menu_require_parent`;
DELIMITER //
CREATE PROCEDURE `_signature_governance_menu_require_parent`()
BEGIN
    IF @signature_governance_parent_menu_id IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Missing prerequisite menu 6815 dcc:controlled-file:signature:manage';
    END IF;
END//
DELIMITER ;
CALL `_signature_governance_menu_require_parent`();
DROP PROCEDURE `_signature_governance_menu_require_parent`;

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900210, '签名治理策略查询', 'signature-governance:policy:query', 3, 1, @signature_governance_parent_menu_id,
       '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @signature_governance_parent_menu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `system_menu`
      WHERE `deleted` = b'0' AND `permission` = 'signature-governance:policy:query'
  );

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900211, '签名治理策略管理', 'signature-governance:policy:manage', 3, 2, @signature_governance_parent_menu_id,
       '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @signature_governance_parent_menu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `system_menu`
      WHERE `deleted` = b'0' AND `permission` = 'signature-governance:policy:manage'
  );

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900212, '签名长期留存查询', 'signature-governance:retention:query', 3, 3, @signature_governance_parent_menu_id,
       '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @signature_governance_parent_menu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `system_menu`
      WHERE `deleted` = b'0' AND `permission` = 'signature-governance:retention:query'
  );

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900213, '签名长期留存管理', 'signature-governance:retention:manage', 3, 4, @signature_governance_parent_menu_id,
       '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @signature_governance_parent_menu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `system_menu`
      WHERE `deleted` = b'0' AND `permission` = 'signature-governance:retention:manage'
  );

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900214, '签名周期审阅查询', 'signature-governance:periodic-review:query', 3, 5, @signature_governance_parent_menu_id,
       '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @signature_governance_parent_menu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `system_menu`
      WHERE `deleted` = b'0' AND `permission` = 'signature-governance:periodic-review:query'
  );

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900215, '签名周期审阅管理', 'signature-governance:periodic-review:manage', 3, 6, @signature_governance_parent_menu_id,
       '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @signature_governance_parent_menu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `system_menu`
      WHERE `deleted` = b'0' AND `permission` = 'signature-governance:periodic-review:manage'
  );

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900216, '签名CSV质量包查询', 'signature-governance:csv-package:query', 3, 7, @signature_governance_parent_menu_id,
       '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @signature_governance_parent_menu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `system_menu`
      WHERE `deleted` = b'0' AND `permission` = 'signature-governance:csv-package:query'
  );

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900217, '签名CSV质量包管理', 'signature-governance:csv-package:manage', 3, 8, @signature_governance_parent_menu_id,
       '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @signature_governance_parent_menu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `system_menu`
      WHERE `deleted` = b'0' AND `permission` = 'signature-governance:csv-package:manage'
  );

INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `updater`, `tenant_id`)
SELECT src.`role_id`, menu.`id`, src.`creator`, src.`updater`, src.`tenant_id`
FROM `system_role_menu` src
JOIN `system_menu` menu
  ON menu.`deleted` = b'0'
 AND menu.`permission` IN (
     'signature-governance:policy:query',
     'signature-governance:policy:manage',
     'signature-governance:retention:query',
     'signature-governance:retention:manage',
     'signature-governance:periodic-review:query',
     'signature-governance:periodic-review:manage',
     'signature-governance:csv-package:query',
     'signature-governance:csv-package:manage'
 )
WHERE src.`menu_id` = @signature_governance_parent_menu_id
  AND src.`deleted` = b'0'
  AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` existing
      WHERE existing.`role_id` = src.`role_id`
        AND existing.`menu_id` = menu.`id`
        AND existing.`tenant_id` = src.`tenant_id`
        AND existing.`deleted` = b'0'
  );
