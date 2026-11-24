-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260814_mes_c015_route_dcc_qa_reconciliation_schema,20261104_mes_pqc_dynamic_patrol_rounds,20261104_mes_pqc_patrol_cancellation; type=schema; riskLevel=high; requiresTargetPreflight=true
-- New rounds use PATROL. Frozen PATROL_AM/PATROL_PM identities remain valid and unchanged.
-- Stop application writers before applying. Never remove a CHECK in a separate DDL statement.
DROP PROCEDURE IF EXISTS migrate_pqc_patrol_rule_key_20261124;
DELIMITER $$
CREATE PROCEDURE migrate_pqc_patrol_rule_key_20261124()
BEGIN
  DECLARE check_count INT DEFAULT 0;
  DECLARE check_signature TEXT;
  DECLARE check_enforced VARCHAR(3);
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='mes_pqc_inspection_task'
        AND COLUMN_NAME='inspection_rule_key' AND COLUMN_TYPE='varchar(32)' AND IS_NULLABLE='NO') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='PQC_PATROL_RULE_COLUMN_CONTRACT_MISMATCH';
  END IF;
  IF EXISTS (SELECT 1 FROM mes_pqc_inspection_task
      WHERE inspection_rule_key IS NULL OR inspection_rule_key NOT IN ('FIRST','PATROL','PATROL_AM','PATROL_PM','FINAL')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='PQC_PATROL_RULE_HISTORY_REQUIRES_EXPLICIT_REMEDIATION';
  END IF;
  SELECT COUNT(*) INTO check_count FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME='mes_pqc_inspection_task'
      AND CONSTRAINT_NAME='chk_mes_pqc_inspection_rule_key';
  IF check_count = 0 THEN
    ALTER TABLE mes_pqc_inspection_task ADD CONSTRAINT chk_mes_pqc_inspection_rule_key
      CHECK (inspection_rule_key IN ('FIRST','PATROL','PATROL_AM','PATROL_PM','FINAL')) ENFORCED;
  ELSE
    SELECT REGEXP_REPLACE(REGEXP_REPLACE(CONVERT(LOWER(cc.CHECK_CLAUSE) USING utf8mb4),'`|_utf8mb4|_utf8mb3|_latin1|[[:space:]()]',''),CONVERT(CONCAT(CHAR(92),CHAR(92)) USING utf8mb4),''), tc.ENFORCED
      INTO check_signature, check_enforced
      FROM information_schema.TABLE_CONSTRAINTS tc JOIN information_schema.CHECK_CONSTRAINTS cc
        ON tc.CONSTRAINT_SCHEMA=cc.CONSTRAINT_SCHEMA AND tc.CONSTRAINT_NAME=cc.CONSTRAINT_NAME
      WHERE tc.CONSTRAINT_SCHEMA=DATABASE() AND tc.TABLE_NAME='mes_pqc_inspection_task'
        AND tc.CONSTRAINT_NAME='chk_mes_pqc_inspection_rule_key' AND tc.CONSTRAINT_TYPE='CHECK';
    IF check_count <> 1 OR check_signature IS NULL OR check_enforced <> 'YES'
        OR check_signature NOT IN (
          'inspection_rule_keyin''first'',''patrol_am'',''patrol_pm'',''final''',
          'inspection_rule_keyin''first'',''patrol'',''patrol_am'',''patrol_pm'',''final''') THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='PQC_PATROL_RULE_CHECK_CONTRACT_MISMATCH';
    END IF;
    IF check_signature = 'inspection_rule_keyin''first'',''patrol_am'',''patrol_pm'',''final''' THEN
      ALTER TABLE mes_pqc_inspection_task
        DROP CHECK chk_mes_pqc_inspection_rule_key,
        ADD CONSTRAINT chk_mes_pqc_inspection_rule_key
          CHECK (inspection_rule_key IN ('FIRST','PATROL','PATROL_AM','PATROL_PM','FINAL')) ENFORCED;
    END IF;
  END IF;
END$$
DELIMITER ;
CALL migrate_pqc_patrol_rule_key_20261124();
DROP PROCEDURE migrate_pqc_patrol_rule_key_20261124;
