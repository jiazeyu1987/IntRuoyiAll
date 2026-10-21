-- release-target-preflight: migrationId=20260725_mes_edhr_recordbook_global_setting; allowedEnvironments=test,backup,prod
-- Existing runtime switch is preserved only when unique and a valid explicit boolean; NULL is not an enabled default.
SELECT CASE WHEN

  (SELECT COUNT(*) FROM infra_config WHERE config_key='mes.edhr.recordbook.global.enabled' AND deleted=b'0')<=1
  AND NOT EXISTS (SELECT 1 FROM infra_config WHERE config_key='mes.edhr.recordbook.global.enabled' AND deleted=b'0'
    AND (value IS NULL OR BINARY value NOT IN ('true','false')))
THEN 'TARGET_PREFLIGHT_PASS:20260725_mes_edhr_recordbook_global_setting' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260725_mes_edhr_recordbook_global_setting' END;
