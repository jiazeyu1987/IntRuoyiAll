-- release-target-preflight: migrationId=20261021_infra_test_mode_config; allowedEnvironments=test,prod,backup
-- Evaluated immediately before this migration, after its declared predecessors.
SELECT CASE WHEN
 ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND ENGINE='InnoDB' AND TABLE_NAME IN ('infra_config'))=1)
 AND ((SELECT COUNT(*) FROM infra_config WHERE config_key='system.ui.test-mode-enabled' AND deleted=0)<=1)
 AND (NOT EXISTS(SELECT 1 FROM infra_config WHERE config_key='system.ui.test-mode-enabled' AND deleted=0 AND (value IS NULL OR BINARY value NOT IN ('true','false') OR visible<>1)))
 AND (NOT EXISTS(SELECT 1 FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA=DATABASE() AND ROUTINE_NAME IN ('ensure_infra_test_mode_20261021')))
 THEN 'TARGET_PREFLIGHT_PASS:20261021_infra_test_mode_config' ELSE 'TARGET_PREFLIGHT_BLOCKED:20261021_infra_test_mode_config:TARGET_PREREQUISITE_OR_CONFLICT' END;
