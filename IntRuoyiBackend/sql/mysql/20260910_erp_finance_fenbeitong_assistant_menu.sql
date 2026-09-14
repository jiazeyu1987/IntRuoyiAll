-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260828_erp_finance_invoice_voucher_print_menu; type=menu; riskLevel=low
-- ERP 系统 / 财务管理 / 分贝通凭证

SET NAMES utf8mb4;
START TRANSACTION;

DROP PROCEDURE IF EXISTS ensure_erp_finance_fenbeitong_assistant_menu_20260910;
DELIMITER //
CREATE PROCEDURE ensure_erp_finance_fenbeitong_assistant_menu_20260910()
BEGIN
  IF NOT EXISTS (
    SELECT 1
      FROM `system_menu`
     WHERE `id` = 2645
       AND `status` = 0
       AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing enabled ERP finance menu 2645';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM `system_menu`
     WHERE `id` = 991200
       AND `deleted` = b'0'
       AND NOT (
         `path` = 'fenbeitong-voucher'
         AND `component` = 'erp/finance/fenbeitong-voucher/index'
         AND `component_name` = 'ErpFenbeitongVoucher'
       )
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Fenbeitong assistant menu id 991200 is already used by another route';
  END IF;

  INSERT INTO `system_menu` (
      `id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
      `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`
  ) VALUES
  (
      991200, '分贝通凭证', 'erp:fenbeitong-voucher:query', 2, 90, 2645, 'fenbeitong-voucher',
      'ep:wallet', 'erp/finance/fenbeitong-voucher/index', 'ErpFenbeitongVoucher',
      0, b'1', b'1', b'1', 'fenbeitong-assistant-integration', NOW(),
      'fenbeitong-assistant-integration', NOW(), b'0'
  ),
  (
      991201, '分贝通凭证查询', 'erp:fenbeitong-voucher:query', 3, 1, 991200, '', '', '', '',
      0, b'1', b'1', b'0', 'fenbeitong-assistant-integration', NOW(),
      'fenbeitong-assistant-integration', NOW(), b'0'
  ),
  (
      991202, '分贝通凭证配置', 'erp:fenbeitong-voucher:config', 3, 2, 991200, '', '', '', '',
      0, b'1', b'1', b'0', 'fenbeitong-assistant-integration', NOW(),
      'fenbeitong-assistant-integration', NOW(), b'0'
  ),
  (
      991203, '分贝通凭证保存', 'erp:fenbeitong-voucher:save', 3, 3, 991200, '', '', '', '',
      0, b'1', b'1', b'0', 'fenbeitong-assistant-integration', NOW(),
      'fenbeitong-assistant-integration', NOW(), b'0'
  )
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
END//
DELIMITER ;

CALL ensure_erp_finance_fenbeitong_assistant_menu_20260910();
DROP PROCEDURE IF EXISTS ensure_erp_finance_fenbeitong_assistant_menu_20260910;

COMMIT;
