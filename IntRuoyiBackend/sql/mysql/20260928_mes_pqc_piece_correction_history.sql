-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260802_mes_pqc_inspection_task,20260811_mes_process_pool_pqc_repeat_review_constraint; type=schema; riskLevel=medium
-- Preserve every historical PQC piece detail while enforcing uniqueness of the current version.
-- No history DML. Code rollback may retain this constraint; restoring the old constraint requires
-- a verified pre-migration backup because multiple historical versions deliberately share its key.

DROP PROCEDURE IF EXISTS migrate_mes_pqc_piece_correction_history;
DELIMITER $$
CREATE PROCEDURE migrate_mes_pqc_piece_correction_history()
BEGIN
  DECLARE duplicate_count bigint DEFAULT 0;
  DECLARE index_columns varchar(255) DEFAULT NULL;
  DECLARE index_non_unique int DEFAULT NULL;
  DECLARE index_prefixes int DEFAULT 0;

  IF (SELECT COUNT(*) FROM information_schema.columns
       WHERE table_schema = DATABASE() AND table_name = 'mes_pqc_inspection_piece_detail'
         AND column_name IN ('tenant_id', 'task_id', 'sample_no', 'item_code', 'deleted')) <> 5 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing PQC piece detail prerequisites';
  END IF;

  SELECT COUNT(*) INTO duplicate_count FROM (
    SELECT tenant_id, task_id, sample_no, item_code
      FROM mes_pqc_inspection_piece_detail WHERE deleted = 0
     GROUP BY tenant_id, task_id, sample_no, item_code HAVING COUNT(*) > 1
  ) duplicate_current;
  IF duplicate_count > 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Duplicate current PQC pieces require explicit resolution';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                  WHERE table_schema = DATABASE() AND table_name = 'mes_pqc_inspection_piece_detail'
                    AND column_name = 'current_sample_no') THEN
    ALTER TABLE mes_pqc_inspection_piece_detail
      ADD COLUMN current_sample_no int GENERATED ALWAYS AS
        (CASE WHEN deleted = 0 THEN sample_no ELSE NULL END) STORED
        COMMENT 'Current sample identity; historical versions are excluded from current uniqueness';
  ELSEIF NOT EXISTS (SELECT 1 FROM information_schema.columns
                  WHERE table_schema = DATABASE() AND table_name = 'mes_pqc_inspection_piece_detail'
                    AND column_name = 'current_sample_no' AND data_type = 'int'
                    AND extra LIKE '%STORED GENERATED%'
                    AND REPLACE(REPLACE(REPLACE(REPLACE(LOWER(generation_expression), '`', ''),
                        ' ', ''), '(', ''), ')', '') = 'casewhendeleted=0thensample_noelsenullend') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Existing current_sample_no definition is incompatible';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                  WHERE table_schema = DATABASE() AND table_name = 'mes_pqc_inspection_piece_detail'
                    AND index_name = 'uk_mes_pqc_piece_current') THEN
    ALTER TABLE mes_pqc_inspection_piece_detail
      ADD UNIQUE KEY uk_mes_pqc_piece_current (tenant_id, task_id, current_sample_no, item_code);
  END IF;
  SELECT GROUP_CONCAT(column_name ORDER BY seq_in_index SEPARATOR ','), MIN(non_unique),
         SUM(CASE WHEN sub_part IS NOT NULL THEN 1 ELSE 0 END)
    INTO index_columns, index_non_unique, index_prefixes
    FROM information_schema.statistics
   WHERE table_schema = DATABASE() AND table_name = 'mes_pqc_inspection_piece_detail'
     AND index_name = 'uk_mes_pqc_piece_current';
  IF index_columns IS NULL OR index_columns <> 'tenant_id,task_id,current_sample_no,item_code'
      OR index_non_unique <> 0 OR index_prefixes <> 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Current PQC piece unique index is incompatible';
  END IF;

  IF EXISTS (SELECT 1 FROM information_schema.statistics
              WHERE table_schema = DATABASE() AND table_name = 'mes_pqc_inspection_piece_detail'
                AND index_name = 'uk_mes_pqc_piece_item') THEN
    SELECT GROUP_CONCAT(column_name ORDER BY seq_in_index SEPARATOR ','), MIN(non_unique)
      INTO index_columns, index_non_unique FROM information_schema.statistics
     WHERE table_schema = DATABASE() AND table_name = 'mes_pqc_inspection_piece_detail'
       AND index_name = 'uk_mes_pqc_piece_item';
    IF index_columns <> 'tenant_id,task_id,sample_no,item_code,deleted' OR index_non_unique <> 0 THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Legacy PQC piece unique index is incompatible';
    END IF;
    ALTER TABLE mes_pqc_inspection_piece_detail DROP INDEX uk_mes_pqc_piece_item;
  END IF;
END$$
DELIMITER ;

CALL migrate_mes_pqc_piece_correction_history();
DROP PROCEDURE IF EXISTS migrate_mes_pqc_piece_correction_history;
