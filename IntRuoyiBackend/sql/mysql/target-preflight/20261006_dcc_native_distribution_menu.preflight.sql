-- release-target-preflight: migrationId=20261006_dcc_native_distribution_menu; allowedEnvironments=test,prod,backup
-- Evaluated immediately before this migration, after its declared predecessors.
SELECT CASE WHEN
 ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND ENGINE='InnoDB' AND TABLE_NAME IN ('system_menu','system_role','system_role_menu'))=3)
 AND ((SELECT COUNT(*) FROM system_menu WHERE id=6800 AND type=1 AND status=0 AND deleted=0)=1)
 AND ((SELECT COUNT(*) FROM system_role WHERE tenant_id=1 AND code='doc_control' AND status=0 AND deleted=0)=1)
 AND ((SELECT COUNT(*) FROM system_role WHERE tenant_id=1 AND BINARY code=BINARY 'doc_control' AND status=0 AND deleted=0)=1)
 AND ((SELECT COUNT(*) FROM system_role_menu WHERE tenant_id=1 AND role_id=(SELECT id FROM system_role WHERE tenant_id=1 AND BINARY code=BINARY 'doc_control' AND deleted=0 AND status=0) AND menu_id=6800 AND deleted=0)=1)
 AND ((SELECT COUNT(*) FROM system_menu WHERE permission='dcc:controlled-file:distribute')<=1)
 AND (NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='dcc:controlled-file:distribute' AND (BINARY permission<>BINARY 'dcc:controlled-file:distribute' OR type<>3 OR parent_id<>6800 OR status<>0 OR deleted<>0)))
 AND (NOT EXISTS(SELECT 1 FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA=DATABASE() AND ROUTINE_NAME IN ('g70_dcc_native_distribute')))
 THEN 'TARGET_PREFLIGHT_PASS:20261006_dcc_native_distribution_menu' ELSE 'TARGET_PREFLIGHT_BLOCKED:20261006_dcc_native_distribution_menu:TARGET_PREREQUISITE_OR_CONFLICT' END;
