-- release-target-preflight: migrationId=20261005_dcc_project_product_application_notify; allowedEnvironments=test,prod,backup
-- Evaluated immediately before this migration, after its declared predecessors.
SELECT CASE WHEN
 ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND ENGINE='InnoDB' AND TABLE_NAME IN ('system_notify_template'))=1)
 AND ((SELECT COUNT(*) FROM system_notify_template WHERE code='dcc-project-product-application-event')<=1)
 AND (NOT EXISTS(SELECT 1 FROM system_notify_template WHERE code='dcc-project-product-application-event' AND (deleted<>0 OR status<>0 OR type<>2 OR BINARY code<>BINARY 'dcc-project-product-application-event')))
 AND (NOT EXISTS(SELECT 1 FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA=DATABASE() AND ROUTINE_NAME IN ('g49_dcc_project_notify')))
 THEN 'TARGET_PREFLIGHT_PASS:20261005_dcc_project_product_application_notify' ELSE 'TARGET_PREFLIGHT_BLOCKED:20261005_dcc_project_product_application_notify:TARGET_PREREQUISITE_OR_CONFLICT' END;
