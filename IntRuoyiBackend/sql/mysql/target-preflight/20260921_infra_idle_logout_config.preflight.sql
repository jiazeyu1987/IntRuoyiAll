-- release-target-preflight: migrationId=20260921_infra_idle_logout_config; allowedEnvironments=test,backup,prod
-- Preserve a valid administrator value; reject ambiguous or invalid existing configuration.
SELECT CASE WHEN
  (SELECT COUNT(*) FROM infra_config WHERE config_key='system.login.idle-timeout-minutes' AND deleted=b'0') <= 1
  AND NOT EXISTS (
    SELECT 1 FROM infra_config WHERE config_key='system.login.idle-timeout-minutes' AND deleted=b'0'
      AND (value IS NULL OR value NOT REGEXP '^[1-9][0-9]{0,3}$'
        OR CAST(value AS UNSIGNED) < 1 OR CAST(value AS UNSIGNED) > 1440)
  ) THEN 'TARGET_PREFLIGHT_PASS:20260921_infra_idle_logout_config'
  ELSE 'TARGET_PREFLIGHT_BLOCKED:idle-timeout-config-invalid-or-duplicated' END;
