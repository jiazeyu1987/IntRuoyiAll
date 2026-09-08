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
           AND column_name = 'subject_id'
    ) THEN
        ALTER TABLE `system_electronic_signature`
            MODIFY COLUMN `subject_id` varchar(2048) NOT NULL COMMENT '被签对象编号';
    END IF;
END$$
DELIMITER ;

CALL intruoyi_fix_esign_subject_id_capacity();
DROP PROCEDURE IF EXISTS intruoyi_fix_esign_subject_id_capacity;
