-- release-target-preflight: migrationId=20261126_dcc_project_access_reason_check_fix; allowedEnvironments=test,backup,prod
SELECT CASE
  WHEN NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_project_access_rule'
      AND COLUMN_NAME='change_reason' AND DATA_TYPE='varchar' AND CHARACTER_MAXIMUM_LENGTH=512
  ) THEN 'TARGET_PREFLIGHT_BLOCKED:20261126_dcc_project_access_reason_check_fix:REASON_SCHEMA'
  WHEN EXISTS (
    SELECT 1 FROM information_schema.TABLE_CONSTRAINTS t
    JOIN information_schema.CHECK_CONSTRAINTS c
      ON c.CONSTRAINT_SCHEMA=t.CONSTRAINT_SCHEMA AND c.CONSTRAINT_NAME=t.CONSTRAINT_NAME
    WHERE t.CONSTRAINT_SCHEMA=DATABASE() AND t.TABLE_NAME='dcc_project_access_rule'
      AND t.CONSTRAINT_NAME='ck_dcc_project_access_reason' AND t.CONSTRAINT_TYPE='CHECK'
      AND REGEXP_REPLACE(LOWER(c.CHECK_CLAUSE),'[` ()]','')<>'char_lengthtrimchange_reason>0'
  ) THEN 'TARGET_PREFLIGHT_BLOCKED:20261126_dcc_project_access_reason_check_fix:UNKNOWN_CHECK'
  ELSE 'TARGET_PREFLIGHT_PASS:20261126_dcc_project_access_reason_check_fix'
END;
