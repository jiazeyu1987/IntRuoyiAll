-- Current application contract; checked after migrations regardless of ledger actions.
-- Require varchar(32) NOT NULL and the enforced CURRENT five-value CHECK; never accept pre-upgrade structure.
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='mes_pqc_inspection_task'
      AND COLUMN_NAME='inspection_rule_key' AND COLUMN_TYPE='varchar(32)' AND IS_NULLABLE='NO') = 1
  AND NOT EXISTS (SELECT 1 FROM mes_pqc_inspection_task
    WHERE inspection_rule_key IS NULL OR inspection_rule_key NOT IN ('FIRST','PATROL','PATROL_AM','PATROL_PM','FINAL'))
  -- MySQL serializes column references with backticks. Reject unknown direct same-column CHECKs.
  AND NOT EXISTS (
    SELECT 1 FROM information_schema.TABLE_CONSTRAINTS extra_tc
    JOIN information_schema.CHECK_CONSTRAINTS extra_cc
      ON extra_tc.CONSTRAINT_SCHEMA=extra_cc.CONSTRAINT_SCHEMA AND extra_tc.CONSTRAINT_NAME=extra_cc.CONSTRAINT_NAME
    WHERE extra_tc.CONSTRAINT_SCHEMA=DATABASE() AND extra_tc.TABLE_NAME='mes_pqc_inspection_task'
      AND extra_tc.CONSTRAINT_TYPE='CHECK' AND extra_tc.CONSTRAINT_NAME<>'chk_mes_pqc_inspection_rule_key'
      AND LOCATE('`inspection_rule_key`',LOWER(extra_cc.CHECK_CLAUSE))>0
  )
  AND (
    EXISTS (
      SELECT 1 FROM information_schema.TABLE_CONSTRAINTS tc JOIN information_schema.CHECK_CONSTRAINTS cc
        ON tc.CONSTRAINT_SCHEMA=cc.CONSTRAINT_SCHEMA AND tc.CONSTRAINT_NAME=cc.CONSTRAINT_NAME
      WHERE tc.CONSTRAINT_SCHEMA=DATABASE() AND tc.TABLE_NAME='mes_pqc_inspection_task'
        AND tc.CONSTRAINT_NAME='chk_mes_pqc_inspection_rule_key' AND tc.CONSTRAINT_TYPE='CHECK' AND tc.ENFORCED='YES'
        AND REGEXP_REPLACE(CONVERT(cc.CHECK_CLAUSE USING utf8mb4),
          CONVERT(CONCAT(CHAR(92),CHAR(92),CHAR(39)) USING utf8mb4),
          CONVERT(CHAR(39) USING utf8mb4)) COLLATE utf8mb4_bin IN (
          '(`inspection_rule_key` in (_utf8mb4''FIRST'',_utf8mb4''PATROL'',_utf8mb4''PATROL_AM'',_utf8mb4''PATROL_PM'',_utf8mb4''FINAL''))',
          '(`inspection_rule_key` in (_utf8mb3''FIRST'',_utf8mb3''PATROL'',_utf8mb3''PATROL_AM'',_utf8mb3''PATROL_PM'',_utf8mb3''FINAL''))',
          '(`inspection_rule_key` in (_latin1''FIRST'',_latin1''PATROL'',_latin1''PATROL_AM'',_latin1''PATROL_PM'',_latin1''FINAL''))',
          '(`inspection_rule_key` in (''FIRST'',''PATROL'',''PATROL_AM'',''PATROL_PM'',''FINAL''))')
    )
  )
THEN 'DATABASE_CONTRACT_PASS:mes-pqc-rule-key'
ELSE 'DATABASE_CONTRACT_FAIL:mes-pqc-rule-key' END;
