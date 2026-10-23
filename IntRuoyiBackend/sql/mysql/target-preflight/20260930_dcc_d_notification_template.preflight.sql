-- release-target-preflight: migrationId=20260930_dcc_d_notification_template; allowedEnvironments=test,prod,backup
-- Evaluated immediately before this migration, after its declared predecessors.
SELECT CASE WHEN
 ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND ENGINE='InnoDB' AND TABLE_NAME IN ('system_notify_template'))=1)
 AND ((SELECT COUNT(*) FROM system_notify_template WHERE code='dcc_relation_remediation')<=1)
 AND (NOT EXISTS(SELECT 1 FROM system_notify_template WHERE code='dcc_relation_remediation' AND (deleted<>0 OR status<>0 OR type<>2 OR BINARY code<>BINARY 'dcc_relation_remediation')))
 AND (NOT EXISTS(SELECT 1 FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA=DATABASE() AND ROUTINE_NAME IN ('ensure_dcc_d_remediation_template')))
 THEN 'TARGET_PREFLIGHT_PASS:20260930_dcc_d_notification_template' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260930_dcc_d_notification_template:TARGET_PREREQUISITE_OR_CONFLICT' END;
