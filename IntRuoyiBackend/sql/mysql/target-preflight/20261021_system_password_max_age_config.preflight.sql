-- release-target-preflight: migrationId=20261021_system_password_max_age_config; allowedEnvironments=test,prod,backup
-- Evaluated immediately before this migration, after its declared predecessors.
SELECT CASE WHEN
 ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND ENGINE='InnoDB' AND TABLE_NAME IN ('infra_config'))=1)
 AND ((SELECT COUNT(*) FROM infra_config WHERE config_key='system.user.password-max-age-days' AND deleted=0)<=1)
 AND (NOT EXISTS(SELECT 1 FROM infra_config WHERE config_key='system.user.password-max-age-days' AND deleted=0 AND (value IS NULL OR value NOT REGEXP '^[1-9][0-9]{0,9}$' OR value REGEXP '[^0-9]' OR CAST(value AS DECIMAL(20,0))>2147483647)))
 AND (NOT EXISTS(SELECT 1 FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA=DATABASE() AND ROUTINE_NAME IN ('ensure_password_max_age_20261021')))
 THEN 'TARGET_PREFLIGHT_PASS:20261021_system_password_max_age_config' ELSE 'TARGET_PREFLIGHT_BLOCKED:20261021_system_password_max_age_config:TARGET_PREREQUISITE_OR_CONFLICT' END;
