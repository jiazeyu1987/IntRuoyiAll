-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260830_mes_edhr_nonconformance_review_mvp,20260924_mes_edhr_deviation_management; type=schema; riskLevel=medium
-- P4: persist QA transfer signature, selected critical-deviation snapshot and replay identity.

DROP PROCEDURE IF EXISTS ensure_mes_edhr_deviation_ncr_schema;
DELIMITER $$
CREATE PROCEDURE ensure_mes_edhr_deviation_ncr_schema()
BEGIN
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'mes_pro_edhr_nonconformance_review'
        AND column_name = 'qa_create_signature_id'
  ) THEN
    ALTER TABLE `mes_pro_edhr_nonconformance_review`
      ADD COLUMN `qa_create_signature_id` bigint DEFAULT NULL COMMENT 'QA转审发起电子签名ID' AFTER `qa_signature`;
  END IF;
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'mes_pro_edhr_nonconformance_review'
        AND column_name = 'deviation_ids_json'
  ) THEN
    ALTER TABLE `mes_pro_edhr_nonconformance_review`
      ADD COLUMN `deviation_ids_json` json DEFAULT NULL COMMENT '本次关联关键偏差ID快照' AFTER `qa_create_signature_id`;
  END IF;
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'mes_pro_edhr_nonconformance_review'
        AND column_name = 'idempotency_key'
  ) THEN
    ALTER TABLE `mes_pro_edhr_nonconformance_review`
      ADD COLUMN `idempotency_key` varchar(128) DEFAULT NULL COMMENT '转审幂等键' AFTER `deviation_ids_json`;
  END IF;
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'mes_pro_edhr_nonconformance_review'
        AND column_name = 'payload_hash'
  ) THEN
    ALTER TABLE `mes_pro_edhr_nonconformance_review`
      ADD COLUMN `payload_hash` char(64) DEFAULT NULL COMMENT '转审规范化载荷摘要' AFTER `idempotency_key`;
  END IF;
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.statistics
      WHERE table_schema = DATABASE() AND table_name = 'mes_pro_edhr_nonconformance_review'
        AND index_name = 'uk_mes_edhr_ncr_idempotency'
  ) THEN
    ALTER TABLE `mes_pro_edhr_nonconformance_review`
      ADD UNIQUE KEY `uk_mes_edhr_ncr_idempotency` (`tenant_id`, `idempotency_key`);
  END IF;
END$$
DELIMITER ;

START TRANSACTION;
CALL ensure_mes_edhr_deviation_ncr_schema();
COMMIT;
DROP PROCEDURE IF EXISTS ensure_mes_edhr_deviation_ncr_schema;

DROP PROCEDURE IF EXISTS ensure_mes_edhr_deviation_ncr_permission;
DELIMITER $$
CREATE PROCEDURE ensure_mes_edhr_deviation_ncr_permission()
BEGIN
  IF NOT EXISTS (
      SELECT 1 FROM `system_menu`
      WHERE `id` = 5700 AND `path` = 'pro' AND `type` = 1 AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing MES production management menu 5700';
  END IF;
  IF EXISTS (
      SELECT 1 FROM `system_menu`
      WHERE `id` = 9008442
        AND NOT (`parent_id` = 5700
          AND `permission` = 'mes:pro-edhr-nonconformance-review:deviation-create'
          AND `type` = 3)
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Deviation NCR permission menu ID is already used';
  END IF;
  IF EXISTS (
      SELECT 1 FROM `system_menu`
      WHERE `deleted` = b'0'
        AND `permission` = 'mes:pro-edhr-nonconformance-review:deviation-create'
        AND `id` <> 9008442
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Deviation NCR permission already belongs to another menu';
  END IF;
  INSERT INTO `system_menu`
    (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  VALUES
    (9008442, '关键偏差转不合格评审', 'mes:pro-edhr-nonconformance-review:deviation-create', 3, 998, 5700, '', '', '', '', 0, b'0', b'0', b'0', 'edhr-deviation-ncr', NOW(), 'edhr-deviation-ncr', NOW(), b'0')
  ON DUPLICATE KEY UPDATE
    `name` = VALUES(`name`), `permission` = VALUES(`permission`), `type` = VALUES(`type`),
    `sort` = VALUES(`sort`), `parent_id` = VALUES(`parent_id`), `status` = VALUES(`status`),
    `visible` = VALUES(`visible`), `keep_alive` = VALUES(`keep_alive`), `always_show` = VALUES(`always_show`),
    `updater` = VALUES(`updater`), `update_time` = VALUES(`update_time`), `deleted` = VALUES(`deleted`);
END$$
DELIMITER ;

START TRANSACTION;
CALL ensure_mes_edhr_deviation_ncr_permission();
COMMIT;
DROP PROCEDURE IF EXISTS ensure_mes_edhr_deviation_ncr_permission;
