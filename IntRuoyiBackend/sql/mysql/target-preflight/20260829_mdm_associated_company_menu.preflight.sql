-- release-target-preflight: migrationId=20260829_mdm_associated_company_menu; allowedEnvironments=test,backup,prod
-- Read-only data prerequisites; evaluated immediately before this migration.
WITH defs(id,permission,type,parent_id,path,component,route) AS(
 SELECT 990200,'' COLLATE utf8mb4_unicode_ci,1,0,'/mdm' COLLATE utf8mb4_unicode_ci,NULL,NULL
 UNION ALL SELECT 990249,'mdm:enterprise:query',2,990200,'enterprise','mdm/enterprise/index','MdmEnterprise'
 UNION ALL SELECT 990250,'mdm:enterprise:create',3,990249,'','',''
 UNION ALL SELECT 990251,'mdm:enterprise:update',3,990249,'','',''
 UNION ALL SELECT 990252,'mdm:enterprise:delete',3,990249,'','','')
SELECT CASE WHEN
  (NOT EXISTS(SELECT 1 FROM system_tenant_package p WHERE p.deleted=0 AND
 CASE WHEN p.menu_ids IS NULL OR NOT JSON_VALID(p.menu_ids) THEN TRUE
 WHEN JSON_TYPE(CAST(p.menu_ids AS JSON))<>'ARRAY' THEN TRUE
 ELSE EXISTS(SELECT 1 FROM JSON_TABLE(p.menu_ids,'$[*]' COLUMNS(n FOR ORDINALITY)) j
 WHERE JSON_TYPE(JSON_EXTRACT(p.menu_ids,CONCAT('$[',j.n-1,']')))<>'INTEGER'
 OR CAST(JSON_UNQUOTE(JSON_EXTRACT(p.menu_ids,CONCAT('$[',j.n-1,']'))) AS DECIMAL(65,0)) NOT BETWEEN 0 AND 9223372036854775807) END))
  AND (NOT EXISTS(SELECT 1 FROM defs d JOIN system_menu m ON m.id=d.id WHERE NOT COALESCE(m.deleted=0 AND m.status=0 AND m.type=d.type AND m.parent_id=d.parent_id AND BINARY m.path=BINARY d.path AND COALESCE(m.permission,'')=d.permission AND (d.type<>2 OR (BINARY m.component=BINARY d.component AND BINARY m.component_name=BINARY d.route)),FALSE)))
  AND (NOT EXISTS(SELECT 1 FROM defs d JOIN system_menu m ON m.deleted=0 AND m.id<>d.id AND ((d.permission<>'' AND m.permission=d.permission) OR (d.type IN(1,2) AND m.parent_id=d.parent_id AND BINARY m.path=BINARY d.path))))
  AND (EXISTS(SELECT 1 FROM system_users u JOIN system_user_role ur ON ur.user_id=u.id AND ur.tenant_id=u.tenant_id AND ur.deleted=0 JOIN system_role r ON r.id=ur.role_id AND r.tenant_id=ur.tenant_id AND r.deleted=0 AND r.code='super_admin' WHERE u.tenant_id=1 AND u.username='admin' AND u.status=0 AND u.deleted=0))
THEN 'TARGET_PREFLIGHT_PASS:20260829_mdm_associated_company_menu' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260829_mdm_associated_company_menu' END;
