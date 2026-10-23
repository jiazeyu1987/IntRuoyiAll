-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260710_dcc_product_catalog_database; type=schema; riskLevel=medium
-- IC12: global data_source sequence for uk_dcc_product_catalog_source_row(data_source,original_row_no).
-- Approved maintenance window: stop catalog writers, apply this sidecar, deploy every writer, then resume.
-- Historical catalog/seed rows are read only. Soft-deleted rows and all tenants contribute to MAX.
-- Rollback: preserve/export this sidecar; revert code only in the same approved maintenance window.
-- Do not re-enable MAX allocation or drop/recreate a sequence that has committed allocations.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS preflight_dcc_product_catalog_source_sequence;
DELIMITER //
CREATE PROCEDURE preflight_dcc_product_catalog_source_sequence()
BEGIN
  IF (SELECT COUNT(*) FROM information_schema.TABLES
      WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_product_catalog'
        AND ENGINE='InnoDB' AND TABLE_COLLATION='utf8mb4_unicode_ci')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC12 catalog table engine/collation missing or drift';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_product_catalog' AND COLUMN_NAME='data_source'
        AND LOWER(COLUMN_TYPE)='varchar(64)' AND IS_NULLABLE='NO'
        AND CHARACTER_SET_NAME='utf8mb4' AND COLLATION_NAME='utf8mb4_unicode_ci'
        AND COALESCE(GENERATION_EXPRESSION,'')='')<>1
     OR (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_product_catalog' AND COLUMN_NAME='original_row_no'
        AND LOWER(COLUMN_TYPE)='int' AND IS_NULLABLE='NO' AND COALESCE(GENERATION_EXPRESSION,'')='')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC12 catalog source/row definition drift';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_product_catalog'
        AND INDEX_NAME='uk_dcc_product_catalog_source_row')<>2
     OR (SELECT COUNT(*) FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_product_catalog'
        AND INDEX_NAME='uk_dcc_product_catalog_source_row' AND NON_UNIQUE=0
        AND ((SEQ_IN_INDEX=1 AND COLUMN_NAME='data_source') OR (SEQ_IN_INDEX=2 AND COLUMN_NAME='original_row_no'))
        AND SUB_PART IS NULL AND INDEX_TYPE='BTREE')<>2 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC12 catalog global source_row unique definition drift';
  END IF;
  IF EXISTS(SELECT 1 FROM dcc_product_catalog
            WHERE data_source IS NULL OR CHAR_LENGTH(data_source)=0
              OR REGEXP_LIKE(data_source,'^[[:space:]]|[[:space:]]$')
              OR original_row_no IS NULL OR original_row_no<1 OR original_row_no>2147483647)
     OR EXISTS(SELECT 1 FROM dcc_product_catalog GROUP BY data_source,original_row_no HAVING COUNT(*)>1) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC12 illegal historical source/row facts';
  END IF;
END//
DELIMITER ;
CALL preflight_dcc_product_catalog_source_sequence();
DROP PROCEDURE preflight_dcc_product_catalog_source_sequence;

CREATE TABLE IF NOT EXISTS dcc_product_catalog_source_sequence (
  data_source VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  last_row_no BIGINT NOT NULL,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (data_source)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS initialize_dcc_product_catalog_source_sequence;
DELIMITER //
CREATE PROCEDURE initialize_dcc_product_catalog_source_sequence()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;
  IF (SELECT COUNT(*) FROM information_schema.TABLES
      WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_product_catalog_source_sequence'
        AND ENGINE='InnoDB' AND TABLE_COLLATION='utf8mb4_unicode_ci')<>1
     OR (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_product_catalog_source_sequence')<>3 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC12 sequence table definition drift';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_product_catalog_source_sequence' AND COLUMN_NAME='data_source'
        AND LOWER(COLUMN_TYPE)='varchar(64)' AND IS_NULLABLE='NO' AND COLUMN_DEFAULT IS NULL
        AND CHARACTER_SET_NAME='utf8mb4' AND COLLATION_NAME='utf8mb4_unicode_ci'
        AND COALESCE(EXTRA,'')='' AND COALESCE(GENERATION_EXPRESSION,'')='')<>1
     OR (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_product_catalog_source_sequence' AND COLUMN_NAME='last_row_no'
        AND LOWER(COLUMN_TYPE)='bigint' AND IS_NULLABLE='NO' AND COLUMN_DEFAULT IS NULL
        AND COALESCE(EXTRA,'')='' AND COALESCE(GENERATION_EXPRESSION,'')='')<>1
     OR (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_product_catalog_source_sequence' AND COLUMN_NAME='update_time'
        AND LOWER(COLUMN_TYPE)='datetime' AND IS_NULLABLE='NO'
        AND LOWER(COALESCE(COLUMN_DEFAULT,'')) IN ('current_timestamp','current_timestamp()')
        AND LOWER(COALESCE(EXTRA,'')) IN ('on update current_timestamp','default_generated on update current_timestamp')
        AND COALESCE(GENERATION_EXPRESSION,'')='')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC12 sequence column definition drift';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_product_catalog_source_sequence')<>1
     OR (SELECT COUNT(*) FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_product_catalog_source_sequence'
        AND INDEX_NAME='PRIMARY' AND SEQ_IN_INDEX=1 AND COLUMN_NAME='data_source'
        AND NON_UNIQUE=0 AND SUB_PART IS NULL AND INDEX_TYPE='BTREE')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC12 sequence primary scope drift';
  END IF;
  IF EXISTS(SELECT 1 FROM dcc_product_catalog_source_sequence
            WHERE CHAR_LENGTH(data_source)=0 OR REGEXP_LIKE(data_source,'^[[:space:]]|[[:space:]]$')
              OR last_row_no<1 OR last_row_no>2147483647) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC12 illegal existing sequence facts';
  END IF;
  START TRANSACTION;
  INSERT INTO dcc_product_catalog_source_sequence(data_source,last_row_no)
    SELECT data_source,GREATEST(MAX(original_row_no),1)
      FROM dcc_product_catalog
     GROUP BY data_source
  ON DUPLICATE KEY UPDATE last_row_no=GREATEST(last_row_no,VALUES(last_row_no));
  INSERT INTO dcc_product_catalog_source_sequence(data_source,last_row_no)
    VALUES('瑛泰产品',1)
  ON DUPLICATE KEY UPDATE last_row_no=GREATEST(last_row_no,VALUES(last_row_no));
  COMMIT;
END//
DELIMITER ;
CALL initialize_dcc_product_catalog_source_sequence();
DROP PROCEDURE initialize_dcc_product_catalog_source_sequence;
