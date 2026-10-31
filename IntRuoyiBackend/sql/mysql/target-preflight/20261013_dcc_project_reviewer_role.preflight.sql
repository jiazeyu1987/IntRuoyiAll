-- release-target-preflight: migrationId=20261013_dcc_project_reviewer_role; allowedEnvironments=test,prod,backup
-- Evaluated immediately before this migration, after its declared predecessors.
SELECT CASE WHEN
 ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND ENGINE='InnoDB' AND TABLE_NAME IN ('system_menu','system_role','system_role_menu','system_tenant'))=4)
 AND ((SELECT COUNT(*) FROM system_menu q JOIN system_menu root ON root.id=q.parent_id WHERE q.permission='dcc:project-code:query' AND q.type=2 AND q.component='dcc/controlled-file/basic-data/project-code/index' AND q.component_name='DccProjectCodeBasicDataPage' AND q.deleted=0 AND q.status=0 AND q.visible=1 AND root.type=1 AND root.deleted=0 AND root.status=0 AND root.visible=1)=1)
 AND ((SELECT COUNT(*) FROM system_menu c JOIN system_menu root ON root.id=c.parent_id WHERE c.type=2 AND c.component='dcc/controlled-file/basic-data/product-catalog/index' AND c.component_name='DccProductCatalogBasicDataPage' AND c.permission='' AND c.deleted=0 AND c.status=0 AND c.visible=1 AND root.deleted=0)=1)
 AND (NOT EXISTS(SELECT 1 FROM system_role WHERE code='dcc_project_reviewer' AND deleted=0 GROUP BY tenant_id HAVING COUNT(*)>1))
 AND (NOT EXISTS(SELECT 1 FROM system_role WHERE code='dcc_project_reviewer' AND deleted=0 AND (BINARY code<>BINARY 'dcc_project_reviewer' OR name<>'项目审核员' OR type<>2 OR data_scope<>5)))
 AND ((SELECT COUNT(*) FROM system_menu WHERE permission='dcc:project-code:review' AND deleted=0)<=1)
 AND (NOT EXISTS(SELECT 1 FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA=DATABASE() AND ROUTINE_NAME IN ('configure_dcc_project_reviewer_role')))
 THEN 'TARGET_PREFLIGHT_PASS:20261013_dcc_project_reviewer_role' ELSE 'TARGET_PREFLIGHT_BLOCKED:20261013_dcc_project_reviewer_role:TARGET_PREREQUISITE_OR_CONFLICT' END;
