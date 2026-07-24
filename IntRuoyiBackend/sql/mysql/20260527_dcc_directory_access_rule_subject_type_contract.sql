-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- Normalize DCC directory access-rule subject types to the string contract used by NAS ACL restore.

UPDATE `dcc_directory_access_rule`
SET `subject_type` = CASE `subject_type`
  WHEN '1' THEN 'USER'
  WHEN '2' THEN 'DEPT'
  WHEN '3' THEN 'ROLE'
  WHEN '4' THEN 'POSITION'
  ELSE `subject_type`
END
WHERE `subject_type` IN ('1', '2', '3', '4');
