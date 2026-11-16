-- release-target-preflight: migrationId=20261105_dcc_library_role_permissions; allowedEnvironments=test,backup,prod
-- Validate menu/role namespaces now; grant scope uses library_scope introduced by the pending prerequisite and is checked at apply time.
WITH dcc_library_role_nodes AS (
SELECT 605072141 id,'QMS' scope,'VIEW' level,'QMS文件夹浏览' name,'dcc_lib_qms_view_legacy' code,'dcc:library:qms:view' permission
UNION ALL SELECT 605072142,'QMS','EDIT','QMS文件夹编辑','dcc_lib_qms_edit_legacy','dcc:library:qms:edit'
UNION ALL SELECT 605072143,'GENERAL','VIEW','通用文件夹浏览','dcc_lib_general_view_legacy','dcc:library:general:view'
UNION ALL SELECT 605072144,'GENERAL','EDIT','通用文件夹编辑','dcc_lib_general_edit_legacy','dcc:library:general:edit'
)
SELECT CASE
 WHEN (NOT EXISTS(SELECT 1 FROM system_menu WHERE id=6807 AND permission='dcc:controlled-file:query' AND deleted=0 AND status=0)) THEN 'TARGET_PREFLIGHT_BLOCKED:20261105_dcc_library_role_permissions:CONDITION_1'
 WHEN (EXISTS(SELECT 1 FROM system_menu m JOIN dcc_library_role_nodes n ON m.id=n.id OR m.permission=n.permission
           WHERE m.id<>n.id OR m.permission<>n.permission OR m.deleted<>0 OR m.status<>0 OR m.parent_id<>6807 OR m.type<>3)) THEN 'TARGET_PREFLIGHT_BLOCKED:20261105_dcc_library_role_permissions:CONDITION_2'
 WHEN (EXISTS(SELECT 1 FROM system_tenant_package WHERE deleted=0 AND NOT JSON_VALID(menu_ids))) THEN 'TARGET_PREFLIGHT_BLOCKED:20261105_dcc_library_role_permissions:CONDITION_3'
 WHEN (EXISTS(SELECT 1 FROM system_role r JOIN dcc_library_role_nodes n ON n.code=r.code
           WHERE r.deleted<>0 OR r.status<>0 OR COALESCE(r.remark,'')<>'DCC library role migration 20261105')) THEN 'TARGET_PREFLIGHT_BLOCKED:20261105_dcc_library_role_permissions:CONDITION_4'
 WHEN (EXISTS(SELECT 1 FROM system_role_category WHERE code='dcc_library_legacy' AND (deleted<>0 OR status<>0 OR COALESCE(remark,'')<>'DCC library role migration 20261105'))) THEN 'TARGET_PREFLIGHT_BLOCKED:20261105_dcc_library_role_permissions:CONDITION_5'
 ELSE 'TARGET_PREFLIGHT_PASS:20261105_dcc_library_role_permissions' END;
