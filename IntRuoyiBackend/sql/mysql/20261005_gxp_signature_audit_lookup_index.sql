-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260924_gxp_audit_event_v2; type=schema; riskLevel=medium
-- Formal signature evidence readers retain all integrity checks and duplicate detection.
-- This additive non-unique index prevents tenant-wide audit scans; no audit rows are rewritten.
DELIMITER $$
CREATE PROCEDURE `ensure_gxp_signature_lookup_index`()
BEGIN
    DECLARE existing_parts int DEFAULT 0;
    DECLARE correct_parts int DEFAULT 0;
    DECLARE required_columns int DEFAULT 0;
    SELECT COUNT(*) INTO required_columns
      FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'gxp_audit_event'
       AND COLUMN_NAME IN ('tenant_id', 'operation_id', 'signature_record_id', 'deleted');
    IF required_columns <> 4 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Formal audit lookup prerequisites missing';
    END IF;
    SELECT COUNT(*), COALESCE(SUM(
        NON_UNIQUE = 1 AND SUB_PART IS NULL AND INDEX_TYPE = 'BTREE' AND IS_VISIBLE = 'YES'
        AND ((SEQ_IN_INDEX = 1 AND COLUMN_NAME = 'tenant_id')
          OR (SEQ_IN_INDEX = 2 AND COLUMN_NAME = 'operation_id')
          OR (SEQ_IN_INDEX = 3 AND COLUMN_NAME = 'signature_record_id')
          OR (SEQ_IN_INDEX = 4 AND COLUMN_NAME = 'deleted'))), 0)
      INTO existing_parts, correct_parts
      FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'gxp_audit_event'
       AND INDEX_NAME = 'idx_gxp_event_signature_lookup';
    IF existing_parts = 0 THEN
        ALTER TABLE `gxp_audit_event`
            ADD INDEX `idx_gxp_event_signature_lookup`
                (`tenant_id`, `operation_id`, `signature_record_id`, `deleted`),
            ALGORITHM = INPLACE, LOCK = NONE;
    ELSEIF existing_parts <> 4 OR correct_parts <> 4 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Formal audit lookup index definition mismatch';
    END IF;
END$$
DELIMITER ;
CALL `ensure_gxp_signature_lookup_index`();
DROP PROCEDURE `ensure_gxp_signature_lookup_index`;
