-- release-target-preflight: migrationId=20261022_qms_menu_package_integration; allowedEnvironments=test,prod,backup
-- Evaluated immediately before this migration, after its declared predecessors.
SELECT CASE WHEN
 ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND ENGINE='InnoDB' AND TABLE_NAME IN ('system_tenant','system_tenant_package','system_role','system_role_menu','system_menu'))=5)
 AND (NOT EXISTS(SELECT 1 FROM system_tenant_package WHERE deleted=0 AND status=0 AND (NOT JSON_VALID(menu_ids) OR JSON_TYPE(CASE WHEN JSON_VALID(menu_ids) THEN CAST(menu_ids AS JSON) ELSE JSON_OBJECT() END)<>'ARRAY')))
 AND (NOT EXISTS(SELECT 1 FROM system_role_menu rm JOIN system_role r ON r.id=rm.role_id WHERE r.code IN ('intern_user','doc_control','dcc_project_reviewer') AND r.deleted=0 AND rm.deleted=0 AND rm.tenant_id<>r.tenant_id))
 THEN 'TARGET_PREFLIGHT_PASS:20261022_qms_menu_package_integration' ELSE 'TARGET_PREFLIGHT_BLOCKED:20261022_qms_menu_package_integration:TARGET_PREREQUISITE_OR_CONFLICT' END;
