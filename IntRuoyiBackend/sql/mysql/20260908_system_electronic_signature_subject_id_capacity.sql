-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260908_system_electronic_signature_t3; type=schema; riskLevel=medium
-- 统一电子签名对象身份容量修复：支持 DCC/BPM 完整 Base64 审批上下文。
DROP PROCEDURE IF EXISTS intruoyi_fix_esign_subject_id_capacity;

DELIMITER $$
CREATE PROCEDURE intruoyi_fix_esign_subject_id_capacity()
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
         WHERE table_schema = DATABASE()
           AND table_name = 'system_electronic_signature'
    ) AND EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
         WHERE table_schema = DATABASE()
           AND table_name = 'system_electronic_signature'
           AND column_name IN ('subject_id', 'idempotency_key')
    ) THEN
        IF EXISTS (
            SELECT 1 FROM information_schema.STATISTICS
             WHERE table_schema = DATABASE()
               AND table_name = 'system_electronic_signature'
               AND index_name = 'uk_system_esign_idempotency'
        ) THEN
            ALTER TABLE `system_electronic_signature` DROP INDEX `uk_system_esign_idempotency`;
        END IF;
        IF EXISTS (
            SELECT 1 FROM information_schema.STATISTICS
             WHERE table_schema = DATABASE()
               AND table_name = 'system_electronic_signature'
               AND index_name = 'idx_system_esign_subject'
        ) THEN
            ALTER TABLE `system_electronic_signature` DROP INDEX `idx_system_esign_subject`;
        END IF;
        ALTER TABLE `system_electronic_signature`
            MODIFY COLUMN `subject_id` varchar(2048) NOT NULL COMMENT '被签对象编号';
        ALTER TABLE `system_electronic_signature`
            MODIFY COLUMN `idempotency_key` varchar(512) NOT NULL COMMENT '幂等键';
        ALTER TABLE `system_electronic_signature`
            ADD UNIQUE INDEX `uk_system_esign_idempotency` (`tenant_id`, `idempotency_key`(191), `deleted`);
        ALTER TABLE `system_electronic_signature`
            ADD INDEX `idx_system_esign_subject` (`tenant_id`, `module_code`, `subject_type`, `subject_id`(191), `deleted`);
    END IF;
END$$
DELIMITER ;

CALL intruoyi_fix_esign_subject_id_capacity();
DROP PROCEDURE IF EXISTS intruoyi_fix_esign_subject_id_capacity;
