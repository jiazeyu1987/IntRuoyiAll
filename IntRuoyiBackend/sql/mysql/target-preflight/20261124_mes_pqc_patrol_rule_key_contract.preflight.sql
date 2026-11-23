-- release-target-preflight: migrationId=20261124_mes_pqc_patrol_rule_key_contract; allowedEnvironments=test,backup,prod
-- Same column, all-history and known-CHECK predicates as the immutable forward migration.
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='mes_pqc_inspection_task'
      AND COLUMN_NAME='inspection_rule_key' AND COLUMN_TYPE='varchar(32)' AND IS_NULLABLE='NO') = 1
  AND NOT EXISTS (SELECT 1 FROM mes_pqc_inspection_task
    WHERE inspection_rule_key IS NULL OR inspection_rule_key NOT IN ('FIRST','PATROL','PATROL_AM','PATROL_PM','FINAL'))
  AND (
    NOT EXISTS (SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
      WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME='mes_pqc_inspection_task'
        AND CONSTRAINT_NAME='chk_mes_pqc_inspection_rule_key')
    OR EXISTS (
      SELECT 1 FROM information_schema.TABLE_CONSTRAINTS tc JOIN information_schema.CHECK_CONSTRAINTS cc
        ON tc.CONSTRAINT_SCHEMA=cc.CONSTRAINT_SCHEMA AND tc.CONSTRAINT_NAME=cc.CONSTRAINT_NAME
      WHERE tc.CONSTRAINT_SCHEMA=DATABASE() AND tc.TABLE_NAME='mes_pqc_inspection_task'
        AND tc.CONSTRAINT_NAME='chk_mes_pqc_inspection_rule_key' AND tc.CONSTRAINT_TYPE='CHECK' AND tc.ENFORCED='YES'
        AND REGEXP_REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(LOWER(cc.CHECK_CLAUSE),'`',''),'_utf8mb4',''),'_utf8mb3',''),'_latin1',''),CONCAT(CHAR(92),CHAR(39)),CHAR(39)),'[[:space:]()]','') IN (
          'inspection_rule_keyin''first'',''patrol_am'',''patrol_pm'',''final''',
          'inspection_rule_keyin''first'',''patrol'',''patrol_am'',''patrol_pm'',''final''')
    )
  )
THEN 'TARGET_PREFLIGHT_PASS:20261124_mes_pqc_patrol_rule_key_contract'
ELSE 'TARGET_PREFLIGHT_BLOCKED:20261124_mes_pqc_patrol_rule_key_contract' END;
