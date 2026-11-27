-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260908_system_electronic_signature_t3; type=schema; riskLevel=medium; requiresTargetPreflight=true
-- 与 eDHR review_opinion 的 1000 UTF-16 单元输入上限一致，不改写历史签名及证据哈希。
-- 仅接受已核对的 varchar(500)/varchar(1000)、NOT NULL、utf8mb4_unicode_ci；未知结构失败，禁止缩小更宽列。
DELIMITER $$
CREATE PROCEDURE migrate_20261126_signature_reason_capacity()
BEGIN
  IF (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'system_electronic_signature'
        AND column_name = 'reason' AND column_type IN ('varchar(500)', 'varchar(1000)')
        AND is_nullable = 'NO' AND character_set_name = 'utf8mb4'
        AND collation_name = 'utf8mb4_unicode_ci') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Unexpected signature reason schema; stop without altering data';
  END IF;
  IF (SELECT character_maximum_length FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'system_electronic_signature'
        AND column_name = 'reason') = 500 THEN
    ALTER TABLE `system_electronic_signature`
      MODIFY COLUMN `reason` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
      NOT NULL COMMENT '签名理由';
  END IF;
END$$
CALL migrate_20261126_signature_reason_capacity()$$
DROP PROCEDURE migrate_20261126_signature_reason_capacity$$
DELIMITER ;
