-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260624_unified_electronic_signature_menu; type=menu; riskLevel=medium
-- 合并电子签名“文件签名记录 / 批记录签名记录”为统一“签名记录”菜单。
-- 仅调整菜单与角色绑定，不创建未确认签名来源数据。

SET NAMES utf8mb4;

SET @unified_signature_records_menu_id := 900411;
SET @legacy_batch_signature_menu_id := 900412;

DROP PROCEDURE IF EXISTS `_unified_signature_records_menu_require_entries`;
DELIMITER //
CREATE PROCEDURE `_unified_signature_records_menu_require_entries`()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM `system_menu`
        WHERE `id` = @unified_signature_records_menu_id
          AND `deleted` = b'0'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Missing unified signature records menu 900411; apply unified electronic signature menu first';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM `system_menu`
        WHERE `id` = @legacy_batch_signature_menu_id
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Missing legacy batch signature menu 900412; cannot preserve legacy role bindings';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM `system_menu`
        WHERE `permission` = 'dcc:controlled-file:signature:manage'
          AND `deleted` = b'0'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Missing DCC signature permission menu; cannot preserve file signature permission';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM `system_menu`
        WHERE `permission` = 'mes:pro-batch-record-execution:signature-query'
          AND `deleted` = b'0'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Missing eDHR signature permission menu; cannot preserve batch signature permission';
    END IF;
END//
DELIMITER ;
CALL `_unified_signature_records_menu_require_entries`();
DROP PROCEDURE `_unified_signature_records_menu_require_entries`;

UPDATE `system_menu`
SET `name` = '签名记录',
    `permission` = 'signature-governance:policy:query',
    `path` = 'signature-records',
    `component` = 'signature-governance/index',
    `component_name` = 'SignatureGovernanceSignatureRecords',
    `visible` = b'1',
    `always_show` = b'0',
    `deleted` = b'0',
    `update_time` = NOW()
WHERE `id` = @unified_signature_records_menu_id
  AND `deleted` = b'0';

UPDATE `system_menu`
SET `name` = '批记录签名记录',
    `visible` = b'0',
    `deleted` = b'1',
    `update_time` = NOW()
WHERE `id` = @legacy_batch_signature_menu_id;

UPDATE `system_menu`
SET `parent_id` = @unified_signature_records_menu_id,
    `visible` = b'0',
    `always_show` = b'0',
    `deleted` = b'0',
    `update_time` = NOW()
WHERE `permission` IN (
    'dcc:controlled-file:signature:manage',
    'mes:pro-batch-record-execution:signature-query'
)
  AND `type` = 3
  AND `id` NOT IN (@unified_signature_records_menu_id, @legacy_batch_signature_menu_id);

INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT src.`role_id`,
       @unified_signature_records_menu_id,
       src.`creator`,
       NOW(),
       src.`updater`,
       NOW(),
       b'0',
       src.`tenant_id`
FROM `system_role_menu` src
JOIN (SELECT @legacy_batch_signature_menu_id AS legacy_role_menu) legacy_role_menu_marker
WHERE src.`menu_id` IN (@unified_signature_records_menu_id, @legacy_batch_signature_menu_id)
  AND src.`deleted` = b'0'
  AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` existing
      WHERE existing.`role_id` = src.`role_id`
        AND existing.`menu_id` = @unified_signature_records_menu_id
        AND existing.`tenant_id` = src.`tenant_id`
        AND existing.`deleted` = b'0'
  );

UPDATE `system_role_menu`
SET `deleted` = b'1',
    `update_time` = NOW()
WHERE `menu_id` = @legacy_batch_signature_menu_id
  AND `deleted` = b'0';
