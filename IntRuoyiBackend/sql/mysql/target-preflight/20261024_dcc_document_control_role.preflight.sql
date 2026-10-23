-- release-target-preflight: migrationId=20261024_dcc_document_control_role; allowedEnvironments=test,prod,backup
SELECT CASE WHEN
 (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND ENGINE='InnoDB' AND TABLE_NAME IN ('system_tenant','system_role','system_menu','system_role_menu'))=4
 AND (SELECT COUNT(*) FROM system_tenant WHERE id=1 AND deleted=0 AND status=0)=1
 AND (SELECT COUNT(*) FROM system_menu WHERE id IN (6800,6807) AND deleted=0 AND status=0)=2
 AND (SELECT COUNT(*) FROM system_menu WHERE id=6807 AND permission='dcc:controlled-file:query' AND parent_id=6800 AND type=2)=1
 AND (SELECT COUNT(*) FROM system_role WHERE tenant_id=1 AND code='doc_control')<=1
 AND NOT EXISTS(SELECT 1 FROM system_role WHERE tenant_id=1 AND code='doc_control' AND (BINARY code<>BINARY 'doc_control' OR deleted<>0 OR status<>0 OR type<>2))
 AND NOT EXISTS(SELECT 1 FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA=DATABASE() AND ROUTINE_NAME='release_dcc_document_control_role')
 THEN 'TARGET_PREFLIGHT_PASS:20261024_dcc_document_control_role' ELSE 'TARGET_PREFLIGHT_BLOCKED:20261024_dcc_document_control_role:TARGET_PREREQUISITE_OR_CONFLICT' END;
